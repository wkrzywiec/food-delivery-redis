package io.wkrzywiec.fooddelivery.food.controller;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FoodItemDTO {
    private String id;
    private String name;
    private BigDecimal pricePerItem;
}
