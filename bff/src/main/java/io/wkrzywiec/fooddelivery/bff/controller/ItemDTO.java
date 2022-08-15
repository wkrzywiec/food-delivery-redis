package io.wkrzywiec.fooddelivery.bff.controller;

import java.math.BigDecimal;

public record ItemDTO(String name, int amount, BigDecimal pricePerItem) {
}
