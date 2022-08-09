package io.wkrzywiec.fooddelivery.domain.ordering.incoming;

public record FoodDelivered(String deliveryId, String orderId) {
}
