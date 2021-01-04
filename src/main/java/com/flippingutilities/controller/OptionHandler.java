package com.flippingutilities.controller;

import com.flippingutilities.model.FlippingItem;
import com.flippingutilities.model.Option;
import com.flippingutilities.utilities.InvalidOptionException;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.item.ItemStats;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class OptionHandler {
    ItemManager itemManager;
    Client client;

    public OptionHandler(ItemManager itemManager, Client client) {
        this.itemManager = itemManager;
        this.client = client;
    }

    private int getCashStackInInv() {
        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        Item[] inventoryItems = inventory.getItems();
        for (Item item : inventoryItems) {
            if (item.getId() == 995) {
                return item.getQuantity();
            }
        }
        return 0;
    }

    public void validateOption(Option option, Optional<FlippingItem> highlightedItem, int highlightedItemId) throws InvalidOptionException {
        if (option.getProperty().equals(Option.GE_LIMIT) || option.getProperty().equals(Option.REMAINING_LIMIT)) {
            ItemStats itemStats = itemManager.getItemStats(highlightedItemId, false);
            int geLimit = itemStats != null ? itemStats.getGeLimit() : 0;
            int totalGeLimit = (highlightedItem.isPresent() ? highlightedItem.get().getTotalGELimit() : geLimit);
            if (totalGeLimit <= 0) {
                throw new InvalidOptionException("Item does not have a known limit. Cannot calculate resulting value");
            }
        } else if (option.getProperty().equals(Option.CASHSTACK)) {
            if (getCashStackInInv() == 0) {
                throw new InvalidOptionException("Player has no cash in inventory");
            }
        }

        Set<String> acceptableOperators = new HashSet<>(Arrays.asList("+", "-", "*"));
        String change = option.getChange();
        if (change.length() < 2) {
            throw new InvalidOptionException("Modifier has to be one of +,-,*, followed by a positive number. Example: +2, -5, *9");
        }
        if (!acceptableOperators.contains(String.valueOf(change.charAt(0)))) {
            throw new InvalidOptionException("Modifier has to be one of +,-,*, followed by a positive number. Example: +2, -5, *9");
        }

        try {
            int num = Integer.parseInt(change.substring(1));
            if (num < 0) {
                throw new InvalidOptionException("Modifier has to be one of +,-,*, followed by a positive number. Example: +2, -5, *9");
            }
        } catch (NumberFormatException e) {
            throw new InvalidOptionException("Modifier has to be one of +,-,*, followed by a positive number. Example: +2, -5, *9");
        }
    }

    public int calculateOptionValue(Option option, Optional<FlippingItem> highlightedItem, int highlightedItemId) throws InvalidOptionException {
        validateOption(option, highlightedItem, highlightedItemId);
        int val = 0;
        String propertyString = option.getProperty();
        if (propertyString.equals(Option.GE_LIMIT)) {
            if (highlightedItem.isPresent()) {
                val = highlightedItem.get().getTotalGELimit();
            } else {
                ItemStats itemStats = itemManager.getItemStats(highlightedItemId, false);
                int geLimit = itemStats != null ? itemStats.getGeLimit() : 0;
                val = geLimit;
            }
        }
        if (propertyString.equals(Option.REMAINING_LIMIT)) {
            if (highlightedItem.isPresent()) {
                val = highlightedItem.get().getRemainingGeLimit();
            } else {
                ItemStats itemStats = itemManager.getItemStats(highlightedItemId, false);
                int geLimit = itemStats != null ? itemStats.getGeLimit() : 0;
                val = geLimit;
            }
        }
        if (propertyString.equals(Option.CASHSTACK)) {
            if (highlightedItem.isPresent() && highlightedItem.get().getLatestBuy().isPresent()) {
                val = getCashStackInInv() / highlightedItem.get().getLatestBuy().get().getPrice();
            } else {
                int price = itemManager.getItemPrice(highlightedItemId);
                if (price <= 0) {
                    throw new InvalidOptionException("Cannot resolve item's price");
                }
                val = getCashStackInInv() / price;
            }
        }

        String operator = String.valueOf(option.getChange().charAt(0));
        int num = Integer.parseInt(option.getChange().substring(1));
        if (operator.equals("-")) {
            val -= num;
        }
        if (operator.equals("+")) {
            val += num;
        }
        if (operator.equals("*")) {
            val *= num;
        }

        if (val < 0) {
            throw new InvalidOptionException("resulting value was negative, which isn't allowed");
        }

        return val;
    }
}
