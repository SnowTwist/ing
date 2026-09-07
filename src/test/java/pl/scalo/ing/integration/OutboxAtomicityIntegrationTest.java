package pl.scalo.ing.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import pl.scalo.ing.order.application.port.in.PlaceOrderCommand;
import pl.scalo.ing.order.application.port.in.PlaceOrderUseCase;
import pl.scalo.ing.order.application.port.out.DomainEventAppender;

@SpringBootTest(properties = "outbox.scheduler-enabled=false")
class OutboxAtomicityIntegrationTest extends AbstractIntegrationTest {
    @Autowired private PlaceOrderUseCase placeOrder;

    @MockitoSpyBean private DomainEventAppender domainEventAppender;

    @Test
    @DisplayName("a failing outbox write rolls the order back")
    void failingOutboxRollsBackTheOrder() {
        doThrow(new IllegalStateException("outbox unavailable"))
                .when(domainEventAppender)
                .append(anyList());

        assertThatThrownBy(() -> placeOrder.placeOrder(new PlaceOrderCommand("123")))
                .isInstanceOf(IllegalStateException.class);

        assertThat(countOrders()).isZero();
        assertThat(countOutboxEvents()).isZero();
    }

    @Test
    @DisplayName("with the outbox healthy both rows are committed")
    void healthyOutboxCommitsBoth() {
        placeOrder.placeOrder(new PlaceOrderCommand("123"));

        assertThat(countOrders()).isEqualTo(1);
        assertThat(countOutboxEvents()).isEqualTo(1);
    }
}
