package io.wkrzywiec.fooddelivery.bff.inbox

import io.wkrzywiec.fooddelivery.bff.controller.model.AddTipDTO
import io.wkrzywiec.fooddelivery.commons.IntegrationTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import spock.lang.Subject

import java.util.concurrent.TimeUnit

import static org.testcontainers.shaded.org.awaitility.Awaitility.await

@Subject([RedisInboxPublisher, RedisInboxListener])
@ActiveProfiles("redis")
class RedisInboxIT extends IntegrationTest {

    @Autowired
    private RedisInboxPublisher redisInboxPublisher

    def "Store object in inbox"() {
        given:
        def addTip = new AddTipDTO("any-order-id", BigDecimal.valueOf(10))

        when:
        redisInboxPublisher.storeMessage("ordering-inbox:tip", addTip)

        then:
        await().atMost(5, TimeUnit.SECONDS)
                .until {
                    def event = redisStreamsClient.getLatestMessageFromStreamAsJson("orders")
                    event.get("header").get("messageId").asText() != null
                    event.get("header").get("channel").asText() == "orders"
                    event.get("header").get("type").asText() == "AddTip"
                    event.get("header").get("itemId").asText() == "any-order-id"
                    event.get("header").get("createdAt").asText() != null
                    event.get("body").get("orderId").asText() == "any-order-id"
                }
    }
}
