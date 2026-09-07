package pl.scalo.ing.order.application.port.out;

import java.util.List;
import pl.scalo.ing.common.domain.DomainEvent;

public interface DomainEventAppender {
    void append(List<? extends DomainEvent> events);
}
