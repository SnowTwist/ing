package pl.scalo.ing.order.adapter.in.web;

import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.scalo.ing.order.application.port.in.PlaceOrderCommand;
import pl.scalo.ing.order.application.port.in.PlaceOrderUseCase;
import pl.scalo.ing.order.domain.Order;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final PlaceOrderUseCase placeOrder;

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        Order order = placeOrder.placeOrder(new PlaceOrderCommand(request.customerId()));
        return ResponseEntity.created(URI.create("/orders/" + order.id())).body(OrderResponse.from(order));
    }
}
