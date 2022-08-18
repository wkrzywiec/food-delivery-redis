package io.wkrzywiec.fooddelivery.delivery.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record DeliveryProcessingError(String orderId, String message, String details) implements DomainMessageBody {
}
