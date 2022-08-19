package io.wkrzywiec.fooddelivery.ordering


import io.wkrzywiec.fooddelivery.commons.infra.messaging.FakeMessagePublisher
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message
import io.wkrzywiec.fooddelivery.commons.incoming.AddTip
import io.wkrzywiec.fooddelivery.commons.incoming.CancelOrder
import io.wkrzywiec.fooddelivery.commons.infra.repository.InMemoryEventStore
import io.wkrzywiec.fooddelivery.ordering.incoming.FoodDelivered
import io.wkrzywiec.fooddelivery.ordering.incoming.FoodInPreparation
import io.wkrzywiec.fooddelivery.ordering.outgoing.OrderCanceled
import io.wkrzywiec.fooddelivery.ordering.outgoing.OrderCompleted
import io.wkrzywiec.fooddelivery.ordering.outgoing.OrderCreated
import io.wkrzywiec.fooddelivery.ordering.outgoing.OrderInProgress
import io.wkrzywiec.fooddelivery.ordering.outgoing.TipAddedToOrder
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.time.Clock
import java.time.Instant

import static ItemTestData.anItem
import static OrderTestData.anOrder
import static io.wkrzywiec.fooddelivery.commons.infra.messaging.Message.message

@Subject(OrderingFacade)
@Title("Specification for ordering process")
class OrderingFacadeSpec extends Specification {

    private final String ORDERS_CHANNEL = "orders"

    OrderingFacade facade
    InMemoryEventStore eventStore
    FakeMessagePublisher publisher

    var testTime = Instant.parse("2022-08-08T05:30:24.00Z")
    Clock testClock = Clock.fixed(testTime)

    def setup() {
        eventStore = new InMemoryEventStore()
        publisher = new FakeMessagePublisher()
        facade = new OrderingFacade(eventStore, publisher, testClock)
    }

    def "Create an order"() {
        given:
        var order = anOrder()
                .withItems(
                        anItem().withName("Pizza").withPricePerItem(2.5),
                        anItem().withName("Spaghetti").withPricePerItem(3.0)
                )

        when:
        facade.handle(order.createOrder())

        then: "Event is saved in a store"
        def expectedEvent = order.orderCreated()
        def storedEvents = eventStore.getEventsForOrder(order.getId())
        storedEvents.size() == 1
        storedEvents[0] == expectedEvent

        and:
        Order.from(storedEvents).getStatus() == OrderStatus.CREATED

        and: "OrderCreated event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, order.id, "OrderCreated")

            def body = event.body() as OrderCreated
            body == expectedEvent
        }
    }

    def "Cancel an order"() {
        given:
        var order = anOrder()
        eventStore.store(message("order", testClock, order.orderCreated()))

        and:
        var cancellationReason = "Not hungry anymore"
        var cancelOrder = new CancelOrder(order.id, cancellationReason)

        when:
        facade.handle(cancelOrder)

        then: "Event is saved in a store"
        def expectedEvent = new OrderCanceled(order.getId(), cancellationReason)
        def storedEvents = eventStore.getEventsForOrder(order.getId())
        storedEvents.size() == 2
        storedEvents[1] == expectedEvent

        and:
        Order.from(storedEvents).getStatus() == OrderStatus.CANCELED

        and: "OrderCancelled event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, order.id, "OrderCancelled")
            def body = event.body() as OrderCanceled
            body == expectedEvent
        }
    }

    def "Set order to IN_PROGRESS"() {
        given:
        var order = anOrder()
        eventStore.store(message("order", testClock, order.orderCreated()))

        and:
        var foodInPreparation = new FoodInPreparation(order.id)

        when:
        facade.handle(foodInPreparation)

        then: "Event is saved in a store"
        def expectedEvent = new OrderInProgress(order.getId())
        def storedEvents = eventStore.getEventsForOrder(order.getId())
        storedEvents.size() == 2
        storedEvents[1] == expectedEvent

        and:
        Order.from(storedEvents).getStatus() == OrderStatus.IN_PROGRESS

        and: "OrderInProgress event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, order.id, "OrderInProgress")

            def body = event.body() as OrderInProgress
            body == expectedEvent
        }
    }

    def "Add tip to an order"() {
        given:
        double itemCost = 10
        double deliveryCharge = 5

        var order = anOrder()
                .withItems(anItem().withPricePerItem(itemCost))
                .withDeliveryCharge(deliveryCharge)
        eventStore.store(message("order", testClock, order.orderCreated()))

        and:
        double tip = 20
        var addTip = new AddTip(order.id, new BigDecimal(tip))

        when:
        facade.handle(addTip)

        then: "Tip was added"
        double total = itemCost + deliveryCharge + tip
        def storedEvents = eventStore.getEventsForOrder(order.getId())
        storedEvents.size() == 2
        def tipAdded = storedEvents[1] as TipAddedToOrder
        tipAdded.tip().doubleValue() == tip
        tipAdded.total().doubleValue() == total

        and:
        Order.from(storedEvents).getTotal().doubleValue() == total

        and: "TipAddedToOrder event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, order.id, "TipAddedToOrder")

            def body = event.body() as TipAddedToOrder
            body.orderId() == order.id
            body.tip().doubleValue() == tip
            body.total().doubleValue() == total
        }
    }

    def "Complete an order"() {
        given:
        var order = anOrder()
        eventStore.store(message("order", testClock, order.orderCreated()))
        eventStore.store(message("order", testClock, new OrderInProgress(order.getId())))

        and:
        var foodDelivered = new FoodDelivered(order.id)

        when:
        facade.handle(foodDelivered)

        then: "Order is completed"
        def expectedEvent = new OrderCompleted(order.getId())
        def storedEvents = eventStore.getEventsForOrder(order.getId())
        storedEvents.size() == 3
        storedEvents[2] == expectedEvent

        and:
        Order.from(storedEvents).getStatus() == OrderStatus.COMPLETED

        and: "OrderCompleted event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, order.id, "OrderCompleted")

            def body = event.body() as OrderCompleted
            body.orderId() == order.id
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
