package pl.scalo.ing.outbox.application.port.out;

import java.time.Instant;
import java.util.List;
import pl.scalo.ing.outbox.domain.DispatchOutcome;
import pl.scalo.ing.outbox.domain.OutboxMessage;

public interface OutboxMessageStore {
    void append(OutboxMessage message);

    List<OutboxMessage> claimDueMessages(int batchSize, Instant now);

    void apply(DispatchOutcome outcome);

    long countPending();
}
