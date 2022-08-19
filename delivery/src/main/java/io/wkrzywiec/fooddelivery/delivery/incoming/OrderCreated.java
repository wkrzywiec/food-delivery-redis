package io.wkrzywiec.fooddelivery.delivery.incoming;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreated(String orderId, String customerId, String restaurantId, String address, List<Item> items, BigDecimal deliveryCharge, BigDecimal total) implements DomainMessageBody {
}
