package pl.scalo.ing.outbox.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pl.scalo.ing.outbox.application.port.out.OutboxMessageStore;
import pl.scalo.ing.outbox.domain.DispatchOutcome;
import pl.scalo.ing.outbox.domain.OutboxMessage;
import pl.scalo.ing.outbox.domain.OutboxStatus;

@Repository
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class JdbcOutboxMessageStore implements OutboxMessageStore {
    private static final String INSERT =
            """
            INSERT INTO outbox_events (
                id, aggregate_type, aggregate_id, event_type, payload,
                status, attempts, next_attempt_at, created_at)
            VALUES (
                :id, :aggregateType, :aggregateId, :eventType, CAST(:payload AS jsonb),
                :status, :attempts, :nextAttemptAt, :createdAt)
            """;

    private static final String CLAIM_DUE =
            """
            SELECT id, aggregate_type, aggregate_id, event_type, payload, status,
                   attempts, next_attempt_at, created_at, published_at, last_error
            FROM outbox_events
            WHERE status = 'PENDING' AND next_attempt_at <= :now
            ORDER BY created_at, id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """;

    private static final String MARK_PUBLISHED =
            """
            UPDATE outbox_events
            SET status = 'PUBLISHED', published_at = :publishedAt, attempts = attempts + 1,
                last_error = NULL
            WHERE id = :id
            """;

    private static final String RESCHEDULE =
            """
            UPDATE outbox_events
            SET attempts = :attempts, next_attempt_at = :nextAttemptAt, last_error = :error
            WHERE id = :id
            """;

    private static final String MARK_FAILED =
            """
            UPDATE outbox_events
            SET status = 'FAILED', attempts = :attempts, last_error = :error
            WHERE id = :id
            """;

    private static final String COUNT_PENDING =
            "SELECT count(*) FROM outbox_events WHERE status = 'PENDING'";

    private static final RowMapper<OutboxMessage> MAPPER = JdbcOutboxMessageStore::mapRow;

    private final JdbcClient jdbc;


    @Override
    public void append(OutboxMessage message) {
        jdbc.sql(INSERT)
                .param("id", message.id())
                .param("aggregateType", message.aggregateType())
                .param("aggregateId", message.aggregateId())
                .param("eventType", message.eventType())
                .param("payload", message.payload())
                .param("status", message.status().name())
                .param("attempts", message.attempts())
                .param("nextAttemptAt", toTimestamp(message.nextAttemptAt()))
                .param("createdAt", toTimestamp(message.createdAt()))
                .update();
    }

    @Override
    public List<OutboxMessage> claimDueMessages(int batchSize, Instant now) {
        return jdbc.sql(CLAIM_DUE)
                .param("now", toTimestamp(now))
                .param("batchSize", batchSize)
                .query(MAPPER)
                .list();
    }

    @Override
    public void apply(DispatchOutcome outcome) {
        switch (outcome) {
            case DispatchOutcome.Published published ->
                    jdbc.sql(MARK_PUBLISHED)
                            .param("id", published.messageId())
                            .param("publishedAt", toTimestamp(published.publishedAt()))
                            .update();
            case DispatchOutcome.Retry retry ->
                    jdbc.sql(RESCHEDULE)
                            .param("id", retry.messageId())
                            .param("attempts", retry.attempts())
                            .param("nextAttemptAt", toTimestamp(retry.nextAttemptAt()))
                            .param("error", retry.error())
                            .update();
            case DispatchOutcome.Dead dead ->
                    jdbc.sql(MARK_FAILED)
                            .param("id", dead.messageId())
                            .param("attempts", dead.attempts())
                            .param("error", dead.error())
                            .update();
        }
    }

    @Override
    public long countPending() {
        return jdbc.sql(COUNT_PENDING).query(Long.class).single();
    }


    private static OutboxMessage mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new OutboxMessage(
                rs.getObject("id", UUID.class),
                rs.getString("aggregate_type"),
                rs.getString("aggregate_id"),
                rs.getString("event_type"),
                rs.getString("payload"),
                OutboxStatus.valueOf(rs.getString("status")),
                rs.getInt("attempts"),
                toInstant(rs, "next_attempt_at"),
                toInstant(rs, "created_at"),
                toInstant(rs, "published_at"),
                rs.getString("last_error"));
    }

    private static OffsetDateTime toTimestamp(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private static Instant toInstant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
