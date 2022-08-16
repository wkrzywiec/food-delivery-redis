package io.wkrzywiec.fooddelivery.bff.inbox;

import com.github.sonus21.rqueue.core.RqueueEndpointManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class RedisQueueCreator implements ApplicationListener<ContextRefreshedEvent> {

    private final RqueueEndpointManager rqueueEndpointManager;
    private final List<String> inboxes = List.of("ordering-inbox", "delivery-inbox");

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        for (String inbox : inboxes) {
            log.info("Creating {} queue in Redis for an inbox.", inbox);
            if (rqueueEndpointManager.isQueueRegistered(inbox)){
                log.info("{} queue already exists, therefore it's creation will be skipped", inbox);
            }

            rqueueEndpointManager.registerQueue(inbox, "1");
        }
    }
}
