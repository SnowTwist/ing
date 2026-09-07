package pl.scalo.ing.outbox.adapter.out.persistence;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.scalo.ing.common.domain.DomainEvent;
import pl.scalo.ing.order.application.port.out.DomainEventAppender;
import pl.scalo.ing.outbox.application.port.out.OutboxMessageStore;
import pl.scalo.ing.outbox.domain.OutboxMessage;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class OutboxDomainEventAppender implements DomainEventAppender {
    private final OutboxMessageStore store;
    private final ObjectMapper objectMapper;
    private final Clock clock;


    @Override
    public void append(List<? extends DomainEvent> events) {
        Instant now = clock.instant();
        for (DomainEvent event : events) {
            store.append(
                    OutboxMessage.pending(
                            UUID.randomUUID(),
                            event.aggregateType(),
                            event.aggregateId(),
                            event.eventType(),
                            serialize(event),
                            now));
        }
    }

    private String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException e) {
            throw new IllegalStateException(
                    "Cannot serialise domain event " + event.eventType(), e);
        }
    }
}
