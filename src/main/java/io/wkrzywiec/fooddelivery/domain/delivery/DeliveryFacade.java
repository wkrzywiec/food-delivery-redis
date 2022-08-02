package io.wkrzywiec.fooddelivery.domain.delivery;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.wkrzywiec.fooddelivery.domain.delivery.incoming.*;
import io.wkrzywiec.fooddelivery.domain.delivery.outgoing.DeliveryCreated;
import io.wkrzywiec.fooddelivery.domain.ordering.outgoing.OrderCanceled;
import io.wkrzywiec.fooddelivery.domain.ordering.outgoing.OrderCreated;
import io.wkrzywiec.fooddelivery.infra.messaging.Header;
import io.wkrzywiec.fooddelivery.infra.messaging.Message;
import io.wkrzywiec.fooddelivery.infra.messaging.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class DeliveryFacade {

    private static final String DELIVERY_CHANNEL = "delivery";
    private final DeliveryRepository repository;
    private final MessagePublisher publisher;
    private final Clock clock;

    public void handle(OrderCreated orderCreated) {
        log.info("Preparing a delivery for an '{}' order.", orderCreated.id());

        Delivery newDelivery = Delivery.from(orderCreated);
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

    public void handle(OrderCanceled cancelDelivery) {

    }

    public void handle(PrepareFood prepareFood) {

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
