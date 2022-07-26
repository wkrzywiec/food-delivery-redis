package io.wkrzywiec.fooddelivery.domain.ordering;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.wkrzywiec.fooddelivery.domain.delivery.outgoing.FoodDelivered;
import io.wkrzywiec.fooddelivery.domain.delivery.outgoing.FoodInPreparation;
import io.wkrzywiec.fooddelivery.domain.ordering.incoming.AddTip;
import io.wkrzywiec.fooddelivery.domain.ordering.incoming.CancelOrder;
import io.wkrzywiec.fooddelivery.domain.ordering.incoming.CreateOrder;
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
public class OrderingFacade {

    private static final String ORDERING_CHANNEL =  "ordering";

    private final OrderingRepository repository;
    private final MessagePublisher publisher;
    private final Clock clock;

    public void handle(CreateOrder createOrder) {
        log.info("Creating a new order: {}", createOrder);
        Order newOrder = Order.from(createOrder);
        Order savedOrder = repository.save(newOrder);

        Message event = null;
        try {
            event = Message.from(
                    new Header(UUID.randomUUID().toString(), ORDERING_CHANNEL, savedOrder.getId(), clock.instant()),
                    new OrderCreated(savedOrder.getId(), savedOrder.getCustomerId(), savedOrder.getRestaurantId(), savedOrder.getAddress(), createOrder.items(), savedOrder.getDeliveryCharge(), savedOrder.getTotal()));
        } catch (JsonProcessingException e) {
            throw new OrderingException("Failed to process order creation. OrderCreated record could not be mapped to JSON", e);
        }

        publisher.send(event);
        log.info("New order with an id: '{}' was created", savedOrder.getId());
    }

    public void handle(CancelOrder cancelOrder) {

    }

    public void handle(FoodInPreparation foodInPreparation) {

    }

    public void handle(AddTip addTip) {

    }

    public void handle(FoodDelivered foodDelivered) {

    }
}
