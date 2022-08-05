package io.wkrzywiec.fooddelivery.domain.delivery.outgoing;

public record DeliveryProcessingError(String id, String message, String details) {
}
