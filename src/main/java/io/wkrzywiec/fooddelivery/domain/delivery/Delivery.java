package io.wkrzywiec.fooddelivery.domain.delivery;

import io.wkrzywiec.fooddelivery.domain.ordering.outgoing.OrderCreated;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@EqualsAndHashCode
@ToString
class Delivery {

    private String id;
    private String orderId;
    private String customerId;
    private String restaurantId;
    private String deliveryManId;
    private DeliveryStatus status;
    private String address;
    private List<Item> items;
    private BigDecimal deliveryCharge = new BigDecimal(0);
    private BigDecimal tip = new BigDecimal(0);
    private BigDecimal total = new BigDecimal(0);

    private Delivery() {};

    private Delivery(String orderId, String customerId, String restaurantId, String address, List<Item> items, BigDecimal deliveryCharge, BigDecimal total) {
        this.id = UUID.randomUUID().toString();
        this.orderId = orderId;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.address = address;
        this.items = items;
        this.deliveryCharge = deliveryCharge;
        this.total = total;
        this.status = DeliveryStatus.CREATED;
    }

    public static Delivery from(OrderCreated orderCreated) {
        return new Delivery(
                orderCreated.id(),
                orderCreated.customerId(),
                orderCreated.restaurantId(),
                orderCreated.address(),
                orderCreated.items().stream().map(dto -> new Item(dto.name(), dto.amount(), dto.pricePerItem())).toList(),
                orderCreated.deliveryCharge(),
                orderCreated.total()
        );
    }
}
