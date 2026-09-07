package pl.scalo.ing.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.scalo.ing.common.domain.DomainEvent;
import pl.scalo.ing.order.application.port.in.PlaceOrderCommand;
import pl.scalo.ing.order.application.port.out.DomainEventAppender;
import pl.scalo.ing.order.application.port.out.OrderRepository;
import pl.scalo.ing.order.domain.Order;
import pl.scalo.ing.order.domain.OrderCreated;

@ExtendWith(MockitoExtension.class)
class PlaceOrderServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-06T10:15:30Z");

    @Mock private OrderRepository orders;
    @Mock private DomainEventAppender events;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private PlaceOrderService service() {
        return new PlaceOrderService(orders, events, clock);
    }

    @Test
    @DisplayName("the order and its event are handed to both ports in one call")
    void savesOrderAndAppendsEvent() {
        Order order = service().placeOrder(new PlaceOrderCommand("123"));

        InOrder ordered = inOrder(orders, events);
        ordered.verify(orders).save(order);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<? extends DomainEvent>> captor = ArgumentCaptor.forClass(List.class);
        ordered.verify(events).append(captor.capture());

        assertThat(captor.getValue())
                .singleElement()
                .isInstanceOfSatisfying(
                        OrderCreated.class,
                        event -> {
                            assertThat(event.orderId()).isEqualTo(order.id().value());
                            assertThat(event.customerId()).isEqualTo("123");
                            assertThat(event.occurredAt()).isEqualTo(NOW);
                        });
    }

    @Test
    @DisplayName("a failing outbox propagates, so the surrounding transaction rolls back")
    void outboxFailurePropagates() {
        doThrow(new IllegalStateException("outbox unavailable")).when(events).append(anyList());

        assertThatThrownBy(() -> service().placeOrder(new PlaceOrderCommand("123")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("outbox unavailable");

        verify(orders).save(org.mockito.ArgumentMatchers.any(Order.class));
    }

    @Test
    void rejectsBlankCustomerId() {
        assertThatThrownBy(() -> new PlaceOrderCommand(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
