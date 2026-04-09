package com.example.orderstobeserved;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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

import java.util.List;

public class AggregationAdapter extends RecyclerView.Adapter<AggregationAdapter.ViewHolder> {

    private Context context;
    private List<AggregatedItem> aggregatedItems;
    private OnAggregatedItemClickListener clickListener;

    private static final int COLOR_BG_DINEIN = Color.parseColor("#BBDEFB");
    private static final int COLOR_BG_TAKEAWAY = Color.parseColor("#FFF3C4");

    public interface OnAggregatedItemClickListener {
        void onAggregatedItemClick(AggregatedItem item);
    }

    public AggregationAdapter(Context context, List<AggregatedItem> aggregatedItems, OnAggregatedItemClickListener listener) {
        this.context = context;
        this.aggregatedItems = aggregatedItems;
        this.clickListener = listener;
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
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

        // Item name
        holder.itemNameText.setText(item.getItemName());

        // Progress counter
        holder.progressText.setText(item.getServedQuantity() + "/" + item.getTotalQuantity());

        // Show options if present
        List<String> optionNames = item.getOptionNames();
        if (optionNames != null && !optionNames.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < optionNames.size(); i++) {
                if (i > 0) sb.append(" \u00B7 ");
                sb.append(optionNames.get(i));
            }
            holder.optionsText.setText(sb.toString());
            holder.optionsText.setVisibility(View.VISIBLE);
        } else {
            holder.optionsText.setVisibility(View.GONE);
        }

        // Color-coded rounded background
        boolean isTakeAway = "take-away".equalsIgnoreCase(item.getOrderType());
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(10));
        bg.setColor(isTakeAway ? COLOR_BG_TAKEAWAY : COLOR_BG_DINEIN);
        holder.cardContainer.setBackground(bg);

        // Text colors
        holder.itemNameText.setTextColor(Color.BLACK);
        holder.progressText.setTextColor(Color.parseColor("#444444"));
        holder.optionsText.setTextColor(Color.parseColor("#555555"));

        // Click listener with feedback
        holder.cardContainer.setOnClickListener(v -> {
            if (clickListener != null && !item.isFullyServed()) {
                performHapticFeedback(v);
                animateClick(v);
                clickListener.onAggregatedItemClick(item);
            }
        });

        // Disable if fully served
        holder.cardContainer.setEnabled(!item.isFullyServed());
        holder.cardContainer.setAlpha(item.isFullyServed() ? 0.5f : 1.0f);
    }

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

    private void animateClick(View view) {
        final float originalAlpha = view.getAlpha();
        view.setAlpha(0.7f);
        view.animate().alpha(originalAlpha).setDuration(150).start();

        view.animate()
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setDuration(80)
            .withEndAction(() -> {
                view.animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(80)
                    .withEndAction(() -> {
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
        LinearLayout cardContainer;
        TextView itemNameText;
        TextView progressText;
        TextView optionsText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardContainer = itemView.findViewById(R.id.aggregatedCardContainer);
            itemNameText = itemView.findViewById(R.id.itemNameText);
            progressText = itemView.findViewById(R.id.progressText);
            optionsText = itemView.findViewById(R.id.optionsText);
        }
    }
}
