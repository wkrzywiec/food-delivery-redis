package io.wkrzywiec.fooddelivery.delivery.incoming;

public record OrderCanceled(String orderId, String reason) {
}
