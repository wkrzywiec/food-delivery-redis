package io.wkrzywiec.fooddelivery.domain.ordering

import com.fasterxml.jackson.databind.ObjectMapper
import io.wkrzywiec.fooddelivery.domain.ordering.incoming.CancelOrder
import io.wkrzywiec.fooddelivery.domain.ordering.outgoing.OrderCanceled
import io.wkrzywiec.fooddelivery.domain.ordering.outgoing.OrderCreated
import io.wkrzywiec.fooddelivery.domain.ordering.outgoing.OrderProcessingError
import io.wkrzywiec.fooddelivery.infra.messaging.FakeMessagePublisher
import spock.lang.Specification
import spock.lang.Subject

import java.time.Clock
import java.time.Instant

import static io.wkrzywiec.fooddelivery.domain.ordering.ItemTestData.anItem
import static io.wkrzywiec.fooddelivery.domain.ordering.OrderTestData.anOrder

@Subject(OrderingFacade)
class OrderingFacadeSpec extends Specification {

    OrderingFacade facade
    InMemoryOrderingRepository repository
    FakeMessagePublisher publisher

    var testTime = Instant.parse("2022-08-08T05:30:24.00Z")
    Clock testClock = Clock.fixed(testTime)

    def setup() {
        repository = new InMemoryOrderingRepository()
        publisher = new FakeMessagePublisher()
        facade = new OrderingFacade(repository, publisher, testClock)
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

        then: "Order is saved"
        with(repository.database.values().find() as Order) { savedOrder ->
            savedOrder.id != null
            savedOrder.customerId == order.getCustomerId()
            savedOrder.restaurantId == order.getRestaurantId()
            savedOrder.deliveryManId == null
            savedOrder.address == order.getAddress()
            savedOrder.items == order.getItems().stream().map(ItemTestData::entity).toList()
            savedOrder.status == OrderStatus.CREATED
            savedOrder.deliveryCharge == order.getDeliveryCharge()
            savedOrder.tip == 0
            savedOrder.total == 5.5 + order.getDeliveryCharge()
        }


        and: "OrderCreated event is published on 'ordering' channel"
        String orderId = repository.database.values().find().id
        with(publisher.messages.get("ordering")) {events ->
            events.size() == 1
            def event = events.get(0)

            def header = event.header()
            header.messageId() != null
            header.channel() == "ordering"
            header.itemId() == orderId
            header.createdAt() == testClock.instant()

            def body = deserializeJson(event.body(), OrderCreated)
            body.id() == orderId
            body.customerId() == order.getCustomerId()
            body.restaurantId() == order.getRestaurantId()
            body.address() == order.getAddress()
            body.items() == order.getItems().stream().map(ItemTestData::dto).toList()
            body.deliveryCharge() == order.getDeliveryCharge()
            body.total() == 5.5 + order.getDeliveryCharge()
        }
    }

    def "Cancel an order"() {
        given:
        var order = anOrder()
        repository.save(order.entity())

        and:
        var cancellationReason = "Not hungry anymore"
        var cancelOrder = new CancelOrder(order.id, cancellationReason)

        when:
        facade.handle(cancelOrder)

        then: "Order is canceled"
        with(repository.findById(order.id).get()) { cancelledOrder ->
            cancelledOrder.status == OrderStatus.CANCELLED
            cancelledOrder.metadata.get("cancellationReason") == cancellationReason
        }

        and: "OrderCancelled event is published on 'ordering' channel"
        with(publisher.messages.get("ordering")) {events ->
            events.size() == 1
            def event = events.get(0)

            def header = event.header()
            header.messageId() != null
            header.channel() == "ordering"
            header.itemId() == order.id
            header.createdAt() == testClock.instant()

            def body = deserializeJson(event.body(), OrderCanceled)
            body.id() == order.id
            body.reason() == cancellationReason
        }
    }

    def "Fail to cancel a #status order"() {
        given:
        var order = anOrder().withStatus(status)
        repository.save(order.entity())

        and:
        var cancelOrder = new CancelOrder(order.id, "Not hungry anymore")

        when:
        facade.handle(cancelOrder)

        then: "Order is not canceled"
        with(repository.findById(order.id).get()) { cancelledOrder ->
            cancelledOrder.status == order.getStatus()
        }

        and: "OrderProcessingError event is published on 'ordering' channel"
        with(publisher.messages.get("ordering")) {events ->
            events.size() == 1
            def event = events.get(0)

            def header = event.header()
            header.messageId() != null
            header.channel() == "ordering"
            header.type() == "OrderProcessingError"
            header.itemId() == order.id
            header.createdAt() == testClock.instant()

            def body = deserializeJson(event.body(), OrderProcessingError)
            body.id() == order.id
            body.details() == "Failed to cancel an $order.id order. It's not possible to cancel an order with '$status' status"
        }

        where:
        status << [OrderStatus.IN_PROGRESS, OrderStatus.COMPLETED, OrderStatus.CANCELLED]
    }

     private <T> T deserializeJson(String json, Class<T> objectType) {
        ObjectMapper objectMapper = new ObjectMapper()
        return objectMapper.readValue(json, objectType)
    }
}
