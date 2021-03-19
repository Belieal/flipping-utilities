package com.flippingutilities.model;

import lombok.Data;

import javax.swing.*;
import java.util.*;

/**
 * Represents one of the sections in the FlippingItemPanel. Since a user can heavily customize what a FlippingItemPanel
 * looks like (its sections), we have to store what their customizations are. This stores the customizations to a section.
 */
@Data
public class Section {
    String name;
    public static final String PRICE_CHECK_BUY_PRICE = "price check buy price";
    public static final String PRICE_CHECK_SELL_PRICE = "price check sell price";
    public static final String LATEST_BUY_PRICE = "latest buy price";
    public static final String LATEST_SELL_PRICE = "latest sell price";
    public static final String PROFIT_EACH = "profit each";
    public static final String POTENTIAL_PROFIT = "potential profit";
    public static final String ROI = "roi";
    public static final String REMAINING_GE_LIMIT = "remaining ge limit";
    public static final String GE_LIMIT_REFRESH_TIMER = "ge limit refresh timer";
    public static final List<String> possibleLabels = Arrays.asList(PRICE_CHECK_BUY_PRICE, PRICE_CHECK_SELL_PRICE, LATEST_BUY_PRICE,
            LATEST_SELL_PRICE, PROFIT_EACH, POTENTIAL_PROFIT, ROI, REMAINING_GE_LIMIT, GE_LIMIT_REFRESH_TIMER);
    Map<String, Boolean> labels;
    boolean defaultExpanded;

    public Section(String name) {
        this.name = name;
        this.labels = new LinkedHashMap<>();
        for (String label:Section.possibleLabels) {
            this.labels.put(label, false);
        }
    }

    public void showLabel(String labelName) {
        if (labels.containsKey(labelName)) {
            labels.put(labelName, true);
        }
    }

    public void hideLabel(String labelName) {
        if (labels.containsKey(labelName)) {
            labels.put(labelName, false);
        }
    }
}
