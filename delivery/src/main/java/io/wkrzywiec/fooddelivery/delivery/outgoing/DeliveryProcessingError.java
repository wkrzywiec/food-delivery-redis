package io.wkrzywiec.fooddelivery.delivery.outgoing;

public record DeliveryProcessingError(String id, String message, String details) {
}
