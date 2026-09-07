package pl.scalo.ing.outbox.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BackoffStrategyTest {

    @Nested
    class Exponential {
        private final BackoffStrategy strategy =
                new ExponentialBackoff(Duration.ofSeconds(1), Duration.ofMinutes(5));

        @ParameterizedTest(name = "attempt {0} waits {1}s")
        @CsvSource({"1, 1", "2, 2", "3, 4", "4, 8", "5, 16"})
        @DisplayName("the delay doubles with every attempt")
        void delayDoubles(int attempt, long expectedSeconds) {
            assertThat(strategy.backoffFor(attempt)).isEqualTo(Duration.ofSeconds(expectedSeconds));
        }

        @Test
        @DisplayName("the delay never exceeds the configured ceiling")
        void delayIsCapped() {
            assertThat(strategy.backoffFor(20)).isEqualTo(Duration.ofMinutes(5));
            assertThat(strategy.backoffFor(Integer.MAX_VALUE)).isEqualTo(Duration.ofMinutes(5));
        }

        @Test
        void rejectsInconsistentConfiguration() {
            assertThatThrownBy(
                            () -> new ExponentialBackoff(Duration.ofMinutes(1), Duration.ofSeconds(1)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new ExponentialBackoff(Duration.ZERO, Duration.ofMinutes(1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Fixed {
        private final BackoffStrategy strategy = new FixedBackoff(Duration.ofSeconds(30));

        @Test
        @DisplayName("every attempt waits the same amount of time")
        void delayIsConstant() {
            assertThat(strategy.backoffFor(1)).isEqualTo(Duration.ofSeconds(30));
            assertThat(strategy.backoffFor(7)).isEqualTo(Duration.ofSeconds(30));
        }

        @Test
        void rejectsNonPositiveDelay() {
            assertThatThrownBy(() -> new FixedBackoff(Duration.ZERO))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
