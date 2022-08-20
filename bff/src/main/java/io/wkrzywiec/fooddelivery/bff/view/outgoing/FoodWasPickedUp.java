package io.wkrzywiec.fooddelivery.bff.view.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record FoodWasPickedUp(String orderId) implements DomainMessageBody {
}
