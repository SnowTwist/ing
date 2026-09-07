package pl.scalo.ing.order.adapter.in.web;

import java.time.Instant;
import java.util.UUID;
import pl.scalo.ing.order.domain.Order;

public record OrderResponse(UUID orderId, String customerId, String status, Instant createdAt) {
    static OrderResponse from(Order order) {
        return new OrderResponse(
                order.id().value(),
                order.customerId().value(),
                order.status().name(),
                order.createdAt());
    }
}
