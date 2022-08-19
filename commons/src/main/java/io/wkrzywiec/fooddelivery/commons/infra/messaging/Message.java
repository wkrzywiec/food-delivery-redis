package io.wkrzywiec.fooddelivery.commons.infra.messaging;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

import java.time.Clock;
import java.util.UUID;

public record Message(Header header, DomainMessageBody body) {

    public static Message message(String channel, Clock clock, DomainMessageBody body) {
        return new Message(
                new Header(UUID.randomUUID().toString(), channel, body.getClass().getSimpleName(), body.orderId(), clock.instant()),
                body
        );
    }
}
