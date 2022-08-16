package io.wkrzywiec.fooddelivery.ordering.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.commons.incoming.AddTip;
import io.wkrzywiec.fooddelivery.commons.incoming.CancelOrder;
import io.wkrzywiec.fooddelivery.commons.incoming.CreateOrder;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.redis.RedisStreamListener;
import io.wkrzywiec.fooddelivery.ordering.OrderingFacade;
import io.wkrzywiec.fooddelivery.ordering.incoming.*;
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

    private final OrderingFacade facade;
    private final ObjectMapper objectMapper;

    @Override
    public String streamName() {
        return "orders";
    }

    @Override
    public String group() {
        return "ordering";
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
                case "CreateOrder" -> facade.handle(mapMessageBody(messageAsJson, CreateOrder.class));
                case "CancelOrder" -> facade.handle(mapMessageBody(messageAsJson, CancelOrder.class));
                case "FoodInPreparation" -> facade.handle(mapMessageBody(messageAsJson, FoodInPreparation.class));
                case "AddTip" -> facade.handle(mapMessageBody(messageAsJson, AddTip.class));
                case "FoodDelivered" -> facade.handle(mapMessageBody(messageAsJson, FoodDelivered.class));
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
