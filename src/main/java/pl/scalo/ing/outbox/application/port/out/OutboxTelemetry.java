package pl.scalo.ing.outbox.application.port.out;

public interface OutboxTelemetry {
    void published(String eventType);

    void retryScheduled(String eventType);

    void deadLettered(String eventType);
}
