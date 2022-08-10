package io.wkrzywiec.fooddelivery.delivery.incoming;

public record CancelDelivery(String deliveryId, String reason) {
}
