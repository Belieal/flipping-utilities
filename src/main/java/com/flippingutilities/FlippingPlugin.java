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

import com.flippingutilities.db.TradePersister;
import com.flippingutilities.model.AccountData;
import com.flippingutilities.model.FlippingItem;
import com.flippingutilities.model.OfferEvent;
import com.flippingutilities.ui.MasterPanel;
import com.flippingutilities.ui.settings.SettingsPanel;
import com.flippingutilities.ui.gehistorytab.GeHistoryTabPanel;
import com.flippingutilities.ui.flipping.FlippingPanel;
import com.flippingutilities.ui.statistics.StatsPanel;
import com.flippingutilities.ui.widgets.OfferEditor;
import com.flippingutilities.ui.widgets.TradeActivityTimer;
import com.flippingutilities.utilities.CacheUpdater;
import com.flippingutilities.utilities.GeHistoryTabExtractor;
import com.google.common.primitives.Shorts;
import com.google.inject.Provides;
import java.awt.Font;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;

import static net.runelite.api.VarPlayer.CURRENT_GE_ITEM;

import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
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
import net.runelite.client.ui.FontManager;
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
	private ScheduledFuture generalRepeatingTasks;
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

	private OfferEditor flippingWidget;
	private SettingsPanel settingsPanel;
	private MasterPanel masterPanel;

	//this flag is to know that when we see the login screen an account has actually logged out and its not just that the
	//client has started.
	private boolean previouslyLoggedIn;

	//hold all account data associated with an account. This account data includes the account's trade history and
	//last offers for every slot (this is to help deduplicate incoming offers)
	@Getter
	@Setter
	private Map<String, AccountData> accountCache = new HashMap<>();

	//the display name of the account whose trade list the user is currently looking at as selected
	//through the dropdown menu
	@Getter
	private String accountCurrentlyViewed = ACCOUNT_WIDE;

	//the display name of the currently logged in user. This is the only account that can actually receive offers
	//as this is the only account currently logged in.
	@Getter
	@Setter
	private String currentlyLoggedInAccount;

	//some events come before a display name has been retrieved and since a display name is crucial for figuring out
	//which account's trade list to add to, we queue the events here to be processed as soon as a display name is set.
	private List<OfferEvent> eventsReceivedBeforeFullLogin = new ArrayList<>();

	//building the account wide trade list is an expensive operation so we store it in this variable and only recompute
	//it if we have gotten an update since the last account wide trade list build.
	boolean updateSinceLastAccountWideBuild = true;
	List<FlippingItem> prevBuiltAccountWideList;

	//updates the cache by monitoring the directory and loading a file's contents into the cache if it has been changed
	private CacheUpdater cacheUpdater;

	private ScheduledFuture slotTimersTask;
	private Instant startUpTime = Instant.now();

	//name of the account this client last stored trades for.
	private String thisClientLastStored;

	private int loginTickCount;

	private GeHistoryTabPanel geHistoryTabPanel;

	@Override
	protected void startUp()
	{
		flippingPanel = new FlippingPanel(this, itemManager, executor);
		statPanel = new StatsPanel(this, itemManager);
		settingsPanel = new SettingsPanel(this);
		geHistoryTabPanel = new GeHistoryTabPanel(this);
		masterPanel = new MasterPanel(this, flippingPanel, statPanel, settingsPanel);
		masterPanel.addView(geHistoryTabPanel, "ge history");
		navButton = NavigationButton.builder()
			.tooltip("Flipping Utilities")
			.icon(ImageUtil.getResourceStreamFromClass(getClass(), "/graph_icon_green.png"))
			.priority(3)
			.panel(masterPanel)
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

			statPanel.setSelectedTimeInterval("Session");

			cacheUpdater = new CacheUpdater();
			cacheUpdater.registerCallback(this::onDirectoryUpdate);
			cacheUpdater.start();

			generalRepeatingTasks = setupRepeatingTasks(1000);

			//this is only relevant if the user downloads/enables the plugin after they login.
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				log.info("user is already logged in when they downloaded/enabled the plugin");
				onLoggedInGameState();
			}

			//stops scheduling this task
			return true;
		});
	}

	@Override
	protected void shutDown()
	{
		if (generalRepeatingTasks != null)
		{
			//Stop all slotTimers
			generalRepeatingTasks.cancel(true);
			slotTimersTask.cancel(true);
			generalRepeatingTasks = null;
		}

		clientToolbar.removeNavigation(navButton);
	}

	@Subscribe(priority = 101)
	public void onClientShutdown(ClientShutdown clientShutdownEvent)
	{
		generalRepeatingTasks.cancel(true);

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
			onLoggedInGameState();
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

	private void onLoggedInGameState()
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

			if (currentlyLoggedInAccount == null)
			{
				handleLogin(name);
			}
			//stops scheduling this task
			return true;
		});
	}

	public void handleLogin(String displayName)
	{
		if (client.getAccountType().isIronman())
		{
			log.info("account is an ironman, not adding it to the cache");
			return;
		}

		log.info("{} has just logged in!", displayName);
		if (!accountCache.containsKey(displayName))
		{
			log.info("cache does not contain data for {}", displayName);
			AccountData accountData = new AccountData();
			accountData.setSlotTimers(setupSlotTimers());
			accountCache.put(displayName, accountData);
			masterPanel.getAccountSelector().addItem(displayName);
		}

		loginTickCount = client.getTickCount();
		currentlyLoggedInAccount = displayName;

		//now that we have a display name we can process any events that we received before the display name
		//was set.
		eventsReceivedBeforeFullLogin.forEach(this::onNewOfferEvent);
		eventsReceivedBeforeFullLogin.clear();

		if (accountCache.keySet().size() > 1)
		{
			masterPanel.getAccountSelector().setVisible(true);
		}
		accountCurrentlyViewed = displayName;
		//this will cause changeView to be invoked which will cause a rebuild of
		//flipping and stats panel
		masterPanel.getAccountSelector().setSelectedItem(displayName);
		if (slotTimersTask == null && config.slotTimersEnabled())
		{
			log.info("starting slot timers on login");
			slotTimersTask = executor.scheduleAtFixedRate(() -> accountCache.get(currentlyLoggedInAccount).getSlotTimers().forEach(timer ->
				clientThread.invokeLater(() -> {
					try {
						timer.updateTimer();
					}
					catch (Exception e) {
						log.info("exception when trying to update timer. e: {}", e);
					}
				})), 1000, 1000, TimeUnit.MILLISECONDS);
		}
	}

	public void handleLogout()
	{
		log.info("{} is logging out", currentlyLoggedInAccount);
		accountCache.get(currentlyLoggedInAccount).setLastSessionTimeUpdate(null);
		storeTrades(currentlyLoggedInAccount);
		if (slotTimersTask != null && !slotTimersTask.isCancelled())
		{
			log.info("cancelling slot timers task on logout");
			slotTimersTask.cancel(true);
		}
		slotTimersTask = null;
		currentlyLoggedInAccount = null;
		masterPanel.revertToSafeDisplay();
	}

	/**
	 * Sets up the account cache when the plugin is starting up. The account cache is a mapping of display name to
	 * AccountData object and is the central source for account related information that needs to be queried, such
	 * as an account's trade list, accumulated session time, etc.
	 *
	 * @return
	 */
	private Map<String, AccountData> setupCache()
	{
		try
		{
			log.info("initiating load on startup");
			TradePersister.setup();
			Map<String, AccountData> accountsData = loadAllTrades();
			accountsData.values().forEach(accountData -> {
				accountData.startNewSession();
				accountData.prepareForUse(itemManager);
				if (accountData.getSlotTimers() == null)
				{
					accountData.setSlotTimers(setupSlotTimers());
				}
				else
				{
					accountData.getSlotTimers().forEach(timer -> {
						timer.setClient(client);
						timer.setPlugin(this);
					});
				}
			});
			return accountsData;
		}

		catch (IOException e)
		{
			log.info("error while loading history, setting accountCache to a blank hashmap for now, e = {}", e);
			return new HashMap<>();
		}
	}

	public List<TradeActivityTimer> setupSlotTimers()
	{
		ArrayList<TradeActivityTimer> slotTimers = new ArrayList<>();
		for (int slotIndex = 0; slotIndex < 8; slotIndex++)
		{
			slotTimers.add(new TradeActivityTimer(this, client, slotIndex));
		}
		return slotTimers;
	}

	/**
	 * sets up the account selector dropdown that lets you change which account's trade list you
	 * are looking at.
	 */
	private void setupAccSelectorDropdown()
	{
		//adding an item causes the event listener (changeView) to fire which causes stat panel
		//and flipping panel to rebuild. I think this only happens on the first item you add.
		masterPanel.getAccountSelector().addItem(ACCOUNT_WIDE);

		accountCache.keySet().forEach(displayName -> masterPanel.getAccountSelector().addItem(displayName));

		//sets the account selector dropdown to visible or not depending on whether the config option has been
		//selected and there are > 1 accounts.
		if (accountCache.keySet().size() > 1)
		{
			masterPanel.getAccountSelector().setVisible(true);
		}
		else
		{
			masterPanel.getAccountSelector().setVisible(false);
		}
	}

	/**
	 * Currently used for updating time sensitive displays such as the accumulated session time,
	 * how long ago an item was flipped, etc.
	 *
	 * @return a future object that can be used to cancel the tasks
	 */
	public ScheduledFuture setupRepeatingTasks(int msStartDelay)
	{
		return executor.scheduleAtFixedRate(() ->
		{
			try
			{
				flippingPanel.updateTimerDisplays();
				statPanel.updateTimeDisplay();
				updateSessionTime();
			}
			catch (ConcurrentModificationException e)
			{
				log.info("concurrent modification exception. This is fine, will just restart tasks after delay." +
					" Cancelling general repeating tasks and starting it again after 5000 ms delay");
				generalRepeatingTasks.cancel(true);
				generalRepeatingTasks = setupRepeatingTasks(5000);
			}
			catch (Exception e)
			{
				log.info("unknown exception in repeating tasks, error = {}, will cancel and restart them after 5 sec delay", e);
				generalRepeatingTasks.cancel(true);
				generalRepeatingTasks = setupRepeatingTasks(5000);
			}

		}, msStartDelay, 1000, TimeUnit.MILLISECONDS);
	}

	/**
	 * This method is invoked every time the plugin receives a GrandExchangeOfferChanged event which is
	 * when the user set an offer, cancelled an offer, or when an offer was updated (items bought/sold partially
	 * or completely).
	 *
	 * @param offerChangedEvent the offer event that represents when an offer is updated
	 *                          (buying, selling, bought, sold, cancelled sell, or cancelled buy)
	 */
	@Subscribe
	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged offerChangedEvent)
	{
		if (currentlyLoggedInAccount == null)
		{
			OfferEvent newOfferEvent = createOfferEvent(offerChangedEvent);
			//event came in before account was fully logged in. This means that the offer actually came through
			//sometime when the account was logged out, at an undetermined time. We need to mark the offer as such to
			//avoid adjusting ge limits and slot timers incorrectly (cause we don't know exactly when the offer came in)
			newOfferEvent.setBeforeLogin(true);
			eventsReceivedBeforeFullLogin.add(newOfferEvent);
			return;
		}
		OfferEvent newOfferEvent = createOfferEvent(offerChangedEvent);
		if (newOfferEvent.getTickArrivedAt() == loginTickCount)
		{
			newOfferEvent.setBeforeLogin(true);
		}
		onNewOfferEvent(newOfferEvent);
	}

	public void onNewOfferEvent(OfferEvent newOfferEvent)
	{
		if (currentlyLoggedInAccount != null)
		{
			newOfferEvent.setMadeBy(currentlyLoggedInAccount);
		}
		Optional<OfferEvent> screenedOfferEvent = screenOfferEvent(newOfferEvent);

		if (!screenedOfferEvent.isPresent())
		{
			return;
		}

		OfferEvent finalizedOfferEvent = screenedOfferEvent.get();

		List<FlippingItem> currentlyLoggedInAccountsTrades = accountCache.get(currentlyLoggedInAccount).getTrades();

		Optional<FlippingItem> flippingItem = currentlyLoggedInAccountsTrades.stream().filter(item -> item.getItemId() == finalizedOfferEvent.getItemId()).findFirst();

		updateTradesList(currentlyLoggedInAccountsTrades, flippingItem, finalizedOfferEvent.clone());

		updateSinceLastAccountWideBuild = true;

		rebuildDisplayAfterOfferEvent(flippingItem, finalizedOfferEvent);
	}

	/**
	 * There is no point rebuilding either the stats panel or flipping panel when the user is looking at the trades list of
	 * one of their accounts that isn't logged in as that trades list won't be being updated anyway.
	 * <p>
	 * Only rebuild flipping panel if the FlippingItem is not already present or if the offer is a margin check. We need to rebuild
	 * when the item isn't present as that means a new FlippingItemPanel had to be created to represent the
	 * new FlippingItem. We also need to rebuild if the offer is a margin check because a margin check offer causes
	 * a reordering of the FlippingItemPanels as the FlippingItemPanel representing the recently margin checked item
	 * floats to the top.
	 * <p>
	 * In the case when the FlippingItem is present and the offer is not a margin check, we don't have to do a full
	 * flipping panel rebuild as we only update the Jlabels that specify the latest buy/sell price. No new panels
	 * are created and nothing is reordered, hence a full rebuild would be wasteful.
	 *
	 * @param flippingItem represents whether the FlippingItem existed in the currently logged in account's tradeslist when
	 *                     the offer came in.
	 * @param offerEvent   offer event just received
	 */
	private void rebuildDisplayAfterOfferEvent(Optional<FlippingItem> flippingItem, OfferEvent offerEvent)
	{
		if (!(accountCurrentlyViewed.equals(currentlyLoggedInAccount) || accountCurrentlyViewed.equals(ACCOUNT_WIDE)))
		{
			return;
		}

		if (!flippingItem.isPresent() || flippingItem.isPresent() && offerEvent.isMarginCheck())
		{
			flippingPanel.rebuild(getTradesForCurrentView());
		}
		else if (flippingItem.isPresent() && !offerEvent.isMarginCheck())
		{
			flippingPanel.refreshPricesForFlippingItemPanel(flippingItem.get().getItemId());
		}

		statPanel.rebuild(getTradesForCurrentView());
	}

	/**
	 * Runelite has some wonky events. For example, every empty/buy/sell/cancelled buy/cancelled sell
	 * spawns two identical events. And when you fully buy/sell item, it spawns two events (a
	 * buying/selling event and a bought/sold event). This method screens out the unwanted events/duplicate
	 * events and sets the ticksSinceFirstOffer field correctly on new OfferEvents.
	 *
	 * @param newOfferEvent event that just occurred
	 * @return an optional containing an OfferEvent.
	 */
	public Optional<OfferEvent> screenOfferEvent(OfferEvent newOfferEvent)
	{
		Map<Integer, OfferEvent> lastOfferEventForEachSlot = accountCache.get(currentlyLoggedInAccount).getLastOffers();

		if (newOfferEvent.isCausedByEmptySlot())
		{
			return Optional.empty();
		}

		if (newOfferEvent.isStartOfOffer() && !isDuplicateStartOfOfferEvent(newOfferEvent))
		{
			accountCache.get(currentlyLoggedInAccount).getSlotTimers().get(newOfferEvent.getSlot()).setCurrentOffer(newOfferEvent);
			lastOfferEventForEachSlot.put(newOfferEvent.getSlot(), newOfferEvent); //tickSinceFirstOffer is 0 here
			return Optional.empty();
		}

		if (newOfferEvent.isStartOfOffer() && isDuplicateStartOfOfferEvent(newOfferEvent))
		{
			return Optional.empty();
		}

		if (newOfferEvent.getCurrentQuantityInTrade() == 0 && newOfferEvent.isComplete())
		{
			lastOfferEventForEachSlot.remove(newOfferEvent.getSlot());
			accountCache.get(currentlyLoggedInAccount).getSlotTimers().get(newOfferEvent.getSlot()).setCurrentOffer(newOfferEvent);
			return Optional.empty();
		}

		if (newOfferEvent.isRedundantEventBeforeOfferCompletion())
		{
			return Optional.empty();
		}

		OfferEvent lastOfferEvent = lastOfferEventForEachSlot.get(newOfferEvent.getSlot());

		//this occurs when the user made the trade while not having the plugin. As such, when the offer was made, no
		//history for the slot was recorded.
		if (lastOfferEvent == null)
		{
			lastOfferEventForEachSlot.put(newOfferEvent.getSlot(), newOfferEvent);
			return Optional.of(newOfferEvent);
		}

		if (lastOfferEvent.isDuplicate(newOfferEvent))
		{
			return Optional.empty();
		}

		newOfferEvent.setTicksSinceFirstOffer(lastOfferEvent);
		lastOfferEventForEachSlot.put(newOfferEvent.getSlot(), newOfferEvent);
		accountCache.get(currentlyLoggedInAccount).getSlotTimers().get(newOfferEvent.getSlot()).setCurrentOffer(newOfferEvent);
		return Optional.of(newOfferEvent);
	}

	/**
	 * We get offer events that mark the start of an offer on login, even though we already received them prior to logging
	 * out. This method is used to identify them.
	 *
	 * @param offerEvent
	 * @return whether or not this trade event is a duplicate "start of trade" event
	 */
	private boolean isDuplicateStartOfOfferEvent(OfferEvent offerEvent)
	{
		Map<Integer, OfferEvent> loggedInAccsLastOffers = accountCache.get(currentlyLoggedInAccount).getLastOffers();
		return loggedInAccsLastOffers.containsKey(offerEvent.getSlot()) &&
			loggedInAccsLastOffers.get(offerEvent.getSlot()).getCurrentQuantityInTrade() == 0 &&
			loggedInAccsLastOffers.get(offerEvent.getSlot()).getState() == offerEvent.getState();
	}

	/**
	 * Creates an OfferEvent object out of a GrandExchangeOfferChanged event and adds additional attributes such as
	 * tickArrivedAt to help identify margin check offers.
	 *
	 * @param newOfferEvent event that we subscribe to.
	 * @return an OfferEvent object with the relevant information from the event.
	 */
	private OfferEvent createOfferEvent(GrandExchangeOfferChanged newOfferEvent)
	{
		OfferEvent offer = OfferEvent.fromGrandExchangeEvent(newOfferEvent);
		offer.setTickArrivedAt(client.getTickCount());
		offer.setMadeBy(currentlyLoggedInAccount);
		return offer;
	}

	/**
	 * This method updates the given trade list in response to an OfferEvent
	 *
	 * @param trades       the trades list to update
	 * @param flippingItem the flipping item to be updated in the tradeslist, if it even exists
	 * @param newOffer     new offer that just came in
	 */
	private void updateTradesList(List<FlippingItem> trades, Optional<FlippingItem> flippingItem, OfferEvent newOffer)
	{
		if (flippingItem.isPresent())
		{
			FlippingItem item = flippingItem.get();
			if (newOffer.isMarginCheck())
			{
				trades.remove(item);
				trades.add(0, item);
			}
			//if a user buys/sells an item they previously deleted from the flipping panel, show the panel again.
			if (!item.getValidFlippingPanelItem())
			{
				item.setValidFlippingPanelItem(true);
				trades.remove(item);
				trades.add(0, item);
			}

			item.updateHistory(newOffer);
			item.updateLatestProperties(newOffer);
		}
		else
		{
			addToTradesList(trades, newOffer);
		}
	}

	/**
	 * Constructs a FlippingItem, the data structure that represents an item the user is currently flipping, and
	 * adds it to the given trades list. This method is invoked when we receive an offer event for an item that isn't
	 * currently present in the trades list.
	 *
	 * @param tradesList the trades list to be updated
	 * @param newOffer   the offer to update the trade list with
	 */
	private void addToTradesList(List<FlippingItem> tradesList, OfferEvent newOffer)
	{
		int tradeItemId = newOffer.getItemId();
		String itemName = itemManager.getItemComposition(tradeItemId).getName();

		ItemStats itemStats = itemManager.getItemStats(tradeItemId, false);
		int geLimit = itemStats != null ? itemStats.getGeLimit() : 0;

		FlippingItem flippingItem = new FlippingItem(tradeItemId, itemName, geLimit, currentlyLoggedInAccount);
		flippingItem.setValidFlippingPanelItem(true);
		flippingItem.updateHistory(newOffer);
		flippingItem.updateLatestProperties(newOffer);

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

	/**
	 * Gets the accumulated time the account currently being viewed has been flipping this session.
	 *
	 * @return accumulated time
	 */
	public Duration getAccumulatedTimeForCurrentView()
	{
		if (accountCurrentlyViewed.equals(ACCOUNT_WIDE))
		{
			return accountCache.values().stream().map(AccountData::getAccumulatedSessionTime).reduce(Duration.ZERO, (d1, d2) -> d1.plus(d2));
		}
		else
		{
			return accountCache.get(accountCurrentlyViewed).getAccumulatedSessionTime();
		}
	}

	public Instant getStartOfSessionForCurrentView()
	{
		if (accountCurrentlyViewed.equals(ACCOUNT_WIDE))
		{
			return startUpTime;
		}
		else
		{
			return accountCache.get(accountCurrentlyViewed).getSessionStartTime();
		}
	}

	/**
	 * Invoked when a user clicks the button to reset the session time in the statistics panel.
	 */
	public void handleSessionTimeReset()
	{
		if (!accountCurrentlyViewed.equals(ACCOUNT_WIDE))
		{
			accountCache.get(accountCurrentlyViewed).startNewSession();
		}
	}

	@Provides
	FlippingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FlippingConfig.class);
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
			thisClientLastStored = displayName;
			AccountData data = accountCache.get(displayName);
			if (data == null)
			{
				log.info("for an unknown reason the data associated with {} has been set to null. Storing" +
					"an empty AccountData object instead.", displayName);
				data = new AccountData();
			}
			TradePersister.storeTrades(displayName, data);
			log.info("successfully stored trades for {}", displayName);
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
			AccountData accountData = TradePersister.loadTrades(displayName);
			accountData.prepareForUse(itemManager);
			if (accountData.getSlotTimers() == null)
			{
				accountData.setSlotTimers(setupSlotTimers());
			}
			else
			{
				accountData.getSlotTimers().forEach(timer -> {
					timer.setClient(client);
					timer.setPlugin(this);
				});
			}
			return accountData;
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

				return !item.getValidFlippingPanelItem() && !item.hasValidOffers()
					&& (!Instant.now().isAfter(item.getGeLimitResetTime()) || item.getGeLimitResetTime().isBefore(startOfRefresh));
			}
			return !item.getValidFlippingPanelItem() && !item.hasValidOffers();
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

		if (displayNameOfChangedAcc.equals(thisClientLastStored))
		{
			log.info("not reloading data for {} into the cache as this client was the last one to store it", displayNameOfChangedAcc);
			thisClientLastStored = null;
			return;
		}

		executor.schedule(() ->
		{
			//have to run on client thread cause loadTrades calls accountData.prepareForUse which uses the itemmanager
			clientThread.invokeLater(() -> {
				log.info("second has passed, updating cache for {}", displayNameOfChangedAcc);

				accountCache.put(displayNameOfChangedAcc, loadTrades(displayNameOfChangedAcc));
				if (!masterPanel.getViewSelectorItems().contains(displayNameOfChangedAcc))
				{
					masterPanel.getAccountSelector().addItem(displayNameOfChangedAcc);
				}

				if (accountCache.keySet().size() > 1)
				{
					masterPanel.getAccountSelector().setVisible(true);
				}

				updateSinceLastAccountWideBuild = true;

				//rebuild if you are currently looking at the account who's cache just got updated or the account wide view.
				if (accountCurrentlyViewed.equals(ACCOUNT_WIDE) || accountCurrentlyViewed.equals(displayNameOfChangedAcc))
				{
					List<FlippingItem> updatedList = getTradesForCurrentView();
					flippingPanel.rebuild(updatedList);
					statPanel.rebuild(updatedList);
				}
			});
		}, 1000, TimeUnit.MILLISECONDS);
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

		Collection<OfferEvent> lastOffers = accountCache.get(currentlyLoggedInAccount).getLastOffers().values();
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
			Instant lastSessionTimeUpdate = accountCache.get(currentlyLoggedInAccount).getLastSessionTimeUpdate();
			Duration accumulatedSessionTime = accountCache.get(currentlyLoggedInAccount).getAccumulatedSessionTime();
			if (lastSessionTimeUpdate == null)
			{
				lastSessionTimeUpdate = Instant.now();
			}
			long millisSinceLastSessionTimeUpdate = Instant.now().toEpochMilli() - lastSessionTimeUpdate.toEpochMilli();
			accumulatedSessionTime = accumulatedSessionTime.plus(millisSinceLastSessionTimeUpdate, ChronoUnit.MILLIS);
			lastSessionTimeUpdate = Instant.now();
			accountCache.get(currentlyLoggedInAccount).setAccumulatedSessionTime(accumulatedSessionTime);
			accountCache.get(currentlyLoggedInAccount).setLastSessionTimeUpdate(lastSessionTimeUpdate);

			if (accountCurrentlyViewed.equals(ACCOUNT_WIDE) || accountCurrentlyViewed.equals(currentlyLoggedInAccount))
			{
				statPanel.updateSessionTimeDisplay(getAccumulatedTimeForCurrentView());
			}
		}

		else if (currentlyLoggedInAccount != null)
		{
			accountCache.get(currentlyLoggedInAccount).setLastSessionTimeUpdate(null);
		}
	}


	private void rebuildTradeTimer()
	{
		for (int slotIndex = 0; slotIndex < 8; slotIndex++)
		{
			TradeActivityTimer timer = accountCache.get(currentlyLoggedInAccount).getSlotTimers().get(slotIndex);

			//Get the offer slots from the window container
			//We add one to the index, as the first widget is the text above the offer slots
			Widget offerSlot = client.getWidget(WidgetID.GRAND_EXCHANGE_GROUP_ID, 5).getStaticChildren()[slotIndex + 1];

			if (offerSlot == null)
			{
				return;
			}

			if (timer.getSlotWidget() == null)
			{
				timer.setWidget(offerSlot);
			}

			clientThread.invokeLater(timer::updateTimer);
		}
	}

	public void setFavoriteOnAllAccounts(FlippingItem item, boolean favoriteStatus)
	{
		for (AccountData account : accountCache.values())
		{
			account.
				getTrades().
				stream().
				filter(accountItem -> accountItem.getItemId() == item.getItemId()).
				findFirst().
				ifPresent(accountItem -> accountItem.setFavorite(favoriteStatus));
		}
	}

	public void addSelectedGeTabOffers(List<OfferEvent> selectedOffers)
	{
		for (OfferEvent offerEvent : selectedOffers)
		{
			addSelectedGeTabOffer(offerEvent);
		}
		//have to add a delay before rebuilding as item limit and name may not have been set yet in addSelectedGeTabOffer due to
		//clientThread being async and not offering a future to wait on when you submit a runnable...
		executor.schedule(() -> {
			flippingPanel.rebuild(getTradesForCurrentView());
			statPanel.rebuild(getTradesForCurrentView());
		}, 500, TimeUnit.MILLISECONDS);
	}

	private void addSelectedGeTabOffer(OfferEvent selectedOffer)
	{
		if (currentlyLoggedInAccount == null)
		{
			return;
		}
		Optional<FlippingItem> flippingItem = accountCache.get(currentlyLoggedInAccount).getTrades().stream().filter(item -> item.getItemId() == selectedOffer.getItemId()).findFirst();
		if (flippingItem.isPresent())
		{
			flippingItem.get().updateHistory(selectedOffer);
			flippingItem.get().updateLatestProperties(selectedOffer);
			//incase it was set to false before
			flippingItem.get().setValidFlippingPanelItem(true);
		}
		else
		{
			int tradeItemId = selectedOffer.getItemId();
			FlippingItem item = new FlippingItem(tradeItemId, "", -1, currentlyLoggedInAccount);
			item.setValidFlippingPanelItem(true);
			item.updateLatestProperties(selectedOffer);
			item.updateHistory(selectedOffer);
			accountCache.get(currentlyLoggedInAccount).getTrades().add(0, item);

			//itemmanager can only be used on the client thread.
			//i can't put everything in the runnable given to the client thread cause then it executes async and if there
			//are multiple offers for the same flipping item that doesn't yet exist in trades list, it might create multiple
			//of them.
			clientThread.invokeLater(() -> {
				String itemName = itemManager.getItemComposition(tradeItemId).getName();
				ItemStats itemStats = itemManager.getItemStats(tradeItemId, false);
				int geLimit = itemStats != null ? itemStats.getGeLimit() : 0;
				item.setItemName(itemName);
				item.setTotalGELimit(geLimit);
			});
		}
	}

	public List<OfferEvent> findOfferMatches(OfferEvent offerEvent, int limit)
	{
		Optional<FlippingItem> flippingItem = accountCache.get(currentlyLoggedInAccount).getTrades().stream().filter(item -> item.getItemId() == offerEvent.getItemId()).findFirst();
		if (!flippingItem.isPresent())
		{
			return new ArrayList<>();
		}
		return flippingItem.get().getOfferMatches(offerEvent, limit);
	}

	public Font getFont()
	{
		return FontManager.getRunescapeSmallFont();
	}

	@Subscribe
	public void onGrandExchangeSearched(GrandExchangeSearched event)
	{
		final String input = client.getVar(VarClientStr.INPUT_TEXT);
		if (!input.equals(config.favoriteSearchCode()))
		{
			return;
		}

		Set<Integer> ids = accountCache.get(currentlyLoggedInAccount).
			getTrades()
			.stream()
			.filter(FlippingItem::isFavorite)
			.map(FlippingItem::getItemId)
			.collect(Collectors.toSet());

		client.setGeSearchResultIndex(0);
		client.setGeSearchResultCount(ids.size());
		client.setGeSearchResultIds(Shorts.toArray(ids));

		event.consume();
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		//ge history interface closed, so the geHistoryTabPanel should no longer show
		if (event.getScriptId() == 29)
		{
			masterPanel.selectPreviouslySelectedTab();
		}

		if (event.getScriptId() == 804)
		{
			//Fired after every GE offer slot redraw
			//This seems to happen after any offer updates or if buttons are pressed inside the interface
			//https://github.com/RuneStar/cs2-scripts/blob/a144f1dceb84c3efa2f9e90648419a11ee48e7a2/scripts/%5Bclientscript%2Cge_offers_switchpanel%5D.cs2
			if (config.slotTimersEnabled())
			{
				rebuildTradeTimer();
			}
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
		//ge history widget loaded
		//GE_HISTORY_TAB_WIDGET_ID does not load when history tab is opened from the banker right click. It only loads, when
		//the "history" button is clicked for the ge interface. However, 383 loads in both situations.
		if (event.getGroupId() == 383)
		{
			clientThread.invokeLater(() -> {
				Widget[] geHistoryTabWidgets = client.getWidget(383, 3).getDynamicChildren();
				List<OfferEvent> offerEvents = GeHistoryTabExtractor.convertWidgetsToOfferEvents(geHistoryTabWidgets);
				List<List<OfferEvent>> matchingOffers = new ArrayList<>();
				offerEvents.forEach(o -> {
					o.setItemName(itemManager.getItemComposition(o.getItemId()).getName());
					o.setMadeBy(currentlyLoggedInAccount);
					matchingOffers.add(findOfferMatches(o, 5));
				});
				geHistoryTabPanel.rebuild(offerEvents, matchingOffers, geHistoryTabWidgets, false);
				masterPanel.showView("ge history");
			});
		}

		//if either ge interface or bank pin interface is loaded, hide the trade history tab panel again
		if (event.getGroupId() == WidgetID.GRAND_EXCHANGE_GROUP_ID || event.getGroupId() == 213)
		{
			masterPanel.selectPreviouslySelectedTab();
		}

		//The player opens the trade history tab from the ge interface. Necessary since the back button isn't considered hidden here.
		//this (id 149 and not id 383) will also trigger when the player just exits out of the ge interface offer window screen, which is good
		//as then the highlight won't linger in that case.
		if (event.getGroupId() == GE_HISTORY_TAB_WIDGET_ID && flippingPanel.isItemHighlighted())
		{
			flippingPanel.dehighlightItem();
		}
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

	public void deleteAccount(String displayName)
	{
		log.info("deleting all data for {}", displayName);
		accountCache.remove(displayName);
		if (accountCurrentlyViewed.equals(displayName))
		{
			masterPanel.getAccountSelector().setSelectedItem(accountCache.keySet().toArray()[0]);
		}
		TradePersister.deleteFile(displayName + ".json");
		if (accountCache.keySet().size() < 2)
		{
			masterPanel.getAccountSelector().setVisible(false);
		}
		masterPanel.getAccountSelector().removeItem(displayName);
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

			if (event.getKey().equals("slotTimersEnabled"))
			{
				if (config.slotTimersEnabled())
				{
					slotTimersTask = executor.scheduleAtFixedRate(() -> accountCache.get(currentlyLoggedInAccount).getSlotTimers().forEach(timer -> clientThread.invokeLater(() -> timer.updateTimer())), 1000, 1000, TimeUnit.MILLISECONDS);
				}
				else
				{
					slotTimersTask.cancel(true);
					accountCache.get(currentlyLoggedInAccount).getSlotTimers().forEach(TradeActivityTimer::resetToDefault);
				}
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
			flippingWidget = new OfferEditor(client.getWidget(WidgetInfo.CHATBOX_CONTAINER), client);

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
					flippingWidget.update("setCurrentQuantityInTrade", itemGELimit);
				}
				else
				{
					flippingWidget.update("setCurrentQuantityInTrade", selectedItem.getRemainingGeLimit());
				}
			}
			else if (chatInputText.equals("Set a price for each item:"))
			{
				if (offerText.equals("Buy offer"))
				{
					//No recorded data; hide the widget
					if (selectedItem == null || !selectedItem.getLatestMarginCheckSell().isPresent())
					{
						flippingWidget.update("reset", 0);
					}
					else
					{
						flippingWidget.update("setBuyPrice", selectedItem.getLatestMarginCheckSell().get().getPrice());
					}
				}
				else if (offerText.equals("Sell offer"))
				{
					//No recorded data; hide the widget
					if (selectedItem == null || !selectedItem.getLatestMarginCheckBuy().isPresent())
					{
						flippingWidget.update("reset", 0);
					}
					else
					{
						flippingWidget.update("setSellPrice", selectedItem.getLatestMarginCheckBuy().get().getPrice());
					}
				}
			}
		});
	}
}