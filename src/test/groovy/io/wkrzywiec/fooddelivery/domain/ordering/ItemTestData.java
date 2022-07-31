package io.wkrzywiec.fooddelivery.domain.ordering;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class ItemTestData {

    private String name = "Pizza Margherita";
    private int amount = 1;
    private BigDecimal pricePerItem = new BigDecimal(10);

    private ItemTestData() {};

    public Item entity() {
        return Item.builder()
                .name(name)
                .amount(amount)
                .pricePerItem(pricePerItem)
                .build();
    }

    public io.wkrzywiec.fooddelivery.domain.ordering.incoming.Item dto() {
        return new io.wkrzywiec.fooddelivery.domain.ordering.incoming.Item(name, amount, pricePerItem);
    }

    public static ItemTestData anItem() {
        return new ItemTestData();
    }

    public ItemTestData withName(String name) {
        this.name = name;
        return this;
    }

    public ItemTestData withPricePerItem(double price) {
        this.pricePerItem = new BigDecimal(price);
        return this;
    }

    public ItemTestData withAmount(int amount) {
        this.amount = amount;
        return this;
    }
}
