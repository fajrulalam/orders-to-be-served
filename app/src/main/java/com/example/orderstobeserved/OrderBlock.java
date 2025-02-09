package com.example.orderstobeserved;

import java.util.ArrayList;

public class OrderBlock {
    private int bungkus;
    private int customerNumber;
    private String namaCustomer;
    private ArrayList<NewOrderItem> orderItems;
    private String waktuPengambilan;
    private String waktuPesan;

    public OrderBlock(int bungkus, int customerNumber, String namaCustomer,
                      ArrayList<NewOrderItem> orderItems, String waktuPengambilan, String waktuPesan) {
        this.bungkus = bungkus;
        this.customerNumber = customerNumber;
        this.namaCustomer = namaCustomer;
        this.orderItems = orderItems;
        this.waktuPengambilan = waktuPengambilan;
        this.waktuPesan = waktuPesan;
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
}