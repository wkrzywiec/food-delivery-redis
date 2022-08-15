package io.wkrzywiec.fooddelivery.delivery.infra

import com.github.javafaker.Faker
import io.wkrzywiec.fooddelivery.commons.IntegrationTest
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message
import io.wkrzywiec.fooddelivery.delivery.incoming.Item
import io.wkrzywiec.fooddelivery.delivery.incoming.OrderCreated
import org.springframework.test.context.ActiveProfiles
import spock.lang.Subject

import java.time.Instant
import java.util.concurrent.TimeUnit

import static io.wkrzywiec.fooddelivery.delivery.DeliveryTestData.aDelivery
import static io.wkrzywiec.fooddelivery.delivery.ItemTestData.anItem
import static org.testcontainers.shaded.org.awaitility.Awaitility.await

@ActiveProfiles("redis")
@Subject(RedisOrdersChannelConsumer)
class RedisOrdersChannelConsumerIT extends IntegrationTest {

    def "Message is consumed correctly"() {
        given:
        Faker faker = new Faker()
        var delivery = aDelivery()
                .withItems(
                        anItem().withName(faker.food().dish()).withPricePerItem(2.5),
                        anItem().withName(faker.food().dish()).withPricePerItem(3.0)
                )
                .withAddress(faker.address().fullAddress())

        def body = new OrderCreated(
                delivery.getOrderId(), delivery.getCustomerId(), delivery.getRestaurantId(), delivery.getAddress(),
                delivery.getItems().stream().map(i -> new Item(i.getName(), i.getAmount(), i.getPricePerItem())).toList(),
                delivery.getDeliveryCharge(), delivery.getTotal())

        def header = new Header(UUID.randomUUID().toString(), "orders", body.getClass().getSimpleName(), delivery.orderId, Instant.now())
        def message = new Message(header, body)

        when:
        redisStreamsClient.publishMessage(message)

        then:
        await().atMost(5, TimeUnit.SECONDS)
                .until {
                    def event = redisStreamsClient.getLatestMessageFromStreamAsJson("orders")

                    event.get("header").get("itemId").asText() == delivery.orderId
                    event.get("header").get("type").asText() == "DeliveryCreated"
                }
    }
}
