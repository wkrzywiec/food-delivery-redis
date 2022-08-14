package io.wkrzywiec.fooddelivery.commons.messaging

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.connection.stream.StreamReadOptions
import org.springframework.data.redis.core.RedisTemplate

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

        objectMapper = new ObjectMapper()
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
