package pl.scalo.ing.common.domain;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    UUID eventId();

    String aggregateType();

    String aggregateId();

    String eventType();

    Instant occurredAt();
}
