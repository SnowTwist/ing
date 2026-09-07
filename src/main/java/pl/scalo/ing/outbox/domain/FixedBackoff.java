package pl.scalo.ing.outbox.domain;

import java.time.Duration;

public record FixedBackoff(Duration delay) implements BackoffStrategy {

    public FixedBackoff {
        if (delay.isNegative() || delay.isZero()) {
            throw new IllegalArgumentException("delay must be positive");
        }
    }

    @Override
    public Duration backoffFor(int attemptNumber) {
        return delay;
    }
}
