package pl.scalo.ing.outbox.domain;

import java.time.Instant;
import java.util.UUID;

public sealed interface DispatchOutcome {
    UUID messageId();

    record Published(UUID messageId, Instant publishedAt) implements DispatchOutcome {}

    record Retry(UUID messageId, int attempts, Instant nextAttemptAt, String error)
            implements DispatchOutcome {}

    record Dead(UUID messageId, int attempts, String error) implements DispatchOutcome {}
}
