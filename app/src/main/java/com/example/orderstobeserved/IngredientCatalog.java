package com.example.orderstobeserved;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * POS recipe data (MenuCollection, OptionGroups, Inventory) and the rules that turn one order line
 * into ingredients.
 *
 * Resolution copies the POS stock deduction (375POS InventoryService.calculateOrderStockDeltas and
 * StockRequirementCalculator.aggregateIngredients) so the kitchen sees exactly what the POS took out
 * of stock for the order:
 *  - a line with a menuItemId uses that menu only, never a name fallback; a line without one uses
 *    the menu whose trimmed name is unique;
 *  - each selected option adds its ingredients (group by groupId, else by unique group name; option
 *    by optionId, else by unique option name within the group);
 *  - ingredients are merged by inventoryItemId (by name when the ID is empty), quantities summed, and
 *    entries with quantityNeeded <= 0 dropped.
 */
public class IngredientCatalog {

    private static final String TAG = "IngredientCatalog";
    private static final String NAME_PREFIX = "__name__";
    private static final String UNRESOLVED_PREFIX = "__unresolved__:";

    /** One ingredient of one order line, per portion. */
    public static class LineIngredient {
        public final String key;   // inventoryItemId, or "__unresolved__:" + name when the ID is empty
        public final String name;
        public final String unit;  // "" when the inventory item is unknown
        public final int perPortion;

        LineIngredient(String key, String name, String unit, int perPortion) {
            this.key = key;
            this.name = name;
            this.unit = unit;
            this.perPortion = perPortion;
        }
    }

    public interface Listener {
        void onCatalogChanged();

        void onCatalogError(String message);
    }

    private static class Ingredient {
        final String inventoryItemId;
        final String name;
        final int quantityNeeded;

        Ingredient(String inventoryItemId, String name, int quantityNeeded) {
            this.inventoryItemId = inventoryItemId;
            this.name = name;
            this.quantityNeeded = quantityNeeded;
        }
    }

    private final Map<String, List<Ingredient>> menusById = new HashMap<>();
    private final Map<String, List<Ingredient>> menusByUniqueName = new HashMap<>();
    // groupId -> (optionId, or "__name__" + unique option name -> ingredients)
    private final Map<String, Map<String, List<Ingredient>>> groupsById = new HashMap<>();
    private final Map<String, Map<String, List<Ingredient>>> groupsByUniqueName = new HashMap<>();
    private final Map<String, String[]> inventoryById = new HashMap<>(); // id -> {name, unit}
    private boolean menusLoaded;
    private boolean groupsLoaded;

    private final List<ListenerRegistration> registrations = new ArrayList<>();

    // ── Firestore ───────────────────────────────────────────────────────────

