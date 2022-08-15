package io.wkrzywiec.fooddelivery.delivery;

import io.wkrzywiec.fooddelivery.delivery.outgoing.Item;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
class ItemTestData {

    private String name = "Pizza Margherita";
    private int amount = 1;
    private BigDecimal pricePerItem = new BigDecimal(10);

    private ItemTestData() {};

    io.wkrzywiec.fooddelivery.delivery.Item entity() {
        return io.wkrzywiec.fooddelivery.delivery.Item.builder()
                .name(name)
                .amount(amount)
                .pricePerItem(pricePerItem)
                .build();
    }

    Item dto() {
        return new Item(name, amount, pricePerItem);
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

    ItemTestData withAmount(int amount) {
        this.amount = amount;
        return this;
    }
}
