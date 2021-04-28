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

package com.flippingutilities.controller;

import com.flippingutilities.FlippingConfig;
import com.flippingutilities.db.TradePersister;
import com.flippingutilities.model.*;
import com.flippingutilities.ui.MasterPanel;
import com.flippingutilities.ui.flipping.FlippingPanel;
import com.flippingutilities.ui.gehistorytab.GeHistoryTabPanel;
import com.flippingutilities.ui.settings.SettingsPanel;
import com.flippingutilities.ui.slots.SlotsPanel;
import com.flippingutilities.ui.statistics.StatsPanel;
import com.flippingutilities.ui.widgets.TradeActivityTimer;
import com.flippingutilities.jobs.CacheUpdaterJob;
import com.flippingutilities.utilities.GeHistoryTabExtractor;
import com.flippingutilities.utilities.InvalidOptionException;
import com.flippingutilities.jobs.WikiDataFetcherJob;
import com.flippingutilities.utilities.WikiRequest;
import com.google.common.primitives.Shorts;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.http.api.item.ItemStats;
import okhttp3.*;

import javax.inject.Inject;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Slf4j
@PluginDescriptor(
        name = "Flipping Utilities",
        description = "Provides utilities for GE flipping"
)
public class FlippingPlugin extends Plugin {
    public static final String CONFIG_GROUP = "flipping";
    public static final String ACCOUNT_WIDE = "Accountwide";

    @Inject
    @Getter
    private Client client;
    @Inject
    @Getter
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
    @Getter
    private ItemManager itemManager;

    @Inject
    private KeyManager keyManager;

    @Inject
    @Getter
    private OkHttpClient httpClient;

    @Getter
    private FlippingPanel flippingPanel;
    @Getter
    private StatsPanel statPanel;
    @Getter
    private SlotsPanel slotsPanel;
    @Getter
    private MasterPanel masterPanel;
    @Getter
    private GeHistoryTabPanel geHistoryTabPanel;
    private SettingsPanel settingsPanel;

    //this flag is to know that when we see the login screen an account has actually logged out and its not just that the
    //client has started.
    private boolean previouslyLoggedIn;

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
    @Getter
    private List<OfferEvent> eventsReceivedBeforeFullLogin = new ArrayList<>();

    //building the account wide trade list is an expensive operation so we store it in this variable and only recompute
    //it if we have gotten an update since the last account wide trade list build.
    @Setter
    boolean updateSinceLastAccountWideBuild = true;
    List<FlippingItem> prevBuiltAccountWideList;

    //updates the cache by monitoring the directory and loading a file's contents into the cache if it has been changed
    private CacheUpdaterJob cacheUpdaterJob;

    private ScheduledFuture slotTimersTask;
    private Instant startUpTime = Instant.now();

    @Getter
    private int loginTickCount;

    private OptionHandler optionHandler;
    @Getter
    private DataHandler dataHandler;
    private GameUiChangesHandler gameUiChangesHandler;
    private NewOfferEventPipelineHandler newOfferEventPipelineHandler;
    @Getter
    private WikiDataFetcherJob wikiDataFetcherJob;

    @Getter
    private WikiRequest lastWikiRequest;
    @Getter
    private Instant timeOfLastWikiRequest;

    @Override
    protected void startUp() {
        optionHandler = new OptionHandler(this);
        dataHandler = new DataHandler(this);
        gameUiChangesHandler = new GameUiChangesHandler(this);
        newOfferEventPipelineHandler = new NewOfferEventPipelineHandler(this);

        flippingPanel = new FlippingPanel(this, itemManager, executor);
        statPanel = new StatsPanel(this, itemManager, executor);
        settingsPanel = new SettingsPanel(this);
        geHistoryTabPanel = new GeHistoryTabPanel(this);
        slotsPanel = new SlotsPanel(itemManager);
        masterPanel = new MasterPanel(this, flippingPanel, statPanel, settingsPanel, slotsPanel);
        masterPanel.addView(geHistoryTabPanel, "ge history");
        navButton = NavigationButton.builder()
                .tooltip("Flipping Utilities")
                .icon(ImageUtil.getResourceStreamFromClass(getClass(), "/graph_icon_green.png"))
                .priority(3)
                .panel(masterPanel)
                .build();

        clientToolbar.addNavigation(navButton);
        keyManager.registerKeyListener(offerEditorKeyListener());
        clientThread.invokeLater(() ->
        {
            switch (client.getGameState()) {
                case STARTING:
                case UNKNOWN:
                    return false;
            }

            dataHandler.loadData();
            masterPanel.setupAccSelectorDropdown(dataHandler.getCurrentAccounts());
            generalRepeatingTasks = setupRepeatingTasks(1000);
            startJobs();

            //this is only relevant if the user downloads/enables the plugin after they login.
            if (client.getGameState() == GameState.LOGGED_IN) {
                log.info("user is already logged in when they downloaded/enabled the plugin");
                onLoggedInGameState();
            }
            //stops scheduling this task
            return true;
        });
    }

