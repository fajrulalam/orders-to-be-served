package com.example.orderstobeserved;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the ingredient view: one row per ingredient of each order line (only lines whose menu has
 * ingredients), and the summary of ingredients still to prepare across all shown orders.
 *
 * A portion counts as prepared when its ingredient was ticked, or when the menu line itself was
 * marked prepared (the cooks cannot have made it without the ingredients). Ticking an ingredient
 * never changes the menu line.
 */
public final class IngredientBoard {

    private IngredientBoard() {
    }

    public interface ProgressSource {
        int getDone(String orderKey, String progressKey);
    }

    public interface ItemFilter {
        boolean matches(NewOrderItem item);
    }

    /** One ingredient of one order line. */
    public static class Task {
        public final OrderBlock order;
        public final NewOrderItem item;
        public final IngredientCatalog.LineIngredient ingredient;
        public final String orderKey;
        public final String progressKey;

        Task(OrderBlock order, NewOrderItem item, IngredientCatalog.LineIngredient ingredient) {
            this.order = order;
            this.item = item;
            this.ingredient = ingredient;
            this.orderKey = IngredientProgressStore.orderKey(order);
            this.progressKey = IngredientProgressStore.progressKey(item, ingredient.key);
        }

        public int portions() {
            return Math.max(0, item.getQuantity());
        }

        public int donePortions(ProgressSource progress) {
            int ticked = progress.getDone(orderKey, progressKey);
            return Math.min(portions(), Math.max(ticked, item.getPreparedQuantity()));
        }

        public boolean isDone(ProgressSource progress) {
            return donePortions(progress) >= portions();
        }

        public int totalAmount() {
            return portions() * ingredient.perPortion;
        }
    }

    /** One ingredient summed over every shown order. */
    public static class Total {
        public final String key;
        public final String name;
        public final String unit;
        public final List<Task> tasks = new ArrayList<>(); // oldest order first
        public final Set<String> menuNames = new LinkedHashSet<>();
        public int totalAmount;
        public int doneAmount;

        Total(String key, String name, String unit) {
            this.key = key;
            this.name = name;
            this.unit = unit;
        }

        public int remainingAmount() {
            return Math.max(0, totalAmount - doneAmount);
        }

        /** The oldest row that still has a portion left, or null. */
        public Task nextOpenTask(ProgressSource progress) {
            for (Task task : tasks) {
                if (!task.isDone(progress)) return task;
            }
            return null;
        }
    }

    /**
     * Rows per order, keyed by customerNumber, in display order: by orderedAt (open-bill rounds),
     * food before drinks, then menu name; ingredients in recipe order. Orders without any ingredient
     * row are left out.
     */
    public static Map<Integer, List<Task>> tasksByOrder(List<OrderBlock> orders, IngredientCatalog catalog,
                                                        ItemFilter filter) {
        Map<Integer, List<Task>> result = new LinkedHashMap<>();
        for (OrderBlock order : orders) {
            List<NewOrderItem> items = order.getOrderItems() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(order.getOrderItems());
            Collections.sort(items, (a, b) -> {
                if (a.getOrderedAt() != b.getOrderedAt()) return Long.compare(a.getOrderedAt(), b.getOrderedAt());
                if (a.getIsMakanan() != b.getIsMakanan()) return Boolean.compare(b.getIsMakanan(), a.getIsMakanan());
                return a.getNamaPesanan().compareToIgnoreCase(b.getNamaPesanan());
            });

            List<Task> tasks = new ArrayList<>();
            for (NewOrderItem item : items) {
                if (item.getQuantity() <= 0 || !filter.matches(item)) continue;
                for (IngredientCatalog.LineIngredient ingredient : catalog.ingredientsFor(item)) {
                    tasks.add(new Task(order, item, ingredient));
                }
            }
            if (!tasks.isEmpty()) result.put(order.getCustomerNumber(), tasks);
        }
        return result;
    }

    /** Ingredients that still have something left to prepare, sorted by name. */
    public static List<Total> openTotals(List<OrderBlock> orders, Map<Integer, List<Task>> tasksByOrder,
                                         ProgressSource progress) {
        Map<String, Total> totals = new HashMap<>();
        for (OrderBlock order : orders) {
            List<Task> tasks = tasksByOrder.get(order.getCustomerNumber());
            if (tasks == null) continue;
            for (Task task : tasks) {
                Total total = totals.get(task.ingredient.key);
                if (total == null) {
                    total = new Total(task.ingredient.key, task.ingredient.name, task.ingredient.unit);
                    totals.put(task.ingredient.key, total);
                }
                total.tasks.add(task);
                total.menuNames.add(task.item.getNamaPesanan());
                total.totalAmount += task.totalAmount();
                total.doneAmount += task.donePortions(progress) * task.ingredient.perPortion;
            }
        }

        List<Total> open = new ArrayList<>();
        for (Total total : totals.values()) {
            if (total.remainingAmount() > 0) open.add(total);
        }
        Collections.sort(open, (a, b) -> a.name.compareToIgnoreCase(b.name));
        return open;
    }

    /** "3 pcs", or just "3" when the unit is unknown. */
    public static String formatAmount(int amount, String unit) {
        return unit == null || unit.isEmpty() ? String.valueOf(amount) : amount + " " + unit;
    }
}
