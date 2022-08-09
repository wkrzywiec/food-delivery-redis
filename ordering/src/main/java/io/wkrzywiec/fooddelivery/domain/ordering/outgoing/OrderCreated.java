package io.wkrzywiec.fooddelivery.domain.ordering.outgoing;

import io.wkrzywiec.fooddelivery.domain.ordering.incoming.Item;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreated(String id, String customerId, String restaurantId, String address, List<Item> items, BigDecimal deliveryCharge, BigDecimal total) {
}
