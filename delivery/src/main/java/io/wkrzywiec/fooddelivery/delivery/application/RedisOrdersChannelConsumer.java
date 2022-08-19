package io.wkrzywiec.fooddelivery.delivery.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.commons.incoming.*;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.redis.RedisStreamListener;
import io.wkrzywiec.fooddelivery.delivery.DeliveryFacade;
import io.wkrzywiec.fooddelivery.delivery.incoming.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("redis")
@RequiredArgsConstructor
public class RedisOrdersChannelConsumer implements RedisStreamListener {

    private final DeliveryFacade facade;
    private final ObjectMapper objectMapper;

    @Override
    public String streamName() {
        return "orders";
    }

    @Override
    public String group() {
        return "delivery";
    }

    @Override
    public String consumer() {
        //TODO randomize ?
        return "1";
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        log.info("Message received from {} stream: {}", streamName(), message);

        var payloadMessage = message.getValue().get("payload");

        try {
            var messageAsJson = objectMapper.readTree(payloadMessage);
            Header header = map(messageAsJson.get("header"), Header.class);

            switch (header.type()) {
                case "OrderCreated" -> facade.handle(mapMessageBody(messageAsJson, OrderCreated.class));
                case "TipAddedToOrder" -> facade.handle(mapMessageBody(messageAsJson, TipAddedToOrder.class));
                case "OrderCanceled" -> facade.handle(mapMessageBody(messageAsJson, OrderCanceled.class));
                case "PrepareFood" -> facade.handle(mapMessageBody(messageAsJson, PrepareFood.class));
                case "AssignDeliveryMan" -> facade.handle(mapMessageBody(messageAsJson, AssignDeliveryMan.class));
                case "UnAssignDeliveryMan" -> facade.handle(mapMessageBody(messageAsJson, UnAssignDeliveryMan.class));
                case "FoodReady" -> facade.handle(mapMessageBody(messageAsJson, FoodReady.class));
                case "PickUpFood" -> facade.handle(mapMessageBody(messageAsJson, PickUpFood.class));
                case "DeliverFood" -> facade.handle(mapMessageBody(messageAsJson, DeliverFood.class));
                default -> log.info("There is not logic for handling {} message", header.type());
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T mapMessageBody(JsonNode fullMessage, Class<T> valueType) throws JsonProcessingException {
        return objectMapper.treeToValue(fullMessage.get("body"), valueType);
    }

    private <T> T map(JsonNode fullMessage, Class<T> valueType) throws JsonProcessingException {
        return objectMapper.treeToValue(fullMessage, valueType);
    }
}
