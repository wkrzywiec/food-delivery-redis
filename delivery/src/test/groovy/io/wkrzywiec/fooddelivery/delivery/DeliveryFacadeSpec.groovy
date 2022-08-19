package io.wkrzywiec.fooddelivery.delivery


import io.wkrzywiec.fooddelivery.commons.incoming.AssignDeliveryMan
import io.wkrzywiec.fooddelivery.commons.infra.repository.InMemoryEventStore
import io.wkrzywiec.fooddelivery.delivery.incoming.Item
import io.wkrzywiec.fooddelivery.commons.incoming.DeliverFood
import io.wkrzywiec.fooddelivery.commons.incoming.FoodReady
import io.wkrzywiec.fooddelivery.delivery.incoming.OrderCanceled
import io.wkrzywiec.fooddelivery.delivery.incoming.OrderCreated
import io.wkrzywiec.fooddelivery.commons.incoming.PickUpFood
import io.wkrzywiec.fooddelivery.commons.incoming.PrepareFood
import io.wkrzywiec.fooddelivery.commons.incoming.UnAssignDeliveryMan
import io.wkrzywiec.fooddelivery.delivery.outgoing.DeliveryCanceled
import io.wkrzywiec.fooddelivery.delivery.outgoing.DeliveryCreated
import io.wkrzywiec.fooddelivery.delivery.outgoing.DeliveryManAssigned
import io.wkrzywiec.fooddelivery.delivery.outgoing.DeliveryManUnAssigned
import io.wkrzywiec.fooddelivery.delivery.outgoing.DeliveryProcessingError
import io.wkrzywiec.fooddelivery.delivery.outgoing.FoodDelivered
import io.wkrzywiec.fooddelivery.delivery.outgoing.FoodInPreparation
import io.wkrzywiec.fooddelivery.delivery.outgoing.FoodWasPickedUp
import io.wkrzywiec.fooddelivery.delivery.outgoing.FoodIsReady
import io.wkrzywiec.fooddelivery.commons.infra.messaging.FakeMessagePublisher
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.time.Clock
import java.time.Instant

import static DeliveryTestData.aDelivery
import static ItemTestData.anItem
import static io.wkrzywiec.fooddelivery.commons.infra.messaging.Message.message

@Subject(DeliveryFacade)
@Title("Specification for delivery process")
class DeliveryFacadeSpec extends Specification {

    private final String ORDERS_CHANNEL = "orders"

    DeliveryFacade facade
    InMemoryEventStore eventStore
    FakeMessagePublisher publisher

    var testTime = Instant.parse("2022-08-08T05:30:24.00Z")
    Clock testClock = Clock.fixed(testTime)

    def setup() {
        eventStore = new InMemoryEventStore()
        publisher = new FakeMessagePublisher()
        facade = new DeliveryFacade(eventStore, publisher, testClock)
    }

