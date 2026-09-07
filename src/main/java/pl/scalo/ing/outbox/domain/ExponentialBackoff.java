package pl.scalo.ing.outbox.domain;

import java.time.Duration;

public record ExponentialBackoff(Duration initialBackoff, Duration maxBackoff)
        implements BackoffStrategy {

    private static final int MAX_EXPONENT = 30;

    public ExponentialBackoff {
        if (initialBackoff.isNegative() || initialBackoff.isZero()) {
            throw new IllegalArgumentException("initialBackoff must be positive");
        }
        if (maxBackoff.compareTo(initialBackoff) < 0) {
            throw new IllegalArgumentException("maxBackoff must not be shorter than initialBackoff");
        }
    }

    @Override
    public Duration backoffFor(int attemptNumber) {
        int exponent = Math.min(attemptNumber - 1, MAX_EXPONENT);
        Duration delay = initialBackoff.multipliedBy(1L << exponent);
        return delay.compareTo(maxBackoff) > 0 ? maxBackoff : delay;
    }
}
