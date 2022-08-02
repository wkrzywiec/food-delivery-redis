package io.wkrzywiec.fooddelivery.domain.delivery;

import io.wkrzywiec.fooddelivery.domain.ordering.outgoing.OrderCreated;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.wkrzywiec.fooddelivery.domain.delivery.DeliveryStatus.*;
import static java.lang.String.format;

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
    @Type(type = "io.wkrzywiec.fooddelivery.infra.repository.MapJsonbType")
    private Map<String, String> metadata = new HashMap<>();

    private Delivery() {};

    private Delivery(String orderId, String customerId, String restaurantId, String address, List<Item> items, BigDecimal deliveryCharge, BigDecimal total, Instant creationTimestamp) {
        this.id = UUID.randomUUID().toString();
        this.orderId = orderId;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.address = address;
        this.items = items;
        this.deliveryCharge = deliveryCharge;
        this.total = total;
        this.status = CREATED;
        this.metadata.put("creationTimestamp", creationTimestamp.toString());
    }

    public static Delivery from(OrderCreated orderCreated, Instant creationTimestamp) {
        return new Delivery(
                orderCreated.id(),
                orderCreated.customerId(),
                orderCreated.restaurantId(),
                orderCreated.address(),
                orderCreated.items().stream().map(dto -> new Item(dto.name(), dto.amount(), dto.pricePerItem())).toList(),
                orderCreated.deliveryCharge(),
                orderCreated.total(),
                creationTimestamp
        );
    }

    public void cancel(String reason, Instant cancellationTimestamp) {
        if (status != CREATED) {
            throw new DeliveryException(format("Failed to cancel a %s delivery. It's not possible to cancel a delivery with '%s' status", id, status));
        }
        this.status = CANCELED;
        metadata.put("cancellationTimestamp", cancellationTimestamp.toString());

        if (reason != null) {
            metadata.put("cancellationReason", reason);
        }
    }

    public void foodInPreparation(Instant startFoodPreparationTimestamp) {
        if (status != CREATED) {
            throw new DeliveryException(format("Failed to start food preparation for a '%s' delivery. It's not possible to cancel a delivery with '%s' status", id, status));
        }
        this.status = FOOD_IN_PREPARATION;
        metadata.put("startFoodPreparationTimestamp", startFoodPreparationTimestamp.toString());
    }
}
