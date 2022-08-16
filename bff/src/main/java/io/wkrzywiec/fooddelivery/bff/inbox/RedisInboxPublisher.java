package io.wkrzywiec.fooddelivery.bff.inbox;

import com.github.sonus21.rqueue.core.RqueueMessageEnqueuer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class RedisInboxPublisher implements InboxPublisher {

    private final RqueueMessageEnqueuer redisQueue;

    @Override
    public void storeMessage(String inbox, Object message) {
        redisQueue.enqueue(inbox, message);
    }
}
