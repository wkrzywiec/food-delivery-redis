package io.wkrzywiec.fooddelivery.delivery.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

import java.math.BigDecimal;
import java.util.List;

public record DeliveryCreated (String orderId, String customerId, String restaurantId, String address, List<Item> items, BigDecimal deliveryCharge, BigDecimal total) implements DomainMessageBody {
}
