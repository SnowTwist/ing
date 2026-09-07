package pl.scalo.ing.integration.support;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import pl.scalo.ing.outbox.application.port.out.MessageBroker;
import pl.scalo.ing.outbox.domain.OutboxMessage;

public class ControllableMessageBroker implements MessageBroker {
    private final List<OutboxMessage> delivered = new CopyOnWriteArrayList<>();
    private final AtomicReference<RuntimeException> failure = new AtomicReference<>();
    private volatile long deliveryLatencyMillis;

    @Override
    public void publish(OutboxMessage message) {
        RuntimeException configured = failure.get();
        if (configured != null) {
            throw configured;
        }
        if (deliveryLatencyMillis > 0) {
            try {
                Thread.sleep(deliveryLatencyMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        delivered.add(message);
    }

    public void failWith(RuntimeException exception) {
        failure.set(exception);
    }

    public void recover() {
        failure.set(null);
    }

    public void deliverSlowly(long millis) {
        this.deliveryLatencyMillis = millis;
    }

    public List<OutboxMessage> delivered() {
        return List.copyOf(delivered);
    }

    public List<UUID> deliveredIds() {
        return delivered.stream().map(OutboxMessage::id).toList();
    }

    public void reset() {
        delivered.clear();
        failure.set(null);
        deliveryLatencyMillis = 0;
    }
}
