package pl.scalo.ing.outbox.adapter.out.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import pl.scalo.ing.outbox.application.port.out.OutboxMessageStore;
import pl.scalo.ing.outbox.application.port.out.OutboxTelemetry;

@Component
class MicrometerOutboxTelemetry implements OutboxTelemetry {

    private static final String PUBLISHED = "outbox.messages.published";
    private static final String RETRIED = "outbox.messages.retried";
    private static final String DEAD_LETTERED = "outbox.messages.dead_lettered";
    private static final String EVENT_TYPE_TAG = "eventType";

    private final MeterRegistry registry;

    MicrometerOutboxTelemetry(MeterRegistry registry, OutboxMessageStore store) {
        this.registry = registry;
        Gauge.builder("outbox.messages.pending", store, OutboxMessageStore::countPending)
                .description("Outbox messages waiting to be relayed")
                .strongReference(true)
                .register(registry);
    }

    @Override
    public void published(String eventType) {
        registry.counter(PUBLISHED, EVENT_TYPE_TAG, eventType).increment();
    }

    @Override
    public void retryScheduled(String eventType) {
        registry.counter(RETRIED, EVENT_TYPE_TAG, eventType).increment();
    }

    @Override
    public void deadLettered(String eventType) {
        registry.counter(DEAD_LETTERED, EVENT_TYPE_TAG, eventType).increment();
    }
}
