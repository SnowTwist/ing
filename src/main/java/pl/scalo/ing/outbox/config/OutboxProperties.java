package pl.scalo.ing.outbox.config;

import jakarta.validation.constraints.Min;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "outbox")
public record OutboxProperties(
        @DefaultValue("100") @Min(1) int batchSize,
        @DefaultValue("2s") Duration pollInterval,
        @DefaultValue("10") @Min(1) int maxBatchesPerPoll,
        @DefaultValue("5") @Min(1) int maxAttempts,
        @DefaultValue("exponential") BackoffType backoffStrategy,
        @DefaultValue("1s") Duration initialBackoff,
        @DefaultValue("5m") Duration maxBackoff,
        @DefaultValue("true") boolean schedulerEnabled) {

    public enum BackoffType {
        EXPONENTIAL,
        FIXED
    }
}
