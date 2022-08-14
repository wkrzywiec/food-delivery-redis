package io.wkrzywiec.fooddelivery.commons.infra.messaging;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
@RequiredArgsConstructor
public class RedisMessagePublisher implements MessagePublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper mapper;
    @Override
    public void send(Message message) {
        log.info("Publishing '{}' message on channel: '{}', body: '{}'", message.header().type(), message.header().channel(), message.body());

        String messageJson = null;
        try {
            messageJson = mapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        log.info(messageJson);

        ObjectRecord<String, String> record = StreamRecords.newRecord()
                .ofObject(messageJson)
                .withStreamKey(message.header().channel());

        RecordId recordId = redisTemplate.opsForStream()
                .add(record);

        log.info("'{}' message was published on channel: '{}', full message: '{}'. Record id: {}",
                message.header().type(), message.header().channel(), message, recordId.getValue());
    }
}
