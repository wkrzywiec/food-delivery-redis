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
import java.util.List;

import static io.wkrzywiec.fooddelivery.domain.ordering.OrderStatus.CREATED;

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

    private Order() {}

    private Order(String customerId, String restaurantId, List<Item> items, String address, BigDecimal deliveryCharge) {
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.items = items;
        this.address = address;
        this.deliveryCharge = deliveryCharge;
    }

    void calculateTotal() {
        this.total = items.stream()
                .map(item -> item.getPricePerItem().multiply(BigDecimal.valueOf(item.getAmount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(deliveryCharge);
    }

    public static Order from(CreateOrder createOrder) {
        var order = new Order(
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

    public List<Message> events() {
        return List.of();
    }
}
