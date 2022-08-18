package io.wkrzywiec.fooddelivery.commons.infra.messaging;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record Message(Header header, DomainMessageBody body) {
}