    /**
     * Listens to the recipe data under {canteensCollection}/canteen375. Pass the testing-prefixed
     * root in testing mode: test orders reference menus that only exist in zTesting_Canteens.
     */
    public void start(FirebaseFirestore fs, String canteensCollection, Listener listener) {
        stop();
        menusLoaded = false;
        groupsLoaded = false;
        DocumentReference canteen = fs.collection(canteensCollection).document("canteen375");

        registrations.add(canteen.collection("MenuCollection").addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e(TAG, "MenuCollection listener failed", error);
                listener.onCatalogError("Gagal memuat resep menu: " + error.getMessage());
                return;
            }
            if (value == null) return;
            setMenus(toDataById(value));
            listener.onCatalogChanged();
        }));
        registrations.add(canteen.collection("OptionGroups").addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e(TAG, "OptionGroups listener failed", error);
                listener.onCatalogError("Gagal memuat bahan opsi menu: " + error.getMessage());
                return;
            }
            if (value == null) return;
            setOptionGroups(toDataById(value));
            listener.onCatalogChanged();
        }));
        // Inventory only supplies current names and units; the catalog works without it.
        registrations.add(canteen.collection("Inventory").addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.w(TAG, "Inventory listener failed; ingredient units will be hidden", error);
                return;
            }
            if (value == null) return;
            setInventory(toDataById(value));
            listener.onCatalogChanged();
        }));
    }

    public void stop() {
        for (ListenerRegistration registration : registrations) {
            registration.remove();
        }
        registrations.clear();
    }

    private static Map<String, Map<String, Object>> toDataById(QuerySnapshot snapshot) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Map<String, Object> data = doc.getData();
            if (data != null) result.put(doc.getId(), data);
        }
        return result;
    }

    // ── Parsing (mirrors MenuClass.fromFirestore, OptionGroup.fromFirestore, InventoryItem) ──

    public void setMenus(Map<String, Map<String, Object>> menuDocs) {
        menusById.clear();
        menusByUniqueName.clear();
        Map<String, List<List<Ingredient>>> byName = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : menuDocs.entrySet()) {
            Map<String, Object> data = entry.getValue();
            List<Ingredient> ingredients = parseIngredients(data, "ingredients");
            if (ingredients == null) continue; // the POS skips menus it cannot parse
            menusById.put(entry.getKey(), ingredients);

            Object rawName = data.get("namaMenu");
            String name = (rawName == null ? "Menu tanpa nama" : String.valueOf(rawName)).trim();
            if (name.isEmpty()) continue;
            List<List<Ingredient>> sameName = byName.get(name);
            if (sameName == null) {
                sameName = new ArrayList<>();
                byName.put(name, sameName);
            }
            sameName.add(ingredients);
        }
        for (Map.Entry<String, List<List<Ingredient>>> entry : byName.entrySet()) {
            if (entry.getValue().size() == 1) {
                menusByUniqueName.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        menusLoaded = true;
    }

    public void setOptionGroups(Map<String, Map<String, Object>> groupDocs) {
        groupsById.clear();
        groupsByUniqueName.clear();
        Set<String> ambiguousGroupNames = new HashSet<>();
        for (Map.Entry<String, Map<String, Object>> entry : groupDocs.entrySet()) {
            Map<String, List<Ingredient>> options = parseOptions(entry.getValue().get("options"));
            if (options == null) continue; // the POS skips groups it cannot parse
            groupsById.put(entry.getKey(), options);

            Object rawName = entry.getValue().get("name");
            String groupName = rawName == null ? "" : String.valueOf(rawName).trim();
            if (groupName.isEmpty() || ambiguousGroupNames.contains(groupName)) continue;
            if (groupsByUniqueName.containsKey(groupName)) {
                groupsByUniqueName.remove(groupName);
                ambiguousGroupNames.add(groupName);
            } else {
                groupsByUniqueName.put(groupName, options);
            }
        }
        groupsLoaded = true;
    }

    public void setInventory(Map<String, Map<String, Object>> inventoryDocs) {
        inventoryById.clear();
        for (Map.Entry<String, Map<String, Object>> entry : inventoryDocs.entrySet()) {
            Object rawName = entry.getValue().get("name");
            Object rawUnit = entry.getValue().get("unit");
            inventoryById.put(entry.getKey(), new String[]{
                    rawName == null ? "" : String.valueOf(rawName).trim(),
                    rawUnit == null ? "pcs" : String.valueOf(rawUnit).trim()});
        }
    }

    /** @return null when the options list is malformed (the POS rejects the whole group). */
    private static Map<String, List<Ingredient>> parseOptions(Object rawOptions) {
        Map<String, List<Ingredient>> options = new HashMap<>();
        if (rawOptions == null) return options;
        if (!(rawOptions instanceof List)) return null;
        Map<String, List<List<Ingredient>>> byName = new HashMap<>();
        for (Object rawOption : (List<?>) rawOptions) {
            if (!(rawOption instanceof Map)) return null;
            @SuppressWarnings("unchecked")
            Map<String, Object> option = (Map<String, Object>) rawOption;
            List<Ingredient> ingredients = parseIngredients(option, "ingredients");
            if (ingredients == null) return null;
            Object rawId = option.get("id");
            options.put(rawId == null ? "" : String.valueOf(rawId), ingredients);

            Object rawName = option.get("name");
            String name = rawName == null ? "" : String.valueOf(rawName).trim();
            List<List<Ingredient>> sameName = byName.get(name);
            if (sameName == null) {
                sameName = new ArrayList<>();
                byName.put(name, sameName);
            }
            sameName.add(ingredients);
        }
        for (Map.Entry<String, List<List<Ingredient>>> entry : byName.entrySet()) {
            if (!entry.getKey().isEmpty() && entry.getValue().size() == 1) {
                options.put(NAME_PREFIX + entry.getKey(), entry.getValue().get(0));
            }
        }
        return options;
    }

    /**
     * @return the ingredients (possibly empty), or null when the field is present but malformed.
     */
    private static List<Ingredient> parseIngredients(Map<String, Object> data, String field) {
        List<Ingredient> ingredients = new ArrayList<>();
        if (!data.containsKey(field) || data.get(field) == null) return ingredients;
        Object raw = data.get(field);
        if (!(raw instanceof List)) return null;
        for (Object rawIngredient : (List<?>) raw) {
            if (!(rawIngredient instanceof Map)) return null;
            Map<?, ?> map = (Map<?, ?>) rawIngredient;
            Object id = map.get("inventoryItemId");
            Object name = map.get("inventoryItemName");
            ingredients.add(new Ingredient(
                    id == null ? "" : String.valueOf(id),
                    name == null ? "" : String.valueOf(name),
                    toInt(map.get("quantityNeeded"))));
        }
        return ingredients;
    }

    private static int toInt(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value).trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    // ── Resolution ──────────────────────────────────────────────────────────

    /** True once menus and option groups have loaded at least once. */
    public boolean isReady() {
        return menusLoaded && groupsLoaded;
    }

    /** Per-portion ingredients of one order line; empty when the menu has none or is unknown. */
    public List<LineIngredient> ingredientsFor(NewOrderItem item) {
        List<Ingredient> menuIngredients = findMenu(item);
        if (menuIngredients == null) return Collections.emptyList();

        List<Ingredient> all = new ArrayList<>(menuIngredients);
        List<SelectedOption> selectedOptions = item.getSelectedOptions();
        if (selectedOptions != null) {
            for (SelectedOption selected : selectedOptions) {
                List<Ingredient> optionIngredients = findOption(selected);
                if (optionIngredients != null) all.addAll(optionIngredients);
            }
        }

        // key -> {name, quantity}; insertion order keeps menu ingredients before option ingredients
        Map<String, Object[]> merged = new LinkedHashMap<>();
        for (Ingredient ingredient : all) {
            if (ingredient.quantityNeeded <= 0) continue;
            String id = ingredient.inventoryItemId.trim();
            String key = id.isEmpty() ? UNRESOLVED_PREFIX + ingredient.name.trim() : id;
            Object[] existing = merged.get(key);
            if (existing == null) {
                merged.put(key, new Object[]{ingredient.name, ingredient.quantityNeeded});
            } else {
                existing[1] = (Integer) existing[1] + ingredient.quantityNeeded;
            }
        }

        List<LineIngredient> result = new ArrayList<>();
        for (Map.Entry<String, Object[]> entry : merged.entrySet()) {
            String[] inventory = inventoryById.get(entry.getKey());
            String name = inventory != null && !inventory[0].isEmpty()
                    ? inventory[0]
                    : String.valueOf(entry.getValue()[0]).trim();
            String unit = inventory != null ? inventory[1] : "";
            result.add(new LineIngredient(entry.getKey(), name, unit, (Integer) entry.getValue()[1]));
        }
        return result;
    }

    private List<Ingredient> findMenu(NewOrderItem item) {
        String menuItemId = item.getMenuItemId().trim();
        if (!menuItemId.isEmpty()) {
            // A populated ID is authoritative, exactly like the POS: no fallback to a same-named menu.
            return menusById.get(menuItemId);
        }
        String name = item.getNamaPesanan();
        return name == null ? null : menusByUniqueName.get(name.trim());
    }

    private List<Ingredient> findOption(SelectedOption selected) {
        String groupId = selected.getGroupId() == null ? "" : selected.getGroupId().trim();
        Map<String, List<Ingredient>> group;
        if (!groupId.isEmpty()) {
            group = groupsById.get(groupId);
        } else {
            String groupName = selected.getGroupName() == null ? "" : selected.getGroupName().trim();
            group = groupsByUniqueName.get(groupName);
        }
        if (group == null) return null;
        String optionId = selected.getOptionId() == null ? "" : selected.getOptionId().trim();
        if (!optionId.isEmpty()) return group.get(optionId);
        String optionName = selected.getOptionName() == null ? "" : selected.getOptionName().trim();
        return group.get(NAME_PREFIX + optionName);
    }
}
