package pl.scalo.ing.order.application.port.out;

import pl.scalo.ing.order.domain.Order;

public interface OrderRepository {
    void save(Order order);
}
