package io.wkrzywiec.fooddelivery.delivery;

import io.vavr.CheckedRunnable;
import io.vavr.control.Try;
import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;
import io.wkrzywiec.fooddelivery.commons.incoming.*;
import io.wkrzywiec.fooddelivery.commons.infra.repository.EventStore;
import io.wkrzywiec.fooddelivery.delivery.incoming.*;
import io.wkrzywiec.fooddelivery.delivery.outgoing.*;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

import static java.lang.String.format;

@RequiredArgsConstructor
@Slf4j
@Component
public class DeliveryFacade {

    private static final String ORDERS_CHANNEL = "orders";
    private final EventStore eventStore;
    private final MessagePublisher publisher;
    private final Clock clock;

    public void handle(OrderCreated orderCreated) {
        log.info("Preparing a delivery for an '{}' order.", orderCreated.orderId());

        Delivery newDelivery = Delivery.from(orderCreated, clock.instant());
        var deliveryCreated = new DeliveryCreated(
                newDelivery.getOrderId(),
                newDelivery.getCustomerId(),
                newDelivery.getRestaurantId(),
                newDelivery.getAddress(),
                newDelivery.getItems().stream().map(i -> new io.wkrzywiec.fooddelivery.delivery.incoming.Item(i.getName(), i.getAmount(), i.getPricePerItem())).toList(),
                newDelivery.getDeliveryCharge(),
                newDelivery.getTotal());

        Message event = resultingEvent(
                newDelivery.getOrderId(),
                deliveryCreated
        );

        eventStore.store(event);
        publisher.send(event);
        log.info("New delivery with an orderId: '{}' was created", newDelivery.getOrderId());
    }

    public void handle(OrderCanceled orderCanceled) {
        log.info("'{}' order was canceled. Canceling delivery", orderCanceled.orderId());

        var storedEvents = eventStore.getEventsForOrder(orderCanceled.orderId());
        if (storedEvents.size() == 0) {
            throw new DeliveryException(format("Failed to cancel a delivery. There is no delivery for an %s order", orderCanceled.orderId()));
        }
        var delivery = Delivery.from(storedEvents);

        process(
                delivery,
                () -> delivery.cancel(orderCanceled.reason(), clock.instant()),
                new DeliveryCanceled(orderCanceled.orderId(), orderCanceled.reason()),
                "Failed to cancel an delivery."
        );
    }

    public void handle(PrepareFood prepareFood) {
        log.info("Starting food preparation for '{}' delivery", prepareFood.orderId());

        var delivery = findDelivery(prepareFood.orderId());

        process(
                delivery,
                () -> delivery.foodInPreparation(clock.instant()),
                new FoodInPreparation(delivery.getOrderId()),
                "Failed to start food preparation."
        );
    }

    public void handle(AssignDeliveryMan assignDeliveryMan) {
        log.info("Assigning a delivery man with id: '{}' to an '{}' order", assignDeliveryMan.deliveryManId(), assignDeliveryMan.orderId());

        var delivery = findDelivery(assignDeliveryMan.orderId());

        process(
                delivery,
                () -> delivery.assignDeliveryMan(assignDeliveryMan.deliveryManId()),
                new DeliveryManAssigned(delivery.getOrderId(), assignDeliveryMan.deliveryManId()),
                "Failed to assign delivery man."
        );
    }

    public void handle(UnAssignDeliveryMan unAssignDeliveryMan) {
        log.info("Un assigning a delivery man from a '{}' delivery", unAssignDeliveryMan.orderId());

        var delivery = findDelivery(unAssignDeliveryMan.orderId());

        process(
                delivery,
                delivery::unAssignDeliveryMan,
                new DeliveryManUnAssigned(delivery.getOrderId(), delivery.getDeliveryManId()),
                "Failed to un assign delivery man."
        );
    }


    public void handle(FoodReady foodReady) {
        log.info("Starting food ready for '{}' delivery", foodReady.orderId());

        var delivery = findDelivery(foodReady.orderId());

        process(
                delivery,
                () -> delivery.foodReady(clock.instant()),
                new FoodIsReady(delivery.getOrderId()),
                "Failed to set food as ready."
        );
    }

    public void handle(PickUpFood pickUpFood) {
        log.info("Starting picking up food for '{}' delivery", pickUpFood.orderId());

        var delivery = findDelivery(pickUpFood.orderId());

        process(
                delivery,
                () -> delivery.pickUpFood(clock.instant()),
                new FoodWasPickedUp(delivery.getOrderId()),
                "Failed to set food as picked up."
        );
    }

    public void handle(DeliverFood deliverFood) {
        log.info("Starting delivering food for '{}' delivery", deliverFood.orderId());

        var delivery = findDelivery(deliverFood.orderId());

        process(
                delivery,
                () -> delivery.deliverFood(clock.instant()),
                new FoodDelivered(delivery.getOrderId()),
                "Failed to set food as delivered."
        );
    }

    private Delivery findDelivery(String orderId) {
        var storedEvents = eventStore.getEventsForOrder(orderId);
        if (storedEvents.size() == 0) {
            throw new DeliveryException(format("There is no delivery with an orderId '%s'.", orderId));
        }
        return Delivery.from(storedEvents);
    }

    private void process(Delivery delivery, CheckedRunnable runProcess, DomainMessageBody successEvent, String failureMessage) {
        Try.run(runProcess)
                .onSuccess(v -> publishSuccessEvent(delivery.getOrderId(), successEvent))
                .onFailure(ex -> publishingFailureEvent(delivery.getOrderId(), failureMessage, ex));
    };

    private void publishSuccessEvent(String orderId, DomainMessageBody eventObject) {
        log.info("Publishing success event: {}", eventObject);
        Message event = resultingEvent(orderId, eventObject);
        eventStore.store(event);
        publisher.send(event);
    }

    private void publishingFailureEvent(String id, String message, Throwable ex) {
        log.error(message + " Publishing DeliveryProcessingError event", ex);
        Message event = resultingEvent(id, new DeliveryProcessingError(id, message, ex.getLocalizedMessage()));
        publisher.send(event);
    }

    private Message resultingEvent(String orderId, DomainMessageBody eventBody) {
        return new Message(eventHeader(orderId, eventBody.getClass().getSimpleName()), eventBody);
    }

    private Header eventHeader(String orderId, String type) {
        return new Header(UUID.randomUUID().toString(), ORDERS_CHANNEL, type, orderId, clock.instant());
    }
}
