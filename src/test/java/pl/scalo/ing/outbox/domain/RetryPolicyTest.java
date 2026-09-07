package pl.scalo.ing.outbox.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RetryPolicyTest {

    private final RetryPolicy policy = new RetryPolicy(5, new FixedBackoff(Duration.ofSeconds(3)));

    @Test
    @DisplayName("the policy delegates the delay to its backoff strategy")
    void delegatesToStrategy() {
        RetryPolicy exponential =
                new RetryPolicy(5, new ExponentialBackoff(Duration.ofSeconds(1), Duration.ofHours(1)));

        assertThat(policy.backoffFor(4)).isEqualTo(Duration.ofSeconds(3));
        assertThat(exponential.backoffFor(4)).isEqualTo(Duration.ofSeconds(8));
    }

    @Test
    void reportsExhaustionAtMaxAttempts() {
        assertThat(policy.isExhausted(4)).isFalse();
        assertThat(policy.isExhausted(5)).isTrue();
        assertThat(policy.isExhausted(6)).isTrue();
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThatThrownBy(() -> new RetryPolicy(0, new FixedBackoff(Duration.ofSeconds(1))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RetryPolicy(5, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> policy.backoffFor(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
