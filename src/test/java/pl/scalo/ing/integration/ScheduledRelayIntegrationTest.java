package pl.scalo.ing.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pl.scalo.ing.order.application.port.in.PlaceOrderCommand;
import pl.scalo.ing.order.application.port.in.PlaceOrderUseCase;

@SpringBootTest(
        properties = {"outbox.scheduler-enabled=true", "outbox.poll-interval=200ms"})
class ScheduledRelayIntegrationTest extends AbstractIntegrationTest {
    @Autowired private PlaceOrderUseCase placeOrder;

    @Test
    @DisplayName("the scheduler drains the outbox on its own")
    void schedulerPublishesPendingEvents() {
        placeOrder.placeOrder(new PlaceOrderCommand("123"));
        placeOrder.placeOrder(new PlaceOrderCommand("456"));

        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(
                        () -> {
                            assertThat(countOutboxEventsWithStatus("PUBLISHED")).isEqualTo(2);
                            assertThat(countOutboxEventsWithStatus("PENDING")).isZero();
                        });
    }
}
