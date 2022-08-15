package io.wkrzywiec.fooddelivery.commons.messaging;

import java.math.BigDecimal;
import java.time.Instant;

public record MessageTestBody(String id, Instant createdAt, BigDecimal value) {
}
