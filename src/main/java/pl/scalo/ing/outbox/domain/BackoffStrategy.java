package pl.scalo.ing.outbox.domain;

import java.time.Duration;

public interface BackoffStrategy {

    Duration backoffFor(int attemptNumber);
}
