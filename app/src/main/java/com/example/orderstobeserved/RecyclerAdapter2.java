package com.example.orderstobeserved;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;
import java.util.List;
import android.widget.ImageView;

public class RecyclerAdapter2 extends RecyclerView.Adapter<RecyclerAdapter2.ViewHolder> {

    public static final int FILTER_ALL = 0;
    public static final int FILTER_FOOD = 1;
    public static final int FILTER_DRINK = 2;

    private Context context;
    private ArrayList<OrderBlock> orderBlockList;
    private Handler timerHandler = new Handler();
    private SparseArray<Runnable> timerRunnables = new SparseArray<>();
    private int filterMode = FILTER_ALL;

    // Dine-in item row (dark text on light blue #BBDEFB)
    private static final int COLOR_DI_TEXT = Color.parseColor("#1A237E");
    private static final int COLOR_DI_PROGRESS_MUTED = Color.parseColor("#5C6BC0");
    private static final int COLOR_DI_PROGRESS_DONE = Color.parseColor("#2E7D32");
    private static final int COLOR_DI_CHIP_TEXT = Color.parseColor("#1A237E");
    private static final int COLOR_DI_CHIP_BG = Color.parseColor("#E3F2FD");
    private static final int COLOR_DI_CHIP_BORDER = Color.parseColor("#90CAF9");

    // Takeaway item row (dark text on light yellow #FFF3C4)
    private static final int COLOR_TA_TEXT = Color.parseColor("#3E2723");
    private static final int COLOR_TA_PROGRESS_MUTED = Color.parseColor("#795548");
    private static final int COLOR_TA_PROGRESS_DONE = Color.parseColor("#2E7D32");
    private static final int COLOR_TA_CHIP_TEXT = Color.parseColor("#4E342E");
    private static final int COLOR_TA_CHIP_BG = Color.parseColor("#FFF8E1");
    private static final int COLOR_TA_CHIP_BORDER = Color.parseColor("#FFD54F");

    private static final int COLOR_CARD_GREYED = Color.parseColor("#F0F0F0");
    private static final int COLOR_MUTED_TEXT = Color.parseColor("#9CA3AF");

    public RecyclerAdapter2(Context context, ArrayList<OrderBlock> orderBlockList) {
        this.context = context;
        this.orderBlockList = orderBlockList;
    }

    public void setFilterMode(int mode) {
        this.filterMode = mode;
    }

    private boolean matchesFilter(NewOrderItem item) {
        if (filterMode == FILTER_ALL) return true;
        if (filterMode == FILTER_FOOD) return item.getIsMakanan();
        if (filterMode == FILTER_DRINK) return !item.getIsMakanan();
        return true;
    }

    private void notifyAggregationChanged() {
        if (context instanceof MainActivity) {
            ((MainActivity) context).notifyAggregationChanged();
        }
    }

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

