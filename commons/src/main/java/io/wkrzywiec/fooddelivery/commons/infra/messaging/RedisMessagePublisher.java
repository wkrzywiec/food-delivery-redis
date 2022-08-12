package io.wkrzywiec.fooddelivery.commons.infra.messaging;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
@RequiredArgsConstructor
public class RedisMessagePublisher implements MessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    @Override
    public void send(Message message) {
        log.info("Publishing '{}' message on channel: '{}', body: '{}'", message.header().type(), message.header().channel(), message.body());

        ObjectRecord<String, Message> record = StreamRecords.newRecord()
                .ofObject(message)
                .withStreamKey(message.header().channel());

        RecordId recordId = redisTemplate.opsForStream()
                .add(record);

        log.info("'{}' message was published on channel: '{}', full message: '{}'. Record id: {}",
                message.header().type(), message.header().channel(), message, recordId.getValue());
    }
}
