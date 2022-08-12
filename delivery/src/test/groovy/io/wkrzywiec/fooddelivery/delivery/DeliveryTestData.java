package io.wkrzywiec.fooddelivery.delivery;

import lombok.Getter;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.math.BigDecimal;
import java.util.*;

import static java.lang.String.format;

@Getter
 class DeliveryTestData {

    private String orderId = UUID.randomUUID().toString();
    private String customerId = "default-customer-orderId";
    private String restaurantId = "default-restaurant-orderId";
    private String deliveryManId = null;
    private DeliveryStatus status = DeliveryStatus.CREATED;
    private String address = "Pizza street, Naples, Italy";
    private List<ItemTestData> items = List.of(ItemTestData.anItem());
    private BigDecimal deliveryCharge = new BigDecimal(5);
    private BigDecimal tip = new BigDecimal(0);
    private BigDecimal total = new BigDecimal(0);
    private Map<String, String> metadata = new HashMap<>();

    private DeliveryTestData() {};

     static DeliveryTestData aDelivery() {
        return new DeliveryTestData();
    }

     Delivery entity() {
        Delivery delivery = createAnEmptyDelivery();
        setValue(delivery, "orderId", orderId);
        setValue(delivery, "customerId", customerId);
        setValue(delivery, "restaurantId", restaurantId);
        setValue(delivery, "deliveryManId", deliveryManId);
        setValue(delivery, "status", status);
        setValue(delivery, "address", address);
        setValue(delivery, "items", items.stream().map(ItemTestData::entity).toList());
        setValue(delivery, "deliveryCharge", deliveryCharge);
        setValue(delivery, "tip", tip);
         setValue(delivery, "total", total);
        setValue(delivery, "metadata", metadata);

        return delivery;
    }

    DeliveryTestData withOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

     DeliveryTestData withCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

     DeliveryTestData withRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
        return this;
    }

     DeliveryTestData withDeliveryManId(String deliveryManId) {
        this.deliveryManId = deliveryManId;
        return this;
    }

     DeliveryTestData withStatus(DeliveryStatus status) {
        this.status = status;
        return this;
    }

     DeliveryTestData withAddress(String address) {
        this.address = address;
        return this;
    }

     DeliveryTestData withItems(ItemTestData... items) {
        this.items = Arrays.asList(items);
        return this;
    }

     DeliveryTestData withDeliveryCharge(double deliveryCharge) {
        this.deliveryCharge = new BigDecimal(deliveryCharge);
        return this;
    }

     DeliveryTestData withTip(BigDecimal tip) {
        this.tip = tip;
        return this;
    }

    DeliveryTestData withTotal(BigDecimal total) {
        this.total = total;
        return this;
    }

     DeliveryTestData withMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    private Delivery createAnEmptyDelivery() {
        try {
            var constructor = Delivery.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct Delivery entity class for tests", e);
        }
    }

    private void setValue(Delivery Delivery, String fieldName, Object value) {
        try {
            FieldUtils.writeField(Delivery, fieldName, value, true);

        } catch (IllegalAccessException e) {
            throw new RuntimeException(format("Failed to set a %s field in Delivery entity class for tests", fieldName), e);
        }
    }
}
