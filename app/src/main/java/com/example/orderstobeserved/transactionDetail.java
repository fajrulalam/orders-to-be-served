package com.example.orderstobeserved;

public class transactionDetail {

    private int transactionDetailID, transactionID, quantity, lineTotal, noCustomer;
    private  String itemID, timeStamp, status;
    public transactionDetail(){}

    public transactionDetail(int transactionDetailID, int transactionID, int quantity, int lineTotal, String itemID, String timeStamp, int noCustomer, String status) {
        this.transactionDetailID = transactionDetailID;
        this.transactionID = transactionID;
        this.quantity = quantity;
        this.lineTotal = lineTotal;
        this.itemID = itemID;
        this.timeStamp = timeStamp;
        this.noCustomer = noCustomer;
        this.status = status;

    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getNoCustomer() {
        return noCustomer;
    }

    public void setNoCustomer(int noCustomer) {
        this.noCustomer = noCustomer;
    }

    public int getTransactionDetailID() {
        return transactionDetailID;
    }

    public void setTransactionDetailID(int transactionDetailID) {
        this.transactionDetailID = transactionDetailID;
    }

    public int getTransactionID() {
        return transactionID;
    }

    public void setTransactionID(int transactionID) {
        this.transactionID = transactionID;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(int lineTotal) {
        this.lineTotal = lineTotal;
    }

    public String getItemID() {
        return itemID;
    }

    public void setItemID(String itemID) {
        this.itemID = itemID;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }
}

