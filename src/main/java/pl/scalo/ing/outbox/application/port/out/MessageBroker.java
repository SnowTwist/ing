package pl.scalo.ing.outbox.application.port.out;

import pl.scalo.ing.outbox.domain.OutboxMessage;

public interface MessageBroker {
    void publish(OutboxMessage message);
}
