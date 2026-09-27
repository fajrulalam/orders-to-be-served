package com.example.orderstobeserved;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Summary panel in ingredient mode: each ingredient still to prepare across all shown orders.
 * Reuses the menu summary card layout; a tap ticks one portion on the oldest open order.
 */
public class IngredientSummaryAdapter extends RecyclerView.Adapter<IngredientSummaryAdapter.ViewHolder> {

    private static final int COLOR_BG_INGREDIENT = Color.parseColor("#DCFCE7");

    public interface OnTotalClickListener {
        void onTotalClick(IngredientBoard.Total total);
    }

    private final Context context;
    private final List<IngredientBoard.Total> totals;
    private final OnTotalClickListener clickListener;

    public IngredientSummaryAdapter(Context context, List<IngredientBoard.Total> totals,
                                    OnTotalClickListener clickListener) {
        this.context = context;
        this.totals = totals;
        this.clickListener = clickListener;
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.aggregated_item_card, parent, false);
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
            params.width = dpToPx(176);
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            view.setLayoutParams(params);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IngredientBoard.Total total = totals.get(position);

        holder.itemNameText.setText(total.name + " ×"
                + IngredientBoard.formatAmount(total.remainingAmount(), total.unit));
        holder.progressText.setText(total.doneAmount + "/" + total.totalAmount);

        StringBuilder menus = new StringBuilder();
        for (String menuName : total.menuNames) {
            if (menus.length() > 0) menus.append(" · ");
            menus.append(menuName);
        }
        holder.optionsText.setText(menus.toString());
        holder.optionsText.setVisibility(menus.length() > 0 ? View.VISIBLE : View.GONE);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(10));
        bg.setColor(COLOR_BG_INGREDIENT);
        holder.cardContainer.setBackground(bg);
        holder.itemNameText.setTextColor(Color.BLACK);
        holder.progressText.setTextColor(Color.parseColor("#444444"));
        holder.optionsText.setTextColor(Color.parseColor("#555555"));
        holder.sectionDivider.setVisibility(View.GONE);
        holder.cardContainer.setAlpha(1f);

        holder.cardContainer.setOnClickListener(v -> {
            v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(80)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80).start())
                    .start();
            clickListener.onTotalClick(total);
        });
    }

    @Override
    public int getItemCount() {
        return totals.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout cardContainer;
        final TextView itemNameText;
        final TextView progressText;
        final TextView optionsText;
        final View sectionDivider;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardContainer = itemView.findViewById(R.id.aggregatedCardContainer);
            itemNameText = itemView.findViewById(R.id.itemNameText);
            progressText = itemView.findViewById(R.id.progressText);
            optionsText = itemView.findViewById(R.id.optionsText);
            sectionDivider = itemView.findViewById(R.id.sectionDivider);
        }
    }
}
