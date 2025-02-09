package com.example.orderstobeserved;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    private ArrayList<OrderBlock> orderBlockArrayList;

    public RecyclerAdapter(ArrayList<OrderBlock> orderBlockArrayList) {
        this.orderBlockArrayList = orderBlockArrayList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.orders_to_be_served, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderBlock currentOrder = orderBlockArrayList.get(position);
        holder.noCustomerTextView.setText(String.valueOf(currentOrder.getCustomerNumber()));

        // Set background based on bungkus value
        if (currentOrder.getBungkus() == 1) {
            holder.noCustomerTextView.setBackgroundColor(Color.parseColor("#F9A825"));
        } else if (currentOrder.getBungkus() == 2) {
            holder.noCustomerTextView.setBackgroundColor(Color.parseColor("#FFC62828"));
            if (!currentOrder.getWaktuPengambilan().isEmpty()) {
                holder.waktuPengambilan.setText(currentOrder.getWaktuPengambilan());
                holder.waktuPengambilan.setVisibility(View.VISIBLE);
            }
        } else {
            holder.noCustomerTextView.setBackgroundColor(Color.parseColor("#FF1565C0"));
        }

        // Remove any previous views from the container
        holder.orderItemsContainer.removeAllViews();

        // For each NewOrderItem, create a dynamic Button
        for (final NewOrderItem orderItem : currentOrder.getOrderItems()) {
            final Button orderButton = new Button(holder.itemView.getContext());
            orderButton.setAllCaps(false);
            orderButton.setBackgroundTintList(ColorStateList.valueOf(Color.LTGRAY));

            // Compute total quantity as dineInQuantity + takeAwayQuantity
            final int totalQuantity = orderItem.getQuantity();
            orderButton.setText(orderItem.getNamaPesanan() + " (0/" + totalQuantity + ")");

            orderButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (orderItem.getPreparedQuantity() < totalQuantity) {
                        orderItem.incrementPrepared();
                        orderButton.setText(orderItem.getNamaPesanan() + " ("
                                + orderItem.getPreparedQuantity() + "/" + totalQuantity + ")");
                        if (orderItem.getPreparedQuantity() == totalQuantity) {
                            orderButton.setPaintFlags(orderButton.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                            orderButton.setEnabled(false);
                        }
                    }
                }
            });

            holder.orderItemsContainer.addView(orderButton);
        }
    }

    @Override
    public int getItemCount() {
        return orderBlockArrayList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView noCustomerTextView;
        TextView waktuPengambilan;
        LinearLayout orderItemsContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            noCustomerTextView = itemView.findViewById(R.id.noCustomerTextView);
            waktuPengambilan = itemView.findViewById(R.id.waktuPengambilanMerah);
            orderItemsContainer = itemView.findViewById(R.id.orderItemsContainer);
        }
    }
}