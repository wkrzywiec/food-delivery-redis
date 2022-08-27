package io.wkrzywiec.fooddelivery.bff.view;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.bff.view.outgoing.*;
import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.redis.RedisStreamListener;
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

    private final DeliveryViewProcessor processor;
    private final ObjectMapper objectMapper;

    @Override
    public String streamName() {
        return "orders";
    }

    @Override
    public String group() {
        return "bff";
    }

    @Override
    public String consumer() {
        return "1";
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        log.info("Message received from {} stream: {}", streamName(), message);

        var payloadMessage = message.getValue().get("payload");

        try {
            var messageAsJson = objectMapper.readTree(payloadMessage);
            Header header = map(messageAsJson.get("header"), Header.class);

            DomainMessageBody event = switch (header.type()) {
                case "DeliveryCreated" -> mapMessageBody(messageAsJson, DeliveryCreated.class);
                case "TipAddedToDelivery" -> mapMessageBody(messageAsJson, TipAddedToDelivery.class);
                case "DeliveryCanceled" -> mapMessageBody(messageAsJson, DeliveryCanceled.class);
                case "FoodInPreparation" -> mapMessageBody(messageAsJson, FoodInPreparation.class);
                case "DeliveryManAssigned" -> mapMessageBody(messageAsJson, DeliveryManAssigned.class);
                case "DeliveryManUnAssigned" -> mapMessageBody(messageAsJson, DeliveryManUnAssigned.class);
                case "FoodIsReady" -> mapMessageBody(messageAsJson, FoodIsReady.class);
                case "FoodWasPickedUp" -> mapMessageBody(messageAsJson, FoodWasPickedUp.class);
                case "FoodDelivered" -> mapMessageBody(messageAsJson, FoodDelivered.class);
                default -> null;
            };

            if (event == null) {
                log.info("There is no delivery view handling logic for {} message.", header.type());
            }
            processor.handle(event);
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
