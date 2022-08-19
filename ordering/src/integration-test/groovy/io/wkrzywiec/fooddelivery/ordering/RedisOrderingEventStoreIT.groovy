package io.wkrzywiec.fooddelivery.ordering

import io.wkrzywiec.fooddelivery.commons.IntegrationTest
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message
import io.wkrzywiec.fooddelivery.commons.infra.repository.RedisEventStore
import io.wkrzywiec.fooddelivery.ordering.outgoing.OrderCompleted
import io.wkrzywiec.fooddelivery.ordering.outgoing.OrderInProgress
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import spock.lang.Subject

import java.time.Instant

@Subject(RedisOrderingEventStore)
@ActiveProfiles("redis")
class RedisOrderingEventStoreIT extends IntegrationTest {

    @Autowired
    private RedisEventStore eventStore

    def "Store an event in event store"() {
        given:
        def eventBody = new OrderInProgress("any-id")
        def eventHeader = new Header(UUID.randomUUID().toString(), "orders", eventBody.getClass().getSimpleName(), eventBody.orderId(), Instant.now())

        when:
        eventStore.store(new Message(eventHeader, eventBody))

        then:
        var savedEvents = redisStreamsClient.getLatestMessageFromStreamAsJson("ordering::any-id")
        savedEvents.get("header").get("type").asText() == "OrderInProgress"
        savedEvents.get("body").get("orderId").asText() == "any-id"
    }

    def "Get all events from event store"() {
        given:
        def orderId = "any-id"

        and: "First Event"
        def firstEventBody = new OrderInProgress("any-id")
        def firstEventHeader = new Header(UUID.randomUUID().toString(), "orders", firstEventBody.getClass().getSimpleName(), firstEventBody.orderId(), Instant.now())
        def firstEvent = new Message(firstEventHeader, firstEventBody)

        and: "Second Event"
        def secondEventBody = new OrderCompleted("any-id")
        def secondEventHeader = new Header(UUID.randomUUID().toString(), "orders", secondEventBody.getClass().getSimpleName(), secondEventBody.orderId(), Instant.now())
        def secondEvent = new Message(secondEventHeader, secondEventBody)

        and: "Both events are stored"
        redisStreamsClient.publishMessage("ordering::any-id", firstEvent)
        redisStreamsClient.publishMessage("ordering::any-id", secondEvent)

        when:
        def storedEvents = eventStore.getEventsForOrder(orderId)

        then:
        storedEvents.size() == 2
    }
}
