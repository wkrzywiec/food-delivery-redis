package io.wkrzywiec.fooddelivery.commons.messaging


import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message
import io.wkrzywiec.fooddelivery.commons.infra.messaging.MessagePublisher
import io.wkrzywiec.fooddelivery.commons.infra.messaging.redis.RedisMessagePublisherConfig
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory

import java.time.Instant

class RedisMessagePublisherIT extends CommonsIntegrationTest {

    private final String testChannel = "testing-channel"

    private MessagePublisher messagePublisher
    private RedisStreamTestClient redis

    def setup() {
        def config = new RedisMessagePublisherConfig()
        def redisStandaloneConfig = new RedisStandaloneConfiguration(REDIS_HOST, REDIS_PORT)
        def connectionFactory = new LettuceConnectionFactory(redisStandaloneConfig)
        connectionFactory.afterPropertiesSet()
        def redisTemplate = config.createRedisTemplateForEntity(connectionFactory)
        messagePublisher = config.messagePublisher(redisTemplate)

        redis = new RedisStreamTestClient(redisTemplate)

        System.out.println("Clearing '$testChannel' stream from old messages")
        redisTemplate.opsForStream().trim(testChannel, 0)
    }

    def "Publish JSON message to Redis stream"() {
        given: "A message"
        String itemId = UUID.randomUUID()
        Message message = resultingEvent(
                itemId,
                new MessageTestBody(
                        itemId,
                        Instant.now(),
                        BigDecimal.valueOf(2.22)
                )
        )

        when: "Publish message"
        messagePublisher.send(message)

        then: "Message was published on $testChannel redis stream"
        def publishedMessage = redis.getLatestMessageFromStreamAsJson(testChannel)

        publishedMessage.get("header").get("itemId").asText() == itemId
        publishedMessage.get("header").get("type").asText() == "MessageTestBody"
        publishedMessage.get("body").get("id").asText() == itemId
    }

    private Message resultingEvent(String itemId, Object eventBody) {
        return new Message(eventHeader(itemId, eventBody.getClass().getSimpleName()), eventBody)
    }

    private Header eventHeader(String itemId, String messageType) {
        return new Header(UUID.randomUUID().toString(), testChannel, messageType, itemId, Instant.now())
    }
}
