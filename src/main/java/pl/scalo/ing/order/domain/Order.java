package pl.scalo.ing.order.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import pl.scalo.ing.common.domain.DomainEvent;

public final class Order {
    public static final String AGGREGATE_TYPE = "Order";

    private final OrderId id;
    private final CustomerId customerId;
    private final OrderStatus status;
    private final Instant createdAt;
    private final List<DomainEvent> registeredEvents = new ArrayList<>();

    private Order(OrderId id, CustomerId customerId, OrderStatus status, Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Order place(OrderId id, CustomerId customerId, Instant placedAt) {
        Order order = new Order(id, customerId, OrderStatus.NEW, placedAt);
        order.registeredEvents.add(
                new OrderCreated(UUID.randomUUID(), id.value(), customerId.value(), placedAt));
        return order;
    }


    public List<DomainEvent> drainEvents() {
        List<DomainEvent> drained = List.copyOf(registeredEvents);
        registeredEvents.clear();
        return drained;
    }

    public List<DomainEvent> registeredEvents() {
        return List.copyOf(registeredEvents);
    }

    public OrderId id() {
        return id;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public OrderStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
