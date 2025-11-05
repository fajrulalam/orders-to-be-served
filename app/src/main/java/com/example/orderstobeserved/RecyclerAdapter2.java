package com.example.orderstobeserved;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
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
    private Handler timerHandler = new Handler();
    private SparseArray<Runnable> timerRunnables = new SparseArray<>();

    public RecyclerAdapter2(Context context, ArrayList<OrderBlock> orderBlockList) {
        this.context = context;
        this.orderBlockList = orderBlockList;
    }
    
    // Notify MainActivity when aggregation needs updating
    private void notifyAggregationChanged() {
        if (context instanceof MainActivity) {
            ((MainActivity) context).notifyAggregationChanged();
        }
    }

    // Haptic feedback with optional duration
    private void performHapticFeedback(View view) {
        performHapticFeedback(view, 10);
    }

    private void performHapticFeedback(View view, int duration) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(duration);
            }
        }
    }

    // Visual click animation
    private void animateClick(View view) {
        view.animate()
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setDuration(80)
            .withEndAction(() -> {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(80)
                    .start();
            })
            .start();
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

        // Set order time
        holder.orderTimeTextView.setText(order.getWaktuPesan());

        // Handle timer display for either serving time or elapsed time
        if (order.getServingTime() != null && !order.getServingTime().equals("...") && !order.getServingTime().equals("00:00")) {
            // This is a served order - show the fixed serving time
            holder.servingTimeTextView.setText(order.getServingTime());
            
            // Use green color for completed orders in RecentlyServedActivity
            holder.servingTimeTextView.setTextColor(Color.parseColor("#4CAF50")); // Green for completed
            holder.servingTimeTextView.setVisibility(View.VISIBLE);

            // Remove any existing timer for this position
            Runnable existingRunnable = timerRunnables.get(position);
            if (existingRunnable != null) {
                timerHandler.removeCallbacks(existingRunnable);
                timerRunnables.remove(position);
            }
        } else if (order.getOrderTimestamp() > 0) {
            // This is a pending order - show count up timer
            holder.servingTimeTextView.setTextColor(Color.parseColor("#FF0000")); // Red for pending
            holder.servingTimeTextView.setVisibility(View.VISIBLE);

            // Set initial time
            holder.servingTimeTextView.setText(order.getElapsedTimeFormatted());

            // Create a timer that updates every second
            final int currentPosition = position;
            Runnable timerRunnable = new Runnable() {
                @Override
                public void run() {
                    // Only update if this position is still valid and visible
                    if (currentPosition < orderBlockList.size() && 
                        currentPosition >= 0 && 
                        orderBlockList.get(currentPosition).getCustomerNumber() == order.getCustomerNumber()) {
                        
                        holder.servingTimeTextView.setText(order.getElapsedTimeFormatted());
                        timerHandler.postDelayed(this, 1000);
                    } else {
                        // Position changed, item no longer exists or not visible
                        Runnable runnable = timerRunnables.get(currentPosition);
                        if (runnable != null) {
                            timerHandler.removeCallbacks(runnable);
                            timerRunnables.remove(currentPosition);
                        }
                    }
                }
            };

            // Remove any existing timer for this position
            Runnable existingRunnable = timerRunnables.get(position);
            if (existingRunnable != null) {
                timerHandler.removeCallbacks(existingRunnable);
            }

            // Store and start the timer
            timerRunnables.put(position, timerRunnable);
            timerHandler.post(timerRunnable);
        } else {
            // No timer info available
            holder.servingTimeTextView.setVisibility(View.GONE);

            // Remove any existing timer for this position
            Runnable existingRunnable = timerRunnables.get(position);
            if (existingRunnable != null) {
                timerHandler.removeCallbacks(existingRunnable);
                timerRunnables.remove(position);
            }
        }

        // Clear any previously added views in the FlexboxLayout
        holder.itemsContainer.removeAllViews();

        ArrayList<NewOrderItem> items = order.getOrderItems();

        // Debug log the number of items
        Log.d("RecyclerAdapter2", "Order for customer #" + order.getCustomerNumber() +
                " has " + (items != null ? items.size() : 0) + " items");

        // For each order item, create a clickable button.
        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                final NewOrderItem item = items.get(i);
                final int totalQuantity = item.getQuantity();

                // Create a Button for this order item (larger size).
                final Button itemButton = new Button(context);
                itemButton.setPadding(28, 20, 28, 20);
                itemButton.setSingleLine(true);
                itemButton.setEllipsize(TextUtils.TruncateAt.END);
                itemButton.setText(item.getNamaPesanan() + " (" + item.getPreparedQuantity() + "/" + totalQuantity + ")");
                itemButton.setTextSize(16); // Larger text
                itemButton.setTextColor(Color.WHITE);
                itemButton.setTypeface(null, android.graphics.Typeface.BOLD);

                // Set modern gradient background based on order type.
                if ("take-away".equalsIgnoreCase(item.getOrderType())) {
                    itemButton.setBackgroundResource(R.drawable.item_button_takeaway_bg);
                    itemButton.setTextColor(Color.parseColor("#3E2723")); // Dark brown for contrast
                } else if ("dine-in".equalsIgnoreCase(item.getOrderType())) {
                    itemButton.setBackgroundResource(R.drawable.item_button_dinein_bg);
                    itemButton.setTextColor(Color.WHITE);
                } else {
                    itemButton.setBackgroundColor(Color.LTGRAY);
                    itemButton.setTextColor(Color.BLACK);
                }

                // Set layout parameters: margins, wrap_content, and max width.
                FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(8, 8, 8, 8);
                // Optionally, set a maximum width.
                DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                int screenWidth = metrics.widthPixels;
                int maxCellWidth = screenWidth / 2; // Adjust as needed.
                itemButton.setMaxWidth(maxCellWidth);
                itemButton.setLayoutParams(params);

                // Reapply UI state based on the prepared count.
                if (item.getPreparedQuantity() == totalQuantity) {
                    itemButton.setPaintFlags(itemButton.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    itemButton.setAlpha(0.7f); // Slightly dimmed
                    
                    // For served orders in RecentlyServed, disable interaction
                    if (order.getServingTime() != null && !order.getServingTime().equals("...")) {
                        itemButton.setEnabled(false);
                    } else {
                        // For pending orders, KEEP ENABLED so user can still decrement
                        itemButton.setEnabled(true);
                    }
                } else {
                    // Clear any existing strikethrough flag.
                    itemButton.setPaintFlags(itemButton.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    itemButton.setEnabled(true);
                    itemButton.setAlpha(1.0f);
                }

                // Set click listener: update prepared count, update UI, and update Firestore.
                // Only add click listener to pending orders
                if (order.getServingTime() == null || order.getServingTime().equals("...")) {
                    // Click to increment
                    itemButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (item.getPreparedQuantity() < totalQuantity) {
                                // Haptic feedback
                                performHapticFeedback(v);
                                
                                // Visual animation
                                animateClick(v);
                                
                                item.incrementPrepared();
                                itemButton.setText(item.getNamaPesanan() + " (" + item.getPreparedQuantity() + "/" + totalQuantity + ")");
                                if (item.getPreparedQuantity() == totalQuantity) {
                                    itemButton.setPaintFlags(itemButton.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                                    itemButton.setAlpha(0.7f);
                                    // Keep enabled so user can still decrement
                                }
                                // Notify MainActivity to update aggregation
                                notifyAggregationChanged();
                                // Update Firestore here as needed.
                            }
                        }
                    });
                    
                    // Long press to decrement
                    itemButton.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            if (item.getPreparedQuantity() > 0) {
                                // Stronger haptic for long press
                                performHapticFeedback(v, 20);
                                
                                // Visual animation
                                animateClick(v);
                                
                                item.setPreparedQuantity(item.getPreparedQuantity() - 1);
                                itemButton.setText(item.getNamaPesanan() + " (" + item.getPreparedQuantity() + "/" + totalQuantity + ")");
                                
                                // Remove strikethrough if we decremented
                                if (item.getPreparedQuantity() < totalQuantity) {
                                    itemButton.setPaintFlags(itemButton.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                                    itemButton.setEnabled(true);
                                    itemButton.setAlpha(1.0f);
                                }
                                
                                // Notify MainActivity to update aggregation
                                notifyAggregationChanged();
                                // Update Firestore here as needed.
                            }
                            return true; // Consume the long click
                        }
                    });
                }

                // Add the button to the container.
                holder.itemsContainer.addView(itemButton);
            }
        } else {
            // If there are no order items, add a placeholder text
            TextView noItemsText = new TextView(context);
            noItemsText.setText("No order items found");
            noItemsText.setTextColor(Color.RED);
            noItemsText.setPadding(16, 16, 16, 16);
            holder.itemsContainer.addView(noItemsText);
        }
    }

    @Override
    public int getItemCount() {
        return orderBlockList.size();
    }

    // This method must be called when the RecyclerView or Activity is destroyed
    public void stopAllTimers() {
        for (int i = 0; i < timerRunnables.size(); i++) {
            int key = timerRunnables.keyAt(i);
            Runnable runnable = timerRunnables.get(key);
            timerHandler.removeCallbacks(runnable);
        }
        timerRunnables.clear();
    }

    // Handle recycling by removing callbacks for recycled views
    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            Runnable runnable = timerRunnables.get(position);
            if (runnable != null) {
                timerHandler.removeCallbacks(runnable);
            }
        }
    }

    // Called when items are detached from the window
    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            Runnable runnable = timerRunnables.get(position);
            if (runnable != null) {
                timerHandler.removeCallbacks(runnable);
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView headerTextView;
        TextView orderTimeTextView;
        TextView servingTimeTextView;
        FlexboxLayout itemsContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            headerTextView = itemView.findViewById(R.id.headerTextView);
            orderTimeTextView = itemView.findViewById(R.id.orderTimeTextView);
            servingTimeTextView = itemView.findViewById(R.id.servingTimeTextView);
            itemsContainer = itemView.findViewById(R.id.itemsContainer);
        }
    }
}