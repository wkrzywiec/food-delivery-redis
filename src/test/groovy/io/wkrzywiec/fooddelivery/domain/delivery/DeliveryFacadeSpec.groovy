package io.wkrzywiec.fooddelivery.domain.delivery

import com.fasterxml.jackson.databind.ObjectMapper
import io.wkrzywiec.fooddelivery.domain.delivery.incoming.AssignDeliveryMan
import io.wkrzywiec.fooddelivery.domain.delivery.incoming.PrepareFood
import io.wkrzywiec.fooddelivery.domain.delivery.outgoing.DeliveryCanceled
import io.wkrzywiec.fooddelivery.domain.delivery.outgoing.DeliveryCreated
import io.wkrzywiec.fooddelivery.domain.delivery.outgoing.DeliveryManAssigned
import io.wkrzywiec.fooddelivery.domain.delivery.outgoing.DeliveryProcessingError
import io.wkrzywiec.fooddelivery.domain.delivery.outgoing.FoodInPreparation
import io.wkrzywiec.fooddelivery.domain.ordering.outgoing.OrderCanceled
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

    def "Cancel a delivery"() {
        given:
        var delivery = aDelivery()
        repository.save(delivery.entity())

        and:
        var cancellationReason = "Not hungry anymore"
        var orderCanceled = new OrderCanceled(delivery.orderId, cancellationReason)

        when:
        facade.handle(orderCanceled)

        then: "Delivery is canceled"
        with(repository.database.values()[0]) { cancelledDelivery ->
            cancelledDelivery.status == DeliveryStatus.CANCELED
            cancelledDelivery.metadata.get("cancellationReason") == cancellationReason
            cancelledDelivery.metadata.get("cancellationTimestamp") == testTime.toString()
        }

        and: "DeliveryCancelled event is published on 'delivery' channel"
        with(publisher.messages.get("delivery").get(0)) {event ->

            verifyEventHeader(event, delivery.id, "DeliveryCanceled")

            def body = deserializeJson(event.body(), DeliveryCanceled)
            body.deliveryId() == delivery.id
            body.orderId() == delivery.orderId
            body.reason() == cancellationReason
        }
    }

    def "Fail to cancel a #status delivery"() {
        given:
        var delivery = aDelivery().withStatus(status)
        repository.save(delivery.entity())

        and:
        var cancelOrder = new OrderCanceled(delivery.orderId, "Not hungry anymore")

        when:
        facade.handle(cancelOrder)

        then: "Delivery is not canceled"
        with(repository.findById(delivery.id).get()) { cancelledOrder ->
            cancelledOrder.status == delivery.getStatus()
        }

        and: "DeliveryProcessingError event is published on 'delivery' channel"
        with(publisher.messages.get("delivery").get(0)) {event ->

            verifyEventHeader(event, delivery.id, "DeliveryProcessingError")

            def body = deserializeJson(event.body(), DeliveryProcessingError)
            body.id() == delivery.id
            body.details() == "Failed to cancel a $delivery.id delivery. It's not possible to cancel a delivery with '$status' status"
        }

        where:
        status << [DeliveryStatus.CANCELED, DeliveryStatus.FOOD_IN_PREPARATION, DeliveryStatus.FOOD_READY, DeliveryStatus.FOOD_PICKED, DeliveryStatus.FOOD_DELIVERED]
    }

    def "Food in preparation"() {
        given:
        var delivery = aDelivery()
        repository.save(delivery.entity())

        and:
        var prepareFood = new PrepareFood(delivery.id)

        when:
        facade.handle(prepareFood)

        then: "Delivery is set to food in preparation status"
        with(repository.database.values()[0]) { cancelledDelivery ->
            cancelledDelivery.status == DeliveryStatus.FOOD_IN_PREPARATION
            cancelledDelivery.metadata.get("startFoodPreparationTimestamp") == testTime.toString()
        }

        and: "FoodInPreparation event is published on 'delivery' channel"
        with(publisher.messages.get("delivery").get(0)) {event ->

            verifyEventHeader(event, delivery.id, "FoodInPreparation")

            def body = deserializeJson(event.body(), FoodInPreparation)
            body.deliveryId() == delivery.id
            body.orderId() == delivery.orderId
        }
    }

    def "Fail to set #status delivery to be in food preparation state"() {
        given:
        var delivery = aDelivery().withStatus(status)
        repository.save(delivery.entity())

        and:
        var prepareFood = new PrepareFood(delivery.id)

        when:
        facade.handle(prepareFood)

        then: "Delivery is not in food preparation state"
        with(repository.findById(delivery.id).get()) { cancelledOrder ->
            cancelledOrder.status == delivery.getStatus()
        }

        and: "DeliveryProcessingError event is published on 'delivery' channel"
        with(publisher.messages.get("delivery").get(0)) {event ->

            verifyEventHeader(event, delivery.id, "DeliveryProcessingError")

            def body = deserializeJson(event.body(), DeliveryProcessingError)
            body.id() == delivery.id
            body.details() == "Failed to start food preparation for a '$delivery.id' delivery. It's not possible to cancel a delivery with '$status' status"
        }

        where:
        status << [DeliveryStatus.CANCELED, DeliveryStatus.FOOD_IN_PREPARATION, DeliveryStatus.FOOD_READY, DeliveryStatus.FOOD_PICKED, DeliveryStatus.FOOD_DELIVERED]
    }

    def "Assign delivery man to delivery"() {
        given:
        var delivery = aDelivery().withDeliveryManId(null)
        repository.save(delivery.entity())

        and:
        var deliveryManId = "any-delivery-man-id"
        var assignDeliveryMan = new AssignDeliveryMan(delivery.id, deliveryManId)

        when:
        facade.handle(assignDeliveryMan)

        then: "Delivery man has been assigned"
        with(repository.database.values()[0]) { cancelledDelivery ->
            cancelledDelivery.deliveryManId == deliveryManId
            cancelledDelivery.metadata.get("assignDeliveryManTimestamp") == testTime.toString()
        }

        and: "DeliveryManAssigned event is published on 'delivery' channel"
        with(publisher.messages.get("delivery").get(0)) {event ->

            verifyEventHeader(event, delivery.id, "DeliveryManAssigned")

            def body = deserializeJson(event.body(), DeliveryManAssigned)
            body.deliveryId() == delivery.id
            body.deliveryManId() == deliveryManId
        }
    }

    def "Fail to assign delivery man for #status delivery"() {
        given:
        var delivery = aDelivery()
                .withStatus(status)
                .withDeliveryManId(null)
        repository.save(delivery.entity())

        and:
        var deliveryManId = "any-delivery-man-id"
        var assignDeliveryMan = new AssignDeliveryMan(delivery.id, deliveryManId)

        when:
        facade.handle(assignDeliveryMan)

        then: "Delivery man was not assigned"
        with(repository.findById(delivery.id).get()) { deliveryEntity ->
            deliveryEntity.deliveryManId == null
        }

        and: "DeliveryProcessingError event is published on 'delivery' channel"
        with(publisher.messages.get("delivery").get(0)) {event ->

            verifyEventHeader(event, delivery.id, "DeliveryProcessingError")

            def body = deserializeJson(event.body(), DeliveryProcessingError)
            body.id() == delivery.id
            body.details() == "Failed to assign a delivery man to a '$delivery.id' delivery. It's not possible do it for a delivery with '$status' status"
        }

        where:
        status << [DeliveryStatus.CANCELED, DeliveryStatus.FOOD_PICKED, DeliveryStatus.FOOD_DELIVERED]
    }

    def "Fail to assign delivery man if it's already assigned"() {
        given:
        def oldDeliveryManId = "old-delivery-man"
        def delivery = aDelivery()
                .withDeliveryManId(oldDeliveryManId)
        repository.save(delivery.entity())

        and:
        def deliveryManId = "any-delivery-man-id"
        def assignDeliveryMan = new AssignDeliveryMan(delivery.id, deliveryManId)

        when:
        facade.handle(assignDeliveryMan)

        then: "Delivery man has not been changed"
        with(repository.findById(delivery.id).get()) { deliveryEntity ->
            deliveryEntity.deliveryManId == oldDeliveryManId
        }

        and: "DeliveryProcessingError event is published on 'delivery' channel"
        with(publisher.messages.get("delivery").get(0)) {event ->

            verifyEventHeader(event, delivery.id, "DeliveryProcessingError")

            def body = deserializeJson(event.body(), DeliveryProcessingError)
            body.id() == delivery.id
            body.details() == "Failed to assign delivery man to a '$delivery.id' delivery. There is already a delivery man assigned with an id $oldDeliveryManId"
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
