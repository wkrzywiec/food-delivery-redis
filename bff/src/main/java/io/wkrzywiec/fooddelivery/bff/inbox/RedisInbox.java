package io.wkrzywiec.fooddelivery.bff.inbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sonus21.rqueue.core.RqueueMessageEnqueuer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class RedisInbox implements Inbox {

    private final RqueueMessageEnqueuer redisQueue;

    private final ObjectMapper objectMapper;

    @Override
    public void storeMessage(String inbox, Object message) {
        redisQueue.enqueue(inbox, message);
    }
}
