package com.example.orderstobeserved;

public class SelectedOption {
    private String optionId;
    private String optionName;
    private String groupId;
    private String groupName;
    private int priceAdjustment;

    public SelectedOption(String optionId, String optionName, String groupId, String groupName, int priceAdjustment) {
        this.optionId = optionId;
        this.optionName = optionName;
        this.groupId = groupId;
        this.groupName = groupName;
        this.priceAdjustment = priceAdjustment;
    }

    public String getOptionId() { return optionId; }
    public String getOptionName() { return optionName; }
    public String getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public int getPriceAdjustment() { return priceAdjustment; }
}
