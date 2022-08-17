package io.wkrzywiec.fooddelivery.ordering;

import io.vavr.control.Try;
import io.wkrzywiec.fooddelivery.commons.incoming.AddTip;
import io.wkrzywiec.fooddelivery.commons.incoming.CancelOrder;
import io.wkrzywiec.fooddelivery.commons.incoming.CreateOrder;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.MessagePublisher;
import io.wkrzywiec.fooddelivery.ordering.incoming.*;
import io.wkrzywiec.fooddelivery.ordering.outgoing.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

import static java.lang.String.format;

@RequiredArgsConstructor
@Slf4j
@Component
public class OrderingFacade {

    private static final String ORDERS_CHANNEL =  "orders";

    private final OrderingRepository repository;
    private final MessagePublisher publisher;
    private final Clock clock;

    public void handle(CreateOrder createOrder) {
        log.info("Creating a new order: {}", createOrder);
        Order newOrder = Order.from(createOrder);
        Order savedOrder = repository.save(newOrder);

        Message event = resultingEvent(
                savedOrder.getId(),
                new OrderCreated(
                        savedOrder.getId(),
                        savedOrder.getCustomerId(),
                        savedOrder.getRestaurantId(),
                        savedOrder.getAddress(),
                        createOrder.items(),
                        savedOrder.getDeliveryCharge(),
                        savedOrder.getTotal())
        );

        publisher.send(event);
        log.info("New order with an id: '{}' was created", savedOrder.getId());
    }

    public void handle(CancelOrder cancelOrder) {
        log.info("Cancelling an order: {}", cancelOrder.id());

        var order = repository.findById(cancelOrder.id())
                .orElseThrow(() -> new OrderingException(format("Failed to cancel an %s order. There is no such order with provided id.", cancelOrder.id())));

        Try.run(() -> order.cancelOrder(cancelOrder.reason()))
                .onSuccess(v -> publishSuccessEvent(order.getId(), new OrderCanceled(cancelOrder.id(), cancelOrder.reason())))
                .onFailure(ex -> publishingFailureEvent(order.getId(), "Failed to cancel an order.", ex))
                .andFinally(() -> log.info("Cancellation of an order '{}' has been completed", order.getId()));
    }

    public void handle(FoodInPreparation foodInPreparation) {
        log.info("Setting '{}' order to IN_PROGRESS state", foodInPreparation.orderId());

        var order = repository.findById(foodInPreparation.orderId())
                .orElseThrow(() -> new OrderingException(format("Failed to set an '%s' order to IN_PROGRESS state. There is no such order with provided id.", foodInPreparation.orderId())));

        Try.run(order::setInProgress)
                .onSuccess(v -> publishSuccessEvent(order.getId(), new OrderInProgress(foodInPreparation.orderId())))
                .onFailure(ex -> publishingFailureEvent(order.getId(), "Failed to set an order to IN_PROGRESS state.", ex))
                .andFinally(() -> log.info("Setting an '{}' order to IN_PROGRESS state has been completed", foodInPreparation.orderId()));
    }

    public void handle(AddTip addTip) {
        log.info("Adding {} tip to '{}' order.", addTip.tip(), addTip.orderId());

        var order = repository.findById(addTip.orderId())
                .orElseThrow(() -> new OrderingException(format("Failed add tip an '%s' order. There is no such order with provided id.", addTip.orderId())));

        Try.run(() -> order.addTip(addTip.tip()))
                .onSuccess(v -> publishSuccessEvent(order.getId(), new TipAddedToOrder(order.getId(), order.getTip(), order.getTotal())))
                .onFailure(ex -> publishingFailureEvent(order.getId(), "Failed to add tip to an order.", ex))
                .andFinally(() -> log.info("Adding a tip to '{}' order has been completed", addTip.orderId()));
    }

    public void handle(FoodDelivered foodDelivered) {
        log.info("Setting '{}' order to COMPLETED state", foodDelivered.orderId());

        var order = repository.findById(foodDelivered.orderId())
                .orElseThrow(() -> new OrderingException(format("Failed to complete an '%s' order. There is no such order with provided id.", foodDelivered.orderId())));

        Try.run(order::complete)
                .onSuccess(v -> publishSuccessEvent(order.getId(), new OrderCompleted(foodDelivered.orderId())))
                .onFailure(ex -> publishingFailureEvent(order.getId(), "Failed to complete an order.", ex))
                .andFinally(() -> log.info("Setting an '{}' order to COMPLETED state has been completed", foodDelivered.orderId()));
    }

    private void publishSuccessEvent(String orderId, Object eventObject) {
        log.info("Publishing success event: {}", eventObject);
        Message event = resultingEvent(orderId, eventObject);
        publisher.send(event);
    }

    private void publishingFailureEvent(String id, String message, Throwable ex) {
        log.error(message + " Publishing OrderProcessingError event", ex);
        Message event = resultingEvent(id, new OrderProcessingError(id, message, ex.getLocalizedMessage()));
        publisher.send(event);
    }

    private Message resultingEvent(String orderId, Object eventBody) {
        return new Message(eventHeader(orderId, eventBody.getClass().getSimpleName()), eventBody);
    }

    private Header eventHeader(String orderId, String type) {
        return new Header(UUID.randomUUID().toString(), ORDERS_CHANNEL, type, orderId, clock.instant());
    }
}
