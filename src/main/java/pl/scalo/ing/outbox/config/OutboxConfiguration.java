package pl.scalo.ing.outbox.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.scalo.ing.outbox.domain.BackoffStrategy;
import pl.scalo.ing.outbox.domain.ExponentialBackoff;
import pl.scalo.ing.outbox.domain.FixedBackoff;
import pl.scalo.ing.outbox.domain.RetryPolicy;

@Configuration(proxyBeanMethods = false)
public class OutboxConfiguration {

    @Bean
    @ConditionalOnMissingBean(BackoffStrategy.class)
    public BackoffStrategy backoffStrategy(OutboxProperties properties) {
        return switch (properties.backoffStrategy()) {
            case EXPONENTIAL ->
                    new ExponentialBackoff(properties.initialBackoff(), properties.maxBackoff());
            case FIXED -> new FixedBackoff(properties.initialBackoff());
        };
    }

    @Bean
    public RetryPolicy outboxRetryPolicy(OutboxProperties properties, BackoffStrategy backoff) {
        return new RetryPolicy(properties.maxAttempts(), backoff);
    }
}
