package pl.scalo.ing.integration.support;

import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class MutableClockConfiguration {
    public static final Instant START = Instant.parse("2026-09-06T10:15:30Z");

    @Bean
    @Primary
    public MutableClock mutableClock() {
        return new MutableClock(START, ZoneOffset.UTC);
    }
}