    private void animateClick(View view) {
        view.animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(80)
            .withEndAction(() -> view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start())
            .start();
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    @NonNull
    @Override
    public RecyclerAdapter2.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.orders_to_be_served2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerAdapter2.ViewHolder holder, int position) {
        OrderBlock order = orderBlockList.get(position);

        // --- HEADER (side-by-side: "#4 (Name)") ---
        holder.customerNumberView.setText("#" + order.getCustomerNumber());
        holder.customerNameView.setText("(" + order.getNamaCustomer() + ")");
        holder.orderTimeTextView.setText(order.getWaktuPesan());

        // --- OPEN BILL BADGE ---
        if (order.isOpenBill()) {
            holder.openBillBadge.setVisibility(View.VISIBLE);
            if (order.isClosed()) {
                holder.openBillBadge.setBackgroundResource(R.drawable.open_bill_badge_unlocked_bg);
                holder.openBillIcon.setImageResource(R.drawable.ic_lock_open);
            } else {
                holder.openBillBadge.setBackgroundResource(R.drawable.open_bill_badge_locked_bg);
                holder.openBillIcon.setImageResource(R.drawable.ic_lock);
            }
        } else {
            holder.openBillBadge.setVisibility(View.GONE);
        }

        // --- TIMER ---
        boolean hasServedTimer = order.getServingTime() != null
            && !order.getServingTime().equals("...")
            && !order.getServingTime().equals("00:00");

        if (hasServedTimer) {
            holder.timerBadge.setText(order.getServingTime());
            holder.timerBadge.setBackgroundResource(R.drawable.timer_badge_served_bg);
            holder.timerBadge.setTextColor(Color.WHITE);
            holder.timerBadge.setVisibility(View.VISIBLE);
            Runnable existingRunnable = timerRunnables.get(position);
            if (existingRunnable != null) {
                timerHandler.removeCallbacks(existingRunnable);
                timerRunnables.remove(position);
            }
        } else if (order.getOrderTimestamp() > 0) {
            holder.timerBadge.setBackgroundResource(R.drawable.timer_badge_bg);
            holder.timerBadge.setTextColor(Color.WHITE);
            holder.timerBadge.setVisibility(View.VISIBLE);
            holder.timerBadge.setText(order.getElapsedTimeFormatted());

            final int currentPosition = position;
            Runnable timerRunnable = new Runnable() {
                @Override
                public void run() {
                    if (currentPosition < orderBlockList.size()
                        && currentPosition >= 0
                        && orderBlockList.get(currentPosition).getCustomerNumber() == order.getCustomerNumber()) {
                        holder.timerBadge.setText(order.getElapsedTimeFormatted());
                        timerHandler.postDelayed(this, 1000);
                    } else {
                        Runnable runnable = timerRunnables.get(currentPosition);
                        if (runnable != null) {
                            timerHandler.removeCallbacks(runnable);
                            timerRunnables.remove(currentPosition);
                        }
                    }
                }
            };
            Runnable existingRunnable = timerRunnables.get(position);
            if (existingRunnable != null) {
                timerHandler.removeCallbacks(existingRunnable);
            }
            timerRunnables.put(position, timerRunnable);
            timerHandler.post(timerRunnable);
        } else {
            holder.timerBadge.setVisibility(View.GONE);
            Runnable existingRunnable = timerRunnables.get(position);
            if (existingRunnable != null) {
                timerHandler.removeCallbacks(existingRunnable);
                timerRunnables.remove(position);
            }
        }

        // --- ITEMS ---
        holder.itemsContainer.removeAllViews();
        ArrayList<NewOrderItem> items = order.getOrderItems();

        Log.d("RecyclerAdapter2", "Order #" + order.getCustomerNumber()
            + " has " + (items != null ? items.size() : 0) + " items");

        boolean isServedOrder = order.getServingTime() != null
            && !order.getServingTime().equals("...");

        boolean allVisibleComplete = true;
        boolean hasVisibleItems = false;

        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                final NewOrderItem item = items.get(i);
                if (!matchesFilter(item)) continue;

                hasVisibleItems = true;
                final int totalQuantity = item.getQuantity();
                final boolean isTakeAway = "take-away".equalsIgnoreCase(item.getOrderType());

                if (item.getPreparedQuantity() < totalQuantity) {
                    allVisibleComplete = false;
                }

                // Color scheme based on order type
                final int textColor = isTakeAway ? COLOR_TA_TEXT : COLOR_DI_TEXT;
                final int progressMuted = isTakeAway ? COLOR_TA_PROGRESS_MUTED : COLOR_DI_PROGRESS_MUTED;
                final int progressDone = isTakeAway ? COLOR_TA_PROGRESS_DONE : COLOR_DI_PROGRESS_DONE;
                final int chipTextColor = isTakeAway ? COLOR_TA_CHIP_TEXT : COLOR_DI_CHIP_TEXT;
                final int chipBgColor = isTakeAway ? COLOR_TA_CHIP_BG : COLOR_DI_CHIP_BG;
                final int chipBorderColor = isTakeAway ? COLOR_TA_CHIP_BORDER : COLOR_DI_CHIP_BORDER;

                // Item block with full-width colored background
                LinearLayout itemBlock = new LinearLayout(context);
                itemBlock.setOrientation(LinearLayout.VERTICAL);
                itemBlock.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
                itemBlock.setBackgroundResource(isTakeAway
                    ? R.drawable.item_button_takeaway_bg
                    : R.drawable.item_button_dinein_bg);
                LinearLayout.LayoutParams blockParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                blockParams.setMargins(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
                itemBlock.setLayoutParams(blockParams);

                // Name row: "Item ×qty" + progress
                LinearLayout topRow = new LinearLayout(context);
                topRow.setOrientation(LinearLayout.HORIZONTAL);
                topRow.setGravity(Gravity.CENTER_VERTICAL);

                final TextView nameView = new TextView(context);
                nameView.setText(item.getNamaPesanan() + " \u00D7" + totalQuantity);
                nameView.setTextSize(17f);
                nameView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                nameView.setTextColor(textColor);
                nameView.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                final TextView progressView = new TextView(context);
                updateProgressDisplay(progressView, item.getPreparedQuantity(),
                    totalQuantity, progressMuted, progressDone);
                LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                progressParams.setMargins(dpToPx(8), 0, 0, 0);
                progressView.setLayoutParams(progressParams);

                topRow.addView(nameView);
                topRow.addView(progressView);
                itemBlock.addView(topRow);

                // Addon chips
                final FlexboxLayout addonsContainer;
                List<SelectedOption> options = item.getSelectedOptions();
                if (options != null && !options.isEmpty()) {
                    addonsContainer = new FlexboxLayout(context);
                    addonsContainer.setFlexWrap(FlexWrap.WRAP);
                    LinearLayout.LayoutParams addonsParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    addonsParams.setMargins(0, dpToPx(6), 0, 0);
                    addonsContainer.setLayoutParams(addonsParams);

                    for (SelectedOption option : options) {
                        TextView chip = createAddonChip(option.getOptionName(),
                            chipTextColor, chipBgColor, chipBorderColor);
                        addonsContainer.addView(chip);
                    }
                    itemBlock.addView(addonsContainer);
                } else {
                    addonsContainer = null;
                }

                // Apply completion state
                applyCompletionState(nameView, addonsContainer, itemBlock,
                    item.getPreparedQuantity() >= totalQuantity);

                // Click handlers (active orders only)
                if (!isServedOrder) {
                    final boolean fIsServedOrder = false;
                    itemBlock.setClickable(true);
                    itemBlock.setFocusable(true);

                    itemBlock.setOnClickListener(v -> {
                        if (item.getPreparedQuantity() < totalQuantity) {
                            performHapticFeedback(v);
                            animateClick(v);
                            item.incrementPrepared();
                            updateProgressDisplay(progressView, item.getPreparedQuantity(),
                                totalQuantity, progressMuted, progressDone);
                            applyCompletionState(nameView, addonsContainer, itemBlock,
                                item.getPreparedQuantity() >= totalQuantity);
                            notifyAggregationChanged();
                            updateCardGreyout(holder, order, fIsServedOrder);
                        }
                    });

                    itemBlock.setOnLongClickListener(v -> {
                        if (item.getPreparedQuantity() > 0) {
                            performHapticFeedback(v, 20);
                            animateClick(v);
                            item.setPreparedQuantity(item.getPreparedQuantity() - 1);
                            updateProgressDisplay(progressView, item.getPreparedQuantity(),
                                totalQuantity, progressMuted, progressDone);
                            applyCompletionState(nameView, addonsContainer, itemBlock,
                                item.getPreparedQuantity() >= totalQuantity);
                            notifyAggregationChanged();
                            updateCardGreyout(holder, order, fIsServedOrder);
                        }
                        return true;
                    });
                }

                holder.itemsContainer.addView(itemBlock);
            }
        }

