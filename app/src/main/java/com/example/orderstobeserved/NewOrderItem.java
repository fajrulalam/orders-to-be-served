package com.example.orderstobeserved;

public class NewOrderItem {
    private String namaPesanan;
    private String orderType; // "dine-in" or "take-away"
    private int quantity;     // the ordered quantity for this type
    private String status;
    private int preparedQuantity; // starts at 0

    public NewOrderItem(String namaPesanan, String orderType, int quantity, String status) {
        this.namaPesanan = namaPesanan;
        this.orderType = orderType;
        this.quantity = quantity;
        this.status = status;
        this.preparedQuantity = 0;
    }

    // Getters and Setters
    public String getNamaPesanan() {
        return namaPesanan;
    }

    public void setNamaPesanan(String namaPesanan) {
        this.namaPesanan = namaPesanan;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPreparedQuantity() {
        return preparedQuantity;
    }

    public void setPreparedQuantity(int preparedQuantity) {
        this.preparedQuantity = preparedQuantity;
    }

    // Increment preparedQuantity if it is less than quantity.
    public void incrementPrepared() {
        if (preparedQuantity < quantity) {
            preparedQuantity++;
        }
    }
}