package io.wkrzywiec.fooddelivery.ordering;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;
import io.wkrzywiec.fooddelivery.commons.infra.repository.RedisEventStore;
import io.wkrzywiec.fooddelivery.ordering.outgoing.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Profile("redis")
@Component
@Slf4j
class RedisOrderingEventStore extends RedisEventStore {

    public RedisOrderingEventStore(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        super(redisTemplate, objectMapper);
    }

    @Override
    protected String streamPrefix() {
        return "ordering::";
    }

    @Override
    protected Class<? extends DomainMessageBody> getClassType(String type) {
        return switch (type) {
            case "OrderCreated" -> OrderCreated.class;
            case "OrderCanceled" -> OrderCanceled.class;
            case "OrderInProgress" -> OrderInProgress.class;
            case "TipAddedToOrder" -> TipAddedToOrder.class;
            case "OrderCompleted" -> OrderCompleted.class;
            default -> {
                log.error("There is not logic for mapping {} event from a store", type);
                yield null;
            }
        };
    }
}
