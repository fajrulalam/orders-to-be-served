package com.example.orderstobeserved;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A named set of menus defined by the kitchen (e.g. "Telur", "Gorengan"), used by the
 * custom filter. Stored at Canteens/canteen375/KdsMenuGroups/{id} and shared by every tablet.
 */
public class MenuGroup {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_MENU_ITEM_IDS = "menuItemIds";
    public static final String FIELD_MENU_NAMES = "menuNames";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_UPDATED_AT = "updatedAt";

    private final String id;
    private final String name;
    private final List<String> menuItemIds; // MenuCollection document IDs
    private final List<String> menuNames;   // namaMenu at save time, for orders without menuItemId
    private final Set<String> menuItemIdSet;
    private final Set<String> menuNameSet;    // trimmed, for ticking entries in the editor
    private final Set<String> menuNameKeySet; // trimmed + lower-case, for matching orders

    public MenuGroup(String id, String name, List<String> menuItemIds, List<String> menuNames) {
        this.id = id;
        this.name = name == null ? "" : name;
        this.menuItemIds = menuItemIds != null ? menuItemIds : new ArrayList<>();
        this.menuNames = menuNames != null ? menuNames : new ArrayList<>();
        this.menuItemIdSet = new HashSet<>(this.menuItemIds);
        this.menuNameSet = new HashSet<>();
        this.menuNameKeySet = new HashSet<>();
        for (String menuName : this.menuNames) {
            if (menuName == null) continue;
            menuNameSet.add(menuName.trim());
            menuNameKeySet.add(nameKey(menuName));
        }
    }

    public static MenuGroup fromSnapshot(DocumentSnapshot doc) {
        return new MenuGroup(
                doc.getId(),
                doc.getString(FIELD_NAME),
                toStringList(doc.get(FIELD_MENU_ITEM_IDS)),
                toStringList(doc.get(FIELD_MENU_NAMES)));
    }

    /** Payload for a create (isNew) or a full overwrite of an existing group. */
    public static Map<String, Object> toFirestore(String name, List<String> menuItemIds,
                                                  List<String> menuNames, boolean isNew) {
        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_NAME, name);
        data.put(FIELD_MENU_ITEM_IDS, menuItemIds);
        data.put(FIELD_MENU_NAMES, menuNames);
        data.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());
        if (isNew) {
            data.put(FIELD_CREATED_AT, FieldValue.serverTimestamp());
        }
        return data;
    }

    /**
     * Matches on the POS menuItemId first (survives menu renames), then on the name
     * (orders written before menuItemId existed, or menus re-created with a new ID).
     * The name check ignores case: MenuCollection holds variants such as "Penyetan lele"
     * and "Penyetan Lele", which are the same dish to the kitchen.
     */
    public boolean matches(NewOrderItem item) {
        String menuItemId = item.getMenuItemId();
        if (!menuItemId.isEmpty() && menuItemIdSet.contains(menuItemId)) return true;
        String itemName = item.getNamaPesanan();
        return itemName != null && menuNameKeySet.contains(nameKey(itemName));
    }

    /** Whether a MenuCollection entry should appear ticked when editing this group. */
    public boolean containsMenu(String menuItemId, String menuName) {
        return menuItemIdSet.contains(menuItemId)
                || (menuName != null && menuNameSet.contains(menuName.trim()));
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public List<String> getMenuItemIds() { return menuItemIds; }
    public List<String> getMenuNames() { return menuNames; }

    private static String nameKey(String menuName) {
        return menuName.trim().toLowerCase(Locale.ROOT);
    }

    private static List<String> toStringList(Object raw) {
        List<String> result = new ArrayList<>();
        if (raw instanceof List) {
            for (Object value : (List<?>) raw) {
                if (value != null) result.add(String.valueOf(value));
            }
        }
        return result;
    }
}
