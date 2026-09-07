package pl.scalo.ing.order.application.port.in;

import pl.scalo.ing.order.domain.Order;

public interface PlaceOrderUseCase {
    Order placeOrder(PlaceOrderCommand command);
}
