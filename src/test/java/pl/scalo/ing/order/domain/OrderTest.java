package pl.scalo.ing.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.scalo.ing.common.domain.DomainEvent;

class OrderTest {
    private static final Instant NOW = Instant.parse("2026-09-06T10:15:30Z");

    @Test
    @DisplayName("placing an order registers exactly one OrderCreated event")
    void registersOrderCreated() {
        Order order = Order.place(OrderId.newId(), new CustomerId("123"), NOW);

        assertThat(order.status()).isEqualTo(OrderStatus.NEW);
        assertThat(order.createdAt()).isEqualTo(NOW);
        assertThat(order.registeredEvents())
                .singleElement()
                .isInstanceOfSatisfying(
                        OrderCreated.class,
                        event -> {
                            assertThat(event.orderId()).isEqualTo(order.id().value());
                            assertThat(event.customerId()).isEqualTo("123");
                            assertThat(event.occurredAt()).isEqualTo(NOW);
                            assertThat(event.eventId()).isNotNull();
                            assertThat(event.aggregateType()).isEqualTo("Order");
                            assertThat(event.aggregateId()).isEqualTo(order.id().toString());
                            assertThat(event.eventType()).isEqualTo("OrderCreated");
                        });
    }

    @Test
    @DisplayName("draining events clears them so they cannot be published twice")
    void drainingClearsEvents() {
        Order order = Order.place(OrderId.newId(), new CustomerId("123"), NOW);

        List<DomainEvent> first = order.drainEvents();
        List<DomainEvent> second = order.drainEvents();

        assertThat(first).hasSize(1);
        assertThat(second).isEmpty();
        assertThat(order.registeredEvents()).isEmpty();
    }


    @Test
    void rejectsBlankCustomerId() {
        assertThatThrownBy(() -> new CustomerId("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer id");
    }
}
