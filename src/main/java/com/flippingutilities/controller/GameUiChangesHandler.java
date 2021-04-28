package com.flippingutilities.controller;

import com.flippingutilities.model.FlippingItem;
import com.flippingutilities.ui.flipping.FlippingPanel;
import com.flippingutilities.ui.widgets.OfferEditor;
import com.flippingutilities.utilities.WikiRequest;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.VarClientInt;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.VarClientIntChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.*;
import net.runelite.http.api.item.ItemStats;

import java.util.Optional;

import static net.runelite.api.VarPlayer.CURRENT_GE_ITEM;

/**
 * This class is responsible for handling all the logic that should trigger when the main game ui changes. For example,
 * this class should detect when the chatbox gets opened, when the ge history box opens, when the user is in the
 * ge offer setup screen, etc and trigger appropriate logic in those cases.
 */
public class GameUiChangesHandler {
    private static final int GE_OFFER_INIT_STATE_CHILD_ID = 18;
    private static final int GE_HISTORY_TAB_WIDGET_ID = 149;
    FlippingPlugin plugin;

    boolean quantityOrPriceChatboxOpen;
    Optional<FlippingItem> highlightedItem = Optional.empty();
    int highlightedItemId;


    GameUiChangesHandler(FlippingPlugin plugin) {
        this.plugin = plugin;
    }


    public void onVarClientIntChanged(VarClientIntChanged event) {
        Client client = plugin.getClient();

        if (event.getIndex() == VarClientInt.INPUT_TYPE.getIndex()
                && client.getVarcIntValue(VarClientInt.INPUT_TYPE.getIndex()) == 14
                && client.getWidget(WidgetInfo.CHATBOX_GE_SEARCH_RESULTS) != null) {
            plugin.getClientThread().invokeLater(() -> {
                Widget geSearchResultBox = client.getWidget(WidgetInfo.CHATBOX_GE_SEARCH_RESULTS);
                Widget child = geSearchResultBox.createChild(-1, WidgetType.TEXT);
                child.setTextColor(0x800000);
                child.setFontId(FontID.VERDANA_13_BOLD);
                child.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER);
                child.setOriginalX(0);
                child.setYPositionMode(WidgetPositionMode.ABSOLUTE_CENTER);
                child.setOriginalY(-15);
                child.setOriginalHeight(20);
                child.setXTextAlignment(WidgetTextAlignment.CENTER);
                child.setYTextAlignment(WidgetTextAlignment.CENTER);
                child.setWidthMode(WidgetSizeMode.MINUS);
                child.setText("Type a quick search code to see all favorited items with that code!");
                child.revalidate();
            });
        }

        if (quantityOrPriceChatboxOpen
                && event.getIndex() == VarClientInt.INPUT_TYPE.getIndex()
                && client.getVarcIntValue(VarClientInt.INPUT_TYPE.getIndex()) == 0
        ) {
            quantityOrPriceChatboxOpen = false;

            return;
        }

        //Check that it was the chat input that got enabled.
        if (event.getIndex() != VarClientInt.INPUT_TYPE.getIndex()
                || client.getWidget(WidgetInfo.CHATBOX_TITLE) == null
                || client.getVarcIntValue(VarClientInt.INPUT_TYPE.getIndex()) != 7
                || client.getWidget(WidgetInfo.GRAND_EXCHANGE_OFFER_CONTAINER) == null) {
            return;
        }
        quantityOrPriceChatboxOpen = true;

