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
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
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
	private ScheduledFuture repeatingTasks;
	@Inject
	private ClientToolbar clientToolbar;
	private NavigationButton navButton;

	@Inject
	private ConfigManager configManager;

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

	//this flag is to know that when we see the login screen an account has actually logged out and its not just that the
	//client has started.
	private boolean previouslyLoggedIn;

	//hold all account data associated with an account. This account data includes the account's trade history and
	//last offers for every slot (this is to help deduplicate incoming offers)
	@Getter
	private Map<String, AccountData> accountCache = new HashMap<>();

	//the display name of the account whose trade list the user is currently looking at as selected
	//through the dropdown menu
	private String accountCurrentlyViewed = ACCOUNT_WIDE;

	//the display name of the currently logged in user. This is the only account that can actually receive offers
	//as this is the only account currently logged in.
	private String currentlyLoggedInAccount;

	//some events come before a display name has been retrieved and since a display name is crucial for figuring out
	//which account's trade list to add to, we queue the events here to be processed as soon as a display name is set.
	private List<GrandExchangeOfferChanged> eventsBeforeNameSet = new ArrayList<>();

	//building the account wide trade list is an expensive operation so we store it in this variable and only recompute
	//it if we have gotten an update since the last account wide trade list build.
	boolean updateSinceLastAccountWideBuild = true;
	List<FlippingItem> prevBuiltAccountWideList;

	//updates the cache by monitoring the directory and loading a file's contents into the cache if it has been changed
	private CacheUpdater cacheUpdater;

	//the amount of time a user has been flipping for since the client started up or since they last reset the session
	//time.
	@Setter
	@Getter
	private Duration accumulatedSessionTime = Duration.ZERO;

	//used to figure out how to accumulate time as the executor may not run the updateSessionTime method every second on
	//the dot (there might be some very small delay).
	private Instant lastSessionTimeUpdate;


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

			accountCache = setupCache();
			setupAccSelectorDropdown();

			//sets which time interval for the stats tab will be displayed on startup
			String lastSelectedInterval = configManager.getConfiguration(CONFIG_GROUP, TIME_INTERVAL_CONFIG_KEY);
			statPanel.setSelectedTimeInterval(lastSelectedInterval);

			cacheUpdater = new CacheUpdater();
			cacheUpdater.registerCallback(this::onDirectoryUpdate);
			cacheUpdater.start();

			repeatingTasks = setupRepeatingTasks();

			//stops scheduling this task
			return true;
		});
	}

	@Subscribe(priority = 101)
	public void onClientShutdown(ClientShutdown clientShutdownEvent)
	{
		configManager.setConfiguration(CONFIG_GROUP, TIME_INTERVAL_CONFIG_KEY, statPanel.getSelectedTimeInterval());

		repeatingTasks.cancel(true);

		cacheUpdater.stop();

		if (currentlyLoggedInAccount != null)
		{
			log.info("Shutting down, saving trades!");
			storeTrades(currentlyLoggedInAccount);
		}
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
				previouslyLoggedIn = true;
				handleLogin(name);
				//stops scheduling this task
				return true;
			});
		}

		else if (event.getGameState() == GameState.LOGIN_SCREEN && previouslyLoggedIn)
		{
			//this randomly fired at night hours after i had logged off...so i'm adding this guard here.
			if (currentlyLoggedInAccount != null && client.getGameState() != GameState.LOGGED_IN)
			{
				handleLogout();
			}
		}
	}

	public void handleLogin(String displayName)
	{
		log.info("{} has just logged in!", displayName);
		if (!accountCache.containsKey(displayName))
		{
			log.info("cache does not contain data for {}", displayName);
			accountCache.put(displayName, new AccountData());
			tabManager.getViewSelector().addItem(displayName);
		}

		currentlyLoggedInAccount = displayName;

		//now that we have a display name we can process any events that we received before the display name
		//was set.
		eventsBeforeNameSet.forEach(this::onGrandExchangeOfferChanged);
		eventsBeforeNameSet.clear();

		if (accountCache.keySet().size() > 1)
		{
			tabManager.getViewSelector().setVisible(true);
		}
		accountCurrentlyViewed = displayName;
		//this will cause changeView to be invoked which will cause a rebuild of
		//flipping and stats panel
		tabManager.getViewSelector().setSelectedItem(displayName);

	}

	public void handleLogout()
	{
		log.info("{} is logging out, storing trades for {}", currentlyLoggedInAccount, currentlyLoggedInAccount);
		storeTrades(currentlyLoggedInAccount);
		currentlyLoggedInAccount = null;
	}

	private Map<String, AccountData> setupCache()
	{
		try
		{
			log.info("initiating load on startup");
			TradePersister.setup();
			return loadAllTrades();
		}

		catch (IOException e)
		{
			log.info("error while loading history, setting accountCache to a blank hashmap for now, e = {}", e);
			return new HashMap<>();
		}
	}

	/**
	 * sets up the account selector dropdown that lets you change which account's trade list you
	 * are looking at.
	 */
	private void setupAccSelectorDropdown()
	{
		//adding an item causes the event listener (changeView) to fire which causes stat panel
		//and flipping panel to rebuild. I think this only happens on the first item you add.
		tabManager.getViewSelector().addItem(ACCOUNT_WIDE);

		accountCache.keySet().forEach(displayName -> tabManager.getViewSelector().addItem(displayName));

		//sets the account selector dropdown to visible or not depending on whether the config option has been
		//selected and there are > 1 accounts.
		if (accountCache.keySet().size() > 1)
		{
			tabManager.getViewSelector().setVisible(true);
		}
		else
		{
			tabManager.getViewSelector().setVisible(false);
		}
	}

	/**
	 * Currently used for updating time sensitive displays such as the accumulate session time,
	 * how long ago an item was flipped, etc.
	 *
	 * @return a future object that can be used to cancel the tasks
	 */
	public ScheduledFuture setupRepeatingTasks()
	{
		return executor.scheduleAtFixedRate(() ->
		{
			try
			{
				flippingPanel.updateActivePanelsPriceOutdatedDisplay();
				flippingPanel.updateActivePanelsGePropertiesDisplay();
				statPanel.updateTimeDisplay();
				updateSessionTime();
			}
			catch (Exception e)
			{
				log.info("unknown exception in repeating tasks, error = {}", e);
			}

		}, 100, 1000, TimeUnit.MILLISECONDS);
	}

	@Override
	protected void shutDown()
	{
		if (repeatingTasks != null)
		{
			//Stop all timers
			repeatingTasks.cancel(true);
			repeatingTasks = null;
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
		if (currentlyLoggedInAccount == null)
		{
			eventsBeforeNameSet.add(newOfferEvent);
			return;
		}

		OfferInfo newOffer = createOffer(newOfferEvent);

		if (isBadOffer(newOffer))
		{
			return;
		}

		List<FlippingItem> currentlyLoggedInAccountsTrades = accountCache.get(currentlyLoggedInAccount).getTrades();

		Optional<FlippingItem> flippingItem = currentlyLoggedInAccountsTrades.stream().
			filter(item -> item.getItemId() == newOffer.getItemId())
			.findFirst();

		updateTradesList(currentlyLoggedInAccountsTrades, flippingItem, newOffer.clone());

		updateSinceLastAccountWideBuild = true;

		//Only rebuild flipping panel if flipping item is not present as in that case a new panel is added or its present
		//and the offer is a margin check as that updates the buy/sell price on the item's panel.
		//There is no point rebuilding the panel when the user is looking at the trades list of
		//another one of their accounts that isn't logged in as that trades list won't be being updated.
		if ((!flippingItem.isPresent() || flippingItem.isPresent() && newOffer.isMarginCheck()) &&
			(accountCurrentlyViewed.equals(currentlyLoggedInAccount) || accountCurrentlyViewed.equals(ACCOUNT_WIDE)))
		{
			flippingPanel.rebuild(getTradesForCurrentView());
		}

		if (accountCurrentlyViewed.equals(currentlyLoggedInAccount) || accountCurrentlyViewed.equals(ACCOUNT_WIDE))
		{
			statPanel.rebuild(getTradesForCurrentView());
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

		Map<Integer, OfferInfo> loggedInAccsLastOffers = accountCache.get(currentlyLoggedInAccount).getLastOffers();

		//this is always the start of any offer (when you first put in an offer), we use these offers to record when an
		//offer was placed. Then, when an offer completes we can see how many ticks it took, thus determining whether it
		//was a margin check or not.
		if (clonedNewOffer.getCurrentQuantityInTrade() == 0)
		{
			//we need to delete the history for the slot in this case so when the user puts in another offer after
			//cancelling, it doesn't ignore the newly generated "quantity of 0" event as a duplicate like we get on login.
			if (clonedNewOffer.getState() == GrandExchangeOfferState.CANCELLED_BUY || clonedNewOffer.getState() == GrandExchangeOfferState.CANCELLED_SELL)
			{
				loggedInAccsLastOffers.remove(clonedNewOffer.getSlot());
				return true;
			}

			if (loggedInAccsLastOffers.containsKey(clonedNewOffer.getSlot()))
			{
				//on login we get "these quantity of 0" offers again amd we don't want to overwrite it with the duplicate
				//one on login as it would have a later tick count and can lead to erroneously marking offers as margin checks.
				if (loggedInAccsLastOffers.get(clonedNewOffer.getSlot()).getCurrentQuantityInTrade() == 0)
				{
					return true;
				}
			}
			loggedInAccsLastOffers.put(clonedNewOffer.getSlot(), clonedNewOffer); //tickSinceFirstOffer is 0 here
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
		if (lastOfferForSlot == null)
		{
			loggedInAccsLastOffers.put(clonedNewOffer.getSlot(), clonedNewOffer);
			return false;
		}

		//if its a duplicate as the last seen event
		if (lastOfferForSlot.equals(clonedNewOffer))
		{
			return true;
		}

		int tickDiffFromLastOffer = Math.abs(clonedNewOffer.getTickArrivedAt() - lastOfferForSlot.getTickArrivedAt());
		clonedNewOffer.setTicksSinceFirstOffer(tickDiffFromLastOffer + lastOfferForSlot.getTicksSinceFirstOffer());
		loggedInAccsLastOffers.put(clonedNewOffer.getSlot(), clonedNewOffer);
		newOffer.setTicksSinceFirstOffer(tickDiffFromLastOffer + lastOfferForSlot.getTicksSinceFirstOffer());
		return false; //not a bad event
	}

	/**
	 * Creates an OfferInfo object out of a GrandExchangeOfferChanged event and adds additional attributes such as
	 * tickArrivedAt to help identify margin check offers.
	 *
	 * @param newOfferEvent event that we subscribe to.
	 * @return an OfferInfo object with the relevant information from the event.
	 */
	private OfferInfo createOffer(GrandExchangeOfferChanged newOfferEvent)
	{
		OfferInfo offer = OfferInfo.fromGrandExchangeEvent(newOfferEvent);
		offer.setTickArrivedAt(client.getTickCount());
		offer.setMadeBy(currentlyLoggedInAccount);
		return offer;
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
		if (flippingItem.isPresent())
		{
			FlippingItem item = flippingItem.get();
			if (newOffer.isMarginCheck())
			{
				item.updateMargin(newOffer);
			}
			item.updateHistory(newOffer);
			item.updateLatestTimes(newOffer);
		}
		else
		{
			addToTradesList(trades, newOffer);
		}
	}

	/**
	 * Constructs a FlippingItem, the data structure that represents an item the user is currently flipping, and
	 * adds it to the given tradelist. This method is invoked when we receive a margin check offer for an item that
	 * isn't currently present in the given trades list.
	 *
	 * @param tradesList the trades list to be updated
	 * @param newOffer   the offer to update the trade list with
	 */
	private void addToTradesList(List<FlippingItem> tradesList, OfferInfo newOffer)
	{
		int tradeItemId = newOffer.getItemId();
		String itemName = itemManager.getItemComposition(tradeItemId).getName();

		ItemStats itemStats = itemManager.getItemStats(tradeItemId, false);
		int geLimit = itemStats != null ? itemStats.getGeLimit() : 0;

		FlippingItem flippingItem = new FlippingItem(tradeItemId, itemName, geLimit, currentlyLoggedInAccount);

		if (newOffer.isMarginCheck())
		{
			flippingItem.updateMargin(newOffer);
		}
		flippingItem.updateHistory(newOffer);
		flippingItem.updateLatestTimes(newOffer);

		tradesList.add(0, flippingItem);
	}

	/**
	 * gets the trade list the user is currently looking at
	 *
	 * @return the trades.
	 */
	public List<FlippingItem> getTradesForCurrentView()
	{
		return accountCurrentlyViewed.equals(ACCOUNT_WIDE) ? createAccountWideList() : accountCache.get(accountCurrentlyViewed).getTrades();
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

	public void storeTrades(String displayName)
	{
		try
		{
			TradePersister.storeTrades(displayName, accountCache.get(displayName));
			log.info("successfully stored trades");
		}
		catch (IOException e)
		{
			log.info("couldn't store trades, error = " + e);
		}
	}

	public Map<String, AccountData> loadAllTrades()
	{
		try
		{
			Map<String, AccountData> trades = TradePersister.loadAllTrades();
			log.info("successfully loaded trades");
			return trades;
		}
		catch (IOException e)
		{
			log.info("couldn't load trades, error: " + e);
			return new HashMap<>();
		}
	}

	public AccountData loadTrades(String displayName)
	{
		try
		{
			return TradePersister.loadTrades(displayName);
		}
		catch (IOException e)
		{
			log.info("couldn't load trades for {}, e = " + e, displayName);
			return new AccountData();
		}
	}

	public void truncateTradeList()
	{
		List<FlippingItem> currItems = getTradesForCurrentView();
		currItems.removeIf((item) ->
		{
			if (item.getGeLimitResetTime() != null)
			{
				Instant startOfRefresh = item.getGeLimitResetTime().minus(4, ChronoUnit.HOURS);

				return !item.hasValidOffers(HistoryManager.PanelSelection.FLIPPING) && !item.hasValidOffers(HistoryManager.PanelSelection.STATS)
					&& (!Instant.now().isAfter(item.getGeLimitResetTime()) || item.getGeLimitResetTime().isBefore(startOfRefresh));
			}
			return !item.hasValidOffers(HistoryManager.PanelSelection.FLIPPING) && !item.hasValidOffers(HistoryManager.PanelSelection.STATS);
		});

		if (!accountCurrentlyViewed.equals(ACCOUNT_WIDE))
		{
			accountCache.get(accountCurrentlyViewed).setTrades(currItems);

		}
	}

	/**
	 * This method is invoked every time a user selects a username from the dropdown at the top of the
	 * panel. If the username selected does not exist in the cache, it uses loadTradeHistory to load it from
	 * disk and set the cache. Otherwise, it just reads what in the cache for that username. It updates the displays
	 * with the trades it either found in the cache or from disk.
	 *
	 * @param selectedName the username the user selected from the dropdown menu.
	 */
	public void changeView(String selectedName)
	{
		log.info("changing view to {}", selectedName);

		List<FlippingItem> tradesListToDisplay;
		if (selectedName.equals(ACCOUNT_WIDE))
		{
			flippingPanel.getResetIcon().setVisible(false);
			statPanel.getResetIcon().setVisible(false);
			tradesListToDisplay = createAccountWideList();
		}
		else
		{
			flippingPanel.getResetIcon().setVisible(true);
			statPanel.getResetIcon().setVisible(true);
			tradesListToDisplay = accountCache.get(selectedName).getTrades();
		}

		accountCurrentlyViewed = selectedName;
		statPanel.rebuild(tradesListToDisplay);
		flippingPanel.rebuild(tradesListToDisplay);
	}

	/**
	 * This is a callback executed by the cacheUpdater when it notices the directory has changed. If the
	 * file changed belonged to a different acc than the currently logged in one, it updates the cache of that
	 * account to ensure this client has the most up to date data on each account. If the user is currently looking
	 * at the account that had its cache updated, a rebuild takes place to display the most recent trade list.
	 *
	 * @param fileName name of the file which was modified.
	 */
	public void onDirectoryUpdate(String fileName)
	{

		String displayNameOfChangedAcc = fileName.split("\\.")[0];

		if (displayNameOfChangedAcc.equals(currentlyLoggedInAccount))
		{
			log.info("not reloading on directory update as this client caused the directory update");
			return;
		}

		accountCache.put(displayNameOfChangedAcc, loadTrades(displayNameOfChangedAcc));
		if (!tabManager.getViewSelectorItems().contains(displayNameOfChangedAcc))
		{
			tabManager.getViewSelector().addItem(displayNameOfChangedAcc);
		}

		if (accountCache.keySet().size() > 1)
		{
			tabManager.getViewSelector().setVisible(true);
		}

		updateSinceLastAccountWideBuild = true;

		//rebuild if you are currently looking at the account who's cache just got updated or the account wide view.
		if (accountCurrentlyViewed.equals(ACCOUNT_WIDE) || accountCurrentlyViewed.equals(displayNameOfChangedAcc))
		{
			List<FlippingItem> updatedList = getTradesForCurrentView();
			flippingPanel.rebuild(updatedList);
			statPanel.rebuild(updatedList);
		}
	}

	/**
	 * creates a view of an "account wide tradelist". An account wide tradelist is just a reflection of the flipping
	 * items currently in each of the account's tradelists. It does this by merging the flipping items of the same type
	 * from each account's trade list into one flipping item.
	 *
	 * @return
	 */
	private List<FlippingItem> createAccountWideList()
	{
		//since this is an expensive operation, cache its results and only recompute it if there has been an update
		//to one of the account's tradelists, (updateSinceLastAccountWideBuild is set in onGrandExchangeOfferChanged)
		if (!updateSinceLastAccountWideBuild)
		{
			return prevBuiltAccountWideList;
		}

		if (accountCache.values().size() == 0)
		{
			return new ArrayList<>();
		}

		//take all flipping items from the account cache, regardless of account, and segregate them based on item name.
		Map<String, List<FlippingItem>> groupedItems = accountCache.values().stream().
			flatMap(accountData -> accountData.getTrades().stream()).
			map(FlippingItem::clone).
			collect(Collectors.groupingBy(FlippingItem::getItemName));

		//take every list containing flipping items of the same type and reduce it to one merged flipping item and put that
		//item in a final merged list
		List<FlippingItem> mergedItems = groupedItems.values().stream().
			map(list -> list.stream().reduce(FlippingItem::merge)).filter(Optional::isPresent).map(Optional::get).
			collect(Collectors.toList());

		mergedItems.sort(Collections.reverseOrder(Comparator.comparing(FlippingItem::getLatestActivityTime)));

		updateSinceLastAccountWideBuild = false;
		prevBuiltAccountWideList = mergedItems;
		return mergedItems;

	}

	/**
	 * Decides whether the user is currently flipping or not. To be flipping a user has to be logged in
	 * and have at least one incomplete offer in the GE
	 *
	 * @return whether the user if currently flipping or not
	 */
	private boolean currentlyFlipping()
	{
		if (currentlyLoggedInAccount == null)
		{
			return false;
		}

		Collection<OfferInfo> lastOffers = accountCache.get(currentlyLoggedInAccount).getLastOffers().values();
		return lastOffers.stream().anyMatch(offerInfo -> !offerInfo.isComplete());
	}

	/**
	 * Calculates and updates the session time display in the statistics tab when a user is viewing
	 * the "Session" time interval.
	 */
	private void updateSessionTime()
	{
		if (currentlyFlipping())
		{
			if (lastSessionTimeUpdate == null)
			{
				lastSessionTimeUpdate = Instant.now();
			}
			long millisSinceLastSessionTimeUpdate = Instant.now().toEpochMilli() - lastSessionTimeUpdate.toEpochMilli();
			accumulatedSessionTime = accumulatedSessionTime.plus(millisSinceLastSessionTimeUpdate, ChronoUnit.MILLIS);
			lastSessionTimeUpdate = Instant.now();
			statPanel.updateSessionTimeDisplay(accumulatedSessionTime);
		}

		else
		{
			lastSessionTimeUpdate = null;
		}
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
			|| client.getVarcIntValue(VarClientInt.INPUT_TYPE.getIndex()) != 7
			|| client.getWidget(WidgetInfo.GRAND_EXCHANGE_OFFER_CONTAINER) == null)
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