package io.wkrzywiec.fooddelivery.bff.view;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.bff.view.outgoing.*;
import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static java.util.Optional.ofNullable;

@Component
@Slf4j
@RequiredArgsConstructor
@Profile("redis")
public class DeliveryViewProcessor {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    void handle(DomainMessageBody event) {
        log.info("Processing event: {}", event);
        DeliveryView deliveryView = getDeliveryView(event);
        deliveryView = updateDeliveryViewModel(event, deliveryView);
        storeViewModel(deliveryView);
    }

    private DeliveryView getDeliveryView(DomainMessageBody event) {
        var deliveryViewOptional = ofNullable(redisTemplate.opsForHash().get("delivery-view", event.orderId()));

        DeliveryView deliveryView = null;
        if (deliveryViewOptional.isPresent()) {
            try {
                deliveryView = objectMapper.readValue((String) deliveryViewOptional.get(), DeliveryView.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            if (!(event instanceof DeliveryCreated)) {
                throw new IllegalStateException(event + " event received, but there is no delivery view for orderId: " + event.orderId());
            }
        }
        return deliveryView;
    }

    private void storeViewModel(DeliveryView deliveryView) {
        log.info("Storing updated deliveryView model: {}", deliveryView);
        String messageJson = null;
        try {
            messageJson = objectMapper.writeValueAsString(deliveryView);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        redisTemplate.opsForHash().put("delivery-view", deliveryView.getOrderId(), messageJson);
    }

    private DeliveryView updateDeliveryViewModel(DomainMessageBody event, DeliveryView deliveryView) {
        if (event instanceof DeliveryCreated created) {
            deliveryView = new DeliveryView(
                    created.orderId(), created.customerId(),
                    created.restaurantId(), null, DeliveryStatus.CREATED,
                    created.address(),created.items(),
                    created.deliveryCharge(), BigDecimal.ZERO,
                    created.total()
            );
        }

        if (event instanceof TipAddedToDelivery tipAddedToDelivery) {
            deliveryView = new DeliveryView(
                    deliveryView.getOrderId(), deliveryView.getCustomerId(),
                    deliveryView.getRestaurantId(), deliveryView.getDeliveryManId(),
                    deliveryView.getStatus(), deliveryView.getAddress(),
                    deliveryView.getItems(), deliveryView.getDeliveryCharge(),
                    tipAddedToDelivery.tip(), tipAddedToDelivery.total()
            );
        }

        if (event instanceof DeliveryCanceled canceled) {
            deliveryView = new DeliveryView(
                    deliveryView.getOrderId(), deliveryView.getCustomerId(),
                    deliveryView.getRestaurantId(), deliveryView.getDeliveryManId(),
                    DeliveryStatus.CANCELED, deliveryView.getAddress(),
                    deliveryView.getItems(), deliveryView.getDeliveryCharge(),
                    deliveryView.getTip(), deliveryView.getTotal()
            );
        }

        if (event instanceof FoodInPreparation) {

            deliveryView = new DeliveryView(
                    deliveryView.getOrderId(), deliveryView.getCustomerId(),
                    deliveryView.getRestaurantId(), deliveryView.getDeliveryManId(),
                    DeliveryStatus.FOOD_IN_PREPARATION, deliveryView.getAddress(),
                    deliveryView.getItems(), deliveryView.getDeliveryCharge(),
                    deliveryView.getTip(), deliveryView.getTotal()
            );
        }

        if (event instanceof DeliveryManAssigned deliveryManAssigned) {
            deliveryView = new DeliveryView(
                    deliveryView.getOrderId(), deliveryView.getCustomerId(),
                    deliveryView.getRestaurantId(), deliveryManAssigned.deliveryManId(),
                    deliveryView.getStatus(), deliveryView.getAddress(),
                    deliveryView.getItems(), deliveryView.getDeliveryCharge(),
                    deliveryView.getTip(), deliveryView.getTotal()
            );
        }

        if (event instanceof DeliveryManUnAssigned deliveryManUnAssigned) {
            deliveryView = new DeliveryView(
                    deliveryView.getOrderId(), deliveryView.getCustomerId(),
                    deliveryView.getRestaurantId(), null,
                    deliveryView.getStatus(), deliveryView.getAddress(),
                    deliveryView.getItems(), deliveryView.getDeliveryCharge(),
                    deliveryView.getTip(), deliveryView.getTotal()
            );
        }

        if (event instanceof FoodIsReady) {
            deliveryView = new DeliveryView(
                    deliveryView.getOrderId(), deliveryView.getCustomerId(),
                    deliveryView.getRestaurantId(), deliveryView.getDeliveryManId(),
                    DeliveryStatus.FOOD_READY, deliveryView.getAddress(),
                    deliveryView.getItems(), deliveryView.getDeliveryCharge(),
                    deliveryView.getTip(), deliveryView.getTotal()
            );
        }

        if (event instanceof FoodWasPickedUp) {
            deliveryView = new DeliveryView(
                    deliveryView.getOrderId(), deliveryView.getCustomerId(),
                    deliveryView.getRestaurantId(), deliveryView.getDeliveryManId(),
                    DeliveryStatus.FOOD_PICKED, deliveryView.getAddress(),
                    deliveryView.getItems(), deliveryView.getDeliveryCharge(),
                    deliveryView.getTip(), deliveryView.getTotal()
            );
        }

        if (event instanceof FoodDelivered) {
            deliveryView = new DeliveryView(
                    deliveryView.getOrderId(), deliveryView.getCustomerId(),
                    deliveryView.getRestaurantId(), deliveryView.getDeliveryManId(),
                    DeliveryStatus.FOOD_DELIVERED, deliveryView.getAddress(),
                    deliveryView.getItems(), deliveryView.getDeliveryCharge(),
                    deliveryView.getTip(), deliveryView.getTotal()
            );
        }
        return deliveryView;
    }

}
