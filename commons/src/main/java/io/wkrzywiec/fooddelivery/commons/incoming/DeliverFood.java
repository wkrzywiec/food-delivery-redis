package io.wkrzywiec.fooddelivery.commons.incoming;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record DeliverFood(String orderId) implements DomainMessageBody {
}
