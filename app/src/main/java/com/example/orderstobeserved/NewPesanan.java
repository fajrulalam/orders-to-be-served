package com.example.orderstobeserved;

public class NewPesanan {
    int customerNumber;
    String rincianPesanan;
    int bungkus_or_not;
    String waktuPengambilan;
    String waktuPesan;
    String waktuServe;

    public NewPesanan(int customerNumber, String rincianPesanan, int bungkus_or_not, String waktuPengambilan, String waktuPesan, String waktuServe) {
        this.customerNumber = customerNumber;
        this.rincianPesanan = rincianPesanan;
        this.bungkus_or_not = bungkus_or_not;
        this.waktuPengambilan = waktuPengambilan;
        this.waktuPesan = waktuPesan;
        this.waktuServe = waktuServe;
    }


    public int getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(int customerNumber) {
        this.customerNumber = customerNumber;
    }

    public String getRincianPesanan() {
        return rincianPesanan;
    }

    public void setRincianPesanan(String rincianPesanan) {
        this.rincianPesanan = rincianPesanan;
    }

    public int getBungkus_or_not() {
        return bungkus_or_not;
    }

    public void setBungkus_or_not(int bungkus_or_not) {
        this.bungkus_or_not = bungkus_or_not;
    }

    public String getWaktuPengambilan() {
        return waktuPengambilan;
    }

    public void setWaktuPengambilan(String waktuPengambilan) {
        this.waktuPengambilan = waktuPengambilan;
    }
}
