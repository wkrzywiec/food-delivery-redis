package io.wkrzywiec.fooddelivery.commons.infra.messaging;

public record Message(Header header, Object body) {
}