    def "Create a delivery"() {
        given:
        var delivery = aDelivery()
                .withItems(
                        anItem().withName("Pizza").withPricePerItem(2.5),
                        anItem().withName("Spaghetti").withPricePerItem(3.0)
                )

        var orderCreated = new OrderCreated(
                delivery.getOrderId(), delivery.getCustomerId(),
                delivery.getRestaurantId(), delivery.getAddress(),
                delivery.getItems().stream().map(i -> new Item(i.name, i.amount, i.pricePerItem)).toList(),
                delivery.getDeliveryCharge(), delivery.getTotal())

        when:
        facade.handle(orderCreated)

        then: "Event is saved in a store"
        def expectedEvent = delivery.deliveryCreated()
        def storedEvents = eventStore.getEventsForOrder(delivery.getOrderId())
        storedEvents.size() == 1
        storedEvents[0].body() == expectedEvent

        and:
        Delivery.from(storedEvents).getStatus() == DeliveryStatus.CREATED

        and: "DeliveryCreated event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "DeliveryCreated")

            def body = event.body() as DeliveryCreated
            body == expectedEvent
        }
    }

    def "Cancel a delivery"() {
        given:
        var delivery = aDelivery()
        eventStore.store(message("orders", testClock, delivery.deliveryCreated()))

        and:
        var cancellationReason = "Not hungry anymore"
        var orderCanceled = new OrderCanceled(delivery.orderId, cancellationReason)

        when:
        facade.handle(orderCanceled)

        then: "Event is saved in a store"
        def expectedEvent = new DeliveryCanceled(delivery.getOrderId(), "Not hungry anymore")
        def storedEvents = eventStore.getEventsForOrder(delivery.getOrderId())
        storedEvents.size() == 2
        storedEvents[1].body() == expectedEvent

        and:
        Delivery.from(storedEvents).getStatus() == DeliveryStatus.CANCELED

        and: "DeliveryCancelled event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "DeliveryCanceled")

            def body = event.body() as DeliveryCanceled
            body == expectedEvent
        }
    }

    def "Food in preparation"() {
        given:
        var delivery = aDelivery()
        eventStore.store(message("orders", testClock, delivery.deliveryCreated()))

        and:
        var prepareFood = new PrepareFood(delivery.orderId)

        when:
        facade.handle(prepareFood)

        then: "Event is saved in a store"
        def expectedEvent = new FoodInPreparation(delivery.getOrderId())
        def storedEvents = eventStore.getEventsForOrder(delivery.getOrderId())
        storedEvents.size() == 2
        storedEvents[1].body() == expectedEvent

        and:
        Delivery.from(storedEvents).getStatus() == DeliveryStatus.FOOD_IN_PREPARATION

        and: "FoodInPreparation event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "FoodInPreparation")

            def body = event.body() as FoodInPreparation
            body == expectedEvent
        }
    }

    def "Assign delivery man to delivery"() {
        given:
        var delivery = aDelivery().withDeliveryManId(null)
        eventStore.store(message("orders", testClock, delivery.deliveryCreated()))

        and:
        var deliveryManId = "any-delivery-man-orderId"
        var assignDeliveryMan = new AssignDeliveryMan(delivery.orderId, deliveryManId)

        when:
        facade.handle(assignDeliveryMan)

        then: "Event is saved in a store"
        def expectedEvent = new DeliveryManAssigned(delivery.getOrderId(), deliveryManId)
        def storedEvents = eventStore.getEventsForOrder(delivery.getOrderId())
        storedEvents.size() == 2
        storedEvents[1].body() == expectedEvent

        and: "DeliveryManAssigned event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "DeliveryManAssigned")

            def body = event.body() as DeliveryManAssigned
            body == expectedEvent
        }
    }

    def "Un assign delivery man from delivery"() {
        given:
        var deliveryManId = "any-delivery-man-orderId"
        var delivery = aDelivery()
        eventStore.store(message("orders", testClock, delivery.deliveryCreated()))
        eventStore.store(message("orders", testClock, new DeliveryManAssigned(delivery.getOrderId(), deliveryManId)))

        and:
        var assignDeliveryMan = new UnAssignDeliveryMan(delivery.orderId)

        when:
        facade.handle(assignDeliveryMan)

        then: "Event is saved in a store"
        def expectedEvent = new DeliveryManUnAssigned(delivery.getOrderId(), deliveryManId)
        def storedEvents = eventStore.getEventsForOrder(delivery.getOrderId())
        storedEvents.size() == 3
        storedEvents[2].body() == expectedEvent


        and: "DeliveryManUnAssigned event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "DeliveryManUnAssigned")

            def body = event.body() as DeliveryManUnAssigned
            body == expectedEvent
        }
    }

    def "Food is ready"() {
        given:
        var delivery = aDelivery()
        eventStore.store(message("orders", testClock, delivery.deliveryCreated()))
        eventStore.store(message("orders", testClock, new FoodInPreparation(delivery.getOrderId())))

        and:
        var foodReady = new FoodReady(delivery.orderId)

        when:
        facade.handle(foodReady)

        then: "Event is saved in a store"
        def expectedEvent = new FoodIsReady(delivery.getOrderId())
        def storedEvents = eventStore.getEventsForOrder(delivery.getOrderId())
        storedEvents.size() == 3
        storedEvents[2].body() == expectedEvent

        and: "FoodIsRead event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "FoodIsRead")

            def body = event.body() as FoodIsReady
            body == expectedEvent
        }
    }

    def "Food is picked up"() {
        given:
        var delivery = aDelivery()
        eventStore.store(message("orders", testClock, delivery.deliveryCreated()))
        eventStore.store(message("orders", testClock, new FoodInPreparation(delivery.getOrderId())))
        eventStore.store(message("orders", testClock, new FoodIsReady(delivery.getOrderId())))

        and:
        var pickUpFood = new PickUpFood(delivery.orderId)

        when:
        facade.handle(pickUpFood)

        then: "Event is saved in a store"
        def expectedEvent = new FoodWasPickedUp(delivery.getOrderId())
        def storedEvents = eventStore.getEventsForOrder(delivery.getOrderId())
        storedEvents.size() == 4
        storedEvents[3].body() == expectedEvent

        and: "FoodIsPickedUp event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "FoodWasPickedUp")

            def body = event.body() as FoodWasPickedUp
            body == expectedEvent
        }
    }

    def "Food is delivered"() {
        given:
        var delivery = aDelivery()
                .withStatus(DeliveryStatus.FOOD_PICKED)
        eventStore.store(message("orders", testClock, delivery.deliveryCreated()))
        eventStore.store(message("orders", testClock, new FoodInPreparation(delivery.getOrderId())))
        eventStore.store(message("orders", testClock, new FoodIsReady(delivery.getOrderId())))
        eventStore.store(message("orders", testClock, new FoodWasPickedUp(delivery.getOrderId())))

        and:
        var deliverFood = new DeliverFood(delivery.orderId)

        when:
        facade.handle(deliverFood)

        then: "Event is saved in a store"
        def expectedEvent = new FoodDelivered(delivery.getOrderId())
        def storedEvents = eventStore.getEventsForOrder(delivery.getOrderId())
        storedEvents.size() == 5
        storedEvents[4].body() == expectedEvent

        and: "FoodDelivered event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "FoodDelivered")

            def body = event.body() as FoodDelivered
            body == expectedEvent
        }
    }

    private void verifyEventHeader(Message event, String orderId, String eventType) {
        def header = event.header()
        header.messageId() != null
        header.channel() == ORDERS_CHANNEL
        header.type() == eventType
        header.itemId() == orderId
        header.createdAt() == testClock.instant()
    }
}
