package io.wkrzywiec.fooddelivery.bff.view;

import io.wkrzywiec.fooddelivery.commons.incoming.Item;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Setter
@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryView {

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
}
