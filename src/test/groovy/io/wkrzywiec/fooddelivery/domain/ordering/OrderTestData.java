package io.wkrzywiec.fooddelivery.domain.ordering;

import io.wkrzywiec.fooddelivery.domain.ordering.incoming.CreateOrder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.wkrzywiec.fooddelivery.domain.ordering.OrderStatus.CREATED;

@Getter
public class OrderTestData {

    private String id = UUID.randomUUID().toString();
    private String customerId = "default-customer-id";
    private String restaurantId = "default-restaurant-id";
    private String deliveryManId = null;
    private OrderStatus status = CREATED;
    private String address = "Pizza street, Naples, Italy";
    private List<ItemTestData> items = new ArrayList<>();
    private BigDecimal deliveryCharge = new BigDecimal(5);
    private BigDecimal tip = new BigDecimal(0);

    private OrderTestData() {};

    public static OrderTestData anOrder() {
        return new OrderTestData();
    }

    public Order entity() {
        return Order.from(createOrder());
    }

    public CreateOrder createOrder() {
        return new CreateOrder(customerId, restaurantId, items.stream().map(ItemTestData::dto).toList(), address, deliveryCharge);
    }

    public OrderTestData withId(String id) {
        this.id = id;
        return this;
    }

    public OrderTestData withCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public OrderTestData withRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
        return this;
    }

    public OrderTestData withDeliveryManId(String deliveryManId) {
        this.deliveryManId = deliveryManId;
        return this;
    }

    public OrderTestData withStatus(OrderStatus status) {
        this.status = status;
        return this;
    }

    public OrderTestData withAddress(String address) {
        this.address = address;
        return this;
    }

    public OrderTestData withItems(ItemTestData... items) {
        this.items = Arrays.asList(items);
        return this;
    }

    public OrderTestData withDeliveryCharge(BigDecimal deliveryCharge) {
        this.deliveryCharge = deliveryCharge;
        return this;
    }

    public OrderTestData withTip(BigDecimal tip) {
        this.tip = tip;
        return this;
    }
}
