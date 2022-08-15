package io.wkrzywiec.fooddelivery.commons.infra.messaging.redis;

import io.lettuce.core.RedisBusyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.time.Duration;

@Slf4j
public abstract class RedisMessageConsumerConfig {


    protected Subscription createSubscription(RedisTemplate<String, String> redisTemplate,
                                              RedisConnectionFactory factory,
                                              RedisStreamListener streamListener) {
        createConsumerGroup(redisTemplate, streamListener);
        var options = StreamMessageListenerContainer
                .StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(Duration.ofSeconds(1))
                .build();

        var listenerContainer = StreamMessageListenerContainer.create(factory,options);
        var subscription = listenerContainer.receiveAutoAck(
                Consumer.from(streamListener.group(), streamListener.consumer()),
                StreamOffset.create(streamListener.streamName(), ReadOffset.lastConsumed()), streamListener);
        listenerContainer.start();
        return subscription;
    }

    protected void createConsumerGroup(RedisTemplate<String, String> redisTemplate, RedisStreamListener streamListener) {
        try {
            redisTemplate.opsForStream().createGroup(streamListener.streamName(), streamListener.group());
        } catch (RedisSystemException e) {
            var cause = e.getRootCause();
            if (cause != null && RedisBusyException.class.equals(cause.getClass())) {
                log.info("STREAM - Redis group already exists, skipping Redis group creation: {}", streamListener.group());
            } else {
                throw e;
            }
        }
    }
}
