package pl.scalo.ing.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "outbox.scheduler-enabled=false")
@AutoConfigureMockMvc
class PlaceOrderApiIntegrationTest extends AbstractIntegrationTest {
    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("POST /orders stores the order and the OrderCreated event together")
    void createsOrderAndOutboxEvent() throws Exception {
        mockMvc.perform(
                        post("/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"customerId\":\"123\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.orderId").isNotEmpty())
                .andExpect(jsonPath("$.customerId").value("123"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        assertThat(countOrders()).isEqualTo(1);
        assertThat(countOutboxEvents()).isEqualTo(1);

        Map<String, Object> event =
                jdbc.sql(
                                """
                                SELECT o.id AS order_id, o.customer_id, e.aggregate_type, e.aggregate_id,
                                       e.event_type, e.status, e.attempts, e.payload::text AS payload,
                                       e.published_at, e.last_error
                                FROM outbox_events e, orders o
                                """)
                        .query()
                        .singleRow();

        assertThat(event.get("aggregate_type")).isEqualTo("Order");
        assertThat(event.get("aggregate_id")).isEqualTo(event.get("order_id").toString());
        assertThat(event.get("event_type")).isEqualTo("OrderCreated");
        assertThat(event.get("status")).isEqualTo("PENDING");
        assertThat(event.get("attempts")).isEqualTo(0);
        assertThat(event.get("published_at")).isNull();
        assertThat(event.get("last_error")).isNull();
        assertThat((String) event.get("payload"))
                .contains("\"customerId\": \"123\"")
                .contains("\"eventId\"")
                .contains("\"occurredAt\"");
    }

    @Test
    @DisplayName("a blank customerId is rejected without touching the database")
    void rejectsBlankCustomerId() throws Exception {
        mockMvc.perform(
                        post("/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"customerId\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.customerId").value("must not be blank"));

        assertThat(countOrders()).isZero();
        assertThat(countOutboxEvents()).isZero();
    }

    @Test
    void rejectsMissingCustomerId() throws Exception {
        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        assertThat(countOutboxEvents()).isZero();
    }
}
