package io.wkrzywiec.fooddelivery.bff.view.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record FoodDelivered(String orderId) implements DomainMessageBody {
}
