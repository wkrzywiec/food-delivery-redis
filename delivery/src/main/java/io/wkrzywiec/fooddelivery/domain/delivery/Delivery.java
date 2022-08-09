package io.wkrzywiec.fooddelivery.domain.delivery;

import io.wkrzywiec.fooddelivery.domain.delivery.incoming.OrderCreated;
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
            throw new DeliveryException(format("Failed to cancel a %s delivery. It's not possible do it for a delivery with '%s' status", id, status));
        }
        this.status = CANCELED;
        metadata.put("cancellationTimestamp", cancellationTimestamp.toString());

        if (reason != null) {
            metadata.put("cancellationReason", reason);
        }
    }

    public void foodInPreparation(Instant foodPreparationTimestamp) {
        if (status != CREATED) {
            throw new DeliveryException(format("Failed to start food preparation for a '%s' delivery. It's not possible do it for a delivery with '%s' status", id, status));
        }
        this.status = FOOD_IN_PREPARATION;
        metadata.put("foodPreparationTimestamp", foodPreparationTimestamp.toString());
    }

    public void foodReady(Instant foodReadyTimestamp) {
        if (status != FOOD_IN_PREPARATION) {
            throw new DeliveryException(format("Failed to set food ready for a '%s' delivery. It's not possible do it for a delivery with '%s' status", id, status));
        }
        this.status = FOOD_READY;
        metadata.put("foodReadyTimestamp", foodReadyTimestamp.toString());
    }

    public void pickUpFood(Instant foodPickedUpTimestamp) {
        if (status != FOOD_READY) {
            throw new DeliveryException(format("Failed to set food as picked up for a '%s' delivery. It's not possible do it for a delivery with '%s' status", id, status));
        }
        this.status = FOOD_PICKED;
        metadata.put("foodPickedUpTimestamp", foodPickedUpTimestamp.toString());
    }

    public void deliverFood(Instant foodDeliveredTimestamp) {
        if (status != FOOD_PICKED) {
            throw new DeliveryException(format("Failed to set food as delivered for a '%s' delivery. It's not possible do it for a delivery with '%s' status", id, status));
        }
        this.status = FOOD_DELIVERED;
        metadata.put("foodDeliveredTimestamp", foodDeliveredTimestamp.toString());
    }

    public void assignDeliveryMan(String deliveryManId) {
        if (this.deliveryManId != null) {
            throw new DeliveryException(format("Failed to assign delivery man to a '%s' delivery. There is already a delivery man assigned with an id %s", id, this.deliveryManId));
        }

        if (List.of(CANCELED, FOOD_PICKED, FOOD_DELIVERED).contains(status)) {
            throw new DeliveryException(format("Failed to assign a delivery man to a '%s' delivery. It's not possible do it for a delivery with '%s' status", id, status));
        }

        this.deliveryManId = deliveryManId;
    }

    public void unAssignDeliveryMan(String deliveryManId) {
        if (this.deliveryManId == null) {
            throw new DeliveryException(format("Failed to un assign delivery man from a '%s' delivery. There is no delivery man assigned to this delivery", id));
        }

        if (!this.deliveryManId.equals(deliveryManId)) {
            throw new DeliveryException(format("Failed to un assign delivery man from a '%s' delivery. Delivery has assigned '%s' person, but was asked to un assign '%s'", id, this.deliveryManId, deliveryManId));
        }

        if (List.of(CANCELED, FOOD_PICKED, FOOD_DELIVERED).contains(status)) {
            throw new DeliveryException(format("Failed to un assign a delivery man from a '%s' delivery. It's not possible do it for a delivery with '%s' status", id, status));
        }

        this.deliveryManId = null;
    }
}
