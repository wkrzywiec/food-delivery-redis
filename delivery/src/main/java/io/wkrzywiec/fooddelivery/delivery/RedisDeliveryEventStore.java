package io.wkrzywiec.fooddelivery.delivery;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;
import io.wkrzywiec.fooddelivery.commons.infra.repository.RedisEventStore;
import io.wkrzywiec.fooddelivery.delivery.outgoing.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Profile("redis")
@Component
class RedisDeliveryEventStore extends RedisEventStore {

    public RedisDeliveryEventStore(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        super(redisTemplate, objectMapper);
    }

    @Override
    public String streamPrefix() {
        return "delivery::";
    }

    @Override
    public Class<? extends DomainMessageBody> getClassType(String type) {
        return switch (type) {
            case "DeliveryCreated" -> DeliveryCreated.class;
            case "DeliveryCanceled" -> DeliveryCanceled.class;
            case "FoodInPreparation" -> FoodInPreparation.class;
            case "DeliveryManAssigned" -> DeliveryManAssigned.class;
            case "DeliveryManUnAssigned" -> DeliveryManUnAssigned.class;
            case "FoodIsReady" -> FoodIsReady.class;
            case "FoodWasPickedUp" -> FoodWasPickedUp.class;
            case "FoodDelivered" -> FoodDelivered.class;
            default -> {
                log.error("There is not logic for mapping {} event from a store", type);
                yield null;
            }
        };
    }
}
