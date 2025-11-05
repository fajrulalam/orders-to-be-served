package com.example.orderstobeserved;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AggregationAdapter extends RecyclerView.Adapter<AggregationAdapter.ViewHolder> {

    private Context context;
    private List<AggregatedItem> aggregatedItems;
    private OnAggregatedItemClickListener clickListener;

    public interface OnAggregatedItemClickListener {
        void onAggregatedItemClick(AggregatedItem item);
    }

    public AggregationAdapter(Context context, List<AggregatedItem> aggregatedItems, OnAggregatedItemClickListener listener) {
        this.context = context;
        this.aggregatedItems = aggregatedItems;
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.aggregated_item_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AggregatedItem item = aggregatedItems.get(position);

        // Set text as "ItemName (x/y)"
        String text = item.getItemName() + " (" + item.getServedQuantity() + "/" + item.getTotalQuantity() + ")";
        holder.textView.setText(text);

        // Color-coded background based on order type
        if ("take-away".equalsIgnoreCase(item.getOrderType())) {
            holder.textView.setBackgroundColor(Color.parseColor("#FFE082")); // Yellow/amber
            holder.textView.setTextColor(Color.parseColor("#000000"));
        } else if ("dine-in".equalsIgnoreCase(item.getOrderType())) {
            holder.textView.setBackgroundColor(Color.parseColor("#90CAF9")); // Light blue
            holder.textView.setTextColor(Color.parseColor("#000000"));
        } else {
            holder.textView.setBackgroundColor(Color.WHITE);
            holder.textView.setTextColor(Color.BLACK);
        }

        // Set click listener with haptic and visual feedback
        holder.textView.setOnClickListener(v -> {
            if (clickListener != null && !item.isFullyServed()) {
                // Haptic feedback
                performHapticFeedback(v);
                
                // Visual feedback animation (scale + background flash)
                animateClick(v);
                
                clickListener.onAggregatedItemClick(item);
            }
        });

        // Disable clicking if fully served
        holder.textView.setEnabled(!item.isFullyServed());
        holder.textView.setAlpha(item.isFullyServed() ? 0.5f : 1.0f);
    }

    // Haptic feedback - slightly stronger for better feel
    private void performHapticFeedback(View view) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(15);
            }
        }
    }

    // Visual click animation with bounce effect
    private void animateClick(View view) {
        // Save original alpha for background flash
        final float originalAlpha = view.getAlpha();
        
        // Quick background flash
        view.setAlpha(0.7f);
        view.animate().alpha(originalAlpha).setDuration(150).start();
        
        // Bounce animation - scale down then back up with overshoot
        view.animate()
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setDuration(80)
            .withEndAction(() -> {
                // Scale back up with slight overshoot for bounce effect
                view.animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(80)
                    .withEndAction(() -> {
                        // Settle back to normal size
                        view.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(80)
                            .start();
                    })
                    .start();
            })
            .start();
    }

    @Override
    public int getItemCount() {
        return aggregatedItems.size();
    }

    public void updateData(List<AggregatedItem> newItems) {
        this.aggregatedItems = newItems;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.aggregatedItemText);
        }
    }
}


