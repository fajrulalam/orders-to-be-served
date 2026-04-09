package com.example.orderstobeserved;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NewOrderItem {
    private String namaPesanan;
    private String orderType; // "dine-in" or "take-away"
    private int quantity;     // the ordered quantity for this type
    private String status;
    private int preparedQuantity; // starts at 0
    private List<SelectedOption> selectedOptions;
    private boolean isMakanan = true; // true=food, false=beverage (default: food)
    private int harga; // unit price

    public NewOrderItem(String namaPesanan, String orderType, int quantity, String status, List<SelectedOption> selectedOptions) {
        this.namaPesanan = namaPesanan;
        this.orderType = orderType;
        this.quantity = quantity;
        this.status = status;
        this.preparedQuantity = 0;
        this.selectedOptions = selectedOptions != null ? selectedOptions : new ArrayList<>();
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

    public List<SelectedOption> getSelectedOptions() {
        return selectedOptions;
    }

    public void setSelectedOptions(List<SelectedOption> selectedOptions) {
        this.selectedOptions = selectedOptions != null ? selectedOptions : new ArrayList<>();
    }

    public boolean getIsMakanan() {
        return isMakanan;
    }

    public void setIsMakanan(boolean isMakanan) {
        this.isMakanan = isMakanan;
    }

    public int getHarga() {
        return harga;
    }

    public void setHarga(int harga) {
        this.harga = harga;
    }

    // Returns a stable key representing the unique combination of name + type + options
    public String getAggregationKey() {
        StringBuilder keyBuilder = new StringBuilder(namaPesanan + "_" + orderType);
        if (selectedOptions != null && !selectedOptions.isEmpty()) {
            List<String> optionIds = new ArrayList<>();
            for (SelectedOption opt : selectedOptions) {
                optionIds.add(opt.getOptionId());
            }
            Collections.sort(optionIds);
            for (String id : optionIds) {
                keyBuilder.append("_").append(id);
            }
        }
        return keyBuilder.toString();
    }

    // Increment preparedQuantity if it is less than quantity.
    public void incrementPrepared() {
        if (preparedQuantity < quantity) {
            preparedQuantity++;
        }
    }
}
