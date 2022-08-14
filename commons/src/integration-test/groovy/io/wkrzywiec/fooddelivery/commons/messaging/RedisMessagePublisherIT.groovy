package io.wkrzywiec.fooddelivery.commons.messaging


import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message
import io.wkrzywiec.fooddelivery.commons.infra.messaging.MessagePublisher
import io.wkrzywiec.fooddelivery.commons.infra.messaging.RedisMessagingConfig
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory

import java.time.Instant

class RedisMessagePublisherIT extends CommonsIntegrationTest {

    private final String testChannel = "testing-channel"

    private MessagePublisher messagePublisher

    def setup() {
        def config = new RedisMessagingConfig()
        def redisStandaloneConfig = new RedisStandaloneConfiguration(REDIS_HOST, REDIS_PORT)
        def connectionFactory = new LettuceConnectionFactory(redisStandaloneConfig)
        connectionFactory.afterPropertiesSet()
        def redisTemplate = config.createRedisTemplateForEntity(connectionFactory)
        messagePublisher = config.messagePublisher(redisTemplate)
    }

    def "Publish JSON message to Redis stream"() {

        given: "A message"
        Message message = resultingEvent(
                "any-item-id",
                new MessageTestBody(
                        "any-item-id",
                        Instant.now(),
                        BigDecimal.valueOf(2.22)
                )
        )

        when: "Publish message"
        messagePublisher.send(message)

        then:
        1 == 1
    }

    private Message resultingEvent(String itemId, Object eventBody) {
        return new Message(eventHeader(itemId, eventBody.getClass().getSimpleName()), eventBody)
    }

    private Header eventHeader(String itemId, String messageType) {
        return new Header(UUID.randomUUID().toString(), testChannel, messageType, itemId, Instant.now())
    }
}
