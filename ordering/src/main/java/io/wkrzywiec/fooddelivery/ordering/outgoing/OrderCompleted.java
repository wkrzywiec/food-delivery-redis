package io.wkrzywiec.fooddelivery.ordering.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record OrderCompleted(String orderId) implements DomainMessageBody {
}
