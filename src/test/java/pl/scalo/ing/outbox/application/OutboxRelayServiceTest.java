package pl.scalo.ing.outbox.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.scalo.ing.outbox.application.port.out.MessageBroker;
import pl.scalo.ing.outbox.application.port.out.OutboxMessageStore;
import pl.scalo.ing.outbox.application.port.out.OutboxTelemetry;
import pl.scalo.ing.outbox.config.OutboxProperties;
import pl.scalo.ing.outbox.domain.DispatchOutcome;
import pl.scalo.ing.outbox.domain.ExponentialBackoff;
import pl.scalo.ing.outbox.domain.OutboxMessage;
import pl.scalo.ing.outbox.domain.OutboxStatus;
import pl.scalo.ing.outbox.domain.RetryPolicy;

@ExtendWith(MockitoExtension.class)
class OutboxRelayServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-06T10:15:30Z");

    @Mock private OutboxMessageStore store;
    @Mock private MessageBroker broker;
    @Mock private OutboxTelemetry telemetry;

    private final OutboxProperties properties =
            new OutboxProperties(
                    100,
                    Duration.ofSeconds(2),
                    10,
                    2,
                    OutboxProperties.BackoffType.EXPONENTIAL,
                    Duration.ofSeconds(1),
                    Duration.ofMinutes(5),
                    true);

    private final RetryPolicy retryPolicy =
            new RetryPolicy(2, new ExponentialBackoff(Duration.ofSeconds(1), Duration.ofMinutes(5)));

    private OutboxRelayService service() {
        return new OutboxRelayService(
                store, broker, telemetry, retryPolicy, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private OutboxMessage pending(int attempts) {
        return new OutboxMessage(
                UUID.randomUUID(),
                "Order",
                "order-1",
                "OrderCreated",
                "{\"orderId\":\"order-1\"}",
                OutboxStatus.PENDING,
                attempts,
                NOW,
                NOW,
                null,
                null);
    }

    @Test
    @DisplayName("a delivered message is marked published and counted")
    void publishesAndMarks() {
        OutboxMessage message = pending(0);
        when(store.claimDueMessages(anyInt(), any())).thenReturn(List.of(message));

        assertThat(service().relayBatch()).isEqualTo(1);

        verify(broker).publish(message);
        verify(store).apply(new DispatchOutcome.Published(message.id(), NOW));
        verify(telemetry).published("OrderCreated");
    }

    @Test
    @DisplayName("a broker failure schedules a retry instead of losing the message")
    void reschedulesOnFailure() {
        OutboxMessage message = pending(0);
        when(store.claimDueMessages(anyInt(), any())).thenReturn(List.of(message));
        doThrow(new IllegalStateException("broker down")).when(broker).publish(message);

        service().relayBatch();

        ArgumentCaptor<DispatchOutcome> captor = ArgumentCaptor.forClass(DispatchOutcome.class);
        verify(store).apply(captor.capture());
        assertThat(captor.getValue())
                .isInstanceOfSatisfying(
                        DispatchOutcome.Retry.class,
                        retry -> {
                            assertThat(retry.attempts()).isEqualTo(1);
                            assertThat(retry.nextAttemptAt()).isEqualTo(NOW.plusSeconds(1));
                        });
        verify(telemetry).retryScheduled("OrderCreated");
        verify(telemetry, never()).published(any());
    }

    @Test
    @DisplayName("the message is parked once the retry policy is exhausted")
    void deadLettersWhenExhausted() {
        OutboxMessage message = pending(1);
        when(store.claimDueMessages(anyInt(), any())).thenReturn(List.of(message));
        doThrow(new IllegalStateException("broker down")).when(broker).publish(message);

        service().relayBatch();

        verify(store).apply(new DispatchOutcome.Dead(message.id(), 2,
                "IllegalStateException: broker down"));
        verify(telemetry).deadLettered("OrderCreated");
    }

    @Test
    @DisplayName("one poisoned message does not stop the rest of the batch")
    void continuesAfterFailure() {
        OutboxMessage failing = pending(0);
        OutboxMessage healthy = pending(0);
        when(store.claimDueMessages(anyInt(), any())).thenReturn(List.of(failing, healthy));
        doThrow(new IllegalStateException("broker down")).when(broker).publish(failing);

        assertThat(service().relayBatch()).isEqualTo(2);

        verify(broker).publish(healthy);
        verify(store).apply(new DispatchOutcome.Published(healthy.id(), NOW));
    }

    @Test
    void doesNothingWhenNothingIsDue() {
        when(store.claimDueMessages(anyInt(), any())).thenReturn(List.of());

        assertThat(service().relayBatch()).isZero();

        verify(broker, never()).publish(any());
    }
}
