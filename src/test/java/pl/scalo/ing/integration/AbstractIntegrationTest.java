package pl.scalo.ing.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.postgresql.PostgreSQLContainer;

abstract class AbstractIntegrationTest {
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("orders")
                    .withUsername("orders")
                    .withPassword("orders");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired protected JdbcClient jdbc;

    @BeforeEach
    void resetDatabase() {
        jdbc.sql("TRUNCATE TABLE outbox_events, orders").update();
    }

    protected long countOrders() {
        return jdbc.sql("SELECT count(*) FROM orders").query(Long.class).single();
    }

    protected long countOutboxEvents() {
        return jdbc.sql("SELECT count(*) FROM outbox_events").query(Long.class).single();
    }

    protected long countOutboxEventsWithStatus(String status) {
        return jdbc.sql("SELECT count(*) FROM outbox_events WHERE status = :status")
                .param("status", status)
                .query(Long.class)
                .single();
    }
}
