package com.example.orderstobeserved;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Merges {@link NewOrderItem} rows into the Firestore {@code Status} {@code orderItems} map shape
 * (one map per name+options+note, with dine-in / take-away quantities and prepared counts).
 */
public final class StatusOrderItemsBuilder {

    private StatusOrderItemsBuilder() {
    }

    public static ArrayList<Map<String, Object>> toFirestoreArrayList(List<NewOrderItem> items) {
        LinkedHashMap<String, Map<String, Object>> itemGroups = new LinkedHashMap<>();

        if (items == null) {
            return new ArrayList<>();
        }

        for (NewOrderItem item : items) {
            String baseKey = item.getNamaPesanan() + "_" + item.getOrderedAt();
            if (item.getSelectedOptions() != null && !item.getSelectedOptions().isEmpty()) {
                List<String> ids = new ArrayList<>();
                for (SelectedOption opt : item.getSelectedOptions()) {
                    ids.add(opt.getOptionId());
                }
                Collections.sort(ids);
                for (String id : ids) {
                    baseKey += "_" + id;
                }
            }
            String note = item.getCustomerNote();
            if (note != null && !note.isEmpty()) {
                baseKey += "\u0001" + note;
            }

            Map<String, Object> group = itemGroups.get(baseKey);
            if (group == null) {
                group = new HashMap<>();
                group.put("namaPesanan", item.getNamaPesanan());
                group.put("menuItemId", item.getMenuItemId());
                group.put("dineInQuantity", 0);
                group.put("takeAwayQuantity", 0);
                group.put("dineInPreparedQuantity", 0);
                group.put("takeAwayPreparedQuantity", 0);
                group.put("status", item.getStatus() != null ? item.getStatus() : "");
                group.put("isMakanan", item.getIsMakanan());
                group.put("harga", item.getHarga());
                group.put("customerNote", item.getCustomerNote());
                group.put("orderedAt", item.getOrderedAt());

                if (item.getSelectedOptions() != null && !item.getSelectedOptions().isEmpty()) {
                    ArrayList<Map<String, Object>> optList = new ArrayList<>();
                    for (SelectedOption opt : item.getSelectedOptions()) {
                        Map<String, Object> optMap = new HashMap<>();
                        optMap.put("optionId", opt.getOptionId());
                        optMap.put("optionName", opt.getOptionName());
                        optMap.put("groupId", opt.getGroupId());
                        optMap.put("groupName", opt.getGroupName());
                        optMap.put("priceAdjustment", opt.getPriceAdjustment());
                        optList.add(optMap);
                    }
                    group.put("selectedOptions", optList);
                }
                itemGroups.put(baseKey, group);
            }

            if ("dine-in".equalsIgnoreCase(item.getOrderType())) {
                group.put("dineInQuantity", item.getQuantity());
                group.put("dineInPreparedQuantity", item.getPreparedQuantity());
            } else {
                group.put("takeAwayQuantity", item.getQuantity());
                group.put("takeAwayPreparedQuantity", item.getPreparedQuantity());
            }
        }

        return new ArrayList<>(itemGroups.values());
    }
}
