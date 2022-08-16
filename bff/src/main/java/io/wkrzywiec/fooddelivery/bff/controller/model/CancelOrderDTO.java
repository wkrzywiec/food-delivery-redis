package io.wkrzywiec.fooddelivery.bff.controller.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderDTO {

    private String orderId;
    private String reason;
}