        if (!hasVisibleItems) {
            TextView noItemsText = new TextView(context);
            noItemsText.setText("No items");
            noItemsText.setTextColor(COLOR_MUTED_TEXT);
            noItemsText.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
            noItemsText.setTextSize(13f);
            holder.itemsContainer.addView(noItemsText);
        }

        // Grey out card when all visible items complete
        updateCardGreyout(holder, order, isServedOrder);
    }

    /** Checks if all filtered items are complete and greys out the card */
    private void updateCardGreyout(ViewHolder holder, OrderBlock order, boolean isServedOrder) {
        boolean allComplete = true;
        boolean hasVisible = false;
        ArrayList<NewOrderItem> items = order.getOrderItems();
        if (items != null) {
            for (NewOrderItem item : items) {
                if (!matchesFilter(item)) continue;
                hasVisible = true;
                if (item.getPreparedQuantity() < item.getQuantity()) {
                    allComplete = false;
                    break;
                }
            }
        }

        CardView cardView = (CardView) holder.itemView;
        if (hasVisible && allComplete && !isServedOrder) {
            cardView.setCardBackgroundColor(COLOR_CARD_GREYED);
            holder.itemView.setAlpha(0.55f);
        } else {
            cardView.setCardBackgroundColor(Color.WHITE);
            holder.itemView.setAlpha(1.0f);
        }
    }

    private TextView createAddonChip(String text, int textColor, int bgColor, int borderColor) {
        TextView chip = new TextView(context);
        chip.setText(text);
        chip.setTextSize(14f);
        chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        chip.setTextColor(textColor);

        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setShape(GradientDrawable.RECTANGLE);
        chipBg.setCornerRadius(dpToPx(6));
        chipBg.setColor(bgColor);
        chipBg.setStroke(1, borderColor);
        chip.setBackground(chipBg);

        chip.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3));
        FlexboxLayout.LayoutParams chipParams = new FlexboxLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        chipParams.setMargins(0, 0, dpToPx(4), dpToPx(4));
        chip.setLayoutParams(chipParams);
        return chip;
    }

    private void updateProgressDisplay(TextView progressView, int prepared, int total,
                                        int mutedColor, int doneColor) {
        if (prepared >= total) {
            progressView.setText("\u2713");
            progressView.setTextColor(doneColor);
            progressView.setTextSize(18f);
        } else {
            progressView.setText(prepared + "/" + total);
            progressView.setTextColor(mutedColor);
            progressView.setTextSize(15f);
        }
        progressView.setTypeface(null, Typeface.BOLD);
    }

    private void applyCompletionState(TextView nameView, FlexboxLayout addonsContainer,
                                       View itemBlock, boolean isDone) {
        int flag = Paint.STRIKE_THRU_TEXT_FLAG;

        if (isDone) {
            nameView.setPaintFlags(nameView.getPaintFlags() | flag);
        } else {
            nameView.setPaintFlags(nameView.getPaintFlags() & ~flag);
        }

        if (addonsContainer != null) {
            for (int i = 0; i < addonsContainer.getChildCount(); i++) {
                View child = addonsContainer.getChildAt(i);
                if (child instanceof TextView) {
                    if (isDone) {
                        ((TextView) child).setPaintFlags(((TextView) child).getPaintFlags() | flag);
                    } else {
                        ((TextView) child).setPaintFlags(((TextView) child).getPaintFlags() & ~flag);
                    }
                }
            }
        }

        itemBlock.setAlpha(isDone ? 0.45f : 1.0f);
    }

    @Override
    public int getItemCount() {
        return orderBlockList.size();
    }

    public void stopAllTimers() {
        for (int i = 0; i < timerRunnables.size(); i++) {
            int key = timerRunnables.keyAt(i);
            Runnable runnable = timerRunnables.get(key);
            timerHandler.removeCallbacks(runnable);
        }
        timerRunnables.clear();
    }

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
        LinearLayout headerContainer;
        TextView customerNumberView;
        TextView customerNameView;
        LinearLayout openBillBadge;
        ImageView openBillIcon;
        TextView openBillText;
        TextView timerBadge;
        TextView orderTimeTextView;
        LinearLayout itemsContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            headerContainer = itemView.findViewById(R.id.headerContainer);
            customerNumberView = itemView.findViewById(R.id.customerNumberView);
            customerNameView = itemView.findViewById(R.id.customerNameView);
            openBillBadge = itemView.findViewById(R.id.openBillBadge);
            openBillIcon = itemView.findViewById(R.id.openBillIcon);
            openBillText = itemView.findViewById(R.id.openBillText);
            timerBadge = itemView.findViewById(R.id.timerBadge);
            orderTimeTextView = itemView.findViewById(R.id.orderTimeTextView);
            itemsContainer = itemView.findViewById(R.id.itemsContainer);
        }
    }
}
