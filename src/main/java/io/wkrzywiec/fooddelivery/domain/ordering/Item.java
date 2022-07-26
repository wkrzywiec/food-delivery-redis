package io.wkrzywiec.fooddelivery.domain.ordering;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.persistence.Entity;
import java.math.BigDecimal;

@Entity
@Getter
@EqualsAndHashCode
@ToString
@Builder
class Item {

    private String name;
    private int amount;
    private BigDecimal pricePerItem;
}
