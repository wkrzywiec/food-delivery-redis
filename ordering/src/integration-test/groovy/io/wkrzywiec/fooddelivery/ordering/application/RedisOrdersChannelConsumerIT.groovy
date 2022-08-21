package io.wkrzywiec.fooddelivery.ordering.application

import com.github.javafaker.Faker
import io.wkrzywiec.fooddelivery.commons.IntegrationTest
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message
import org.springframework.test.context.ActiveProfiles
import spock.lang.Subject

import java.time.Instant
import java.util.concurrent.TimeUnit

import static io.wkrzywiec.fooddelivery.ordering.ItemTestData.anItem
import static io.wkrzywiec.fooddelivery.ordering.OrderTestData.anOrder
import static org.testcontainers.shaded.org.awaitility.Awaitility.await

@ActiveProfiles("redis")
@Subject(RedisOrdersChannelConsumer)
class RedisOrdersChannelConsumerIT extends IntegrationTest {

    def "Message is consumed correctly"() {
        given:
        Faker faker = new Faker()
        var order = anOrder()
                .withItems(
                        anItem().withName(faker.food().dish()).withPricePerItem(2.5),
                        anItem().withName(faker.food().dish()).withPricePerItem(3.0)
                )
                .withAddress(faker.address().fullAddress())

        def body = order.createOrder()
        def header = new Header(UUID.randomUUID().toString(), "orders", body.getClass().getSimpleName(), order.id, Instant.now())
        def message = new Message(header, body)

        when:
        redisStreamsClient.publishMessage(message)

        then:
        await().atMost(5, TimeUnit.SECONDS)
                .until {
                    def event = redisStreamsClient.getLatestMessageFromStreamAsJson("orders")

                    event.get("header").get("itemId").asText() == order.id
                    event.get("header").get("type").asText() == "OrderCreated"
                }
    }
}
