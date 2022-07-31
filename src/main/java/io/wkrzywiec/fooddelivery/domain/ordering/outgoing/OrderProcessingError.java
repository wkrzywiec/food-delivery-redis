package io.wkrzywiec.fooddelivery.domain.ordering.outgoing;

public record OrderProcessingError(String id, String message, String details) {
}
