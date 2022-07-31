package io.wkrzywiec.fooddelivery.domain.ordering;

import io.wkrzywiec.fooddelivery.domain.ordering.incoming.CreateOrder;
import io.wkrzywiec.fooddelivery.infra.messaging.Message;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.wkrzywiec.fooddelivery.domain.ordering.OrderStatus.*;
import static java.lang.String.format;

@Entity
@Getter
@EqualsAndHashCode
@ToString
class Order {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    private String id;
    private String customerId;
    private String restaurantId;
    private String deliveryManId;
    private OrderStatus status = CREATED;
    private String address;
    private List<Item> items;
    private BigDecimal deliveryCharge = new BigDecimal(0);
    private BigDecimal tip = new BigDecimal(0);
    private BigDecimal total = new BigDecimal(0);
    private Map<String, String> metadata = new HashMap<>();

    private Order() {}

    private Order(String id, String customerId, String restaurantId, List<Item> items, String address, BigDecimal deliveryCharge) {
        if (id == null) {
            this.id = UUID.randomUUID().toString();
        } else {
            this.id = id;
        }
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.items = items;
        this.address = address;
        this.deliveryCharge = deliveryCharge;
    }

    static Order from(CreateOrder createOrder) {
        var order = new Order(
                createOrder.id(),
                createOrder.customerId(),
                createOrder.restaurantId(),
                createOrder.items().stream().map(dto -> Item.builder()
                        .name(dto.name())
                        .amount(dto.amount())
                        .pricePerItem(dto.pricePerItem())
                        .build()).toList(),
                createOrder.address(),
                createOrder.deliveryCharge());
        order.calculateTotal();
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
        this.status = CANCELLED;

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
