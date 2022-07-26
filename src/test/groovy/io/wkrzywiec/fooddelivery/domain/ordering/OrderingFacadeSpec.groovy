package io.wkrzywiec.fooddelivery.domain.ordering

import com.fasterxml.jackson.databind.ObjectMapper
import io.wkrzywiec.fooddelivery.domain.ordering.outgoing.OrderCreated
import io.wkrzywiec.fooddelivery.infra.messaging.FakeMessagePublisher
import spock.lang.Specification
import spock.lang.Subject

import java.time.Clock

import static io.wkrzywiec.fooddelivery.domain.ordering.ItemTestData.anItem
import static io.wkrzywiec.fooddelivery.domain.ordering.OrderTestData.anOrder

@Subject(OrderingFacade)
class OrderingFacadeSpec extends Specification {

    OrderingFacade facade
    InMemoryOrderingRepository repository
    FakeMessagePublisher publisher

    def setup() {
        repository = new InMemoryOrderingRepository()
        publisher = new FakeMessagePublisher()
        facade = new OrderingFacade(repository, publisher, Clock.systemUTC())
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
            header.createdAt() != null

            def body = deserializeMessage(event.body(), OrderCreated) as OrderCreated
            body.id() == orderId
            body.customerId() == order.getCustomerId()
            body.restaurantId() == order.getRestaurantId()
            body.address() == order.getAddress()
            body.items() == order.getItems().stream().map(ItemTestData::dto).toList()
            body.deliveryCharge() == order.getDeliveryCharge()
            body.total() == 5.5 + order.getDeliveryCharge()
        }

    }

     private Object deserializeMessage(String json, Class objectType) {
        ObjectMapper objectMapper = new ObjectMapper()
        return objectMapper.readValue(json, objectType)
    }
}
