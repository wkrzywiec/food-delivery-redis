package io.wkrzywiec.fooddelivery.domain.delivery

import com.fasterxml.jackson.databind.ObjectMapper
import io.wkrzywiec.fooddelivery.domain.delivery.outgoing.DeliveryCreated
import io.wkrzywiec.fooddelivery.domain.ordering.outgoing.OrderCreated
import io.wkrzywiec.fooddelivery.infra.messaging.FakeMessagePublisher
import io.wkrzywiec.fooddelivery.infra.messaging.Message
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.time.Clock
import java.time.Instant

import static io.wkrzywiec.fooddelivery.domain.delivery.DeliveryTestData.aDelivery
import static io.wkrzywiec.fooddelivery.domain.delivery.ItemTestData.anItem

@Subject(DeliveryFacade)
@Title("Specification for delivery process")
class DeliveryFacadeSpec extends Specification {

    DeliveryFacade facade
    InMemoryDeliveryRepository repository
    FakeMessagePublisher publisher

    var testTime = Instant.parse("2022-08-08T05:30:24.00Z")
    Clock testClock = Clock.fixed(testTime)

    def setup() {
        repository = new InMemoryDeliveryRepository()
        publisher = new FakeMessagePublisher()
        facade = new DeliveryFacade(repository, publisher, testClock)
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
                delivery.getItems().stream().map(i -> new io.wkrzywiec.fooddelivery.domain.ordering.incoming.Item(i.name, i.amount, i.pricePerItem)).toList(),
                delivery.getDeliveryCharge(), delivery.getTotal())

        when:
        facade.handle(orderCreated)

        then: "Delivery is saved"
        with(repository.database.values().find() as Delivery) { savedDelivery ->
            savedDelivery.id != null
            savedDelivery.orderId == delivery.getOrderId()
            savedDelivery.customerId == delivery.getCustomerId()
            savedDelivery.restaurantId == delivery.getRestaurantId()
            savedDelivery.deliveryManId == null
            savedDelivery.status == DeliveryStatus.CREATED
            savedDelivery.address == delivery.getAddress()
            savedDelivery.items == delivery.getItems().stream().map(ItemTestData::entity).toList()
            savedDelivery.deliveryCharge == delivery.getDeliveryCharge()
            savedDelivery.tip == 0
            savedDelivery.total == delivery.getTotal()
        }


        and: "DeliveryCreated event is published on 'delivery' channel"
        String deliveryId = repository.database.values().find().id
        with(publisher.messages.get("delivery").get(0)) {event ->

            verifyEventHeader(event, delivery.id, "DeliveryCreated")

            def body = deserializeJson(event.body(), DeliveryCreated)
            body.id() == deliveryId
            body.orderId() == delivery.getOrderId()
            body.customerId() == delivery.getCustomerId()
            body.restaurantId() == delivery.getRestaurantId()
            body.address() == delivery.getAddress()
            body.items() == delivery.getItems().stream().map(ItemTestData::dto).toList()
            body.deliveryCharge() == delivery.getDeliveryCharge()
            body.total() == delivery.getTotal()
        }
    }

    private void verifyEventHeader(Message event, String deliveryId, String eventType) {
        def header = event.header()
        header.messageId() != null
        header.channel() == "delivery"
        header.type() == eventType
        header.itemId() == deliveryId
        header.createdAt() == testClock.instant()
    }

    private <T> T deserializeJson(String json, Class<T> objectType) {
        ObjectMapper objectMapper = new ObjectMapper()
        return objectMapper.readValue(json, objectType)
    }
}
