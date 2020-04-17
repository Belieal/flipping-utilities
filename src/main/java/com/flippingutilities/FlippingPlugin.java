/*
 * Copyright (c) 2020, Belieal <https://github.com/Belieal>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.flippingutilities;

import com.flippingutilities.ui.TabManager;
import com.flippingutilities.ui.flipping.FlippingItemWidget;
import com.flippingutilities.ui.flipping.FlippingPanel;
import com.flippingutilities.ui.statistics.StatisticsPanel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.VarClientInt;
import static net.runelite.api.VarPlayer.CURRENT_GE_ITEM;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.api.events.VarClientIntChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetHiddenChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.account.AccountSession;
import net.runelite.client.account.SessionManager;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.SessionClose;
import net.runelite.client.events.SessionOpen;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.http.api.item.ItemStats;

@Slf4j
@PluginDescriptor(
	name = "Flipping Utilities",
	description = "Provides utilities for GE flipping"
)

public class FlippingPlugin extends Plugin
{
	private static final int GE_HISTORY_TAB_WIDGET_ID = 149;
	private static final int GE_BACK_BUTTON_WIDGET_ID = 30474244;
	private static final int GE_OFFER_INIT_STATE_CHILD_ID = 18;

	public static final String CONFIG_GROUP = "flipping";
	public static final String ACCOUNT_WIDE = "accountwide";

	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private ScheduledExecutorService executor;
	private ScheduledFuture timeUpdateFuture;
	@Inject
	private ClientToolbar clientToolbar;
	private NavigationButton navButton;

	@Inject
	private ConfigManager configManager;
	@Inject
	private SessionManager sessionManager;
	@Inject
	@Getter
	private FlippingConfig config;

	@Inject
	private ItemManager itemManager;

	private FlippingPanel flippingPanel;
	private StatisticsPanel statPanel;
	private FlippingItemWidget flippingWidget;

	private TabManager tabManager;

	//Stores all bought or sold trades.
	private ArrayList<FlippingItem> accountSpecificTrades = new ArrayList<>();

	private ArrayList<FlippingItem> accountWideTrades = new ArrayList<>();

	//Ensures we don't rebuild constantly when highlighting
	@Setter
	private int prevHighlight;

	private String username;


	//will store the last seen events for each GE slot and so that we can screen out duplicate/bad events
	private Map<Integer, OfferInfo> lastOffers = new HashMap<>();

	@Override
	protected void startUp()
	{
		//Main visuals.
		flippingPanel = new FlippingPanel(this, itemManager, executor);
		statPanel = new StatisticsPanel(this, itemManager, executor);

		//Represents the panel navigation that switches between panels using tabs at the top.
		tabManager = new TabManager(flippingPanel, statPanel);

		// I wanted to put it below the GE plugin, but can't as the GE and world switcher button have the same priority...
		navButton = NavigationButton.builder()
			.tooltip("Flipping Plugin")
			.icon(ImageUtil.getResourceStreamFromClass(getClass(), "/graphIconGreen.png"))
			.priority(3)
			.panel(tabManager)
			.build();

		clientToolbar.addNavigation(navButton);

		clientThread.invokeLater(() ->
		{
			switch (client.getGameState())
			{
				case STARTING:
				case UNKNOWN:
					return false;
			}
			//Loads accountWideData when the client (and thus this plugin) is first started
			//with data from previous sessions stored using the ConfigManager
			if (config.storeTradeHistory())
			{
				accountWideTrades = loadTradeHistory(ACCOUNT_WIDE);
				updateDisplays(accountWideTrades);
			}

			return true;
		});

		//Ensures the panel displays for the margin check being outdated and the next ge refresh
		//are updated every second.
		timeUpdateFuture = executor.scheduleAtFixedRate(() ->
		{
			flippingPanel.updateActivePanelsPriceOutdatedDisplay();
			flippingPanel.updateActivePanelsGePropertiesDisplay();
		}, 100, 1000, TimeUnit.MILLISECONDS);
	}

	@Override
	protected void shutDown()
	{
		if (timeUpdateFuture != null)
		{
			//Stop all timers
			timeUpdateFuture.cancel(true);
			timeUpdateFuture = null;
		}

		clientToolbar.removeNavigation(navButton);
	}

	@Subscribe
	public void onSessionOpen(SessionOpen event)
	{
		//Load new account config
		final AccountSession session = sessionManager.getAccountSession();
		if (session != null && session.getUsername() != null)
		{
			clientThread.invokeLater(() ->
			{
				//don't know if the user is logged in or not, so get key depending on whether username is set.
				loadTradeHistory(getAccountWideOrSpecificKey());
				SwingUtilities.invokeLater(() -> flippingPanel.rebuildFlippingPanel(getTrades()));
				return true;
			});
		}
	}

	@Subscribe
	public void onSessionClose(SessionClose event)
	{
		//Config is now locally stored
		clientThread.invokeLater(() ->
		{
			loadTradeHistory(getAccountWideOrSpecificKey());
			SwingUtilities.invokeLater(() -> flippingPanel.rebuildFlippingPanel(getTrades()));
			return true;
		});
	}

	/**
	 * Returns the correct config key to fetch history based on whether the a user is logged in or not.
	 *
	 * @return either the username or ACCOUNT_WIDE
	 */
	private String getAccountWideOrSpecificKey()
	{
		return username == null ? ACCOUNT_WIDE : username;

	}

	/**
	 * Gets an already loaded tradesList depending on whether the user is logged in or not.
	 * If the user is logged in, and thus username will be set, then the accountSpecificTrades
	 * is returned. Otherwise if the username hasn't been set, such as when a user just opens
	 * the client, the accountWideTrades are returned.
	 *
	 * @return trades
	 */
	public ArrayList<FlippingItem> getTrades()
	{
		return username == null ? accountWideTrades : accountSpecificTrades;
	}

	/**
	 * This method is invoked every time the plugin receives a GrandExchangeOfferChanged event.
	 * If the event is determined to be bad event (duplicate, doesn't convey unique info), it is
	 * screened out by isBadEvent.
	 * Otherwise, the information is extracted from the event and used to construct an OfferInfo
	 * object.
	 * Then, the method tries to find whether the item exists and passes the result of that along with
	 * which tradesList should be updated, and the OfferInfo object, to updateTradesList.
	 * After updateTradesList updates the trade lists it was passed, the contents of both the tradeslists
	 * are persisted as they have changed. (TODO perhaps only persist on shutdown?).
	 * Finally, because the underlying data that the flipping panel and the stats panel rely on has
	 * been updated, they need to be refreshed.
	 *
	 * @param newOfferEvent the offer event that represents when an offer is updated
	 *                      (buying, selling, bought, sold, cancelled sell, or cancelled buy)
	 */
	@Subscribe
	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged newOfferEvent)
	{

		OfferInfo newOffer = extractRelevantInfo(newOfferEvent);

		if (isBadOffer(newOffer))
		{
			return;
		}

		Optional<FlippingItem> accountSpecificItem = findItemInTradesList(
			accountSpecificTrades,
			(item) -> item.getItemId() == newOffer.getItemId());

		Optional<FlippingItem> accountWideItem = findItemInTradesList(
			accountWideTrades,
			(item) -> item.getItemId() == newOffer.getItemId() && item.flippedBy.equals(username));

		updateTradesList(accountWideTrades, accountWideItem, newOffer);
		updateTradesList(accountSpecificTrades, accountSpecificItem, newOffer);

		storeTradeHistory();

		//only way items can float to the top of the list (hence requiring a rebuild) is when
		//the offer is a margin check.
		if (newOffer.isMarginCheck())
		{
			flippingPanel.rebuildFlippingPanel(accountSpecificTrades);
		}

		statPanel.updateDisplays();

	}

	/**
	 * Runelite has some wonky events at times. For example, every empty/buy/sell/cancelled buy/cancelled sell
	 * spawns two identical events. And when you fully buy/sell item, it also spawns two events (a
	 * buying/selling event and a bought/sold event). This method screens out the unwanted events/duplicate
	 * events and also sets the ticks since the first offer in that slot to help with figuring out whether
	 * an offer is a margin check.
	 *
	 * @param newOffer
	 * @return a boolean representing whether the offer should be passed on or discarded
	 */
	private boolean isBadOffer(OfferInfo newOffer)
	{
		//i am mutating offers and they are being passed around, so i'm cloning to avoid passing the same reference around.
		OfferInfo clonedNewOffer = newOffer.clone();

		//Check empty offers.
		if (clonedNewOffer.getItemId() == 0 || clonedNewOffer.getState() == GrandExchangeOfferState.EMPTY)
		{
			return true;
		}

		//this is always the start of any offer (when you first put in an offer)
		if (clonedNewOffer.getQuantity() == 0)
		{
			lastOffers.put(clonedNewOffer.getSlot(), clonedNewOffer);//tickSinceFirstOffer is 0 here
			return true;
		}

		//when an offer is complete, two events are generated: a buying/selling event and a bought/sold event.
		//this clause ignores the buying/selling event as it conveys the same info. We can tell its the buying/selling
		//event right before a bought/sold event due to the quantity of the offer being == to the total quantity of the offer.
		if ((clonedNewOffer.getState() == GrandExchangeOfferState.BUYING || clonedNewOffer.getState() == GrandExchangeOfferState.SELLING) && clonedNewOffer.getQuantity() == newOffer.getTotalQuantity())
		{
			return true;
		}

		OfferInfo lastOfferForSlot = lastOffers.get(clonedNewOffer.getSlot());

		//if its a duplicate as the last seen event
		if (lastOfferForSlot.equals(clonedNewOffer))
		{
			return true;
		}

		int tickDiffFromLastOffer = clonedNewOffer.getTickArrivedAt() - lastOfferForSlot.getTickArrivedAt();
		clonedNewOffer.setTicksSinceFirstOffer(tickDiffFromLastOffer + lastOfferForSlot.getTicksSinceFirstOffer());
		lastOffers.put(clonedNewOffer.getSlot(), clonedNewOffer);
		newOffer.setTicksSinceFirstOffer(tickDiffFromLastOffer + lastOfferForSlot.getTicksSinceFirstOffer());
		return false; //not a bad event

	}

	/**
	 * This method extracts the data from the GrandExchangeOfferChanged event, which is a nested
	 * data structure, and puts it into a flat data structure- OfferInfo. It also adds time to the offer.
	 *
	 * @param newOfferEvent new offer event just received
	 * @return an OfferInfo with the relevant information.
	 */
	private OfferInfo extractRelevantInfo(GrandExchangeOfferChanged newOfferEvent)
	{

		GrandExchangeOffer offer = newOfferEvent.getOffer();

		boolean isBuy = offer.getState() == GrandExchangeOfferState.BOUGHT || offer.getState() == GrandExchangeOfferState.CANCELLED_BUY || offer.getState() == GrandExchangeOfferState.BUYING;

		OfferInfo offerInfo = new OfferInfo(
			isBuy,
			offer.getItemId(),
			offer.getQuantitySold(),
			offer.getQuantitySold() == 0 ? 0 : offer.getSpent() / offer.getQuantitySold(),
			Instant.now(),
			newOfferEvent.getSlot(),
			offer.getState(),
			client.getTickCount(),
			0,
			offer.getTotalQuantity());

		return offerInfo;
	}

	/**
	 * Finds an item in the given trades list that matches the given criteria.
	 *
	 * @param trades   the trades list to search through
	 * @param criteria a function that needs to return true for the item to be returned
	 * @return the item that matches the criteria
	 */
	private Optional<FlippingItem> findItemInTradesList(ArrayList<FlippingItem> trades, Predicate<FlippingItem> criteria)
	{
		return trades.stream().filter(criteria).findFirst();
	}

	/**
	 * This method updates the given trade list in response to an OfferInfo based on whether an item
	 * that matches what the offer was for already exists and whether the offer was a margin check.
	 * <p>
	 * If the offer was a margin check, and the item is present, that item's history and margin need
	 * to be updated. If the item isn't presented, a FlippingItem for the item in that offer and added to the trades
	 * list.
	 * <p>
	 * If the offer was not a margin check and the item was present, just update the history and last traded
	 * times of the object. (no need to update margins as the offer was not a margin check)
	 * <p>
	 * if the offer was not a margin check and the item wasn't present, we don't do anything as there
	 * is no way to know what to display for the margin checked prices (as it wasn't a margin check) when
	 * updating the FlippingItem that would have had to be constructed.
	 *
	 * @param trades       the trades list to update
	 * @param flippingItem the flipping item to be updated in the tradeslist, if it even exists
	 * @param newOffer     new offer that just came in
	 */
	private void updateTradesList(ArrayList<FlippingItem> trades, Optional<FlippingItem> flippingItem, OfferInfo newOffer)
	{
		if (newOffer.isMarginCheck())
		{
			if (flippingItem.isPresent())
			{
				FlippingItem item = flippingItem.get();
				item.updateMargin(newOffer);
				item.updateHistoryAndTradedTime(newOffer);

				trades.remove(item);
				trades.add(0, item);
			}
			else
			{
				addToTradesList(trades, newOffer);
			}
		}

		//if the item exists in the trades list but its not a margin check, you only need to update its history and
		//last traded times.
		else if (flippingItem.isPresent())
		{
			flippingItem.get().updateHistoryAndTradedTime(newOffer);
		}
	}

	/**
	 * Given a new offer, this method creates a FlippingItem, the data structure that represents an item
	 * you are currently flipping, and adds it to the accountSpecificTrades. The accountSpecificTrades is a crucial part of the state
	 * of the flippingPlugin, as only items from the accountSpecificTrades are rendered and updated.
	 *
	 * @param newOffer new offer just received
	 */
	private void addToTradesList(ArrayList<FlippingItem> tradesList, OfferInfo newOffer)
	{
		int tradeItemId = newOffer.getItemId();
		String itemName = itemManager.getItemComposition(tradeItemId).getName();

		ItemStats itemStats = itemManager.getItemStats(tradeItemId, false);
		int geLimit = itemStats != null ? itemStats.getGeLimit() : 0;

		FlippingItem flippingItem = new FlippingItem(tradeItemId, itemName, geLimit, username);
		flippingItem.updateMargin(newOffer);
		flippingItem.updateHistoryAndTradedTime(newOffer);

		tradesList.add(0, flippingItem);

	}

	@Provides
	FlippingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FlippingConfig.class);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getIndex() == CURRENT_GE_ITEM.getId() &&
			client.getVar(CURRENT_GE_ITEM) != -1 && client.getVar(CURRENT_GE_ITEM) != 0)
		{
			highlightOffer();
		}
	}

	@Subscribe
	public void onWidgetHiddenChanged(WidgetHiddenChanged event)
	{
		Widget widget = event.getWidget();
		// If the back button is no longer visible, we know we aren't in the offer setup.
		if (flippingPanel.isItemHighlighted() && widget.isHidden() && widget.getId() == GE_BACK_BUTTON_WIDGET_ID)
		{
			flippingPanel.dehighlightItem();
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		// The player opens the trade history tab. Necessary since the back button isn't considered hidden here.
		if (event.getGroupId() == GE_HISTORY_TAB_WIDGET_ID && flippingPanel.isItemHighlighted())
		{
			flippingPanel.dehighlightItem();
		}
	}

	//TODO: Refactor this with a search on the search bar
	private void highlightOffer()
	{
		int currentGEItemId = client.getVar(CURRENT_GE_ITEM);
		if (currentGEItemId == prevHighlight || flippingPanel.isItemHighlighted())
		{
			return;
		}
		prevHighlight = currentGEItemId;
		flippingPanel.highlightItem(currentGEItemId);
	}

	//Functionality to the top right reset button.
	public void resetTradeHistory()
	{

		//if username is null, then user hasn't logged in. So reset account wide trade list
		if (username == null)
		{
			accountWideTrades.clear();
			log.info("resetting account wide trades");
			configManager.unsetConfiguration(CONFIG_GROUP, ACCOUNT_WIDE);
		}
		//user has logged in, so reset username specific trade list.
		else
		{
			accountSpecificTrades.clear();
			log.info("resetting username specific");
			configManager.unsetConfiguration(CONFIG_GROUP, username);
		}
	}

	/**
	 * Currently, we use this method for finding out when a user logs in so that we can
	 * get their username to load their specific trade history to display, as opposed to the global
	 * trade history across all their accounts.
	 *
	 * @param event GameStateChanged event, such as when a user logs in.
	 */
	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			username = client.getUsername();

			if (config.storeTradeHistory())
			{
				accountSpecificTrades = loadTradeHistory(username);
				updateDisplays(accountSpecificTrades);
				tabManager.setWhichTradesListDisplay(username);
			}
		}
	}

	/**
	 * This method is used check whether the items in the flipping list have old values that need
	 * to be reset. For example, the ge refresh time might be from > 4 hours ago, so that
	 * would need to be reset, same thing with the ge limit.
	 *
	 * @param tradesList all the current trades.
	 */
	private void validateAllItems(List<FlippingItem> tradesList)
	{
		for (FlippingItem item : tradesList)
		{
			item.validateGeProperties();
		}
	}


	/**
	 * This method is called when history has just been loaded. This history, which is a list of
	 * FlippingItem can have old data that needs to be refreshed so validateAllItems is called. Furthermore,
	 * the statPanel and the FlippingPanel need to have their displays updated. This method is invoked in
	 * {@link FlippingPlugin#startUp()} and {@link FlippingPlugin#onGameStateChanged(GameStateChanged)}, when a user
	 * logs in.
	 */
	private void updateDisplays(ArrayList<FlippingItem> trades)
	{
		executor.submit(() -> clientThread.invokeLater(() -> SwingUtilities.invokeLater(() ->
		{
			validateAllItems(trades);
			flippingPanel.rebuildFlippingPanel(trades);
			statPanel.updateDisplays();
		})));
	}

	/**
	 * Stores the trade history
	 */
	public void storeTradeHistory()
	{

		if (accountSpecificTrades.isEmpty())
		{
			return;
		}
		final Gson gson = new Gson();
		executor.submit(() ->
		{
			String accountSpecificTradesJson = gson.toJson(accountSpecificTrades);
			String accountWideTradesJson = gson.toJson(accountWideTrades);
			configManager.setConfiguration(CONFIG_GROUP, ACCOUNT_WIDE, accountWideTradesJson);
			//username shouldn't be null as storeTradeHistory is only called when we get offers and even
			//login offers only come in after the username is set, but just in case...
			if (username != null)
			{
				configManager.setConfiguration(CONFIG_GROUP, username, accountSpecificTradesJson);
			}

		});
	}

	/**
	 * Gets the trade history for the specified key as JSON, turns it into an arraylist of FlippingItem
	 * and returns it.
	 *
	 * @param key can be either ACCOUNT_WIDE (which is currently the string "accountwide") or a username
	 *            which is the username of the currently logged in user.
	 */
	public ArrayList<FlippingItem> loadTradeHistory(String key)
	{
		log.info(String.format("Loading flipping history for %s", key));

		String tradesJson = configManager.getConfiguration(CONFIG_GROUP, key);
		return tradesJson == null ? new ArrayList<>() : jsonToTradesList(tradesJson);
	}

	private ArrayList<FlippingItem> jsonToTradesList(String json)
	{
		try
		{
			final Gson gson = new Gson();
			Type type = new TypeToken<ArrayList<FlippingItem>>()
			{

			}.getType();
			return gson.fromJson(json, type);
		}
		catch (Exception e)
		{
			log.info("Error loading flipping data: " + e);
			return new ArrayList<>();
		}

	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(CONFIG_GROUP))
		{
			//Ensure that user configs are updated after being changed
			switch (event.getKey())
			{
				case ("storeTradeHistory"):
				case ("outOfDateWarning"):
				case ("roiGradientMax"):
				case ("marginCheckLoss"):
				case ("twelveHourFormat"):
					flippingPanel.rebuildFlippingPanel(accountSpecificTrades);
					break;
				default:
					break;
			}
		}
	}

	@Subscribe
	public void onVarClientIntChanged(VarClientIntChanged event)
	{
		//Check that it was the chat input that got enabled.
		if (event.getIndex() != VarClientInt.INPUT_TYPE.getIndex()
			|| client.getWidget(WidgetInfo.CHATBOX_TITLE) == null
			|| client.getVarcIntValue(VarClientInt.INPUT_TYPE.getIndex()) != 7)
		{
			return;
		}

		clientThread.invokeLater(() ->
		{

			flippingWidget = new FlippingItemWidget(client.getWidget(WidgetInfo.CHATBOX_CONTAINER), client);


			FlippingItem selectedItem = null;
			//Check that if we've recorded any data for the item.
			for (FlippingItem item : accountSpecificTrades)
			{
				if (item.getItemId() == client.getVar(CURRENT_GE_ITEM))
				{
					selectedItem = item;
					break;
				}
			}

			String chatInputText = client.getWidget(WidgetInfo.CHATBOX_TITLE).getText();
			String offerText = client.getWidget(WidgetInfo.GRAND_EXCHANGE_OFFER_CONTAINER).getChild(GE_OFFER_INIT_STATE_CHILD_ID).getText();
			if (chatInputText.equals("How many do you wish to buy?"))
			{
				//No recorded data; default to total GE limit
				if (selectedItem == null)
				{
					ItemStats itemStats = itemManager.getItemStats(client.getVar(CURRENT_GE_ITEM), false);
					int itemGELimit = itemStats != null ? itemStats.getGeLimit() : 0;
					flippingWidget.showWidget("setQuantity", itemGELimit);
				}
				else
				{
					flippingWidget.showWidget("setQuantity", selectedItem.remainingGeLimit());
				}
			}
			else if (chatInputText.equals("Set a price for each item:"))
			{
				if (offerText.equals("Buy offer"))
				{
					//No recorded data; hide the widget
					if (selectedItem == null || selectedItem.getMarginCheckBuyPrice() == 0)
					{
						flippingWidget.showWidget("reset", 0);
					}
					else
					{
						flippingWidget.showWidget("setBuyPrice", selectedItem.getMarginCheckBuyPrice());
					}
				}
				else if (offerText.equals("Sell offer"))
				{
					//No recorded data; hide the widget
					if (selectedItem == null || selectedItem.getMarginCheckSellPrice() == 0)
					{
						flippingWidget.showWidget("reset", 0);
					}
					else
					{
						flippingWidget.showWidget("setSellPrice", selectedItem.getMarginCheckSellPrice());
					}
				}
			}
		});
	}
}