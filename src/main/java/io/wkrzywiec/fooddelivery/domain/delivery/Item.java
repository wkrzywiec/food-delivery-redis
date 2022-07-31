package io.wkrzywiec.fooddelivery.domain.delivery;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@EqualsAndHashCode
@ToString
class Item {
    private String name;
    private int amount;
    private BigDecimal pricePerItem;
}
