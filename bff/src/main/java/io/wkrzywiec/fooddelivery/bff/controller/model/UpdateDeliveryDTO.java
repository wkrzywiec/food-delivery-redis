package io.wkrzywiec.fooddelivery.bff.controller.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDeliveryDTO {

    private String orderId;
    @Schema(allowableValues = {"prepareFood", "foodReady", "pickUpFood", "deliverFood"}, required = true)
    private String status;
}
