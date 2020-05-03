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
import com.flippingutilities.ui.statistics.StatsPanel;
import com.google.inject.Provides;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javax.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.Player;
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
import net.runelite.client.account.SessionManager;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.events.ConfigChanged;
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
	public static final String ITEMS_CONFIG_KEY = "items";
	public static final String TIME_INTERVAL_CONFIG_KEY = "selectedinterval";
	public static final String ACCOUNT_WIDE = "Accountwide";

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

	//Ensures we don't rebuild constantly when highlighting
	@Setter
	private int prevHighlight;

	@Getter
	private FlippingPanel flippingPanel;
	@Getter
	private StatsPanel statPanel;
	private FlippingItemWidget flippingWidget;

	private TabManager tabManager;

	private boolean previouslyLoggedIn;

	//used to load and store trades from a file on disk.
	private TradePersister tradePersister = new TradePersister();

	//holds all the trades each of the user's accounts that has a flipping history. This is a map
	//of display name to flipping items.
	@Getter
	private Map<String, AccountData> allAccountsData = new HashMap<>();

	//the display name of the account whose trade list the user is currently looking at as selected
	//through the dropdown menu
	private String accountCurrentlyViewed = ACCOUNT_WIDE;

	//the display name of the currently logged in user. This is the only account that can actually receive offers
	//as this is the only account currently logged in.
	private String currentlyLoggedInAccount;

	@Override
	protected void startUp()
	{
		//Main visuals.
		flippingPanel = new FlippingPanel(this, itemManager, executor);
		statPanel = new StatsPanel(this, itemManager);

		//Represents the panel navigation that switches between panels using tabs at the top.
		tabManager = new TabManager(this::changeView, flippingPanel, statPanel);

		// I wanted to put it below the GE plugin, but can't as the GE and world switcher button have the same priority...
		navButton = NavigationButton.builder()
			.tooltip("Flipping Utilities")
			.icon(ImageUtil.getResourceStreamFromClass(getClass(), "/graph_icon_green.png"))
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
			//Loads tradesList with data from previous sessions.
			if (config.storeTradeHistory())
			{
				try
				{
					tradePersister.setup();
					allAccountsData = loadTrades();
				}
				catch (IOException e)
				{
					log.info("couldn't set up trade persistor: " + e);
					allAccountsData = new HashMap<>();
				}

				if (!allAccountsData.containsKey(ACCOUNT_WIDE))
				{
					allAccountsData.put(ACCOUNT_WIDE, new AccountData());
				}
			}

			//adding an item causes the event listener (changeView) to fire which causes stat panel
			//and flipping panel to rebuild.
			tabManager.getViewSelector().addItem(ACCOUNT_WIDE);

			allAccountsData.keySet().forEach(displayName ->
			{
				if (!displayName.equals(ACCOUNT_WIDE))
				{
					tabManager.getViewSelector().addItem(displayName);
				}
			});

			//sets the account selector dropdown to visible or not depending on whether the config option has been selected.
			tabManager.getViewSelector().setVisible(config.multiAccTracking());

			String lastSelectedInterval = configManager.getConfiguration(CONFIG_GROUP, TIME_INTERVAL_CONFIG_KEY);
			if (lastSelectedInterval == null)
			{
				statPanel.setTimeInterval("All", true);
			}
			else
			{
				statPanel.setTimeInterval(lastSelectedInterval, true);
			}

			return true;
		});


		//Ensures the panel displays for the margin check being outdated and the next ge reset
		//are updated every second.
		timeUpdateFuture = executor.scheduleAtFixedRate(() ->
		{
			flippingPanel.updateActivePanelsPriceOutdatedDisplay();
			flippingPanel.updateActivePanelsGePropertiesDisplay();
			statPanel.updateSessionTime();
		}, 100, 1000, TimeUnit.MILLISECONDS);
	}

	/**
	 * Gets invoked when the client is shutting down. This method calls storeTrades and blocks the main
	 * thread until it finishes, thus allowing trades to be stored safely.
	 *
	 * @param clientShutdownEvent even that we receive when the client is shutting down
	 */

	@Subscribe
	public void onClientShutdown(ClientShutdown clientShutdownEvent)
	{
		log.info("Shutting down, saving trades!");
		storeTrades(allAccountsData);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			//keep scheduling this task until it returns true (when we have access to a display name)
			clientThread.invokeLater(() ->
			{
				//we return true in this case as something went wrong and somehow the state isn't logged in, so we don't
				//want to keep scheduling this task.
				if (client.getGameState() != GameState.LOGGED_IN)
				{
					return true;
				}

				final Player player = client.getLocalPlayer();

				//player is null, so we can't get the display name so, return false, which will schedule
				//the task on the client thread again.
				if (player == null)
				{
					return false;
				}

				final String name = player.getName();

				if (name == null)
				{
					return false;
				}

				if (name.equals(""))
				{
					return false;
				}
				log.info("{} just logged in", name);
				previouslyLoggedIn = true;
				handleLogin(name);

				return true;
			});
		}

		else if (event.getGameState() == GameState.LOGIN_SCREEN && previouslyLoggedIn)
		{
			log.info("{} just logged out", currentlyLoggedInAccount);
			currentlyLoggedInAccount = null;
			storeTrades(allAccountsData);
		}
	}

	public void handleLogin(String displayName)
	{
		//if the account has no trade history, add its name to the cache and give it a blank history.
		if (!allAccountsData.containsKey(displayName))
		{
			allAccountsData.put(displayName, new AccountData());
			tabManager.getViewSelector().addItem(displayName);
		}

		currentlyLoggedInAccount = displayName;

		if (config.multiAccTracking()) {
			accountCurrentlyViewed = displayName;
			//this will cause changeView to be invoked which will cause a rebuild of
			//flipping and stats panel
			tabManager.getViewSelector().setSelectedItem(displayName);
		}


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

	/**
	 * This method is invoked every time the plugin receives a GrandExchangeOfferChanged event.
	 * The events are handled in one of two ways:
	 * <p>
	 * if the offer is deemed a margin check, its either added
	 * to the tradesForCurrentView (if it doesn't exist), or, if the item exists, it is updated to reflect the margins as
	 * discovered by the margin check.
	 * <p>
	 * The second way events are handled is in all other cases except for margin checks. If an offer is
	 * not a margin check and the offer exists, you don't need to update the margins of the item, but you do need
	 * to update its history (which updates its ge limit/reset time and the profit a user made for that item.
	 * <p>
	 * The history of a flipping item is updated in every branch of this method.
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

		List<FlippingItem> currentlyLoggedInAccountsTrades = allAccountsData.get(currentlyLoggedInAccount).getTrades();
		List<FlippingItem> accountWideTrades = allAccountsData.get(ACCOUNT_WIDE).getTrades();

		Optional<FlippingItem> accountSpecificItem = findItemInTradesList(
			currentlyLoggedInAccountsTrades,
			(item) -> item.getItemId() == newOffer.getItemId());

		Optional<FlippingItem> accountWideItem = findItemInTradesList(
			accountWideTrades,
			(item) -> item.getItemId() == newOffer.getItemId() && item.getFlippedBy().equals(currentlyLoggedInAccount));

		updateTradesList(accountWideTrades, accountWideItem, newOffer.clone());
		updateTradesList(currentlyLoggedInAccountsTrades, accountSpecificItem, newOffer.clone());

		//only way items can float to the top of the list (hence requiring a rebuild) is when
		//the offer is a margin check. Additionally, there is no point rebuilding the panel when
		//the user is looking at the trades list of another one of their accounts that isn't logged in as that
		//trades list won't be being updated.
		if (newOffer.isMarginCheck() && (accountCurrentlyViewed.equals(currentlyLoggedInAccount) || accountCurrentlyViewed.equals(ACCOUNT_WIDE)))
		{
			flippingPanel.rebuild(allAccountsData.get(accountCurrentlyViewed).getTrades());
		}

		if (accountCurrentlyViewed.equals(currentlyLoggedInAccount) || accountCurrentlyViewed.equals(ACCOUNT_WIDE))
		{
			statPanel.rebuild(getTradesForCurrentView());
			if (!newOffer.isMarginCheck())
			{
				flippingPanel.updateActivePanelsGePropertiesDisplay();
			}

		}
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

		//Check empty offers (we always get them for every empty slot there is)
		if (clonedNewOffer.getItemId() == 0 || clonedNewOffer.getState() == GrandExchangeOfferState.EMPTY)
		{
			return true;
		}

		//empty offers (handled right above) come before currentlyLoggedInAccount is set which is why this is put after
		//the empty offer check and empty offers should always be rejected anyway.
		Map<Integer, OfferInfo> loggedInAccsLastOffers = allAccountsData.get(currentlyLoggedInAccount).getLastOffers();


		//this is always the start of any offer (when you first put in an offer)
		if (clonedNewOffer.getCurrentQuantityInTrade() == 0)
		{
			loggedInAccsLastOffers.put(clonedNewOffer.getSlot(), clonedNewOffer);//tickSinceFirstOffer is 0 here
			return true;
		}

		//when an offer is complete, two events are generated: a buying/selling event and a bought/sold event.
		//this clause ignores the buying/selling event as it conveys the same info. We can tell its the buying/selling
		//event right before a bought/sold event due to the currentQuantityInTrade of the offer being == to the total currentQuantityInTrade of the offer.
		if ((clonedNewOffer.getState() == GrandExchangeOfferState.BUYING || clonedNewOffer.getState() == GrandExchangeOfferState.SELLING) && clonedNewOffer.getCurrentQuantityInTrade() == newOffer.getTotalQuantityInTrade())
		{
			return true;
		}

		OfferInfo lastOfferForSlot = loggedInAccsLastOffers.get(clonedNewOffer.getSlot());

		//this occurs when the user made the trade on a different client (not runelite) or doesn't have
		//the plugin. In both cases, when the offer was made no history for the slot was recorded, so when
		//they switch to runelite/get the plugin, there will be no last offer for the slot.
		if (lastOfferForSlot == null) {
			loggedInAccsLastOffers.put(clonedNewOffer.getSlot(), clonedNewOffer);
			return false;
		}

		//if its a duplicate as the last seen event
		if (lastOfferForSlot.equals(clonedNewOffer))
		{
			return true;
		}

		int tickDiffFromLastOffer = clonedNewOffer.getTickArrivedAt() - lastOfferForSlot.getTickArrivedAt();
		clonedNewOffer.setTicksSinceFirstOffer(tickDiffFromLastOffer + lastOfferForSlot.getTicksSinceFirstOffer());
		loggedInAccsLastOffers.put(clonedNewOffer.getSlot(), clonedNewOffer);
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

		boolean isBuy = offer.getState() == GrandExchangeOfferState.BOUGHT
			|| offer.getState() == GrandExchangeOfferState.CANCELLED_BUY
			|| offer.getState() == GrandExchangeOfferState.BUYING;

		return new OfferInfo(
			isBuy,
			offer.getItemId(),
			offer.getQuantitySold(),
			offer.getQuantitySold() == 0 ? 0 : offer.getSpent() / offer.getQuantitySold(),
			Instant.now().truncatedTo(ChronoUnit.SECONDS),
			newOfferEvent.getSlot(),
			offer.getState(),
			client.getTickCount(),
			0,
			offer.getTotalQuantity(),
			0,
			true,
			true);
	}

	/**
	 * Finds an item in the given trades list that matches the given criteria.
	 *
	 * @param trades   the trades list to search through
	 * @param criteria a function that needs to return true for the item to be returned
	 * @return the item that matches the criteria
	 */
	private Optional<FlippingItem> findItemInTradesList(List<FlippingItem> trades, Predicate<FlippingItem> criteria)
	{
		return trades.stream().filter(criteria).findFirst();
	}

	/**
	 * This method updates the given trade list in response to an OfferInfo based on whether an item
	 * that matches what the offer was for already exists and whether the offer was a margin check.
	 * <p>
	 * If the offer was a margin check, and the item is present, that item's history and margin need
	 * to be updated. If the item isn't present, a FlippingItem for the item in that offer and added to the trades
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
	private void updateTradesList(List<FlippingItem> trades, Optional<FlippingItem> flippingItem, OfferInfo newOffer)
	{
		if (newOffer.isMarginCheck())
		{
			if (flippingItem.isPresent())
			{
				FlippingItem item = flippingItem.get();
				item.updateMargin(newOffer);
				item.update(newOffer);

				trades.remove(item);
				trades.add(0, item);
			}
			else
			{
				addToTradesList(trades, newOffer);
			}
		}

		//if the item exists in the trades list but its not a margin check, you only need to update its history and
		//last traded times, not its margin.
		else if (flippingItem.isPresent())
		{
			flippingItem.get().update(newOffer);
		}
	}

	/**
	 * Given a new offer, this method creates a FlippingItem, the data structure that represents an item
	 * you are currently flipping, and adds it to the loggedInUserTrades. The loggedInUserTrades is a crucial part of the state
	 * of the flippingPlugin, as only items from the loggedInUserTrades are rendered and updated.
	 *
	 * @param newOffer new offer just received
	 */
	private void addToTradesList(List<FlippingItem> tradesList, OfferInfo newOffer)
	{
		int tradeItemId = newOffer.getItemId();
		String itemName = itemManager.getItemComposition(tradeItemId).getName();

		ItemStats itemStats = itemManager.getItemStats(tradeItemId, false);
		int geLimit = itemStats != null ? itemStats.getGeLimit() : 0;

		FlippingItem flippingItem = new FlippingItem(tradeItemId, itemName, geLimit, currentlyLoggedInAccount);

		flippingItem.updateMargin(newOffer);
		flippingItem.update(newOffer);

		tradesList.add(0, flippingItem);

	}

	/**
	 * gets the trade list the user is currently looking at
	 *
	 * @return the trades.
	 */
	public List<FlippingItem> getTradesForCurrentView()
	{
		return allAccountsData.get(accountCurrentlyViewed).getTrades();
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

	public void storeTrades(Map<String, AccountData> trades)
	{
		try
		{
			tradePersister.storeTrades(trades);
			log.info("successfully stored trades");
		}
		catch (IOException e)
		{
			log.info("couldn't store trades, error = " + e);
		}
	}

	public Map<String, AccountData> loadTrades()
	{
		try
		{
			Map<String, AccountData> trades = tradePersister.loadTrades();
			log.info("successfully loaded trades");
			return trades;
		}
		catch (IOException e)
		{
			log.info("couldn't load trades, error = " + e);
			return new HashMap<>();
		}
	}

	public void truncateTradeList()
	{
		getTradesForCurrentView().removeIf((item) ->
		{
			if (item.getGeLimitResetTime() != null)
			{
				Instant startOfRefresh = item.getGeLimitResetTime().minus(4, ChronoUnit.HOURS);

				return !item.hasValidOffers(HistoryManager.PanelSelection.FLIPPING) && !item.hasValidOffers(HistoryManager.PanelSelection.STATS)
					&& (!Instant.now().isAfter(item.getGeLimitResetTime()) || item.getGeLimitResetTime().isBefore(startOfRefresh));
			}
			return !item.hasValidOffers(HistoryManager.PanelSelection.FLIPPING) && !item.hasValidOffers(HistoryManager.PanelSelection.STATS);
		});
	}

	/**
	 * This method is invoked every time a user selects a username from the dropdown at the top of the
	 * panel. If the username selected does not exist in the cache, it uses loadTradeHistory to load it from
	 * disk and set the cache. Otherwise, it just reads what in the cache for that username. It updates the displays
	 * with the trades it either found in the cache or from disk.
	 *
	 * @param selectedUsername the username the user selected from the dropdown menu.
	 */
	public void changeView(String selectedUsername)
	{
		log.info("changing view to {}", selectedUsername);
		List<FlippingItem> tradesListToDisplay = allAccountsData.get(selectedUsername).getTrades();
		accountCurrentlyViewed = selectedUsername;
		statPanel.rebuild(tradesListToDisplay);
		flippingPanel.rebuild(tradesListToDisplay);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		//Ensure that user configs are updated after being changed
		if (event.getGroup().equals(CONFIG_GROUP))
		{
			if (event.getKey().equals(ITEMS_CONFIG_KEY) || event.getKey().equals(TIME_INTERVAL_CONFIG_KEY))
			{
				return;
			}

			if (event.getKey().equals("multiAccTracking"))
			{
				if (config.multiAccTracking())
				{
					tabManager.getViewSelector().setVisible(true);
				}
				else
				{
					tabManager.getViewSelector().setSelectedItem(ACCOUNT_WIDE);
					tabManager.getViewSelector().setVisible(false);
				}
				return;
			}

			statPanel.rebuild(getTradesForCurrentView());
			flippingPanel.rebuild(getTradesForCurrentView());
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
			for (FlippingItem item : getTradesForCurrentView())
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
					flippingWidget.showWidget("setCurrentQuantityInTrade", itemGELimit);
				}
				else
				{
					flippingWidget.showWidget("setCurrentQuantityInTrade", selectedItem.remainingGeLimit());
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