package io.wkrzywiec.fooddelivery.domain.delivery;

import lombok.*;

import java.math.BigDecimal;

@Getter
@EqualsAndHashCode
@ToString
@Builder
class Item {
    private String name;
    private int amount;
    private BigDecimal pricePerItem;
}
