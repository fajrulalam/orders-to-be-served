package com.example.orderstobeserved;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RecyclerAdapter  extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    private static final String TAG = "RecycleAdapter";
    int count = 0;

    ArrayList<Integer> NewCustomerNumber;
    ArrayList<String> NewOrders;

    public RecyclerAdapter(ArrayList<Integer> newCustomerNumber, ArrayList<String> newOrders) {
        this.NewCustomerNumber = newCustomerNumber;
        this.NewOrders = newOrders;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        Log.i(TAG, "onCreateViewHolder: " + count++ );

        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.orders_to_be_served, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.pesananTextView.setText(String.valueOf(NewOrders.get(position)));
        holder.noCustomerTextView.setText(String.valueOf(NewCustomerNumber.get(position)));
    }

    @Override
    public int getItemCount() {
        return NewCustomerNumber.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView noCustomerTextView;
        TextView pesananTextView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            noCustomerTextView = itemView.findViewById(R.id.noCustomerTextView);
            pesananTextView = itemView.findViewById(R.id.pesananTextView);
        }
    }
}
