package io.wkrzywiec.fooddelivery.bff.controller.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeDeliveryManDTO {
    private String orderId;
    private String deliveryManId;
}
