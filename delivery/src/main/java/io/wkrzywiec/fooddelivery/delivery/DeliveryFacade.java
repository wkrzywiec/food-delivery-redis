package io.wkrzywiec.fooddelivery.delivery;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.vavr.CheckedRunnable;
import io.vavr.control.Try;
import io.wkrzywiec.fooddelivery.delivery.incoming.*;
import io.wkrzywiec.fooddelivery.delivery.outgoing.*;
import io.wkrzywiec.fooddelivery.delivery.infra.messaging.Header;
import io.wkrzywiec.fooddelivery.delivery.infra.messaging.Message;
import io.wkrzywiec.fooddelivery.delivery.infra.messaging.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.util.UUID;

import static java.lang.String.format;

@RequiredArgsConstructor
@Slf4j
public class DeliveryFacade {

    private static final String DELIVERY_CHANNEL = "delivery";
    private final DeliveryRepository repository;
    private final MessagePublisher publisher;
    private final Clock clock;

    public void handle(OrderCreated orderCreated) {
        log.info("Preparing a delivery for an '{}' order.", orderCreated.id());

        Delivery newDelivery = Delivery.from(orderCreated, clock.instant());
        var savedDelivery = repository.save(newDelivery);

        Message event = resultingEvent(
                savedDelivery.getId(),
                new DeliveryCreated(
                        savedDelivery.getId(),
                        savedDelivery.getOrderId(),
                        savedDelivery.getCustomerId(),
                        savedDelivery.getRestaurantId(),
                        savedDelivery.getAddress(),
                        savedDelivery.getItems().stream().map(i -> new io.wkrzywiec.fooddelivery.delivery.outgoing.Item(i.getName(), i.getAmount(), i.getPricePerItem())).toList(),
                        savedDelivery.getDeliveryCharge(),
                        savedDelivery.getTotal())
        );

        publisher.send(event);
        log.info("New delivery with an id: '{}' was created", savedDelivery.getId());
    }

    public void handle(OrderCanceled orderCanceled) {
        log.info("'{}' order was canceled. Canceling delivery", orderCanceled.id());

        var delivery = repository.findByOrderId(orderCanceled.id())
                .orElseThrow(() -> new DeliveryException(format("Failed to cancel a delivery. There is no delivery for an %s order", orderCanceled.id())));

        process(
                delivery,
                () -> delivery.cancel(orderCanceled.reason(), clock.instant()),
                new DeliveryCanceled(delivery.getId(), orderCanceled.id(), orderCanceled.reason()),
                "Failed to cancel an delivery."
        );
    }

    public void handle(PrepareFood prepareFood) {
        log.info("Starting food preparation for '{}' delivery", prepareFood.deliveryId());

        var delivery = findDelivery(prepareFood.deliveryId());

        process(
                delivery,
                () -> delivery.foodInPreparation(clock.instant()),
                new FoodInPreparation(delivery.getId(), delivery.getOrderId()),
                "Failed to start food preparation."
        );
    }

    public void handle(AssignDeliveryMan assignDeliveryMan) {
        log.info("Assigning a delivery man with id: '{}' to a '{}' delivery", assignDeliveryMan.deliveryManId(), assignDeliveryMan.deliveryId());

        var delivery = findDelivery(assignDeliveryMan.deliveryId());

        process(
                delivery,
                () -> delivery.assignDeliveryMan(assignDeliveryMan.deliveryManId()),
                new DeliveryManAssigned(delivery.getId(), assignDeliveryMan.deliveryManId()),
                "Failed to assign delivery man."
        );
    }

    public void handle(UnAssignDeliveryMan unAssignDeliveryMan) {
        log.info("Un assigning a delivery man with id: '{}' from a '{}' delivery", unAssignDeliveryMan.deliveryManId(), unAssignDeliveryMan.deliveryId());

        var delivery = findDelivery(unAssignDeliveryMan.deliveryId());

        process(
                delivery,
                () -> delivery.unAssignDeliveryMan(unAssignDeliveryMan.deliveryManId()),
                new DeliveryManUnAssigned(delivery.getId(), delivery.getDeliveryManId()),
                "Failed to un assign delivery man."
        );
    }


    public void handle(FoodReady foodReady) {
        log.info("Starting food ready for '{}' delivery", foodReady.deliveryId());

        var delivery = findDelivery(foodReady.deliveryId());

        process(
                delivery,
                () -> delivery.foodReady(clock.instant()),
                new FoodIsReady(delivery.getId()),
                "Failed to set food as ready."
        );
    }

    public void handle(PickUpFood pickUpFood) {
        log.info("Starting picking up food for '{}' delivery", pickUpFood.deliveryId());

        var delivery = findDelivery(pickUpFood.deliveryId());

        process(
                delivery,
                () -> delivery.pickUpFood(clock.instant()),
                new FoodWasPickedUp(delivery.getId()),
                "Failed to set food as picked up."
        );
    }

    public void handle(DeliverFood deliverFood) {
        log.info("Starting delivering food for '{}' delivery", deliverFood.deliveryId());

        var delivery = findDelivery(deliverFood.deliveryId());

        process(
                delivery,
                () -> delivery.deliverFood(clock.instant()),
                new FoodDelivered(delivery.getId(), delivery.getOrderId()),
                "Failed to set food as delivered."
        );
    }

    private Delivery findDelivery(String deliveryId) {
        return repository.findById(deliveryId)
                .orElseThrow(() -> new DeliveryException(format("There is no delivery with an id '%s'.", deliveryId)));
    }

    private void process(Delivery delivery, CheckedRunnable runProcess, Object successEvent, String failureMessage) {
        Try.run(runProcess)
                .onSuccess(v -> publishSuccessEvent(delivery.getId(), successEvent))
                .onFailure(ex -> publishingFailureEvent(delivery.getId(), failureMessage, ex));
    };

    private void publishSuccessEvent(String orderId, Object eventObject) {
        log.info("Publishing success event: {}", eventObject);
        Message event = resultingEvent(orderId, eventObject);
        publisher.send(event);
    }

    private void publishingFailureEvent(String id, String message, Throwable ex) {
        log.error(message + " Publishing DeliveryProcessingError event", ex);
        Message event = resultingEvent(id, new DeliveryProcessingError(id, message, ex.getLocalizedMessage()));
        publisher.send(event);
    }

    private Message resultingEvent(String deliveryId, Object eventBody) {
        Message event = null;
        try {
            event = Message.from(eventHeader(deliveryId, eventBody.getClass().getSimpleName()), eventBody);
        } catch (JsonProcessingException e) {
            throw new DeliveryException("Failed to map a Java event to JSON", e);
        }
        return event;
    }

    private Header eventHeader(String orderId, String type) {
        return new Header(UUID.randomUUID().toString(), DELIVERY_CHANNEL, type, orderId, clock.instant());
    }
}
