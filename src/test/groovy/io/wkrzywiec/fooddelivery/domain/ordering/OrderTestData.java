package io.wkrzywiec.fooddelivery.domain.ordering;

import io.wkrzywiec.fooddelivery.domain.ordering.incoming.CreateOrder;
import lombok.Getter;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.math.BigDecimal;
import java.util.*;

import static io.wkrzywiec.fooddelivery.domain.ordering.ItemTestData.anItem;
import static io.wkrzywiec.fooddelivery.domain.ordering.OrderStatus.CREATED;
import static java.lang.String.format;

@Getter
 class OrderTestData {

    private String id = UUID.randomUUID().toString();
    private String customerId = "default-customer-id";
    private String restaurantId = "default-restaurant-id";
    private String deliveryManId = null;
    private OrderStatus status = CREATED;
    private String address = "Pizza street, Naples, Italy";
    private List<ItemTestData> items = List.of(anItem());
    private BigDecimal deliveryCharge = new BigDecimal(5);
    private BigDecimal tip = new BigDecimal(0);
    private Map<String, String> metadata = new HashMap<>();

    private OrderTestData() {};

     static OrderTestData anOrder() {
        return new OrderTestData();
    }

     Order entity() {
        Order order = createAnEmptyOrder();
        setValue(order, "id", id);
        setValue(order, "customerId", customerId);
        setValue(order, "restaurantId", restaurantId);
        setValue(order, "deliveryManId", deliveryManId);
        setValue(order, "status", status);
        setValue(order, "address", address);
        setValue(order, "items", items.stream().map(ItemTestData::entity).toList());
        setValue(order, "deliveryCharge", deliveryCharge);
        setValue(order, "tip", tip);
        setValue(order, "metadata", metadata);

        order.calculateTotal();
        return order;
    }

     CreateOrder createOrder() {
        return new CreateOrder(id, customerId, restaurantId, items.stream().map(ItemTestData::dto).toList(), address, deliveryCharge);
    }

     OrderTestData withId(String id) {
        this.id = id;
        return this;
    }

     OrderTestData withCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

     OrderTestData withRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
        return this;
    }

     OrderTestData withDeliveryManId(String deliveryManId) {
        this.deliveryManId = deliveryManId;
        return this;
    }

     OrderTestData withStatus(OrderStatus status) {
        this.status = status;
        return this;
    }

     OrderTestData withAddress(String address) {
        this.address = address;
        return this;
    }

     OrderTestData withItems(ItemTestData... items) {
        this.items = Arrays.asList(items);
        return this;
    }

     OrderTestData withDeliveryCharge(double deliveryCharge) {
        this.deliveryCharge = new BigDecimal(deliveryCharge);
        return this;
    }

     OrderTestData withTip(BigDecimal tip) {
        this.tip = tip;
        return this;
    }

     OrderTestData withMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    private Order createAnEmptyOrder() {
        try {
            var constructor = Order.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct Order entity class for tests", e);
        }
    }

    private void setValue(Order order, String fieldName, Object value) {
        try {
            FieldUtils.writeField(order, fieldName, value, true);

        } catch (IllegalAccessException e) {
            throw new RuntimeException(format("Failed to set a %s field in Order entity class for tests", fieldName), e);
        }
    }
}
