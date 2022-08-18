package io.wkrzywiec.fooddelivery.ordering;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message;
import io.wkrzywiec.fooddelivery.ordering.outgoing.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.connection.stream.Record;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Profile("redis")
@Component
class RedisOrderingEventStore implements OrderingEventStore {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String ORDERING_STREAM_PREFIX = "ordering::";

    @Override
    public void store(Message event) {
        log.info("Storing event in a stream '{}', body: '{}'", ORDERING_STREAM_PREFIX + event.body().orderId(), event);

        String messageJsonAsString;
        try {
            messageJsonAsString = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse event to json: {}", event);
            throw new RuntimeException("Parsing error", e);
        }

        log.info("Storing event: {}", messageJsonAsString);

        ObjectRecord<String, String> record = StreamRecords.newRecord()
                .ofObject(messageJsonAsString)
                .withStreamKey(ORDERING_STREAM_PREFIX + event.body().orderId());

        RecordId recordId = redisTemplate.opsForStream()
                .add(record);

        log.info("Event was stored in stream: '{}', full message: '{}'. Record id: {}",
                ORDERING_STREAM_PREFIX + event.body().orderId(), messageJsonAsString, recordId.getValue());
    }

    @Override
    public List<DomainMessageBody> getEventsForOrder(String orderId) {
        log.info("Fetching events from '{}{}' Redis stream", ORDERING_STREAM_PREFIX, orderId);
        return getAllMessagesInStream(ORDERING_STREAM_PREFIX + orderId);
    }

    private List<DomainMessageBody> getAllMessagesInStream(String stream) {

        var streamReadOptions = StreamReadOptions.empty()
                .block(Duration.ofMillis(1000))
                .count(5);

        List<ObjectRecord<String, String>> objectRecords = redisTemplate.opsForStream()
                .read(String.class, streamReadOptions, StreamOffset.create(stream, ReadOffset.from("0")));

        return objectRecords.stream()
                .map(Record::getValue)
                .map(this::mapToJsonNode)
                .map(this::mapToDomainEvent)
                .toList();
    }

    private JsonNode mapToJsonNode(String eventAsString) {
        try {
            return objectMapper.readTree(eventAsString);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse event to json: {}", eventAsString);
            throw new RuntimeException("Parsing error", e);
        }
    }

    private DomainMessageBody mapToDomainEvent(JsonNode eventAsJson) {
        var eventType = eventAsJson.get("header").get("type").asText();
        var eventBody =  eventAsJson.get("body");

        return switch (eventType) {
            case "OrderCreated" -> mapEventBody(eventBody, OrderCreated.class);
            case "OrderCanceled" -> mapEventBody(eventBody, OrderCanceled.class);
            case "OrderInProgress" -> mapEventBody(eventBody, OrderInProgress.class);
            case "TipAddedToOrder" -> mapEventBody(eventBody, TipAddedToOrder.class);
            case "OrderCompleted" -> mapEventBody(eventBody, OrderCompleted.class);
            default -> {
                log.error("There is not logic for mapping {} event from a store", eventType);
                yield null;
            }
        };
    }

    private <T extends DomainMessageBody> T mapEventBody(JsonNode eventBody, Class<T> valueType) {
        try {
            return objectMapper.treeToValue(eventBody, valueType);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse event json: {} to '{}' class", eventBody, valueType.getCanonicalName());
            throw new RuntimeException("Parsing error", e);
        }
    }
}
