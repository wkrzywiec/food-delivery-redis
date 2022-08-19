package io.wkrzywiec.fooddelivery.ordering.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record OrderProcessingError(String orderId, String message, String details) implements DomainMessageBody {
}
