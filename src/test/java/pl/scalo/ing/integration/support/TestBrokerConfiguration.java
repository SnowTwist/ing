package pl.scalo.ing.integration.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class TestBrokerConfiguration {
    @Bean
    @Primary
    public ControllableMessageBroker controllableMessageBroker() {
        return new ControllableMessageBroker();
    }
}
