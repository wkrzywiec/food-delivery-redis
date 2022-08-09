package io.wkrzywiec.fooddelivery.ordering.infra.messaging;

import java.time.Instant;

public record Header(String messageId, String channel, String type, String itemId, Instant createdAt) {
}
