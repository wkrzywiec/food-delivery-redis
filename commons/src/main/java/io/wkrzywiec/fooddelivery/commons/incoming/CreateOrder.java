package io.wkrzywiec.fooddelivery.commons.incoming;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrder(
        String orderId,
        String customerId,
        String restaurantId,
        List<Item> items,
        String address,
        BigDecimal deliveryCharge) implements DomainMessageBody {
}