    @Override
    protected void shutDown() {
        if (generalRepeatingTasks != null) {
            generalRepeatingTasks.cancel(true);
            generalRepeatingTasks = null;
        }
        if (slotTimersTask != null) {
            slotTimersTask.cancel(true);
            slotTimersTask = null;
        }

        clientToolbar.removeNavigation(navButton);
    }

    @Subscribe(priority = 101)
    public void onClientShutdown(ClientShutdown clientShutdownEvent) {
        if (generalRepeatingTasks != null) {
            generalRepeatingTasks.cancel(true);
        }
        if (slotTimersTask != null) {
            slotTimersTask.cancel(true);
            slotTimersTask = null;
        }
        dataHandler.storeData();
        cacheUpdaterJob.stop();
        wikiDataFetcherJob.stop();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGGED_IN) {
            onLoggedInGameState();
        } else if (event.getGameState() == GameState.LOGIN_SCREEN && previouslyLoggedIn) {
            //this randomly fired at night hours after i had logged off...so i'm adding this guard here.
            if (currentlyLoggedInAccount != null && client.getGameState() != GameState.LOGGED_IN) {
                handleLogout();
            }
        }
    }

    private void onLoggedInGameState() {
        //keep scheduling this task until it returns true (when we have access to a display name)
        clientThread.invokeLater(() ->
        {
            //we return true in this case as something went wrong and somehow the state isn't logged in, so we don't
            //want to keep scheduling this task.
            if (client.getGameState() != GameState.LOGGED_IN) {
                return true;
            }

            final Player player = client.getLocalPlayer();

            //player is null, so we can't get the display name so, return false, which will schedule
            //the task on the client thread again.
            if (player == null) {
                return false;
            }

            final String name = player.getName();

            if (name == null) {
                return false;
            }

            if (name.equals("")) {
                return false;
            }
            previouslyLoggedIn = true;

            if (currentlyLoggedInAccount == null) {
                handleLogin(name);
            }
            //stops scheduling this task
            return true;
        });
    }

    public void handleLogin(String displayName) {
        if (client.getAccountType().isIronman()) {
            log.info("account is an ironman, not adding it to the cache");
            return;
        }

        log.info("{} has just logged in!", displayName);
        if (!dataHandler.getCurrentAccounts().contains(displayName)) {
            log.info("data handler does not contain data for {}", displayName);
            dataHandler.addAccount(displayName);
            masterPanel.getAccountSelector().addItem(displayName);
        }

        loginTickCount = client.getTickCount();
        currentlyLoggedInAccount = displayName;

        //now that we have a display name we can process any events that we received before the display name
        //was set.
        eventsReceivedBeforeFullLogin.forEach(newOfferEventPipelineHandler::onNewOfferEvent);
        eventsReceivedBeforeFullLogin.clear();

        if (dataHandler.getCurrentAccounts().size() > 1) {
            masterPanel.getAccountSelector().setVisible(true);
        }
        accountCurrentlyViewed = displayName;
        //this will cause changeView to be invoked which will cause a rebuild of
        //flipping and stats panel
        masterPanel.getAccountSelector().setSelectedItem(displayName);

        if (slotTimersTask == null && config.slotTimersEnabled()) {
            log.info("starting slot timers on login");
            slotTimersTask = startSlotTimers();
        }
    }

    public void handleLogout() {
        log.info("{} is logging out", currentlyLoggedInAccount);

        dataHandler.getAccountData(currentlyLoggedInAccount).setLastSessionTimeUpdate(null);
        dataHandler.storeData();

        if (slotTimersTask != null && !slotTimersTask.isCancelled()) {
            log.info("cancelling slot timers task on logout");
            slotTimersTask.cancel(true);
        }
        slotTimersTask = null;
        currentlyLoggedInAccount = null;
        masterPanel.revertToSafeDisplay();
    }

    /**
     * Currently used for updating time sensitive displays such as the accumulated session time,
     * how long ago an item was flipped, etc.
     *
     * @return a future object that can be used to cancel the tasks
     */
    public ScheduledFuture setupRepeatingTasks(int msStartDelay) {
        return executor.scheduleAtFixedRate(() ->
        {
            try {
                flippingPanel.updateTimerDisplays();
                statPanel.updateTimeDisplay();
                updateSessionTime();
            } catch (ConcurrentModificationException e) {
                log.info("concurrent modification exception. This is fine, will just restart tasks after delay." +
                        " Cancelling general repeating tasks and starting it again after 5000 ms delay");
                generalRepeatingTasks.cancel(true);
                generalRepeatingTasks = setupRepeatingTasks(5000);
            } catch (Exception e) {
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
    public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged offerChangedEvent) {
        newOfferEventPipelineHandler.onGrandExchangeOfferChanged(offerChangedEvent);
    }

    public List<FlippingItem> getTradesForCurrentView() {
        return accountCurrentlyViewed.equals(ACCOUNT_WIDE) ? createAccountWideList() : dataHandler.getAccountData(accountCurrentlyViewed).getTrades();
    }

    public List<FlippingItem> viewTradesForCurrentView() {
        return accountCurrentlyViewed.equals(ACCOUNT_WIDE) ? createAccountWideList() : dataHandler.viewAccountData(accountCurrentlyViewed).getTrades();
    }

    public Duration viewAccumulatedTimeForCurrentView() {
        if (accountCurrentlyViewed.equals(ACCOUNT_WIDE)) {
            return dataHandler.viewAllAccountData().stream().map(AccountData::getAccumulatedSessionTime).reduce(Duration.ZERO, (d1, d2) -> d1.plus(d2));
        } else {
            return dataHandler.viewAccountData(accountCurrentlyViewed).getAccumulatedSessionTime();
        }
    }

    public Instant viewStartOfSessionForCurrentView() {
        if (accountCurrentlyViewed.equals(ACCOUNT_WIDE)) {
            return startUpTime;
        } else {
            return dataHandler.viewAccountData(accountCurrentlyViewed).getSessionStartTime();
        }
    }

    /**
     * Invoked when a user clicks the button to reset the session time in the statistics panel.
     */
    public void handleSessionTimeReset() {
        if (!accountCurrentlyViewed.equals(ACCOUNT_WIDE)) {
            dataHandler.getAccountData(accountCurrentlyViewed).startNewSession();
        }
    }

    @Provides
    FlippingConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(FlippingConfig.class);
    }

    public void deleteRemovedItems(List<FlippingItem> currItems) {
        currItems.removeIf((item) ->
        {
            if (item.getGeLimitResetTime() != null) {
                Instant startOfRefresh = item.getGeLimitResetTime().minus(4, ChronoUnit.HOURS);

                return !item.getValidFlippingPanelItem() && !item.hasValidOffers()
                        && (!Instant.now().isAfter(item.getGeLimitResetTime()) || item.getGeLimitResetTime().isBefore(startOfRefresh));
            }
            return !item.getValidFlippingPanelItem() && !item.hasValidOffers();
        });
    }

    public void truncateTradeList() {
        if (accountCurrentlyViewed.equals(ACCOUNT_WIDE)) {
            dataHandler.getAllAccountData().forEach(accountData -> deleteRemovedItems(accountData.getTrades()));
        } else {
            deleteRemovedItems(getTradesForCurrentView());
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
    public void changeView(String selectedName) {
        log.info("changing view to {}", selectedName);

        List<FlippingItem> tradesListToDisplay;
        if (selectedName.equals(ACCOUNT_WIDE)) {
            tradesListToDisplay = createAccountWideList();
        } else {
            tradesListToDisplay = dataHandler.getAccountData(selectedName).getTrades();
        }

        accountCurrentlyViewed = selectedName;
        statPanel.rebuild(tradesListToDisplay);
        flippingPanel.rebuild(tradesListToDisplay);
    }

    private void startJobs() {
        cacheUpdaterJob = new CacheUpdaterJob();
        cacheUpdaterJob.subscribe(this::onDirectoryUpdate);
        cacheUpdaterJob.start();

        wikiDataFetcherJob = new WikiDataFetcherJob(this, httpClient);
        wikiDataFetcherJob.subscribe(this::onWikiFetch);
        wikiDataFetcherJob.start();
    }

    private void onWikiFetch(WikiRequest wikiRequest, Instant timeOfRequestCompletion) {
        lastWikiRequest = wikiRequest;
        timeOfLastWikiRequest = timeOfRequestCompletion;
        flippingPanel.updateWikiDisplays(wikiRequest, timeOfRequestCompletion);
    }

    /**
     * This is a callback executed by the cacheUpdater when it notices the directory has changed. If the
     * file changed belonged to a different acc than the currently logged in one, it updates the cache of that
     * account to ensure this client has the most up to date data on each account. If the user is currently looking
     * at the account that had its cache updated, a rebuild takes place to display the most recent trade list.
     *
     * @param fileName name of the file which was modified.
     */
    public void onDirectoryUpdate(String fileName) {
        String displayNameOfChangedAcc = fileName.split("\\.")[0];

        if (displayNameOfChangedAcc.equals(dataHandler.thisClientLastStored)) {
            log.info("not reloading data for {} into the cache as this client was the last one to store it", displayNameOfChangedAcc);
            dataHandler.thisClientLastStored = null;
            return;
        }

        if (fileName.equals("accountwide.json")) {
            executor.schedule(() -> {
                dataHandler.loadAccountWideData();
            }, 1000, TimeUnit.MILLISECONDS);
            return;
        }

        executor.schedule(() ->
        {
            //have to run on client thread cause loadAccount calls accountData.prepareForUse which uses the itemmanager
            clientThread.invokeLater(() -> {
                log.info("second has passed, updating cache for {}", displayNameOfChangedAcc);
                dataHandler.loadAccountData(displayNameOfChangedAcc);
                if (!masterPanel.getViewSelectorItems().contains(displayNameOfChangedAcc)) {
                    masterPanel.getAccountSelector().addItem(displayNameOfChangedAcc);
                }

                if (dataHandler.getCurrentAccounts().size() > 1) {
                    masterPanel.getAccountSelector().setVisible(true);
                }

                updateSinceLastAccountWideBuild = true;

                //rebuild if you are currently looking at the account who's cache just got updated or the account wide view.
                if (accountCurrentlyViewed.equals(ACCOUNT_WIDE) || accountCurrentlyViewed.equals(displayNameOfChangedAcc)) {
                    List<FlippingItem> updatedList = viewTradesForCurrentView();
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
    private List<FlippingItem> createAccountWideList() {
        //since this is an expensive operation, cache its results and only recompute it if there has been an update
        //to one of the account's tradelists, (updateSinceLastAccountWideBuild is set in onGrandExchangeOfferChanged)
        if (!updateSinceLastAccountWideBuild) {
            return prevBuiltAccountWideList;
        }

        if (dataHandler.getCurrentAccounts().size() == 0) {
            return new ArrayList<>();
        }

        //take all flipping items from the account cache, regardless of account, and segregate them based on item name.
        Map<Integer, List<FlippingItem>> groupedItems = dataHandler.viewAllAccountData().stream().
                flatMap(accountData -> accountData.getTrades().stream()).
                map(FlippingItem::clone).
                collect(Collectors.groupingBy(FlippingItem::getItemId));

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
    private boolean currentlyFlipping() {
        if (currentlyLoggedInAccount == null) {
            return false;
        }

        Collection<OfferEvent> lastOffers = dataHandler.viewAccountData(currentlyLoggedInAccount).getLastOffers().values();
        return lastOffers.stream().anyMatch(offerInfo -> !offerInfo.isComplete());
    }

    /**
     * Calculates and updates the session time display in the statistics tab when a user is viewing
     * the "Session" time interval.
     */
    private void updateSessionTime() {
        if (currentlyFlipping()) {
            Instant lastSessionTimeUpdate = dataHandler.viewAccountData(currentlyLoggedInAccount).getLastSessionTimeUpdate();
            Duration accumulatedSessionTime = dataHandler.viewAccountData(currentlyLoggedInAccount).getAccumulatedSessionTime();
            if (lastSessionTimeUpdate == null) {
                lastSessionTimeUpdate = Instant.now();
            }
            long millisSinceLastSessionTimeUpdate = Instant.now().toEpochMilli() - lastSessionTimeUpdate.toEpochMilli();
            accumulatedSessionTime = accumulatedSessionTime.plus(millisSinceLastSessionTimeUpdate, ChronoUnit.MILLIS);
            lastSessionTimeUpdate = Instant.now();
            dataHandler.getAccountData(currentlyLoggedInAccount).setAccumulatedSessionTime(accumulatedSessionTime);
            dataHandler.getAccountData(currentlyLoggedInAccount).setLastSessionTimeUpdate(lastSessionTimeUpdate);

            if (accountCurrentlyViewed.equals(ACCOUNT_WIDE) || accountCurrentlyViewed.equals(currentlyLoggedInAccount)) {
                statPanel.updateSessionTimeDisplay(viewAccumulatedTimeForCurrentView());
            }
        } else if (currentlyLoggedInAccount != null) {
            dataHandler.getAccountData(currentlyLoggedInAccount).setLastSessionTimeUpdate(null);
        }
    }


    public void rebuildTradeTimers() {
        for (int slotIndex = 0; slotIndex < 8; slotIndex++) {
            TradeActivityTimer timer = dataHandler.viewAccountData(currentlyLoggedInAccount).getSlotTimers().get(slotIndex);

            //Get the offer slots from the window container
            //We add one to the index, as the first widget is the text above the offer slots
            Widget offerSlot = client.getWidget(WidgetID.GRAND_EXCHANGE_GROUP_ID, 5).getStaticChildren()[slotIndex + 1];

            if (offerSlot == null) {
                return;
            }

            if (timer.getSlotWidget() == null) {
                timer.setWidget(offerSlot);
            }

            clientThread.invokeLater(timer::updateTimerDisplay);
        }
    }

    public void setFavoriteOnAllAccounts(FlippingItem item, boolean favoriteStatus) {
        for (String accountName : dataHandler.getCurrentAccounts()) {
            AccountData account = dataHandler.viewAccountData(accountName);
            account.
                    getTrades().
                    stream().
                    filter(accountItem -> accountItem.getItemId() == item.getItemId()).
                    findFirst().
                    ifPresent(accountItem -> {
                        accountItem.setFavorite(favoriteStatus);
                        markAccountTradesAsHavingChanged(accountName);
                    });
        }
    }

    public void setFavoriteCodeOnAllAccounts(FlippingItem item, String favoriteCode) {
        for (String accountName : dataHandler.getCurrentAccounts()) {
            AccountData account = dataHandler.viewAccountData(accountName);
            account.
                    getTrades().
                    stream().
                    filter(accountItem -> accountItem.getItemId() == item.getItemId()).
                    findFirst().
                    ifPresent(accountItem -> {
                        accountItem.setFavoriteCode(favoriteCode);
                        markAccountTradesAsHavingChanged(accountName);
                    });
        }
    }

    public void addSelectedGeTabOffers(List<OfferEvent> selectedOffers) {
        for (OfferEvent offerEvent : selectedOffers) {
            addSelectedGeTabOffer(offerEvent);
        }

        //have to add a delay before rebuilding as item limit and name may not have been set yet in addSelectedGeTabOffer due to
        //clientThread being async and not offering a future to wait on when you submit a runnable...
        executor.schedule(() -> {
            flippingPanel.rebuild(viewTradesForCurrentView());
            statPanel.rebuild(viewTradesForCurrentView());
        }, 500, TimeUnit.MILLISECONDS);
    }

    private void addSelectedGeTabOffer(OfferEvent selectedOffer) {
        if (currentlyLoggedInAccount == null) {
            return;
        }
        Optional<FlippingItem> flippingItem = dataHandler.getAccountData(currentlyLoggedInAccount).getTrades().stream().filter(item -> item.getItemId() == selectedOffer.getItemId()).findFirst();
        if (flippingItem.isPresent()) {
            flippingItem.get().updateHistory(selectedOffer);
            flippingItem.get().updateLatestProperties(selectedOffer);
            //incase it was set to false before
            flippingItem.get().setValidFlippingPanelItem(true);
        } else {
            int tradeItemId = selectedOffer.getItemId();
            FlippingItem item = new FlippingItem(tradeItemId, "", -1, currentlyLoggedInAccount);
            item.setValidFlippingPanelItem(true);
            item.updateLatestProperties(selectedOffer);
            item.updateHistory(selectedOffer);
            dataHandler.getAccountData(currentlyLoggedInAccount).getTrades().add(0, item);

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

    public void showGeHistoryTabPanel() {
        clientThread.invokeLater(() -> {
            Widget[] geHistoryTabWidgets = client.getWidget(383, 3).getDynamicChildren();
            List<OfferEvent> offerEvents = GeHistoryTabExtractor.convertWidgetsToOfferEvents(geHistoryTabWidgets);
            List<List<OfferEvent>> matchingOffers = new ArrayList<>();
            offerEvents.forEach(o -> {
                o.setItemName(itemManager.getItemComposition(o.getItemId()).getName());
                o.setMadeBy(getCurrentlyLoggedInAccount());
                matchingOffers.add(findOfferMatches(o, 5));
            });
            geHistoryTabPanel.rebuild(offerEvents, matchingOffers, geHistoryTabWidgets, false);
            masterPanel.showView("ge history");
        });
    }

    public List<OfferEvent> findOfferMatches(OfferEvent offerEvent, int limit) {
        Optional<FlippingItem> flippingItem = dataHandler.getAccountData(currentlyLoggedInAccount).getTrades().stream().filter(item -> item.getItemId() == offerEvent.getItemId()).findFirst();
        if (!flippingItem.isPresent()) {
            return new ArrayList<>();
        }
        return flippingItem.get().getOfferMatches(offerEvent, limit);
    }

    public Font getFont() {
        return FontManager.getRunescapeSmallFont();
    }

    /**
     * Used by the stats panel to invalidate all offers for a certain interval when a user hits the reset button.
     *
     * @param startOfInterval
     */
    public void invalidateOffers(Instant startOfInterval) {
        if (accountCurrentlyViewed.equals(ACCOUNT_WIDE)) {
            for (AccountData accountData : dataHandler.getAllAccountData()) {
                accountData.getTrades().forEach(item -> item.invalidateOffers(item.getIntervalHistory(startOfInterval)));
            }
        } else {
            getTradesForCurrentView().forEach(item -> item.invalidateOffers(item.getIntervalHistory(startOfInterval)));
        }

        updateSinceLastAccountWideBuild = true;
        truncateTradeList();
    }

    /**
     * Used by the flipping panel to hide all items (set the validfFippingItem property to false) when a user hits the
     * reset button
     */
    public void setAllFlippingItemsAsHidden() {
        if (accountCurrentlyViewed.equals(ACCOUNT_WIDE)) {
            for (AccountData accountData : dataHandler.getAllAccountData()) {
                accountData.getTrades().forEach(item -> item.setValidFlippingPanelItem(false));
            }
        } else {
            getTradesForCurrentView().forEach(flippingItem -> flippingItem.setValidFlippingPanelItem(false));
        }
        updateSinceLastAccountWideBuild = true;
        truncateTradeList();
    }

    public void exportToCsv(File parentDirectory, Instant startOfInterval, String startOfIntervalName) throws IOException {
        if (parentDirectory.equals(TradePersister.PARENT_DIRECTORY)) {
            throw new RuntimeException("Cannot save csv file in the flipping directory, pick another directory");
        }
        //create new flipping item list with only history from that interval
        List<FlippingItem> items = new ArrayList<>();
        for (FlippingItem item : viewTradesForCurrentView()) {
            List<OfferEvent> offersInInterval = item.getIntervalHistory(startOfInterval);
            if (offersInInterval.isEmpty()) {
                continue;
            }
            FlippingItem itemWithOnlySelectedIntervalHistory = new FlippingItem(item.getItemId(), item.getItemName(), item.getTotalGELimit(), item.getFlippedBy());
            itemWithOnlySelectedIntervalHistory.getHistory().setCompressedOfferEvents(offersInInterval);
            items.add(itemWithOnlySelectedIntervalHistory);
        }

        TradePersister.exportToCsv(new File(parentDirectory, accountCurrentlyViewed + ".csv"), items, startOfIntervalName);
    }

    public int calculateOptionValue(Option option) throws InvalidOptionException {
        return optionHandler.calculateOptionValue(option, gameUiChangesHandler.highlightedItem, gameUiChangesHandler.highlightedItemId);
    }

    public void markAccountTradesAsHavingChanged(String displayName) {
        dataHandler.markDataAsHavingChanged(displayName);
    }

    public Set<String> getCurrentDisplayNames() {
        return dataHandler.getCurrentAccounts();
    }

    private KeyListener offerEditorKeyListener() {
        return new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (gameUiChangesHandler.quantityOrPriceChatboxOpen && gameUiChangesHandler.highlightedItem.isPresent()) {
                    String keyPressed = KeyEvent.getKeyText(e.getKeyCode()).toLowerCase();
                    if (flippingPanel.getOfferEditorContainerPanel() == null) {
                        return;
                    }
                    boolean currentlyViewingQuantityEditor = flippingPanel.getOfferEditorContainerPanel().currentlyViewingQuantityEditor();

                    Optional<Option> optionExercised = dataHandler.viewAccountWideData().getOptions().stream().filter(option -> option.isQuantityOption() == currentlyViewingQuantityEditor && option.getKey().equals(keyPressed)).findFirst();

                    optionExercised.ifPresent(option -> clientThread.invoke(() -> {
                        try {
                            int optionValue = calculateOptionValue(option);
                            client.getWidget(WidgetInfo.CHATBOX_FULL_INPUT).setText(optionValue + "*");
                            client.setVar(VarClientStr.INPUT_TEXT, String.valueOf(optionValue));
                            flippingPanel.getOfferEditorContainerPanel().highlightPressedOption(keyPressed);
                            e.consume();
                        } catch (InvalidOptionException ex) {
                            //ignore
                        } catch (Exception ex) {
                            log.info("exception during key press for offer editor", ex);
                        }
                    }));
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        };
    }

    public void deleteAccount(String displayName) {
        dataHandler.deleteAccount(displayName);
        if (accountCurrentlyViewed.equals(displayName)) {
            masterPanel.getAccountSelector().setSelectedItem(dataHandler.getCurrentAccounts().toArray()[0]);
        }
        if (dataHandler.getCurrentAccounts().size() < 2) {
            masterPanel.getAccountSelector().setVisible(false);
        }
        masterPanel.getAccountSelector().removeItem(displayName);
    }

    private ScheduledFuture startSlotTimers() {
        return executor.scheduleAtFixedRate(() ->
                dataHandler.viewAccountData(currentlyLoggedInAccount).getSlotTimers().forEach(slotWidgetTimer ->
                        clientThread.invokeLater(() -> {
                            try {

                                slotsPanel.updateTimerDisplays(slotWidgetTimer.getSlotIndex(), slotWidgetTimer.createFormattedTimeString());
                                slotWidgetTimer.updateTimerDisplay();
                            } catch (Exception e) {
                                log.info("exception when trying to update timer. e: {}", e);
                            }
                        })), 1000, 1000, TimeUnit.MILLISECONDS);
    }

    @Subscribe
    public void onGrandExchangeSearched(GrandExchangeSearched event) {
        final String input = client.getVar(VarClientStr.INPUT_TEXT);
        Set<Integer> ids = dataHandler.viewAccountData(currentlyLoggedInAccount).
                getTrades()
                .stream()
                .filter(item -> item.isFavorite() && input.equals(item.getFavoriteCode()))
                .map(FlippingItem::getItemId)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) {
            return;
        }

        client.setGeSearchResultIndex(0);
        client.setGeSearchResultCount(ids.size());
        client.setGeSearchResultIds(Shorts.toArray(ids));
        event.consume();
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event) {
        gameUiChangesHandler.onScriptPostFired(event);
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        gameUiChangesHandler.onWidgetLoaded(event);
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        gameUiChangesHandler.onVarbitChanged(event);
    }

    @Subscribe
    public void onVarClientIntChanged(VarClientIntChanged event) {
        gameUiChangesHandler.onVarClientIntChanged(event);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (event.getGroup().equals(CONFIG_GROUP)) {
            if (event.getKey().equals("slotTimersEnabled")) {
                if (config.slotTimersEnabled()) {
                    slotTimersTask = startSlotTimers();
                } else {
                    if (slotTimersTask != null) {
                        slotTimersTask.cancel(true);
                    }
                    dataHandler.viewAccountData(currentlyLoggedInAccount).getSlotTimers().forEach(TradeActivityTimer::resetToDefault);
                }
            }

            statPanel.rebuild(viewTradesForCurrentView());
            flippingPanel.rebuild(viewTradesForCurrentView());
        }
    }
}