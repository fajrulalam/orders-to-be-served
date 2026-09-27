package com.example.orderstobeserved;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IngredientBoardTest {

    private IngredientCatalog catalog;
    private final Map<String, Integer> ticked = new HashMap<>();
    private final IngredientBoard.ProgressSource progress =
            (orderKey, progressKey) -> ticked.containsKey(orderKey + "/" + progressKey)
                    ? ticked.get(orderKey + "/" + progressKey) : 0;

    private static Map<String, Object> ingredient(String id, String name, Object quantity) {
        Map<String, Object> map = new HashMap<>();
        map.put("inventoryItemId", id);
        map.put("inventoryItemName", name);
        map.put("quantityNeeded", quantity);
        return map;
    }

    private static Map<String, Object> menu(String name, Object ingredients) {
        Map<String, Object> map = new HashMap<>();
        map.put("namaMenu", name);
        if (ingredients != null) map.put("ingredients", ingredients);
        return map;
    }

    private static Map<String, Object> option(String id, String name, Map<String, Object>... ingredients) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("ingredients", Arrays.asList(ingredients));
        return map;
    }

    private static Map<String, Object> group(String name, Map<String, Object>... options) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("options", Arrays.asList(options));
        return map;
    }

    private static NewOrderItem line(String menuItemId, String name, String type, int qty, long orderedAt,
                                     SelectedOption... options) {
        NewOrderItem item = new NewOrderItem(name, type, qty, "", new ArrayList<>(Arrays.asList(options)));
        item.setMenuItemId(menuItemId);
        item.setOrderedAt(orderedAt);
        return item;
    }

    private static OrderBlock order(int number, NewOrderItem... items) {
        OrderBlock order = new OrderBlock(0, number, "Pelanggan", new ArrayList<>(Arrays.asList(items)),
                "", "08:00", 1000L * number, 0);
        order.setFirestoreDocumentId(number + "_plazaUnipdu");
        return order;
    }

    @Before
    public void setUp() {
        catalog = new IngredientCatalog();
        Map<String, Map<String, Object>> menus = new LinkedHashMap<>();
        menus.put("mieId", menu("Masak Mie", Collections.singletonList(ingredient("inv-mie", "Mie", 1))));
        menus.put("teh", menu("Es Teh", null)); // no ingredients
        menus.put("dup1", menu("Penyetan", Collections.singletonList(ingredient("inv-a", "A", 1))));
        menus.put("dup2", menu("Penyetan ", Collections.singletonList(ingredient("inv-b", "B", 1))));
        menus.put("zero", menu("Gratis", Arrays.asList(ingredient("inv-x", "X", 0), ingredient("inv-y", "Y", "2"))));
        menus.put("broken", menu("Rusak", "not a list"));
        catalog.setMenus(menus);

        Map<String, Map<String, Object>> groups = new LinkedHashMap<>();
        groups.put("g-mie", group("Mie Goreng",
                option("opt-rendang", "Rendang", ingredient("inv-rendang", "Mie Rendang", 1)),
                option("opt-telur", "Telur Dadar", ingredient("inv-telur", "Telur", 1), ingredient("inv-mie", "Mie", 1))));
        groups.put("g-dup1", group("Level", option("o1", "Pedas", ingredient("inv-cabai", "Cabai", 1))));
        groups.put("g-dup2", group("Level", option("o2", "Pedas", ingredient("inv-cabai", "Cabai", 1))));
        catalog.setOptionGroups(groups);

        Map<String, Map<String, Object>> inventory = new HashMap<>();
        Map<String, Object> telur = new HashMap<>();
        telur.put("name", "Telur Ayam");
        telur.put("unit", "butir");
        inventory.put("inv-telur", telur);
        catalog.setInventory(inventory);
    }

    // ── Catalog: mirrors the POS stock deduction ──

    @Test
    public void menuAndOptionIngredientsAreMergedByInventoryId() {
        NewOrderItem item = line("mieId", "Masak Mie", "dine-in", 2, 1,
                new SelectedOption("opt-telur", "Telur Dadar", "g-mie", "Mie Goreng", 0));
        List<IngredientCatalog.LineIngredient> ingredients = catalog.ingredientsFor(item);
        assertEquals(2, ingredients.size());
        assertEquals("inv-mie", ingredients.get(0).key);
        assertEquals(2, ingredients.get(0).perPortion); // 1 from the menu + 1 from the option
        assertEquals("Telur Ayam", ingredients.get(1).name); // current Inventory name
        assertEquals("butir", ingredients.get(1).unit);
    }

    @Test
    public void populatedMenuIdIsAuthoritative() {
        // The POS never falls back to the name when the ID is set but unknown.
        assertTrue(catalog.ingredientsFor(line("deletedId", "Masak Mie", "dine-in", 1, 1)).isEmpty());
    }

    @Test
    public void legacyLineWithoutIdUsesUniqueTrimmedName() {
        assertEquals(1, catalog.ingredientsFor(line("", " Masak Mie ", "dine-in", 1, 1)).size());
        // "Penyetan" and "Penyetan " trim to the same name, so it is ambiguous.
        assertTrue(catalog.ingredientsFor(line("", "Penyetan", "dine-in", 1, 1)).isEmpty());
    }

    @Test
    public void optionsResolveByNameWhenIdsAreEmpty() {
        NewOrderItem item = line("mieId", "Masak Mie", "dine-in", 1, 1,
                new SelectedOption("", "Rendang", "", "Mie Goreng", 0));
        assertEquals(2, catalog.ingredientsFor(item).size());
        // Two groups named "Level": ambiguous, so the option is skipped like in the POS.
        NewOrderItem ambiguous = line("mieId", "Masak Mie", "dine-in", 1, 1,
                new SelectedOption("", "Pedas", "", "Level", 0));
        assertEquals(1, catalog.ingredientsFor(ambiguous).size());
    }

    @Test
    public void nonPositiveQuantitiesAreDroppedAndStringsParsed() {
        List<IngredientCatalog.LineIngredient> ingredients = catalog.ingredientsFor(line("zero", "Gratis", "dine-in", 1, 1));
        assertEquals(1, ingredients.size());
        assertEquals(2, ingredients.get(0).perPortion);
        assertEquals("", ingredients.get(0).unit); // not in Inventory
    }

    @Test
    public void malformedMenuIsSkipped() {
        assertTrue(catalog.ingredientsFor(line("broken", "Rusak", "dine-in", 1, 1)).isEmpty());
    }

    // ── Board ──

    @Test
    public void linesAndOrdersWithoutIngredientsAreLeftOut() {
        OrderBlock teaOnly = order(1, line("teh", "Es Teh", "dine-in", 1, 1));
        OrderBlock mixed = order(2, line("teh", "Es Teh", "dine-in", 1, 1), line("mieId", "Masak Mie", "take-away", 3, 1));
        Map<Integer, List<IngredientBoard.Task>> tasks =
                IngredientBoard.tasksByOrder(Arrays.asList(teaOnly, mixed), catalog, item -> true);
        assertFalse(tasks.containsKey(1));
        assertEquals(1, tasks.get(2).size());
        assertEquals(3, tasks.get(2).get(0).portions());
    }

    @Test
    public void filterIsRespected() {
        OrderBlock mie = order(1, line("mieId", "Masak Mie", "dine-in", 1, 1));
        assertTrue(IngredientBoard.tasksByOrder(Collections.singletonList(mie), catalog, item -> false).isEmpty());
    }

    @Test
    public void preparedMenuPortionsCountAsDoneButTickingNeverTouchesTheMenu() {
        NewOrderItem mieLine = line("mieId", "Masak Mie", "dine-in", 3, 1);
        mieLine.setPreparedQuantity(1);
        OrderBlock mie = order(1, mieLine);
        IngredientBoard.Task task = IngredientBoard.tasksByOrder(Collections.singletonList(mie), catalog, item -> true)
                .get(1).get(0);
        assertEquals(1, task.donePortions(progress));

        ticked.put(task.orderKey + "/" + task.progressKey, 2);
        assertEquals(2, task.donePortions(progress));
        assertEquals(1, mieLine.getPreparedQuantity());

        ticked.put(task.orderKey + "/" + task.progressKey, 9);
        assertEquals(3, task.donePortions(progress)); // capped at the portions ordered
        assertTrue(task.isDone(progress));
    }

    @Test
    public void totalsSumAcrossOrdersAndHideFinishedIngredients() {
        OrderBlock first = order(1, line("mieId", "Masak Mie", "dine-in", 2, 1,
                new SelectedOption("opt-telur", "Telur Dadar", "g-mie", "Mie Goreng", 0)));
        OrderBlock second = order(2, line("mieId", "Masak Mie", "take-away", 1, 2));
        List<OrderBlock> orders = Arrays.asList(first, second);
        Map<Integer, List<IngredientBoard.Task>> tasks = IngredientBoard.tasksByOrder(orders, catalog, item -> true);

        List<IngredientBoard.Total> totals = IngredientBoard.openTotals(orders, tasks, progress);
        assertEquals(2, totals.size());
        IngredientBoard.Total mie = totals.get(0);
        assertEquals("Mie", mie.name);
        assertEquals(5, mie.totalAmount); // 2 portions x 2 + 1 portion x 1
        assertSame(first, mie.nextOpenTask(progress).order); // oldest order first

        IngredientBoard.Task telur = tasks.get(1).get(1);
        ticked.put(telur.orderKey + "/" + telur.progressKey, 2);
        totals = IngredientBoard.openTotals(orders, tasks, progress);
        assertEquals(1, totals.size()); // Telur is finished and hidden
        assertEquals("Mie", totals.get(0).name);
    }

    @Test
    public void nextOpenTaskIsNullWhenEverythingIsDone() {
        OrderBlock mie = order(1, line("mieId", "Masak Mie", "dine-in", 1, 1));
        Map<Integer, List<IngredientBoard.Task>> tasks =
                IngredientBoard.tasksByOrder(Collections.singletonList(mie), catalog, item -> true);
        IngredientBoard.Task task = tasks.get(1).get(0);
        ticked.put(task.orderKey + "/" + task.progressKey, 1);
        assertTrue(IngredientBoard.openTotals(Collections.singletonList(mie), tasks, progress).isEmpty());
    }

    // ── Progress keys ──

    @Test
    public void progressKeysSeparateRoundsAndOrderTypesAndUseSafeCharacters() {
        String roundOne = IngredientProgressStore.progressKey(line("mieId", "Masak Mie", "dine-in", 1, 1000), "inv-mie");
        String roundTwo = IngredientProgressStore.progressKey(line("mieId", "Masak Mie", "dine-in", 1, 2000), "inv-mie");
        String takeAway = IngredientProgressStore.progressKey(line("mieId", "Masak Mie", "take-away", 1, 1000), "inv-mie");
        assertNotEquals(roundOne, roundTwo);
        assertNotEquals(roundOne, takeAway);
        assertTrue(roundOne.matches("[A-Za-z0-9_-]+"));
        assertEquals(roundOne, IngredientProgressStore.progressKey(line("mieId", "Masak Mie", "dine-in", 1, 1000), "inv-mie"));
    }

    @Test
    public void formatAmountOmitsUnknownUnit() {
        assertEquals("3 pcs", IngredientBoard.formatAmount(3, "pcs"));
        assertEquals("3", IngredientBoard.formatAmount(3, ""));
    }
}
