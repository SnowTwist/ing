package pl.scalo.ing.outbox.domain;

import java.time.Instant;
import java.util.UUID;

public record OutboxMessage(
        UUID id,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload,
        OutboxStatus status,
        int attempts,
        Instant nextAttemptAt,
        Instant createdAt,
        Instant publishedAt,
        String lastError) {
    private static final int MAX_ERROR_LENGTH = 1000;

    public static OutboxMessage pending(
            UUID id,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload,
            Instant now) {
        return new OutboxMessage(
                id,
                aggregateType,
                aggregateId,
                eventType,
                payload,
                OutboxStatus.PENDING,
                0,
                now,
                now,
                null,
                null);
    }

    public DispatchOutcome dispatched(Instant now) {
        return new DispatchOutcome.Published(id, now);
    }

    public DispatchOutcome dispatchFailed(Instant now, RetryPolicy policy, Throwable cause) {
        int nextAttempts = attempts + 1;
        String error = describe(cause);
        if (policy.isExhausted(nextAttempts)) {
            return new DispatchOutcome.Dead(id, nextAttempts, error);
        }
        return new DispatchOutcome.Retry(
                id, nextAttempts, now.plus(policy.backoffFor(nextAttempts)), error);
    }

    private static String describe(Throwable cause) {
        String message = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        return message.length() <= MAX_ERROR_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_LENGTH);
    }
}
