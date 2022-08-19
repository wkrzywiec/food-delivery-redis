package io.wkrzywiec.fooddelivery.delivery;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message;
import io.wkrzywiec.fooddelivery.delivery.incoming.OrderCreated;
import io.wkrzywiec.fooddelivery.delivery.outgoing.*;
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

import static java.lang.String.format;

@Getter
@EqualsAndHashCode
@ToString
class Delivery {
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
    @Type(type = "io.wkrzywiec.fooddelivery.commons.infra.repository.MapJsonbType")
    private Map<String, String> metadata = new HashMap<>();

    private Delivery() {};

    private Delivery(String orderId, String customerId, String restaurantId, String address, List<Item> items, BigDecimal deliveryCharge, BigDecimal total, Instant creationTimestamp) {
        this(orderId, customerId, restaurantId, null, DeliveryStatus.CREATED, address, items, deliveryCharge, BigDecimal.ZERO, total, new HashMap<>());
        this.metadata.put("creationTimestamp", creationTimestamp.toString());
    }

    private Delivery(String orderId, String customerId, String restaurantId, String deliveryManId, DeliveryStatus status, String address, List<Item> items, BigDecimal deliveryCharge, BigDecimal tip, BigDecimal total, Map<String, String> metadata) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.deliveryManId = deliveryManId;
        this.status = status;
        this.address = address;
        this.items = items;
        this.deliveryCharge = deliveryCharge;
        this.tip = tip;
        this.total = total;
        this.metadata = metadata;
    }

    public static Delivery from(OrderCreated orderCreated, Instant creationTimestamp) {
        return new Delivery(
                orderCreated.orderId(),
                orderCreated.customerId(),
                orderCreated.restaurantId(),
                orderCreated.address(),
                mapItems(orderCreated.items()),
                orderCreated.deliveryCharge(),
                orderCreated.total(),
                creationTimestamp
        );
    }

    private static List<Item> mapItems(List<io.wkrzywiec.fooddelivery.delivery.incoming.Item> items) {
        return items.stream().map(dto -> Item.builder()
                .name(dto.name())
                .amount(dto.amount())
                .pricePerItem(dto.pricePerItem())
                .build()).toList();
    }

    public static Delivery from(List<Message> events) {
        Delivery delivery = null;
        for (Message event: events) {
            System.out.println(event.body());
            if (event.body() instanceof DeliveryCreated created) {
                Map<String, String> metadata = new HashMap<>();
                metadata.put("creationTimestamp", event.header().createdAt().toString());
                delivery = new Delivery(
                        created.orderId(), created.customerId(),
                        created.restaurantId(), null, DeliveryStatus.CREATED,
                        created.address(), mapItems(created.items()),
                        created.deliveryCharge(), BigDecimal.ZERO,
                        created.total(), metadata
                );
            }

            if (event.body() instanceof TipAddedToDelivery tipAddedToDelivery) {
                delivery = new Delivery(
                        delivery.getOrderId(), delivery.getCustomerId(),
                        delivery.getRestaurantId(), delivery.getDeliveryManId(),
                        delivery.getStatus(), delivery.getAddress(),
                        delivery.getItems(), delivery.getDeliveryCharge(),
                        tipAddedToDelivery.tip(), tipAddedToDelivery.total(), delivery.getMetadata()
                );
            }

            if (event.body() instanceof DeliveryCanceled canceled) {
                var metadata = delivery.getMetadata();
                metadata.put("cancellationReason", canceled.reason());
                metadata.put("cancellationTimestamp", event.header().createdAt().toString());

                delivery = new Delivery(
                        delivery.getOrderId(), delivery.getCustomerId(),
                        delivery.getRestaurantId(), delivery.getDeliveryManId(),
                        DeliveryStatus.CANCELED, delivery.getAddress(),
                        delivery.getItems(), delivery.getDeliveryCharge(),
                        delivery.getTip(), delivery.getTotal(), metadata
                );
            }

            if (event.body() instanceof FoodInPreparation) {
                var metadata = delivery.getMetadata();
                metadata.put("foodPreparationTimestamp", event.header().createdAt().toString());

                delivery = new Delivery(
                        delivery.getOrderId(), delivery.getCustomerId(),
                        delivery.getRestaurantId(), delivery.getDeliveryManId(),
                        DeliveryStatus.FOOD_IN_PREPARATION, delivery.getAddress(),
                        delivery.getItems(), delivery.getDeliveryCharge(),
                        delivery.getTip(), delivery.getTotal(), metadata
                );
            }

            if (event.body() instanceof DeliveryManAssigned deliveryManAssigned) {
                delivery = new Delivery(
                        delivery.getOrderId(), delivery.getCustomerId(),
                        delivery.getRestaurantId(), deliveryManAssigned.deliveryManId(),
                        delivery.getStatus(), delivery.getAddress(),
                        delivery.getItems(), delivery.getDeliveryCharge(),
                        delivery.getTip(), delivery.getTotal(), delivery.getMetadata()
                );
            }

            if (event.body() instanceof DeliveryManUnAssigned deliveryManUnAssigned) {
                delivery = new Delivery(
                        delivery.getOrderId(), delivery.getCustomerId(),
                        delivery.getRestaurantId(), null,
                        delivery.getStatus(), delivery.getAddress(),
                        delivery.getItems(), delivery.getDeliveryCharge(),
                        delivery.getTip(), delivery.getTotal(), delivery.getMetadata()
                );
            }

            if (event.body() instanceof FoodIsReady) {
                var metadata = delivery.getMetadata();
                metadata.put("foodReadyTimestamp", event.header().createdAt().toString());

                delivery = new Delivery(
                        delivery.getOrderId(), delivery.getCustomerId(),
                        delivery.getRestaurantId(), delivery.getDeliveryManId(),
                        DeliveryStatus.FOOD_READY, delivery.getAddress(),
                        delivery.getItems(), delivery.getDeliveryCharge(),
                        delivery.getTip(), delivery.getTotal(), metadata
                );
            }

            if (event.body() instanceof FoodWasPickedUp) {
                var metadata = delivery.getMetadata();
                metadata.put("foodPickedUpTimestamp", event.header().createdAt().toString());

                delivery = new Delivery(
                        delivery.getOrderId(), delivery.getCustomerId(),
                        delivery.getRestaurantId(), delivery.getDeliveryManId(),
                        DeliveryStatus.FOOD_PICKED, delivery.getAddress(),
                        delivery.getItems(), delivery.getDeliveryCharge(),
                        delivery.getTip(), delivery.getTotal(), metadata
                );
            }

            if (event.body() instanceof FoodDelivered) {
                var metadata = delivery.getMetadata();
                metadata.put("foodDeliveredTimestamp", event.header().createdAt().toString());

                delivery = new Delivery(
                        delivery.getOrderId(), delivery.getCustomerId(),
                        delivery.getRestaurantId(), delivery.getDeliveryManId(),
                        DeliveryStatus.FOOD_DELIVERED, delivery.getAddress(),
                        delivery.getItems(), delivery.getDeliveryCharge(),
                        delivery.getTip(), delivery.getTotal(), metadata
                );
            }
        }
        System.out.println(delivery);
        return delivery;
    }

    public void cancel(String reason, Instant cancellationTimestamp) {
        if (status != DeliveryStatus.CREATED) {
            throw new DeliveryException(format("Failed to cancel a %s delivery. It's not possible do it for a delivery with '%s' status", orderId, status));
        }
        this.status = DeliveryStatus.CANCELED;
        metadata.put("cancellationTimestamp", cancellationTimestamp.toString());

        if (reason != null) {
            metadata.put("cancellationReason", reason);
        }
    }

    public void foodInPreparation(Instant foodPreparationTimestamp) {
        if (status != DeliveryStatus.CREATED) {
            throw new DeliveryException(format("Failed to start food preparation for an '%s' order. It's not possible do it for a delivery with '%s' status", orderId, status));
        }
        this.status = DeliveryStatus.FOOD_IN_PREPARATION;
        metadata.put("foodPreparationTimestamp", foodPreparationTimestamp.toString());
    }

    public void foodReady(Instant foodReadyTimestamp) {
        if (status != DeliveryStatus.FOOD_IN_PREPARATION) {
            throw new DeliveryException(format("Failed to set food ready for an '%s' order. It's not possible do it for a delivery with '%s' status", orderId, status));
        }
        this.status = DeliveryStatus.FOOD_READY;
        metadata.put("foodReadyTimestamp", foodReadyTimestamp.toString());
    }

    public void pickUpFood(Instant foodPickedUpTimestamp) {
        if (status != DeliveryStatus.FOOD_READY) {
            throw new DeliveryException(format("Failed to set food as picked up for an '%s' order. It's not possible do it for a delivery with '%s' status", orderId, status));
        }
        this.status = DeliveryStatus.FOOD_PICKED;
        metadata.put("foodPickedUpTimestamp", foodPickedUpTimestamp.toString());
    }

    public void deliverFood(Instant foodDeliveredTimestamp) {
        if (status != DeliveryStatus.FOOD_PICKED) {
            throw new DeliveryException(format("Failed to set food as delivered for an '%s' order. It's not possible do it for a delivery with '%s' status", orderId, status));
        }
        this.status = DeliveryStatus.FOOD_DELIVERED;
        metadata.put("foodDeliveredTimestamp", foodDeliveredTimestamp.toString());
    }

    public void assignDeliveryMan(String deliveryManId) {
        if (this.deliveryManId != null) {
            throw new DeliveryException(format("Failed to assign delivery man to an '%s' order. There is already a delivery man assigned with an orderId %s", orderId, this.deliveryManId));
        }

        if (List.of(DeliveryStatus.CANCELED, DeliveryStatus.FOOD_PICKED, DeliveryStatus.FOOD_DELIVERED).contains(status)) {
            throw new DeliveryException(format("Failed to assign a delivery man to an '%s' order. It's not possible do it for a delivery with '%s' status", orderId, status));
        }

        this.deliveryManId = deliveryManId;
    }

    public void unAssignDeliveryMan() {
        if (this.deliveryManId == null) {
            throw new DeliveryException(format("Failed to un assign delivery man from an '%s' order. There is no delivery man assigned to this delivery", orderId));
        }

        if (List.of(DeliveryStatus.CANCELED, DeliveryStatus.FOOD_PICKED, DeliveryStatus.FOOD_DELIVERED).contains(status)) {
            throw new DeliveryException(format("Failed to un assign a delivery man from an '%s' order. It's not possible do it for a delivery with '%s' status", orderId, status));
        }

        this.deliveryManId = null;
    }

    public void addTip(BigDecimal tip, BigDecimal total) {
        this.tip = tip;
        this.total = total;
    }
}
