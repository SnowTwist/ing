package pl.scalo.ing.outbox.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OutboxMessageTest {
    private static final Instant NOW = Instant.parse("2026-09-06T10:15:30Z");
    private static final RetryPolicy POLICY =
            new RetryPolicy(3, new ExponentialBackoff(Duration.ofSeconds(1), Duration.ofMinutes(5)));

    private final OutboxMessage message =
            OutboxMessage.pending(
                    UUID.randomUUID(), "Order", "order-1", "OrderCreated", "{}", NOW);

    @Test
    void newMessagesAreDueImmediately() {
        assertThat(message.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(message.attempts()).isZero();
        assertThat(message.nextAttemptAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("a failed attempt with retries left schedules the next one after the backoff")
    void failureSchedulesRetry() {
        DispatchOutcome outcome =
                message.dispatchFailed(NOW, POLICY, new IllegalStateException("broker down"));

        assertThat(outcome)
                .isInstanceOfSatisfying(
                        DispatchOutcome.Retry.class,
                        retry -> {
                            assertThat(retry.attempts()).isEqualTo(1);
                            assertThat(retry.nextAttemptAt()).isEqualTo(NOW.plusSeconds(1));
                            assertThat(retry.error()).contains("IllegalStateException", "broker down");
                        });
    }

    @Test
    @DisplayName("the last allowed attempt parks the message instead of retrying forever")
    void exhaustedRetriesDeadLetter() {
        OutboxMessage lastAttempt =
                new OutboxMessage(
                        message.id(),
                        "Order",
                        "order-1",
                        "OrderCreated",
                        "{}",
                        OutboxStatus.PENDING,
                        2,
                        NOW,
                        NOW,
                        null,
                        "previous failure");

        DispatchOutcome outcome =
                lastAttempt.dispatchFailed(NOW, POLICY, new IllegalStateException("still down"));

        assertThat(outcome)
                .isInstanceOfSatisfying(
                        DispatchOutcome.Dead.class,
                        dead -> assertThat(dead.attempts()).isEqualTo(3));
    }

    @Test
    void longErrorMessagesAreTruncated() {
        DispatchOutcome outcome =
                message.dispatchFailed(
                        NOW, POLICY, new IllegalStateException("x".repeat(5000)));

        assertThat(((DispatchOutcome.Retry) outcome).error()).hasSize(1000);
    }
}
