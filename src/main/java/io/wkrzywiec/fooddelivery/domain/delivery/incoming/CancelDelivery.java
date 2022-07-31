package io.wkrzywiec.fooddelivery.domain.delivery.incoming;

public record CancelDelivery(String deliveryId, String reason) {
}
