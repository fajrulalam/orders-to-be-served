package com.example.orderstobeserved;

import java.util.ArrayList;
import java.util.Date;

public class OrderBlock {
    private int bungkus;
    private int customerNumber;
    private String namaCustomer;
    private ArrayList<NewOrderItem> orderItems;
    private String waktuPengambilan;
    private String waktuPesan;
    private String servingTime; // Duration for served orders
    private long orderTimestamp; // Unix timestamp in milliseconds for count-up timer

    private int total;

    // Constructor without servingTime (for pending orders)
    public OrderBlock(int bungkus, int customerNumber, String namaCustomer,
                      ArrayList<NewOrderItem> orderItems, String waktuPengambilan, String waktuPesan) {
        this.bungkus = bungkus;
        this.customerNumber = customerNumber;
        this.namaCustomer = namaCustomer;
        this.orderItems = orderItems;
        this.waktuPengambilan = waktuPengambilan;
        this.waktuPesan = waktuPesan;
        this.servingTime = "..."; // Default value
        this.orderTimestamp = 0; // Default, should be set later
    }

    // Constructor with servingTime (for served orders)
    public OrderBlock(int bungkus, int customerNumber, String namaCustomer,
                      ArrayList<NewOrderItem> orderItems, String waktuPengambilan,
                      String waktuPesan, String servingTime) {
        this.bungkus = bungkus;
        this.customerNumber = customerNumber;
        this.namaCustomer = namaCustomer;
        this.orderItems = orderItems;
        this.waktuPengambilan = waktuPengambilan;
        this.waktuPesan = waktuPesan;
        this.servingTime = servingTime;
        this.orderTimestamp = 0; // Default, should be set later
    }

    // Constructor with timestamp (for count-up timer in pending orders)
    public OrderBlock(int bungkus, int customerNumber, String namaCustomer,
                      ArrayList<NewOrderItem> orderItems, String waktuPengambilan,
                      String waktuPesan, long orderTimestamp, int total) {
        this.bungkus = bungkus;
        this.customerNumber = customerNumber;
        this.namaCustomer = namaCustomer;
        this.orderItems = orderItems;
        this.waktuPengambilan = waktuPengambilan;
        this.waktuPesan = waktuPesan;
        this.servingTime = "..."; // Default value
        this.orderTimestamp = orderTimestamp;
        this.total = total;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    // Getters and Setters
    public int getBungkus() {
        return bungkus;
    }

    public void setBungkus(int bungkus) {
        this.bungkus = bungkus;
    }

    public int getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(int customerNumber) {
        this.customerNumber = customerNumber;
    }

    public String getNamaCustomer() {
        return namaCustomer;
    }

    public void setNamaCustomer(String namaCustomer) {
        this.namaCustomer = namaCustomer;
    }

    public ArrayList<NewOrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(ArrayList<NewOrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public String getWaktuPengambilan() {
        return waktuPengambilan;
    }

    public void setWaktuPengambilan(String waktuPengambilan) {
        this.waktuPengambilan = waktuPengambilan;
    }

    public String getWaktuPesan() {
        return waktuPesan;
    }

    public void setWaktuPesan(String waktuPesan) {
        this.waktuPesan = waktuPesan;
    }

    public String getServingTime() {
        return servingTime;
    }

    public void setServingTime(String servingTime) {
        this.servingTime = servingTime;
    }

    public long getOrderTimestamp() {
        return orderTimestamp;
    }

    public void setOrderTimestamp(long orderTimestamp) {
        this.orderTimestamp = orderTimestamp;
    }

    // Get elapsed time since order was placed in a formatted string (mm:ss)
    public String getElapsedTimeFormatted() {
        if (orderTimestamp == 0) {
            return "00:00";
        }

        long currentTime = System.currentTimeMillis();
        long elapsedTimeMs = currentTime - orderTimestamp;
        long seconds = (elapsedTimeMs / 1000) % 60;
        long minutes = (elapsedTimeMs / (1000 * 60));

        // Format with leading zeros as mm:ss
        return String.format("%02d:%02d", minutes, seconds);
    }
}