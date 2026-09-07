package pl.scalo.ing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import pl.scalo.ing.integration.support.ControllableMessageBroker;
import pl.scalo.ing.integration.support.TestBrokerConfiguration;
import pl.scalo.ing.order.application.port.in.PlaceOrderCommand;
import pl.scalo.ing.order.application.port.in.PlaceOrderUseCase;
import pl.scalo.ing.outbox.application.port.in.RelayOutboxMessages;

@SpringBootTest(properties = {"outbox.scheduler-enabled=false", "outbox.batch-size=5"})
@Import(TestBrokerConfiguration.class)
class OutboxConcurrencyIntegrationTest extends AbstractIntegrationTest {
    private static final int ORDERS = 40;
    private static final int WORKERS = 4;

    @Autowired private PlaceOrderUseCase placeOrder;
    @Autowired private RelayOutboxMessages relay;
    @Autowired private ControllableMessageBroker broker;

    @Test
    @DisplayName("concurrent relays deliver every event exactly once")
    void concurrentRelaysDoNotDuplicateEvents() throws Exception {
        broker.reset();

        broker.deliverSlowly(5);
        for (int i = 0; i < ORDERS; i++) {
            placeOrder.placeOrder(new PlaceOrderCommand("customer-" + i));
        }
        assertThat(countOutboxEventsWithStatus("PENDING")).isEqualTo(ORDERS);

        try (ExecutorService pool = Executors.newFixedThreadPool(WORKERS)) {
            List<CompletableFuture<Void>> workers =
                    java.util.stream.IntStream.range(0, WORKERS)
                            .mapToObj(
                                    ignored ->
                                            CompletableFuture.runAsync(this::drain, pool))
                            .toList();
            CompletableFuture.allOf(workers.toArray(CompletableFuture[]::new))
                    .get(60, TimeUnit.SECONDS);
        }

        List<UUID> deliveredIds = broker.deliveredIds();
        assertThat(deliveredIds).hasSize(ORDERS);
        assertThat(deliveredIds).doesNotHaveDuplicates();
        assertThat(countOutboxEventsWithStatus("PUBLISHED")).isEqualTo(ORDERS);
        assertThat(countOutboxEventsWithStatus("PENDING")).isZero();
        assertThat(maxAttempts()).as("no event was attempted twice").isEqualTo(1);
    }

    private void drain() {
        while (relay.relayBatch() > 0) {
        }
    }

    private int maxAttempts() {
        return jdbc.sql("SELECT coalesce(max(attempts), 0) FROM outbox_events")
                .query(Integer.class)
                .single();
    }
}
