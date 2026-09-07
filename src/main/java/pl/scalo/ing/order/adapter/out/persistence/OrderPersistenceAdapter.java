package pl.scalo.ing.order.adapter.out.persistence;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.scalo.ing.order.application.port.out.OrderRepository;
import pl.scalo.ing.order.domain.Order;

@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class OrderPersistenceAdapter implements OrderRepository {
    private final OrderJpaRepository repository;

    @Override
    public void save(Order order) {
        repository.save(OrderEntity.from(order));
    }
}
