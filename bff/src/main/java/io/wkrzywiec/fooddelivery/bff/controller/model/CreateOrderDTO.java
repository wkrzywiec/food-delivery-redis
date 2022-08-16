package io.wkrzywiec.fooddelivery.bff.controller.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderDTO {

    private String id;
    private String customerId;
    private String restaurantId;
    private List<ItemDTO> items;
    private String address;
    private BigDecimal deliveryCharge;
}
