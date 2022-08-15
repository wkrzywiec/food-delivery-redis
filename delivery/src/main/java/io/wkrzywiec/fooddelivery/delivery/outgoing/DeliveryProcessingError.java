package io.wkrzywiec.fooddelivery.delivery.outgoing;

public record DeliveryProcessingError(String orderId, String message, String details) {
}
