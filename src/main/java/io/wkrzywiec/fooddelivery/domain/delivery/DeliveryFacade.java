package io.wkrzywiec.fooddelivery.domain.delivery;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.vavr.control.Try;
import io.wkrzywiec.fooddelivery.domain.delivery.incoming.*;
import io.wkrzywiec.fooddelivery.domain.delivery.outgoing.DeliveryCanceled;
import io.wkrzywiec.fooddelivery.domain.delivery.outgoing.DeliveryCreated;
import io.wkrzywiec.fooddelivery.domain.delivery.outgoing.DeliveryProcessingError;
import io.wkrzywiec.fooddelivery.domain.delivery.outgoing.FoodInPreparation;
import io.wkrzywiec.fooddelivery.domain.ordering.outgoing.OrderCanceled;
import io.wkrzywiec.fooddelivery.domain.ordering.outgoing.OrderCreated;
import io.wkrzywiec.fooddelivery.infra.messaging.Header;
import io.wkrzywiec.fooddelivery.infra.messaging.Message;
import io.wkrzywiec.fooddelivery.infra.messaging.MessagePublisher;
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
                        savedDelivery.getItems().stream().map(i -> new io.wkrzywiec.fooddelivery.domain.delivery.outgoing.Item(i.getName(), i.getAmount(), i.getPricePerItem())).toList(),
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

        Try.run(() -> delivery.cancel(orderCanceled.reason(), clock.instant()))
                .onSuccess(v -> publishSuccessEvent(delivery.getId(), new DeliveryCanceled(delivery.getId(), orderCanceled.id(), orderCanceled.reason())))
                .onFailure(ex -> publishingFailureEvent(delivery.getId(), "Failed to cancel an delivery.", ex))
                .andFinally(() -> log.info("Cancellation of a delivery '{}' has been completed", delivery.getId()));
    }

    public void handle(PrepareFood prepareFood) {
        log.info("Starting food preparation for '{}' delivery", prepareFood.deliveryId());

        var delivery = repository.findById(prepareFood.deliveryId())
                .orElseThrow(() -> new DeliveryException(format("Failed to start food preparation for a delivery. There is no delivery with an id '%s'.", prepareFood.deliveryId())));

        Try.run(() -> delivery.foodInPreparation(clock.instant()))
                .onSuccess(v -> publishSuccessEvent(delivery.getId(), new FoodInPreparation(delivery.getId(), delivery.getOrderId())))
                .onFailure(ex -> publishingFailureEvent(delivery.getId(), "Failed to start food preparation.", ex))
                .andFinally(() -> log.info("Food in preparation of a delivery '{}' has been completed", delivery.getId()));
    }

    public void handle(AssignedDeliveryMan assignedDeliveryMan) {

    }

    public void handle(UnAssignDeliveryMan unAssignDeliveryMan) {

    }

    public void handle(FoodReady foodReady) {

    }

    public void handle(PickUpFood pickUpFood) {

    }

    public void handle(DeliverFood deliverFood) {

    }

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
