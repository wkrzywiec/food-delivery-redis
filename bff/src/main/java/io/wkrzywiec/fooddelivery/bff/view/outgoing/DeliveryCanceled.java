package io.wkrzywiec.fooddelivery.bff.view.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record DeliveryCanceled(String orderId, String reason) implements DomainMessageBody {
}
