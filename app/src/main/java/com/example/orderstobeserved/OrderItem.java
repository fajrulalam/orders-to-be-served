package com.example.orderstobeserved;

public class OrderItem {
    private String itemName;
    private int totalQuantity;
    private int preparedQuantity;

    public OrderItem(String itemName, int totalQuantity) {
        this.itemName = itemName;
        this.totalQuantity = totalQuantity;
        this.preparedQuantity = 0;
    }

    public String getItemName() {
        return itemName;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getPreparedQuantity() {
        return preparedQuantity;
    }

    // Increment the prepared quantity if possible.
    public void incrementPrepared() {
        if (preparedQuantity < totalQuantity) {
            preparedQuantity++;
        }
    }
}