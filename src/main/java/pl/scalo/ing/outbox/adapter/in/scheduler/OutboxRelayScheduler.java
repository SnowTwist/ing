package pl.scalo.ing.outbox.adapter.in.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.scalo.ing.outbox.application.port.in.RelayOutboxMessages;
import pl.scalo.ing.outbox.config.OutboxProperties;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "outbox.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayScheduler {
    private final RelayOutboxMessages relay;
    private final OutboxProperties properties;


    @Scheduled(
            fixedDelayString = "${outbox.poll-interval:2s}",
            initialDelayString = "${outbox.poll-interval:2s}")
    public void poll() {
        try {
            for (int batch = 0; batch < properties.maxBatchesPerPoll(); batch++) {
                if (relay.relayBatch() < properties.batchSize()) {
                    return;
                }
            }
        } catch (RuntimeException e) {
            log.error("Outbox poll failed, will retry on the next tick", e);
        }
    }
}
