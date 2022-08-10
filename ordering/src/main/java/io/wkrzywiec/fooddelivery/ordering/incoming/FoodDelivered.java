package io.wkrzywiec.fooddelivery.ordering.incoming;

public record FoodDelivered(String deliveryId, String orderId) {
}
