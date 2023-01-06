package com.example.orderstobeserved;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    private static final String TAG = "RecycleAdapter";
    int count = 0;

//    ArrayList<Integer> NewCustomerNumber;
//    ArrayList<String> NewOrders;
//    ArrayList<String> NewQuantity;
//    ArrayList<String> NewBungkusArrayList;
//    ArrayList<String> NewWaktuPengambilan;
//
//    public RecyclerAdapter(ArrayList<Integer> newCustomerNumber, ArrayList<String> newOrders, ArrayList<String> NewQuantity, ArrayList<String> NewBungkusArrayList, ArrayList<String> NewWaktuPengambilan) {
//        this.NewCustomerNumber = newCustomerNumber;
//        this.NewOrders = newOrders;
//        this.NewQuantity = NewQuantity;
//        this.NewBungkusArrayList = NewBungkusArrayList;
//        this.NewWaktuPengambilan =  NewWaktuPengambilan;
//    }

    ArrayList<NewPesanan> newPesananArrayList;

    public RecyclerAdapter(ArrayList<NewPesanan> newPesananArrayList) {
        this.newPesananArrayList = newPesananArrayList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        Log.i(TAG, "onCreateViewHolder: " + count++);
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.orders_to_be_served, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {


        int bungkus = (newPesananArrayList.get(position).bungkus_or_not);
        if (bungkus == 1 ) {
            holder.noCustomerTextView.setBackgroundColor(Color.parseColor("#F9A825"));

        } else if (bungkus == 2) {
            holder.noCustomerTextView.setBackgroundColor(Color.parseColor("#FFC62828"));
            holder.waktuPengambilan.setText(newPesananArrayList.get(position).waktuPengambilan);
            holder.waktuPengambilan.setVisibility(View.VISIBLE);

        }
            holder.pesananTextView.setText(String.valueOf(newPesananArrayList.get(position).rincianPesanan));
            holder.noCustomerTextView.setText(String.valueOf(newPesananArrayList.get(position).customerNumber));

    }

    @Override
    public int getItemCount() {
        return newPesananArrayList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView noCustomerTextView;
        TextView pesananTextView;
        TextView waktuPengambilan;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            noCustomerTextView = itemView.findViewById(R.id.noCustomerTextView);
            pesananTextView = itemView.findViewById(R.id.pesananTextView);
            waktuPengambilan = itemView.findViewById(R.id.waktuPengambilanMerah);
        }
    }
}
