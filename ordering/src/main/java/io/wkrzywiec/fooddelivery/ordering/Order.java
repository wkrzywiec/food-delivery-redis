package io.wkrzywiec.fooddelivery.ordering;

import io.wkrzywiec.fooddelivery.commons.incoming.CreateOrder;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message;
import io.wkrzywiec.fooddelivery.ordering.outgoing.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.*;

import static io.wkrzywiec.fooddelivery.ordering.OrderStatus.*;
import static java.lang.String.format;

@Getter
@EqualsAndHashCode
@ToString
class Order {

    private String id;
    private String customerId;
    private String restaurantId;
    private OrderStatus status;
    private String address;
    private List<Item> items;
    private BigDecimal deliveryCharge;
    private BigDecimal tip = new BigDecimal(0);
    private BigDecimal total = new BigDecimal(0);
    private Map<String, String> metadata;

    private Order() {}

    private Order(String id, String customerId, String restaurantId, List<Item> items, String address, BigDecimal deliveryCharge) {
        this(id, customerId, restaurantId, CREATED, address, items, deliveryCharge, BigDecimal.ZERO, new HashMap<>());
    }

    private Order(String id, String customerId, String restaurantId, OrderStatus status, String address, List<Item> items, BigDecimal deliveryCharge, BigDecimal tip, Map<String, String> metadata) {
        if (id == null) {
            this.id = UUID.randomUUID().toString();
        } else {
            this.id = id;
        }
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.status = status;
        this.address = address;
        this.items = items;
        this.deliveryCharge = deliveryCharge;
        this.tip = tip;
        this.metadata = metadata;
        this.calculateTotal();
    }

    static Order from(CreateOrder createOrder) {
        var order = new Order(
                createOrder.orderId(),
                createOrder.customerId(),
                createOrder.restaurantId(),
                mapItems(createOrder.items()),
                createOrder.address(),
                createOrder.deliveryCharge());

        return order;
    }

    private static List<Item> mapItems(List<io.wkrzywiec.fooddelivery.commons.incoming.Item> items) {
        return items.stream().map(dto -> Item.builder()
                .name(dto.name())
                .amount(dto.amount())
                .pricePerItem(dto.pricePerItem())
                .build()).toList();
    }

    static Order from(List<Message> events) {
        Order order = null;
        for (Message event: events) {
            if (event.body() instanceof OrderCreated created) {
                order = new Order(
                        created.orderId(), created.customerId(),
                        created.restaurantId(), mapItems(created.items()),
                        created.address(), created.deliveryCharge()
                );
            }

            if (event.body() instanceof OrderCanceled canceled) {
                var meta = order.getMetadata();
                meta.put("cancellationReason", canceled.reason());
                order = new Order(
                        order.getId(), order.getCustomerId(),
                        order.getRestaurantId(), CANCELED,
                        order.getAddress(), order.getItems(),
                        order.getDeliveryCharge(), order.getTip(),
                        meta
                );
            }

            if (event.body() instanceof OrderInProgress) {
                order = new Order(
                        order.getId(), order.getCustomerId(),
                        order.getRestaurantId(), IN_PROGRESS,
                        order.getAddress(), order.getItems(),
                        order.getDeliveryCharge(), order.getTip(),
                        order.getMetadata()
                );
            }

            if (event.body() instanceof TipAddedToOrder tipAdded) {
                order = new Order(
                        order.getId(), order.getCustomerId(),
                        order.getRestaurantId(), order.getStatus(),
                        order.getAddress(), order.getItems(),
                        order.getDeliveryCharge(), tipAdded.tip(),
                        order.getMetadata()
                );
            }

            if (event.body() instanceof OrderCompleted) {
                order = new Order(
                        order.getId(), order.getCustomerId(),
                        order.getRestaurantId(), COMPLETED,
                        order.getAddress(), order.getItems(),
                        order.getDeliveryCharge(), order.getTip(),
                        order.getMetadata()
                );
            }
        }
        return order;
    }

    void calculateTotal() {
        this.total = items.stream()
                .map(item -> item.getPricePerItem().multiply(BigDecimal.valueOf(item.getAmount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(deliveryCharge)
                .add(tip);
    }

    void cancelOrder(String reason) {
        if (status != OrderStatus.CREATED) {
            throw new OrderingException(format("Failed to cancel an %s order. It's not possible to cancel an order with '%s' status", id, status));
        }
        this.status = CANCELED;

        if (reason != null) {
            metadata.put("cancellationReason", reason);
        }
    }

    void setInProgress() {
        if (status == CREATED) {
            this.status = IN_PROGRESS;
            return;
        }
        throw new OrderingException(format("Failed to set an '%s' order to IN_PROGRESS. It's not allowed to do it for an order with '%s' status", id, status));
    }

    public void addTip(BigDecimal tip) {
        this.tip = tip;
        this.calculateTotal();
    }

    public void complete() {
        if (status == IN_PROGRESS) {
            this.status = COMPLETED;
            return;
        }
        throw new OrderingException(format("Failed to set an '%s' order to COMPLETED. It's not allowed to do it for an order with '%s' status", id, status));
    }
}
