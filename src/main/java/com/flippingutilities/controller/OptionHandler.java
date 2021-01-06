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

import java.util.Optional;

public class OptionHandler {
    ItemManager itemManager;
    Client client;

    public OptionHandler(ItemManager itemManager, Client client) {
        this.itemManager = itemManager;
        this.client = client;
    }

    public int calculateOptionValue(Option option, Optional<FlippingItem> highlightedItem, int highlightedItemId) throws InvalidOptionException {
        int val = 0;
        String propertyString = option.getProperty();
        switch (propertyString) {
            case Option.GE_LIMIT:
                val = geLimitCalculation(highlightedItem, highlightedItemId);
                break;
            case Option.REMAINING_LIMIT:
                val = remainingGeLimitCalculation(highlightedItem, highlightedItemId);
                break;
            case Option.CASHSTACK:
                val = cashStackCalculation(highlightedItem, highlightedItemId);
                break;
            case Option.MARGIN_BUY:
                val = marginBuyCalculation(highlightedItem);
                break;
            case Option.MARGIN_SELL:
                val = marginSellCalculation(highlightedItem);
                break;
            case Option.LAST_BUY:
                val = latestBuyCalculation(highlightedItem);
                break;
            case Option.LAST_SELL:
                val = latestSellCalculation(highlightedItem);
                break;
        }

        int finalValue = applyModifier(option.getModifier(), val);
        if (finalValue < 0) {
            throw new InvalidOptionException("resulting value was negative");
        }
        return finalValue;
    }

    private int remainingGeLimitCalculation(Optional<FlippingItem> item, int itemId) throws InvalidOptionException {
        ItemStats itemStats = itemManager.getItemStats(itemId, false);
        int geLimit = itemStats != null ? itemStats.getGeLimit() : 0;
        int totalGeLimit = item.map(FlippingItem::getTotalGELimit).orElse(geLimit);
        if (totalGeLimit <= 0) {
            throw new InvalidOptionException("Item does not have a known limit. Cannot calculate resulting value");
        }
        return item.map(FlippingItem::getRemainingGeLimit).orElse(geLimit);
    }

    private int geLimitCalculation(Optional<FlippingItem> item, int itemId) throws InvalidOptionException {
        ItemStats itemStats = itemManager.getItemStats(itemId, false);
        int geLimit = itemStats != null ? itemStats.getGeLimit() : 0;
        int totalGeLimit = item.map(FlippingItem::getTotalGELimit).orElse(geLimit);
        if (totalGeLimit <= 0) {
            throw new InvalidOptionException("Item does not have a known limit. Cannot calculate resulting value");
        }
        return item.map(FlippingItem::getTotalGELimit).orElse(geLimit);
    }

    private int cashStackCalculation(Optional<FlippingItem> item, int itemId) throws InvalidOptionException {
        if (getCashStackInInv() == 0) {
            throw new InvalidOptionException("Player has no cash in inventory");
        }
        if (item.isPresent() && item.get().getLatestBuy().isPresent()) {
            return getCashStackInInv() / item.get().getLatestBuy().get().getPrice();
        } else {
            int price = itemManager.getItemPrice(itemId);
            if (price <= 0) {
                throw new InvalidOptionException("Cannot resolve item's price");
            }
            return getCashStackInInv() / price;
        }
    }

    private int marginSellCalculation(Optional<FlippingItem> item) throws InvalidOptionException {
        if (!item.isPresent()) {
            throw new InvalidOptionException("item was not bought or sold");
        }
        else {
            if (item.get().getLatestMarginCheckBuy().isPresent()) {
                return item.get().getLatestMarginCheckBuy().get().getPrice();
            }
            else {
                throw new InvalidOptionException("item does not have a margin check sell price");
            }
        }
    }

    private int marginBuyCalculation(Optional<FlippingItem> item) throws InvalidOptionException {
        if (!item.isPresent()) {
            throw new InvalidOptionException("item was not bought or sold");
        }
        else {
            if (item.get().getLatestMarginCheckSell().isPresent()) {
                return item.get().getLatestMarginCheckSell().get().getPrice();
            }
            else {
                throw new InvalidOptionException("item does not have a margin check buy price");
            }
        }
    }

    private int latestSellCalculation(Optional<FlippingItem> item) throws InvalidOptionException {
        if (!item.isPresent()) {
            throw new InvalidOptionException("item was not bought or sold");
        }
        else {
            if (item.get().getLatestSell().isPresent()) {
                return item.get().getLatestSell().get().getPrice();
            }
            else {
                throw new InvalidOptionException("item does not have a sell");
            }
        }
    }

    private int latestBuyCalculation(Optional<FlippingItem> item) throws InvalidOptionException {
        if (!item.isPresent()) {
            throw new InvalidOptionException("item was not bought or sold");
        }
        else {
            if (item.get().getLatestBuy().isPresent()) {
                return item.get().getLatestBuy().get().getPrice();
            }
            else {
                throw new InvalidOptionException("item does not have a buy");
            }
        }
    }

    private int applyModifier(String modifier, int value) throws InvalidOptionException {
        if (modifier.length() < 2) {
            throw new InvalidOptionException("Modifier has to be one of +,-,*, followed by a positive number. Example: +2, -5, *9");
        }

        try {
            int num = Integer.parseInt(modifier.substring(1));
            if (num < 0) {
                throw new InvalidOptionException("Modifier has to be one of +,-,*, followed by a positive number. Example: +2, -5, *9");
            }
        } catch (NumberFormatException e) {
            throw new InvalidOptionException("Modifier has to be one of +,-,*, followed by a positive number. Example: +2, -5, *9");
        }

        String operator = String.valueOf(modifier.charAt(0));
        int num = Integer.parseInt(modifier.substring(1));
        switch (operator) {
            case "-":
                return value - num;
            case "+":
                return value + num;
            case "*":
                return value * num;
            default:
                throw new InvalidOptionException("Modifier has to be one of +,-,*, followed by a positive number. Example: +2, -5, *9");
        }
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

}