        plugin.getClientThread().invokeLater(() ->
        {
            OfferEditor flippingWidget = new OfferEditor(client.getWidget(WidgetInfo.CHATBOX_CONTAINER), client);
            Optional<FlippingItem> selectedItem = plugin.viewTradesForCurrentView().stream().filter(item -> item.getItemId() == client.getVar(CURRENT_GE_ITEM)).findFirst();

            String chatInputText = client.getWidget(WidgetInfo.CHATBOX_TITLE).getText();
            String offerText = client.getWidget(WidgetInfo.GRAND_EXCHANGE_OFFER_CONTAINER).getChild(GE_OFFER_INIT_STATE_CHILD_ID).getText();

            if (chatInputText.equals("How many do you wish to buy?")) {
                plugin.getFlippingPanel().getOfferEditorContainerPanel().selectQuantityEditor();
                //No recorded data; default to total GE limit
                if (!selectedItem.isPresent()) {
                    ItemStats itemStats = plugin.getItemManager().getItemStats(client.getVar(CURRENT_GE_ITEM), false);
                    int itemGELimit = itemStats != null ? itemStats.getGeLimit() : 0;
                    flippingWidget.showQuantityWidgets(itemGELimit);
                } else {
                    flippingWidget.showQuantityWidgets(selectedItem.get().getRemainingGeLimit());
                }
            } else if (chatInputText.equals("Set a price for each item:")) {
                plugin.getFlippingPanel().getOfferEditorContainerPanel().selectPriceEditor();
                WikiRequest wikiRequest = plugin.getLastWikiRequest();

                if (offerText.equals("Buy offer")) {
                    int instaSellPrice = 0;
                    int wikiInstaSellPrice = 0;
                    if (selectedItem.isPresent() && selectedItem.get().getLatestInstaSell().isPresent()) {
                        instaSellPrice = selectedItem.get().getLatestInstaSell().get().getPrice();
                    }
                    if (wikiRequest != null && wikiRequest.getData().containsKey(highlightedItemId) && wikiRequest.getData().get(highlightedItemId).getLow() != 0) {
                        wikiInstaSellPrice = wikiRequest.getData().get(highlightedItemId).getLow();
                    }
                    flippingWidget.showInstaSellPrices(instaSellPrice, wikiInstaSellPrice);
                }
                else if (offerText.equals("Sell offer")) {
                    int instaBuyPrice = 0;
                    int wikiInstaBuyPrice = 0;
                    if (selectedItem.isPresent() && selectedItem.get().getLatestInstaBuy().isPresent()) {
                        instaBuyPrice = selectedItem.get().getLatestInstaBuy().get().getPrice();
                    }

                    if (wikiRequest != null && wikiRequest.getData().containsKey(highlightedItemId) && wikiRequest.getData().get(highlightedItemId).getHigh() != 0) {
                        wikiInstaBuyPrice = wikiRequest.getData().get(highlightedItemId).getHigh();
                    }
                    flippingWidget.showInstaBuyPrices(instaBuyPrice, wikiInstaBuyPrice);
                }
            }
        });
    }

    public void onVarbitChanged(VarbitChanged event) {
        Client client = plugin.getClient();
        FlippingPanel flippingPanel = plugin.getFlippingPanel();
        if (event.getIndex() == CURRENT_GE_ITEM.getId() && client.getVar(CURRENT_GE_ITEM) != -1 && client.getVar(CURRENT_GE_ITEM) != 0) {
            highlightOffer();
        }

        //need to check if panel is highlighted in this case because curr ge item is changed if you come back to ge interface after exiting out
        //and curr ge item would be -1 or 0 in that case and would trigger a dehighlight erroneously.
        if (event.getIndex() == CURRENT_GE_ITEM.getId() &&
                (client.getVar(CURRENT_GE_ITEM) == -1 || client.getVar(CURRENT_GE_ITEM) == 0) && highlightedItem.isPresent()) {
            deHighlightOffer();
        }
    }

    public void onWidgetLoaded(WidgetLoaded event) {
        //ge history widget loaded
        //GE_HISTORY_TAB_WIDGET_ID does not load when history tab is opened from the banker right click. It only loads when
        //the "history" button is clicked for the ge interface. However, 383 loads in both situations.
        if (event.getGroupId() == 383) {
            plugin.showGeHistoryTabPanel();
        }

        //if either ge interface or bank pin interface is loaded, hide the ge history tab panel again
        if (event.getGroupId() == WidgetID.GRAND_EXCHANGE_GROUP_ID || event.getGroupId() == 213) {
            plugin.getMasterPanel().selectPreviouslySelectedTab();
        }

        //remove highlighted item
        //The player opens the trade history tab from the ge interface. Necessary since the back button isn't considered hidden here.
        //this (id 149 and not id 383) will also trigger when the player just exits out of the ge interface offer window screen, which is good
        //as then the highlight won't linger in that case.
        if (event.getGroupId() == GE_HISTORY_TAB_WIDGET_ID && highlightedItem.isPresent()) {
            deHighlightOffer();
        }
    }

    public void onScriptPostFired(ScriptPostFired event) {
        //ge history interface closed, so the geHistoryTabPanel should no longer show
        if (event.getScriptId() == 29) {
            plugin.getMasterPanel().selectPreviouslySelectedTab();
        }

        if (event.getScriptId() == 804) {
            //Fired after every GE offer slot redraw
            //This seems to happen after any offer updates or if buttons are pressed inside the interface
            //https://github.com/RuneStar/cs2-scripts/blob/a144f1dceb84c3efa2f9e90648419a11ee48e7a2/scripts/%5Bclientscript%2Cge_offers_switchpanel%5D.cs2
            if (plugin.getConfig().slotTimersEnabled()) {
                plugin.rebuildTradeTimers();
            }
        }
    }

    private void highlightOffer() {
        highlightedItemId = plugin.getClient().getVar(CURRENT_GE_ITEM);
        Optional<FlippingItem> itemInHistory = plugin.viewTradesForCurrentView().stream().filter(item -> item.getItemId() == highlightedItemId && item.getValidFlippingPanelItem()).findFirst();
        if (itemInHistory.isPresent()) {
            highlightedItem = itemInHistory;
        }
        else {
            String itemName = plugin.getItemManager().getItemComposition(highlightedItemId).getName();
            ItemStats itemStats = plugin.getItemManager().getItemStats(highlightedItemId, false);
            int geLimit = itemStats != null ? itemStats.getGeLimit() : 0;
            FlippingItem dummyFlippingItem = new FlippingItem(highlightedItemId, itemName, geLimit, "NA");
            dummyFlippingItem.setValidFlippingPanelItem(true);
            highlightedItem = Optional.of(dummyFlippingItem);
        }

        plugin.getFlippingPanel().highlightItem(highlightedItem.get());
    }

    private void deHighlightOffer() {
        highlightedItem = Optional.empty();
        plugin.getFlippingPanel().dehighlightItem();
    }
}
