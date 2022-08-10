package io.wkrzywiec.fooddelivery.ordering.outgoing;

public record OrderProcessingError(String id, String message, String details) {
}
