package io.wkrzywiec.fooddelivery.delivery.incoming;

public record CancelDelivery(String orderId, String reason) {
}
