package pl.scalo.ing.outbox.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.scalo.ing.outbox.domain.BackoffStrategy;
import pl.scalo.ing.outbox.domain.ExponentialBackoff;
import pl.scalo.ing.outbox.domain.FixedBackoff;
import pl.scalo.ing.outbox.domain.RetryPolicy;

class OutboxConfigurationTest {

    private final OutboxConfiguration configuration = new OutboxConfiguration();

    private static OutboxProperties properties(OutboxProperties.BackoffType type) {
        return new OutboxProperties(
                100,
                Duration.ofSeconds(2),
                10,
                5,
                type,
                Duration.ofSeconds(4),
                Duration.ofMinutes(5),
                true);
    }

    @Test
    @DisplayName("configuration selects the exponential strategy")
    void buildsExponentialStrategy() {
        BackoffStrategy strategy =
                configuration.backoffStrategy(properties(OutboxProperties.BackoffType.EXPONENTIAL));

        assertThat(strategy)
                .isEqualTo(new ExponentialBackoff(Duration.ofSeconds(4), Duration.ofMinutes(5)));
        assertThat(strategy.backoffFor(3)).isEqualTo(Duration.ofSeconds(16));
    }

    @Test
    @DisplayName("configuration selects the fixed strategy")
    void buildsFixedStrategy() {
        BackoffStrategy strategy =
                configuration.backoffStrategy(properties(OutboxProperties.BackoffType.FIXED));

        assertThat(strategy).isEqualTo(new FixedBackoff(Duration.ofSeconds(4)));
        assertThat(strategy.backoffFor(3)).isEqualTo(Duration.ofSeconds(4));
    }

    @Test
    void retryPolicyCarriesMaxAttemptsAndStrategy() {
        OutboxProperties properties = properties(OutboxProperties.BackoffType.FIXED);
        BackoffStrategy strategy = configuration.backoffStrategy(properties);

        RetryPolicy policy = configuration.outboxRetryPolicy(properties, strategy);

        assertThat(policy.maxAttempts()).isEqualTo(5);
        assertThat(policy.backoff()).isSameAs(strategy);
    }
}
