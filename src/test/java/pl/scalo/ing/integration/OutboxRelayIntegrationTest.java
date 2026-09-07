package pl.scalo.ing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import pl.scalo.ing.integration.support.ControllableMessageBroker;
import pl.scalo.ing.integration.support.MutableClock;
import pl.scalo.ing.integration.support.MutableClockConfiguration;
import pl.scalo.ing.integration.support.TestBrokerConfiguration;
import pl.scalo.ing.order.application.port.in.PlaceOrderCommand;
import pl.scalo.ing.order.application.port.in.PlaceOrderUseCase;
import pl.scalo.ing.outbox.application.port.in.RelayOutboxMessages;

@SpringBootTest(
        properties = {
            "outbox.scheduler-enabled=false",
            "outbox.max-attempts=3",
            "outbox.initial-backoff=10s",
            "outbox.max-backoff=1m"
        })
@Import({TestBrokerConfiguration.class, MutableClockConfiguration.class})
class OutboxRelayIntegrationTest extends AbstractIntegrationTest {
    @Autowired private PlaceOrderUseCase placeOrder;
    @Autowired private RelayOutboxMessages relay;
    @Autowired private ControllableMessageBroker broker;
    @Autowired private MutableClock clock;

    @BeforeEach
    void resetBroker() {
        broker.reset();
    }

    @Test
    @DisplayName("a pending event is published once and marked PUBLISHED")
    void publishesPendingEvent() {
        placeOrder.placeOrder(new PlaceOrderCommand("123"));

        assertThat(relay.relayBatch()).isEqualTo(1);

        assertThat(broker.delivered()).singleElement().satisfies(message -> {
            assertThat(message.eventType()).isEqualTo("OrderCreated");
            assertThat(message.payload()).contains("\"customerId\": \"123\"");
        });

        Map<String, Object> row = outboxRow();
        assertThat(row.get("status")).isEqualTo("PUBLISHED");
        assertThat(row.get("attempts")).isEqualTo(1);
        assertThat(row.get("published_at")).isNotNull();

        assertThat(relay.relayBatch()).isZero();
        assertThat(broker.delivered()).hasSize(1);
    }

    @Test
    @DisplayName("a broker outage backs the event off instead of losing or duplicating it")
    void reschedulesWithBackoffOnFailure() {
        placeOrder.placeOrder(new PlaceOrderCommand("123"));
        broker.failWith(new IllegalStateException("broker down"));

        relay.relayBatch();

        Map<String, Object> row = outboxRow();
        assertThat(row.get("status")).isEqualTo("PENDING");
        assertThat(row.get("attempts")).isEqualTo(1);
        assertThat((String) row.get("last_error")).contains("broker down");
        assertThat(nextAttemptAt()).isEqualTo(clock.instant().plus(Duration.ofSeconds(10)));

        assertThat(relay.relayBatch()).isZero();

        clock.advanceBy(Duration.ofSeconds(10));
        broker.recover();
        assertThat(relay.relayBatch()).isEqualTo(1);

        assertThat(broker.delivered()).hasSize(1);
        assertThat(outboxRow().get("status")).isEqualTo("PUBLISHED");
    }

    @Test
    @DisplayName("after the last attempt the event is parked as FAILED and stops being claimed")
    void parksEventAfterExhaustingRetries() {
        placeOrder.placeOrder(new PlaceOrderCommand("123"));
        broker.failWith(new IllegalStateException("broker down"));

        for (int attempt = 1; attempt <= 3; attempt++) {
            assertThat(relay.relayBatch()).as("attempt %d is claimed", attempt).isEqualTo(1);
            clock.advanceBy(Duration.ofMinutes(1));
        }

        Map<String, Object> row = outboxRow();
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(row.get("attempts")).isEqualTo(3);
        assertThat((String) row.get("last_error")).contains("broker down");

        broker.recover();
        assertThat(relay.relayBatch()).as("parked events are not retried").isZero();
        assertThat(countOutboxEventsWithStatus("FAILED")).isEqualTo(1);
    }

    private Map<String, Object> outboxRow() {
        return jdbc.sql(
                        """
                        SELECT status, attempts, published_at, last_error, next_attempt_at
                        FROM outbox_events
                        """)
                .query()
                .singleRow();
    }

    private Instant nextAttemptAt() {
        return jdbc.sql("SELECT next_attempt_at FROM outbox_events")
                .query(java.time.OffsetDateTime.class)
                .single()
                .toInstant();
    }
}
