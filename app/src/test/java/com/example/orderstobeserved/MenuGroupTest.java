package com.example.orderstobeserved;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class MenuGroupTest {

    private static NewOrderItem item(String menuItemId, String name) {
        NewOrderItem item = new NewOrderItem(name, "dine-in", 1, "", null);
        item.setMenuItemId(menuItemId);
        return item;
    }

    private final MenuGroup telur = new MenuGroup("g1", "Telur",
            Arrays.asList("7gQTFRYLJbCA2KGsGUpj", "Nasi Goreng"),
            Arrays.asList("Telur Dadar", "Nasi Goreng"));

    @Test
    public void matchesByMenuItemIdEvenAfterRename() {
        assertTrue(telur.matches(item("7gQTFRYLJbCA2KGsGUpj", "Telur Dadar Spesial")));
    }

    @Test
    public void matchesByNameWhenOrderHasNoMenuItemId() {
        assertTrue(telur.matches(item("", "Telur Dadar")));
        assertTrue(telur.matches(item("", "  Telur Dadar ")));
    }

    @Test
    public void matchesByNameWhenMenuWasRecreatedWithNewId() {
        assertTrue(telur.matches(item("brandNewAutoId", "Telur Dadar")));
    }

    @Test
    public void legacyMenuWhoseIdIsItsName() {
        assertTrue(telur.matches(item("Nasi Goreng", "Nasi Goreng")));
    }

    @Test
    public void nameMatchIgnoresCase() {
        // Real case: a testing-mode order line "Dadar Jagung" (menuItemId "Dadar Jagung")
        // against the production menu "Dadar jagung" with a different ID.
        MenuGroup group = new MenuGroup("g3", "Tes",
                Collections.singletonList("prodAutoId"),
                Collections.singletonList("Dadar jagung"));
        assertTrue(group.matches(item("Dadar Jagung", "Dadar Jagung")));
    }

    @Test
    public void editorTicksOnlyExactNames() {
        // "Penyetan lele" and "Penyetan Lele" are separate MenuCollection entries; saving one
        // must not make the other appear ticked when the group is reopened.
        MenuGroup group = new MenuGroup("g4", "Gorengan",
                Collections.singletonList("idUpper"),
                Collections.singletonList("Penyetan Lele"));
        assertFalse(group.containsMenu("idLower", "Penyetan lele"));
    }

    @Test
    public void doesNotMatchOtherMenus() {
        assertFalse(telur.matches(item("otherId", "Es Teh")));
        assertFalse(telur.matches(item("", "")));
    }

    @Test
    public void emptyGroupMatchesNothing() {
        MenuGroup empty = new MenuGroup("g2", "Kosong",
                Collections.<String>emptyList(), Collections.<String>emptyList());
        assertFalse(empty.matches(item("", "Telur Dadar")));
    }

    @Test
    public void containsMenuPreselectsByIdOrName() {
        assertTrue(telur.containsMenu("7gQTFRYLJbCA2KGsGUpj", "Renamed"));
        assertTrue(telur.containsMenu("newId", "Telur Dadar"));
        assertFalse(telur.containsMenu("newId", "Es Teh"));
    }
}
