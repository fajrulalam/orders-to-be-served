package com.example.orderstobeserved;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;
import java.util.HashMap;

public class RecyclerAdapter2 extends RecyclerView.Adapter<RecyclerAdapter2.ViewHolder> {

    private Context context;
    private ArrayList<OrderBlock> orderBlockList;
    // Local cache: keys formatted as "customerNumber_index" mapped to preparedQuantity.
//    private HashMap<String, Integer> preparedCache;

    public RecyclerAdapter2(Context context, ArrayList<OrderBlock> orderBlockList) {
        this.context = context;
        this.orderBlockList = orderBlockList;
//        this.preparedCache = preparedCache;
    }

    @NonNull
    @Override
    public RecyclerAdapter2.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the updated layout.
        View view = LayoutInflater.from(context).inflate(R.layout.orders_to_be_served2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerAdapter2.ViewHolder holder, int position) {
        OrderBlock order = orderBlockList.get(position);
        // Set header text as "customerNumber (namaCustomer)"
        holder.headerTextView.setText(order.getCustomerNumber() + " (" + order.getNamaCustomer() + ")");
        // Clear any previously added views from the FlexboxLayout.
        holder.itemsContainer.removeAllViews();

        ArrayList<NewOrderItem> items = order.getOrderItems();
        // For each order item, create a button.
        for (int i = 0; i < items.size(); i++) {
            final NewOrderItem item = items.get(i);
            final int totalQuantity = item.getQuantity();
            // Create a unique key based on order's customer number and the item’s index.
            final String key = order.getCustomerNumber() + "_" + i;
            // Merge cached prepared count if available.
//            if (preparedCache.containsKey(key)) {
//                item.setPreparedQuantity(preparedCache.get(key));
//            }
            // Create a button for this order item.
            final Button itemButton = new Button(context);
            itemButton.setPadding(16, 8, 16, 8);
            itemButton.setSingleLine(true);
            itemButton.setEllipsize(TextUtils.TruncateAt.END);
            itemButton.setText(item.getNamaPesanan() + " (" + item.getPreparedQuantity() + "/" + totalQuantity + ")");
            // Set background color based on the order type.
            if ("take-away".equalsIgnoreCase(item.getOrderType())) {
                itemButton.setBackgroundColor(Color.parseColor("#FFD700")); // Yellow for take-away.
            } else if ("dine-in".equalsIgnoreCase(item.getOrderType())) {
                itemButton.setBackgroundColor(Color.parseColor("#87CEEB")); // Light Blue for dine-in.
            } else {
                itemButton.setBackgroundColor(Color.LTGRAY);
            }
            // Use FlexboxLayout.LayoutParams.
            FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 8, 8, 8);
            // Optionally, set a max width. For example, half the screen width.
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            int screenWidth = metrics.widthPixels;
            int maxCellWidth = screenWidth / 2; // Adjust this value as needed.
            itemButton.setMaxWidth(maxCellWidth);
            itemButton.setLayoutParams(params);

            // Set click listener: increment prepared count and update both the model and local cache.
            itemButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (item.getPreparedQuantity() < totalQuantity) {
                        item.incrementPrepared();
                        itemButton.setText(item.getNamaPesanan() + " (" + item.getPreparedQuantity() + "/" + totalQuantity + ")");
                        if (item.getPreparedQuantity() == totalQuantity) {
                            itemButton.setPaintFlags(itemButton.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                            itemButton.setEnabled(false);
                        }
                    }
                }
            });
            // Add the button to the FlexboxLayout.
            holder.itemsContainer.addView(itemButton);
        }
    }

    @Override
    public int getItemCount() {
        return orderBlockList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView headerTextView;
        FlexboxLayout itemsContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            headerTextView = itemView.findViewById(R.id.headerTextView);
            itemsContainer = itemView.findViewById(R.id.itemsContainer);
        }
    }
}