package pl.scalo.ing.outbox.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.scalo.ing.outbox.application.port.in.RelayOutboxMessages;
import pl.scalo.ing.outbox.application.port.out.MessageBroker;
import pl.scalo.ing.outbox.application.port.out.OutboxMessageStore;
import pl.scalo.ing.outbox.application.port.out.OutboxTelemetry;
import pl.scalo.ing.outbox.config.OutboxProperties;
import pl.scalo.ing.outbox.domain.DispatchOutcome;
import pl.scalo.ing.outbox.domain.OutboxMessage;
import pl.scalo.ing.outbox.domain.RetryPolicy;

@Slf4j
@Service
public class OutboxRelayService implements RelayOutboxMessages {

    private final OutboxMessageStore store;
    private final MessageBroker broker;
    private final OutboxTelemetry telemetry;
    private final RetryPolicy retryPolicy;
    private final int batchSize;
    private final Clock clock;

    public OutboxRelayService(
            OutboxMessageStore store,
            MessageBroker broker,
            OutboxTelemetry telemetry,
            RetryPolicy retryPolicy,
            OutboxProperties properties,
            Clock clock) {
        this.store = store;
        this.broker = broker;
        this.telemetry = telemetry;
        this.retryPolicy = retryPolicy;
        this.batchSize = properties.batchSize();
        this.clock = clock;
    }

    @Override
    @Transactional
    public int relayBatch() {
        List<OutboxMessage> batch = store.claimDueMessages(batchSize, clock.instant());
        for (OutboxMessage message : batch) {
            relay(message);
        }
        return batch.size();
    }

    private void relay(OutboxMessage message) {
        Instant now = clock.instant();
        try {
            broker.publish(message);
            store.apply(message.dispatched(now));
            telemetry.published(message.eventType());
        } catch (RuntimeException failure) {
            DispatchOutcome outcome = message.dispatchFailed(now, retryPolicy, failure);
            store.apply(outcome);
            report(message, outcome, failure);
        }
    }

    private void report(OutboxMessage message, DispatchOutcome outcome, RuntimeException failure) {
        switch (outcome) {
            case DispatchOutcome.Retry retry -> {
                telemetry.retryScheduled(message.eventType());
                log.warn(
                        "Outbox publish failed, retry {} of {} scheduled at {} for messageId={} eventType={}",
                        retry.attempts(),
                        retryPolicy.maxAttempts(),
                        retry.nextAttemptAt(),
                        message.id(),
                        message.eventType(),
                        failure);
            }
            case DispatchOutcome.Dead dead -> {
                telemetry.deadLettered(message.eventType());
                log.error(
                        "Outbox publish failed permanently after {} attempts, messageId={} eventType={} parked as FAILED",
                        dead.attempts(),
                        message.id(),
                        message.eventType(),
                        failure);
            }
            case DispatchOutcome.Published ignored -> {}
        }
    }
}
