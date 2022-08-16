package io.wkrzywiec.fooddelivery.delivery


import io.wkrzywiec.fooddelivery.commons.incoming.AssignDeliveryMan
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

@Subject(DeliveryFacade)
@Title("Specification for delivery process")
class DeliveryFacadeSpec extends Specification {

    private final String ORDERS_CHANNEL = "orders"

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
                delivery.getItems().stream().map(i -> new Item(i.name, i.amount, i.pricePerItem)).toList(),
                delivery.getDeliveryCharge(), delivery.getTotal())

        when:
        facade.handle(orderCreated)

        then: "Delivery is saved"
        with(repository.database.values().find() as Delivery) { savedDelivery ->
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


        and: "DeliveryCreated event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "DeliveryCreated")

            def body = event.body() as DeliveryCreated
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

        and: "DeliveryCancelled event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "DeliveryCanceled")

            def body = event.body() as DeliveryCanceled
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
        with(repository.findByOrderId(delivery.orderId).get()) { cancelledOrder ->
            cancelledOrder.status == delivery.getStatus()
        }

        and: "DeliveryProcessingError event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "DeliveryProcessingError")

            def body = event.body() as DeliveryProcessingError
            body.orderId() == delivery.orderId
            body.details() == "Failed to cancel a $delivery.orderId delivery. It's not possible do it for a delivery with '$status' status"
        }

        where:
        status << [DeliveryStatus.CANCELED, DeliveryStatus.FOOD_IN_PREPARATION, DeliveryStatus.FOOD_READY, DeliveryStatus.FOOD_PICKED, DeliveryStatus.FOOD_DELIVERED]
    }

    def "Food in preparation"() {
        given:
        var delivery = aDelivery()
        repository.save(delivery.entity())

        and:
        var prepareFood = new PrepareFood(delivery.orderId)

        when:
        facade.handle(prepareFood)

        then: "Delivery is set to food in preparation status"
        with(repository.database.values()[0]) { deliveryEntity ->
            deliveryEntity.status == DeliveryStatus.FOOD_IN_PREPARATION
            deliveryEntity.metadata.get("foodPreparationTimestamp") == testTime.toString()
        }

        and: "FoodInPreparation event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "FoodInPreparation")

            def body = event.body() as FoodInPreparation
            body.orderId() == delivery.orderId
        }
    }

    def "Fail to set #status delivery to be in food preparation state"() {
        given:
        var delivery = aDelivery().withStatus(status)
        repository.save(delivery.entity())

        and:
        var prepareFood = new PrepareFood(delivery.orderId)

        when:
        facade.handle(prepareFood)

        then: "Delivery is not in food preparation state"
        with(repository.findByOrderId(delivery.orderId).get()) { deliveryEntity ->
            deliveryEntity.status == delivery.getStatus()
        }

        and: "DeliveryProcessingError event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "DeliveryProcessingError")

            def body = event.body() as DeliveryProcessingError
            body.orderId() == delivery.orderId
            body.details() == "Failed to start food preparation for an '$delivery.orderId' order. It's not possible do it for a delivery with '$status' status"
        }

        where:
        status << [DeliveryStatus.CANCELED, DeliveryStatus.FOOD_IN_PREPARATION, DeliveryStatus.FOOD_READY, DeliveryStatus.FOOD_PICKED, DeliveryStatus.FOOD_DELIVERED]
    }

    def "Assign delivery man to delivery"() {
        given:
        var delivery = aDelivery().withDeliveryManId(null)
        repository.save(delivery.entity())

        and:
        var deliveryManId = "any-delivery-man-orderId"
        var assignDeliveryMan = new AssignDeliveryMan(delivery.orderId, deliveryManId)

        when:
        facade.handle(assignDeliveryMan)

        then: "Delivery man has been assigned"
        with(repository.database.values()[0]) { cancelledDelivery ->
            cancelledDelivery.deliveryManId == deliveryManId
        }

        and: "DeliveryManAssigned event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "DeliveryManAssigned")

            def body = event.body() as DeliveryManAssigned
            body.orderId() == delivery.orderId
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
        var deliveryManId = "any-delivery-man-orderId"
        var assignDeliveryMan = new AssignDeliveryMan(delivery.orderId, deliveryManId)

        when:
        facade.handle(assignDeliveryMan)

        then: "Delivery man was not assigned"
        with(repository.findByOrderId(delivery.orderId).get()) { deliveryEntity ->
            deliveryEntity.deliveryManId == null
        }

        and: "DeliveryProcessingError event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "DeliveryProcessingError")

            def body = event.body() as DeliveryProcessingError
            body.orderId() == delivery.orderId
            body.details() == "Failed to assign a delivery man to an '$delivery.orderId' order. It's not possible do it for a delivery with '$status' status"
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
        def deliveryManId = "any-delivery-man-orderId"
        def assignDeliveryMan = new AssignDeliveryMan(delivery.orderId, deliveryManId)

        when:
        facade.handle(assignDeliveryMan)

        then: "Delivery man has not been changed"
        with(repository.findByOrderId(delivery.orderId).get()) { deliveryEntity ->
            deliveryEntity.deliveryManId == oldDeliveryManId
        }

        and: "DeliveryProcessingError event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "DeliveryProcessingError")

            def body = event.body() as DeliveryProcessingError
            body.orderId() == delivery.orderId
            body.details() == "Failed to assign delivery man to an '$delivery.orderId' order. There is already a delivery man assigned with an orderId $oldDeliveryManId"
        }
    }

    def "Un assign delivery man from delivery"() {
        given:
        var deliveryManId = "any-delivery-man-orderId"
        var delivery = aDelivery().withDeliveryManId(deliveryManId)
        repository.save(delivery.entity())

        and:
        var assignDeliveryMan = new UnAssignDeliveryMan(delivery.orderId, deliveryManId)

        when:
        facade.handle(assignDeliveryMan)

        then: "Delivery man has been un assigned"
        with(repository.database.values()[0]) { cancelledDelivery ->
            cancelledDelivery.deliveryManId == null
        }

        and: "DeliveryManUnAssigned event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "DeliveryManUnAssigned")

            def body = event.body() as DeliveryManUnAssigned
            body.orderId() == delivery.orderId
            body.deliveryManId() == deliveryManId
        }
    }

    def "Fail to un assign delivery man from #status delivery"() {
        given:
        var deliveryManId = "any-delivery-man-orderId"
        var delivery = aDelivery()
                .withStatus(status)
                .withDeliveryManId(deliveryManId)
        repository.save(delivery.entity())

        and:
        var assignDeliveryMan = new UnAssignDeliveryMan(delivery.orderId, deliveryManId)

        when:
        facade.handle(assignDeliveryMan)

        then: "Delivery man was not un assigned"
        with(repository.findByOrderId(delivery.orderId).get()) { deliveryEntity ->
            deliveryEntity.deliveryManId == deliveryManId
        }

        and: "DeliveryProcessingError event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "DeliveryProcessingError")

            def body = event.body() as DeliveryProcessingError
            body.orderId() == delivery.orderId
            body.details() == "Failed to un assign a delivery man from an '$delivery.orderId' order. It's not possible do it for a delivery with '$status' status"
        }

        where:
        status << [DeliveryStatus.CANCELED, DeliveryStatus.FOOD_PICKED, DeliveryStatus.FOOD_DELIVERED]
    }

    def "Fail to un assign delivery man if it's not assigned"() {
        given:
        def otherDeliveryManId = "other-delivery-man"
        def delivery = aDelivery()
                .withDeliveryManId(otherDeliveryManId)
        repository.save(delivery.entity())

        and:
        def deliveryManId = "any-delivery-man-orderId"
        def assignDeliveryMan = new UnAssignDeliveryMan(delivery.orderId, deliveryManId)

        when:
        facade.handle(assignDeliveryMan)

        then: "Delivery man has not been changed"
        with(repository.findByOrderId(delivery.orderId).get()) { deliveryEntity ->
            deliveryEntity.deliveryManId == otherDeliveryManId
        }

        and: "DeliveryProcessingError event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "DeliveryProcessingError")

            def body = event.body() as DeliveryProcessingError
            body.orderId() == delivery.orderId
            body.details() == "Failed to un assign delivery man from an '$delivery.orderId' order. Delivery has assigned '$otherDeliveryManId' person, but was asked to un assign '$deliveryManId'"
        }
    }

    def "Fail to un assign delivery man if there is no one assigned"() {
        given:
        def delivery = aDelivery()
                .withDeliveryManId(null)
        repository.save(delivery.entity())

        and:
        def deliveryManId = "any-delivery-man-orderId"
        def assignDeliveryMan = new UnAssignDeliveryMan(delivery.orderId, deliveryManId)

        when:
        facade.handle(assignDeliveryMan)

        then: "DeliveryProcessingError event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "DeliveryProcessingError")

            def body = event.body() as DeliveryProcessingError
            body.orderId() == delivery.orderId
            body.details() == "Failed to un assign delivery man from an '$delivery.orderId' order. There is no delivery man assigned to this delivery"
        }
    }

    def "Food is ready"() {
        given:
        var delivery = aDelivery()
                .withStatus(DeliveryStatus.FOOD_IN_PREPARATION)
        repository.save(delivery.entity())

        and:
        var foodReady = new FoodReady(delivery.orderId)

        when:
        facade.handle(foodReady)

        then: "Delivery is set to food is ready status"
        with(repository.database.values()[0]) { deliveryEntity ->
            deliveryEntity.status == DeliveryStatus.FOOD_READY
            deliveryEntity.metadata.get("foodReadyTimestamp") == testTime.toString()
        }

        and: "FoodIsRead event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "FoodIsRead")

            def body = event.body() as FoodIsReady
            body.orderId() == delivery.orderId
        }
    }

    def "Fail to set #status delivery to be in food is ready state"() {
        given:
        var delivery = aDelivery().withStatus(status)
        repository.save(delivery.entity())

        and:
        var foodReady = new FoodReady(delivery.orderId)

        when:
        facade.handle(foodReady)

        then: "Delivery is not in food ready state"
        with(repository.findByOrderId(delivery.orderId).get()) { deliveryEntity ->
            deliveryEntity.status == delivery.getStatus()
        }

        and: "DeliveryProcessingError event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "DeliveryProcessingError")

            def body = event.body() as DeliveryProcessingError
            body.orderId() == delivery.orderId
            body.details() == "Failed to set food ready for an '$delivery.orderId' order. It's not possible do it for a delivery with '$status' status"
        }

        where:
        status << [DeliveryStatus.CREATED, DeliveryStatus.CANCELED, DeliveryStatus.FOOD_READY, DeliveryStatus.FOOD_PICKED, DeliveryStatus.FOOD_DELIVERED]
    }

    def "Food is picked up"() {
        given:
        var delivery = aDelivery()
                .withStatus(DeliveryStatus.FOOD_READY)
        repository.save(delivery.entity())

        and:
        var pickUpFood = new PickUpFood(delivery.orderId)

        when:
        facade.handle(pickUpFood)

        then: "Delivery is set to food is picked up status"
        with(repository.database.values()[0]) { deliveryEntity ->
            deliveryEntity.status == DeliveryStatus.FOOD_PICKED
            deliveryEntity.metadata.get("foodPickedUpTimestamp") == testTime.toString()
        }

        and: "FoodIsPickedUp event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "FoodWasPickedUp")

            def body = event.body() as FoodWasPickedUp
            body.orderId() == delivery.orderId
        }
    }

    def "Fail to set #status delivery to be in food is picked up state"() {
        given:
        var delivery = aDelivery().withStatus(status)
        repository.save(delivery.entity())

        and:
        var pickUpFood = new PickUpFood(delivery.orderId)

        when:
        facade.handle(pickUpFood)

        then: "Delivery is not in food picked up state"
        with(repository.findByOrderId(delivery.orderId).get()) { deliveryEntity ->
            deliveryEntity.status == delivery.getStatus()
        }

        and: "DeliveryProcessingError event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "DeliveryProcessingError")

            def body = event.body() as DeliveryProcessingError
            body.orderId() == delivery.orderId
            body.details() == "Failed to set food as picked up for an '$delivery.orderId' order. It's not possible do it for a delivery with '$status' status"
        }

        where:
        status << [DeliveryStatus.CREATED, DeliveryStatus.CANCELED, DeliveryStatus.FOOD_IN_PREPARATION, DeliveryStatus.FOOD_PICKED, DeliveryStatus.FOOD_DELIVERED]
    }

    def "Food is delivered"() {
        given:
        var delivery = aDelivery()
                .withStatus(DeliveryStatus.FOOD_PICKED)
        repository.save(delivery.entity())

        and:
        var deliverFood = new DeliverFood(delivery.orderId)

        when:
        facade.handle(deliverFood)

        then: "Delivery is set to food is delivered status"
        with(repository.database.values()[0]) { deliveryEntity ->
            deliveryEntity.status == DeliveryStatus.FOOD_DELIVERED
            deliveryEntity.metadata.get("foodDeliveredTimestamp") == testTime.toString()
        }

        and: "FoodDelivered event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "FoodDelivered")

            def body = event.body() as FoodDelivered
            body.orderId() == delivery.orderId
        }
    }

    def "Fail to set #status delivery to be in food is delivered state"() {
        given:
        var delivery = aDelivery().withStatus(status)
        repository.save(delivery.entity())

        and:
        var deliverFood = new DeliverFood(delivery.orderId)

        when:
        facade.handle(deliverFood)

        then: "Delivery is not in food picked up state"
        with(repository.findByOrderId(delivery.orderId).get()) { deliveryEntity ->
            deliveryEntity.status == delivery.getStatus()
        }

        and: "DeliveryProcessingError event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "DeliveryProcessingError")

            def body = event.body() as DeliveryProcessingError
            body.orderId() == delivery.orderId
            body.details() == "Failed to set food as delivered for an '$delivery.orderId' order. It's not possible do it for a delivery with '$status' status"
        }

        where:
        status << [DeliveryStatus.CREATED, DeliveryStatus.CANCELED, DeliveryStatus.FOOD_IN_PREPARATION, DeliveryStatus.FOOD_READY, DeliveryStatus.FOOD_DELIVERED]
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
