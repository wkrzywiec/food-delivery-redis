package io.wkrzywiec.fooddelivery.commons.infra

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import groovy.util.logging.Slf4j
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.RecordId
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.connection.stream.StreamReadOptions
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

import java.time.Duration

@Slf4j
class RedisStreamTestClient {

    private final RedisTemplate redisTemplate
    private final StreamReadOptions streamReadOptions
    private final ObjectMapper objectMapper

    RedisStreamTestClient(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate

        streamReadOptions = StreamReadOptions.empty()
                .block(Duration.ofMillis(1000))
                .count(10)

        objectMapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(
                        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                        SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS,
                        DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS
                )
                .modules(
                        new JavaTimeModule()
                )
                .build()
    }

    public String publishMessage(Message message) {
        publishMessage(message.header().channel(), message)
    }

    public String publishMessage(String streamName, Message message) {
        log.info("Publishing '{}' message to stream: '{}', body: '{}'", message.header().type(), streamName, message.body())

        String messageJson = null
        try {
            messageJson = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to publish message", e)
        }

        log.info(messageJson)

        ObjectRecord<String, String> record = StreamRecords.newRecord()
                .ofObject(messageJson)
                .withStreamKey(streamName)

        RecordId recordId = redisTemplate.opsForStream()
                .add(record)

        log.info("'{}' message was published to stream: '{}', full message: '{}'. Record id: {}",
                message.header().type(), streamName, message, recordId.getValue())
    }

    public JsonNode getLatestMessageFromStreamAsJson(String stream) {
        return objectMapper.readTree(getLatestMessageFromStream(stream))
    }

    public String getLatestMessageFromStream(String stream) {
        def allMessages = getAllMessagesInStream(stream)
        return allMessages.get(allMessages.size() - 1)
    }

    public List<String> getAllMessagesInStream(String stream) {
        log.info("Fetching messages from '$stream' Redis stream")
        List<ObjectRecord<String, String>> objectRecords = redisTemplate.opsForStream()
                .read(String, streamReadOptions,
                        StreamOffset.create(stream, ReadOffset.from("0")))

        return objectRecords.stream()
                .map(r -> r.getValue())
                .peek(value -> log.info("Fetched from '$stream' stream: $value"))
                .toList()
    }
}
