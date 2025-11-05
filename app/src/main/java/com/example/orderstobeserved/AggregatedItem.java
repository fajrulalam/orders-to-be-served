package com.example.orderstobeserved;

import java.util.ArrayList;
import java.util.List;

public class AggregatedItem {
    private String itemName;
    private String orderType; // "dine-in" or "take-away"
    private int totalQuantity;
    private int servedQuantity;
    
    // References to the actual order items for synchronization
    private List<ItemReference> itemReferences;

    public AggregatedItem(String itemName, String orderType) {
        this.itemName = itemName;
        this.orderType = orderType;
        this.totalQuantity = 0;
        this.servedQuantity = 0;
        this.itemReferences = new ArrayList<>();
    }

    // Add a reference to an order item
    public void addItemReference(int customerNumber, NewOrderItem orderItem) {
        itemReferences.add(new ItemReference(customerNumber, orderItem));
        totalQuantity += orderItem.getQuantity();
        servedQuantity += orderItem.getPreparedQuantity();
    }

    // Get all item references
    public List<ItemReference> getItemReferences() {
        return itemReferences;
    }

    // Getters
    public String getItemName() {
        return itemName;
    }

    public String getOrderType() {
        return orderType;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getServedQuantity() {
        return servedQuantity;
    }

    // Setters
    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public void setServedQuantity(int servedQuantity) {
        this.servedQuantity = servedQuantity;
    }

    // Check if all items are served
    public boolean isFullyServed() {
        return servedQuantity >= totalQuantity;
    }

    // Recalculate totals from references
    public void recalculateTotals() {
        totalQuantity = 0;
        servedQuantity = 0;
        for (ItemReference ref : itemReferences) {
            totalQuantity += ref.getOrderItem().getQuantity();
            servedQuantity += ref.getOrderItem().getPreparedQuantity();
        }
    }

    // Inner class to hold reference to actual order items
    public static class ItemReference {
        private int customerNumber;
        private NewOrderItem orderItem;

        public ItemReference(int customerNumber, NewOrderItem orderItem) {
            this.customerNumber = customerNumber;
            this.orderItem = orderItem;
        }

        public int getCustomerNumber() {
            return customerNumber;
        }

        public NewOrderItem getOrderItem() {
            return orderItem;
        }
    }

    // Create a unique key for aggregation
    public static String createKey(String itemName, String orderType) {
        return itemName + "_" + orderType;
    }

    public String getKey() {
        return createKey(itemName, orderType);
    }
}


