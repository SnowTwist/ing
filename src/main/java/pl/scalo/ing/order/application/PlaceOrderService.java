package pl.scalo.ing.order.application;

import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.scalo.ing.order.application.port.in.PlaceOrderCommand;
import pl.scalo.ing.order.application.port.in.PlaceOrderUseCase;
import pl.scalo.ing.order.application.port.out.DomainEventAppender;
import pl.scalo.ing.order.application.port.out.OrderRepository;
import pl.scalo.ing.order.domain.CustomerId;
import pl.scalo.ing.order.domain.Order;
import pl.scalo.ing.order.domain.OrderId;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceOrderService implements PlaceOrderUseCase {
    private final OrderRepository orders;
    private final DomainEventAppender events;
    private final Clock clock;

    @Override
    @Transactional
    public Order placeOrder(PlaceOrderCommand command) {
        Instant now = clock.instant();
        Order order = Order.place(OrderId.newId(), new CustomerId(command.customerId()), now);

        orders.save(order);
        events.append(order.drainEvents());

        log.info("Placed order orderId={} customerId={}", order.id(), order.customerId());
        return order;
    }
}
