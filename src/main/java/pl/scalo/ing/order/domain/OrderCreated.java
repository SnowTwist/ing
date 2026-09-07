package pl.scalo.ing.order.domain;

import java.time.Instant;
import java.util.UUID;
import pl.scalo.ing.common.domain.DomainEvent;

public record OrderCreated(UUID eventId, UUID orderId, String customerId, Instant occurredAt)
        implements DomainEvent {
    public static final String EVENT_TYPE = "OrderCreated";

    @Override
    public String aggregateType() {
        return Order.AGGREGATE_TYPE;
    }

    @Override
    public String aggregateId() {
        return orderId.toString();
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }
}
