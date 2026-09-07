package pl.scalo.ing.outbox.adapter.out.messaging;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import pl.scalo.ing.outbox.application.port.out.MessageBroker;
import pl.scalo.ing.outbox.domain.OutboxMessage;

@Slf4j(topic = "outbox.events")
@Component
public class LoggingMessageBroker implements MessageBroker {
    @Override
    public void publish(OutboxMessage message) {
        MDC.put("outboxMessageId", message.id().toString());
        MDC.put("eventType", message.eventType());
        MDC.put("aggregateType", message.aggregateType());
        MDC.put("aggregateId", message.aggregateId());
        try {
            log.info(
                    "Publishing outbox event type={} aggregate={}#{} attempt={} payload={}",
                    message.eventType(),
                    message.aggregateType(),
                    message.aggregateId(),
                    message.attempts() + 1,
                    message.payload());
        } finally {
            MDC.remove("outboxMessageId");
            MDC.remove("eventType");
            MDC.remove("aggregateType");
            MDC.remove("aggregateId");
        }
    }
}
