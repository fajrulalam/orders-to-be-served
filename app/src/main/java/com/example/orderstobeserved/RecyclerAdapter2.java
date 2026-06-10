package com.example.orderstobeserved;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;
import android.widget.ImageView;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class RecyclerAdapter2 extends RecyclerView.Adapter<RecyclerAdapter2.ViewHolder> {

    public static final int FILTER_ALL = 0;
    public static final int FILTER_FOOD = 1;
    public static final int FILTER_DRINK = 2;
    public static final int FILTER_CUSTOM = 3;

    private Context context;
    private ArrayList<OrderBlock> orderBlockList;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Map<Integer, Runnable> timerRunnables = new HashMap<>(); // keyed by customerNumber
    private int filterMode = FILTER_ALL;
    private List<String> customFilterMenus = new ArrayList<>();
    private TextToSpeech tts;

    public void setCustomFilterMenus(List<String> menus) {
        this.customFilterMenus = menus;
    }

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
    private static final int COLOR_NOTE_CARD_TEXT = Color.parseColor("#E65100");

    public RecyclerAdapter2(Context context, ArrayList<OrderBlock> orderBlockList) {
        this.context = context;
        this.orderBlockList = orderBlockList;
        this.tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                if (tts != null) {
                    int result = tts.setLanguage(new Locale("id", "ID"));
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("RecyclerAdapter2", "Language not supported or missing data for Indonesian (id-ID)");
                    }
                }
            } else {
                Log.e("RecyclerAdapter2", "Initialization of TextToSpeech failed");
            }
        });
    }

    public void setFilterMode(int mode) {
        this.filterMode = mode;
    }

    private boolean matchesFilter(NewOrderItem item) {
        if (filterMode == FILTER_ALL) return true;
        if (filterMode == FILTER_FOOD) return item.getIsMakanan();
        if (filterMode == FILTER_DRINK) return !item.getIsMakanan();
        if (filterMode == FILTER_CUSTOM) return customFilterMenus != null && customFilterMenus.contains(item.getNamaPesanan());
        return true;
    }

    private void notifyAggregationChanged() {
        if (context instanceof MainActivity) {
            ((MainActivity) context).notifyAggregationChanged();
        }
    }

    private void notifyPreparedQuantityChanged(OrderBlock order) {
        if (context instanceof MainActivity) {
            ((MainActivity) context).onPreparedQuantityChanged(order);
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

    private String toTitleCase(String text) {
        if (text == null || text.isEmpty()) return text;
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                sb.append(c);
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
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
        String formattedName = toTitleCase(order.getNamaCustomer());
        
        if (order.isMember()) {
            holder.customerNameView.setText(formattedName);
            holder.customerNameView.setTextColor(context.getResources().getColor(R.color.kds_member_accent));
            holder.customerNameView.setBackgroundResource(R.drawable.member_badge_bg);
            holder.customerNameView.setPadding(dpToPx(12), dpToPx(5), dpToPx(14), dpToPx(5));
            holder.customerNameView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            holder.customerNameView.setTextSize(14f);
            
            // Add person icon to the left
            holder.customerNameView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_member_person, 0, 0, 0);
            holder.customerNameView.setCompoundDrawablePadding(dpToPx(8));
            
            // Adjust margin
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.customerNameView.getLayoutParams();
            params.setMargins(dpToPx(12), 0, 0, 0);
            holder.customerNameView.setLayoutParams(params);
        } else {
            holder.customerNameView.setText("(" + formattedName + ")");
            holder.customerNameView.setTextColor(context.getResources().getColor(R.color.kds_customer_name));
            holder.customerNameView.setBackground(null);
            holder.customerNameView.setPadding(0, 0, 0, 0);
            holder.customerNameView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0); // Remove icon
            holder.customerNameView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            holder.customerNameView.setTextSize(13f);
            
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.customerNameView.getLayoutParams();
            params.setMargins(dpToPx(6), 0, 0, 0);
            holder.customerNameView.setLayoutParams(params);
        }

        holder.customerNameView.setOnClickListener(v -> {
            performHapticFeedback(v);
            animateClick(v);
            String nameToSpeak = order.getNamaCustomer();
            if (tts != null && nameToSpeak != null && !nameToSpeak.isEmpty()) {
                tts.speak(nameToSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });
        
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

        final int customerNum = order.getCustomerNumber();

        // Stop any existing timer for this order before rebinding
        Runnable existingRunnable = timerRunnables.get(customerNum);
        if (existingRunnable != null) {
            timerHandler.removeCallbacks(existingRunnable);
            timerRunnables.remove(customerNum);
        }

        if (hasServedTimer) {
            holder.timerBadge.setText(order.getServingTime());
            holder.timerBadge.setBackgroundResource(R.drawable.timer_badge_served_bg);
            holder.timerBadge.setTextColor(Color.WHITE);
            holder.timerBadge.setVisibility(View.VISIBLE);
        } else if (order.getOrderTimestamp() > 0) {
            holder.timerBadge.setBackgroundResource(R.drawable.timer_badge_bg);
            holder.timerBadge.setTextColor(Color.WHITE);
            holder.timerBadge.setVisibility(View.VISIBLE);
            holder.timerBadge.setText(order.getElapsedTimeFormatted());

            Runnable timerRunnable = new Runnable() {
                @Override
                public void run() {
                    // Verify the holder still shows this order
                    int pos = holder.getAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;
                    if (pos >= orderBlockList.size()) return;
                    if (orderBlockList.get(pos).getCustomerNumber() != customerNum) return;

                    holder.timerBadge.setText(order.getElapsedTimeFormatted());
                    timerHandler.postDelayed(this, 1000);
                }
            };
            timerRunnables.put(customerNum, timerRunnable);
            timerHandler.postDelayed(timerRunnable, 1000);
        } else {
            holder.timerBadge.setVisibility(View.GONE);
        }

        // --- ITEMS ---
        holder.itemsContainer.removeAllViews();
        ArrayList<NewOrderItem> rawItems = order.getOrderItems();
        ArrayList<NewOrderItem> items = new ArrayList<>();
        if (rawItems != null) {
            items.addAll(rawItems);
            Collections.sort(items, new Comparator<NewOrderItem>() {
                @Override
                public int compare(NewOrderItem o1, NewOrderItem o2) {
                    if (o1.getOrderedAt() != o2.getOrderedAt()) {
                        return Long.compare(o1.getOrderedAt(), o2.getOrderedAt());
                    }
                    if (o1.getIsMakanan() != o2.getIsMakanan()) {
                        return Boolean.compare(o2.getIsMakanan(), o1.getIsMakanan());
                    }
                    return o1.getNamaPesanan().compareToIgnoreCase(o2.getNamaPesanan());
                }
            });
        }

        Log.d("RecyclerAdapter2", "Order #" + order.getCustomerNumber()
            + " has " + items.size() + " items");

        boolean isServedOrder = order.getServingTime() != null
            && !order.getServingTime().equals("...");

        boolean allVisibleComplete = true;
        boolean hasVisibleItems = false;

        long lastOrderedAt = -1;
        int orderRound = 1;

        if (!items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                final NewOrderItem item = items.get(i);
                if (!matchesFilter(item)) continue;

                hasVisibleItems = true;

                // Add timeline divider if this item belongs to a new order timestamp
                if (item.getOrderedAt() > 0 && item.getOrderedAt() != lastOrderedAt) {
                    TextView dividerHeader = new TextView(context);
                    String formattedTime = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                            .format(new java.util.Date(item.getOrderedAt()));
                    dividerHeader.setText("🕒 Order #" + orderRound + " (" + formattedTime + ")");
                    dividerHeader.setTextSize(12f);
                    dividerHeader.setTextColor(Color.parseColor("#78909C")); // slate gray
                    dividerHeader.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                    
                    LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    int topMargin = holder.itemsContainer.getChildCount() > 0 ? dpToPx(14) : dpToPx(6);
                    headerParams.setMargins(dpToPx(14), topMargin, dpToPx(14), dpToPx(4));
                    dividerHeader.setLayoutParams(headerParams);
                    
                    holder.itemsContainer.addView(dividerHeader);
                    
                    lastOrderedAt = item.getOrderedAt();
                    orderRound++;
                }

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

                final String itemDisplayName = item.getNamaPesanan();
                final TextView nameView = new TextView(context);
                updateItemNameRemainingQuantity(nameView, itemDisplayName,
                    item.getPreparedQuantity(), totalQuantity);
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

                final TextView customerNoteView;
                String noteText = item.getCustomerNote() != null ? item.getCustomerNote().trim() : "";
                if (!noteText.isEmpty()) {
                    customerNoteView = new TextView(context);
                    customerNoteView.setText(noteText);
                    customerNoteView.setTextSize(14f);
                    customerNoteView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                    customerNoteView.setTextColor(COLOR_NOTE_CARD_TEXT);
                    customerNoteView.setBackgroundResource(R.drawable.customer_note_card_bg);
                    customerNoteView.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8));
                    LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    noteParams.setMargins(0, dpToPx(6), 0, 0);
                    customerNoteView.setLayoutParams(noteParams);
                    itemBlock.addView(customerNoteView);
                } else {
                    customerNoteView = null;
                }

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
                applyCompletionState(nameView, addonsContainer, customerNoteView, itemBlock,
                    item.getPreparedQuantity() >= totalQuantity);

                // Add visual divider ONLY if transitioning from Food to Drink
                if (hasVisibleItems && !item.getIsMakanan()) {
                    // Check if there was a food item before this drink item
                    boolean hasFoodBefore = false;
                    for (int j = 0; j < i; j++) {
                        if (items.get(j).getIsMakanan() && matchesFilter(items.get(j))) {
                            hasFoodBefore = true;
                            break;
                        }
                    }
                    
                    // Only add divider if this is the FIRST drink item after food items
                    boolean isFirstDrink = true;
                    for (int j = 0; j < i; j++) {
                        if (!items.get(j).getIsMakanan() && matchesFilter(items.get(j))) {
                            isFirstDrink = false;
                            break;
                        }
                    }

                    if (hasFoodBefore && isFirstDrink) {
                        View sectionDivider = new View(context);
                        sectionDivider.setBackgroundColor(Color.BLACK);
                        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(2));
                        dividerParams.setMargins(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
                        sectionDivider.setLayoutParams(dividerParams);
                        holder.itemsContainer.addView(sectionDivider);
                    }
                }

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
                            updateItemNameRemainingQuantity(nameView, itemDisplayName,
                                item.getPreparedQuantity(), totalQuantity);
                            updateProgressDisplay(progressView, item.getPreparedQuantity(),
                                totalQuantity, progressMuted, progressDone);
                            applyCompletionState(nameView, addonsContainer, customerNoteView, itemBlock,
                                item.getPreparedQuantity() >= totalQuantity);
                            notifyPreparedQuantityChanged(order);
                            updateCardGreyout(holder, order, fIsServedOrder);
                        }
                    });

                    itemBlock.setOnLongClickListener(v -> {
                        if (item.getPreparedQuantity() > 0) {
                            performHapticFeedback(v, 20);
                            animateClick(v);
                            item.setPreparedQuantity(0);
                            updateItemNameRemainingQuantity(nameView, itemDisplayName,
                                item.getPreparedQuantity(), totalQuantity);
                            updateProgressDisplay(progressView, item.getPreparedQuantity(),
                                totalQuantity, progressMuted, progressDone);
                            applyCompletionState(nameView, addonsContainer, customerNoteView, itemBlock,
                                item.getPreparedQuantity() >= totalQuantity);
                            notifyPreparedQuantityChanged(order);
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

    /** Name row: shows how many units are still left to prepare (updates with tap / long-press reset). */
    private void updateItemNameRemainingQuantity(TextView nameView, String itemName,
                                                  int prepared, int total) {
        int remaining = total - prepared;
        if (remaining < 0) {
            remaining = 0;
        }
        nameView.setText(itemName + " \u00D7" + remaining);
    }

    private void updateProgressDisplay(TextView progressView, int prepared, int total,
                                        int mutedColor, int doneColor) {
        if (prepared >= total) {
            progressView.setText(prepared + "/" + total + " \u2713");
            progressView.setTextColor(doneColor);
            progressView.setTextSize(16f);
        } else {
            progressView.setText(prepared + "/" + total);
            progressView.setTextColor(mutedColor);
            progressView.setTextSize(15f);
        }
        progressView.setTypeface(null, Typeface.BOLD);
    }

    private void applyCompletionState(TextView nameView, FlexboxLayout addonsContainer,
                                       TextView customerNoteView, View itemBlock, boolean isDone) {
        int flag = Paint.STRIKE_THRU_TEXT_FLAG;

        if (isDone) {
            nameView.setPaintFlags(nameView.getPaintFlags() | flag);
        } else {
            nameView.setPaintFlags(nameView.getPaintFlags() & ~flag);
        }

        if (customerNoteView != null) {
            if (isDone) {
                customerNoteView.setPaintFlags(customerNoteView.getPaintFlags() | flag);
            } else {
                customerNoteView.setPaintFlags(customerNoteView.getPaintFlags() & ~flag);
            }
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
        for (Runnable runnable : timerRunnables.values()) {
            timerHandler.removeCallbacks(runnable);
        }
        timerRunnables.clear();
    }

    public void shutdownTTS() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    private void stopTimerForHolder(@NonNull ViewHolder holder) {
        int position = holder.getAdapterPosition();
        if (position == RecyclerView.NO_POSITION || position >= orderBlockList.size()) return;
        int customerNum = orderBlockList.get(position).getCustomerNumber();
        Runnable runnable = timerRunnables.get(customerNum);
        if (runnable != null) {
            timerHandler.removeCallbacks(runnable);
            timerRunnables.remove(customerNum);
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        stopTimerForHolder(holder);
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
