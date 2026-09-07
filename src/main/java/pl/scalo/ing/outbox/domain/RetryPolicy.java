package pl.scalo.ing.outbox.domain;

import java.time.Duration;
import java.util.Objects;

public record RetryPolicy(int maxAttempts, BackoffStrategy backoff) {

    public RetryPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        Objects.requireNonNull(backoff, "backoff must not be null");
    }

    public Duration backoffFor(int attemptNumber) {
        if (attemptNumber < 1) {
            throw new IllegalArgumentException("attemptNumber must be at least 1");
        }
        return backoff.backoffFor(attemptNumber);
    }

    public boolean isExhausted(int attempts) {
        return attempts >= maxAttempts;
    }
}
