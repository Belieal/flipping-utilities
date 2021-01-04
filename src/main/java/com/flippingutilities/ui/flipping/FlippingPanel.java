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

package com.flippingutilities.ui.flipping;

import com.flippingutilities.model.FlippingItem;
import com.flippingutilities.controller.FlippingPlugin;
import com.flippingutilities.ui.offereditor.OfferEditorPanel;
import com.flippingutilities.ui.uiutilities.Icons;
import com.flippingutilities.ui.uiutilities.Paginator;
import com.flippingutilities.ui.uiutilities.UIUtilities;
import com.google.common.base.Strings;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.PluginErrorPanel;


@Slf4j
public class FlippingPanel extends JPanel
{
	@Getter
	private static final String WELCOME_PANEL = "WELCOME_PANEL";
	private static final String ITEMS_PANEL = "ITEMS_PANEL";
	private static final int DEBOUNCE_DELAY_MS = 250;
	private static final Border TOP_PANEL_BORDER = new CompoundBorder(
		BorderFactory.createMatteBorder(0, 0, 0, 0, ColorScheme.DARKER_GRAY_COLOR.darker()),
		BorderFactory.createEmptyBorder(4, 0, 0, 0));

	private final FlippingPlugin plugin;
	private final ItemManager itemManager;

	private final IconTextField searchBar = new IconTextField();
	private Future<?> runningRequest = null;

	public final CardLayout cardLayout = new CardLayout();

	private final JPanel flippingItemsPanel = new JPanel();
	public final JPanel flippingItemContainer = new JPanel(cardLayout);

	//Keeps track of all items currently displayed on the panel.
	private ArrayList<FlippingItemPanel> activePanels = new ArrayList<>();

	@Getter
	JLabel resetIcon;

	@Getter
	@Setter
	private boolean itemHighlighted = false;

	@Getter
	@Setter
	private String selectedSort;

	@Getter
	private Paginator paginator;

	@Getter
	private OfferEditorPanel offerEditorPanel;

	public FlippingPanel(final FlippingPlugin plugin, final ItemManager itemManager, ScheduledExecutorService executor)
	{
		super(false);

		this.plugin = plugin;
		this.itemManager = itemManager;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		//Holds all the item panels
		flippingItemsPanel.setLayout(new BoxLayout(flippingItemsPanel, BoxLayout.Y_AXIS));
		flippingItemsPanel.setBorder((new EmptyBorder(0, 5, 0, 3)));
		flippingItemsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.add(flippingItemsPanel, BorderLayout.NORTH);

		JScrollPane scrollWrapper = new JScrollPane(wrapper);
		scrollWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollWrapper.getVerticalScrollBar().setPreferredSize(new Dimension(5, 0));
		scrollWrapper.getVerticalScrollBar().setBorder(new EmptyBorder(0, 0, 0, 0));

		//Contains the main content panel and top panel
		JPanel container = new JPanel();
		container.setLayout(new BorderLayout(0, 0));
		container.setBackground(ColorScheme.DARK_GRAY_COLOR);

		//Search bar beneath the tab manager.
		searchBar.setIcon(IconTextField.Icon.SEARCH);
		searchBar.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 32));
		searchBar.setBackground(ColorScheme.DARK_GRAY_COLOR);
		searchBar.setBorder(BorderFactory.createMatteBorder(0, 5, 7, 5, ColorScheme.DARKER_GRAY_COLOR.darker()));
		searchBar.setHoverBackgroundColor(ColorScheme.DARKER_GRAY_HOVER_COLOR);
		searchBar.setMinimumSize(new Dimension(0, 35));
		searchBar.addActionListener(e -> executor.execute(this::updateSearch));
		searchBar.addClearListener(this::updateSearch);
		searchBar.addKeyListener(key ->
		{
			if (runningRequest != null)
			{
				runningRequest.cancel(false);
			}
			runningRequest = executor.schedule(this::updateSearch, DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);
		});

		//Contains a greeting message when the items panel is empty.
		JPanel welcomeWrapper = new JPanel(new BorderLayout());
		welcomeWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		PluginErrorPanel welcomePanel = new PluginErrorPanel();
		welcomeWrapper.add(welcomePanel, BorderLayout.NORTH);

		//The welcome panel instructs the user on how to use the plugin
		//Shown whenever there are no items on the panel
		welcomePanel.setContent("Flipping Utilities",
			"Make offers for items to show up!");

		//Clears the config and resets the items panel.
		resetIcon = new JLabel(Icons.TRASH_ICON_OFF);
		resetIcon.setBorder(new EmptyBorder(0,0,8,0));
		resetIcon.setToolTipText("Reset trade history");
		resetIcon.setPreferredSize(Icons.ICON_SIZE);
		resetIcon.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e))
				{
					//Display warning message
					final int result = JOptionPane.showOptionDialog(resetIcon, "Are you sure you want to reset the flipping panel?",
						"Are you sure?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
						null, new String[]{"Yes", "No"}, "No");

					//If the user pressed "Yes"
					if (result == JOptionPane.YES_OPTION)
					{
						plugin.setAllFlippingItemsAsHidden();
						setItemHighlighted(false);
						cardLayout.show(flippingItemContainer, WELCOME_PANEL);
						rebuild(plugin.getTradesForCurrentView());
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				resetIcon.setIcon(Icons.TRASH_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				resetIcon.setIcon(Icons.TRASH_ICON_OFF);
			}
		});

		flippingItemContainer.add(scrollWrapper, ITEMS_PANEL);
		flippingItemContainer.add(welcomeWrapper, WELCOME_PANEL);
		flippingItemContainer.setBorder(new EmptyBorder(5, 0, 0, 0));

		//Top panel that holds the plugin title and reset button.
		final JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		topPanel.add(resetIcon, BorderLayout.EAST);
		topPanel.add(searchBar, BorderLayout.CENTER);
		topPanel.setBorder(TOP_PANEL_BORDER);

		final JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		contentPanel.add(new FlippingPanelToolbar(this, plugin), BorderLayout.NORTH);
		contentPanel.add(flippingItemContainer, BorderLayout.CENTER);

		paginator = new Paginator(() -> rebuild(plugin.getTradesForCurrentView()));

		//To switch between greeting and items panels
		cardLayout.show(flippingItemContainer, WELCOME_PANEL);
		container.add(topPanel, BorderLayout.NORTH);
		container.add(contentPanel, BorderLayout.CENTER);
		container.add(paginator, BorderLayout.SOUTH);

		add(container, BorderLayout.CENTER);
	}

	/**
	 * Creates and renders the panel using the flipping items in the listed parameter.
	 * An item is only displayed if it contains a valid OfferInfo object in its history.
	 *
	 * @param flippingItems List of flipping items that the rebuild will render.
	 */
	public void rebuild(List<FlippingItem> flippingItems)
	{
		activePanels.clear();

		SwingUtilities.invokeLater(() ->
		{
			Instant rebuildStart = Instant.now();
			flippingItemsPanel.removeAll();
			if (flippingItems == null)
			{
				//Show the welcome panel if there are no valid flipping items in the list
				cardLayout.show(flippingItemContainer, WELCOME_PANEL);
				return;
			}
			int vGap = 4;
			cardLayout.show(flippingItemContainer, ITEMS_PANEL);
			List<FlippingItem> sortedItems = sortTradeList(flippingItems);
			List<FlippingItem> itemsThatShouldHavePanels = sortedItems.stream().filter(item -> item.getValidFlippingPanelItem()).collect(Collectors.toList());
			paginator.updateTotalPages(itemsThatShouldHavePanels.size());
			List<FlippingItem> itemsOnCurrentPage = paginator.getCurrentPageItems(itemsThatShouldHavePanels);
			List<FlippingItemPanel> newPanels = itemsOnCurrentPage.stream().map(item -> new FlippingItemPanel(plugin, itemManager.getImage(item.getItemId()), item)).collect(Collectors.toList());
			UIUtilities.stackPanelsVertically((List) newPanels, flippingItemsPanel, vGap);
			activePanels.addAll(newPanels);

			if (isItemHighlighted() && !plugin.getAccountCurrentlyViewed().equals(FlippingPlugin.ACCOUNT_WIDE)) {
				offerEditorPanel = new OfferEditorPanel(plugin);
				if (!activePanels.isEmpty()) {
					flippingItemsPanel.add(Box.createVerticalStrut(vGap));
				}
				flippingItemsPanel.add(offerEditorPanel);
			}

			if (activePanels.isEmpty() && !itemHighlighted)
			{
				cardLayout.show(flippingItemContainer, WELCOME_PANEL);
			}

			revalidate();
			repaint();

			log.info("flipping panel rebuild took {}", Duration.between(rebuildStart, Instant.now()).toMillis());
		});

	}

	public List<FlippingItem> sortTradeList(List<FlippingItem> tradeList)
	{
		List<FlippingItem> result = new ArrayList<>(tradeList);

		if (selectedSort == null || result.isEmpty())
		{
			return result;
		}

		switch (selectedSort)
		{
			case "Most Recent":
				result.sort((item1, item2) ->
				{
					if (item1 == null || item2 == null)
					{
						return -1;
					}

					return item1.getLatestActivityTime().compareTo(item2.getLatestActivityTime());
				});
				break;
			case "favorite":
				result = result.stream().filter(item -> item.isFavorite()).collect(Collectors.toList());
				break;
			case "profit":
				result.sort((item1, item2) ->
				{
					if (item1 == null || item2 == null)
					{
						return -1;
					}
					if ((item1.getLatestMarginCheckBuy().isPresent() && item1.getLatestMarginCheckSell().isPresent()) && (!item2.getLatestMarginCheckSell().isPresent() || !item2.getLatestMarginCheckBuy().isPresent()))
					{
						return -1;
					}

					if ((item2.getLatestMarginCheckBuy().isPresent() && item2.getLatestMarginCheckSell().isPresent()) && (!item1.getLatestMarginCheckSell().isPresent() || !item1.getLatestMarginCheckBuy().isPresent()))
					{
						return 1;
					}

					if ((!item2.getLatestMarginCheckBuy().isPresent() || !item2.getLatestMarginCheckSell().isPresent()) && (!item1.getLatestMarginCheckSell().isPresent() || !item1.getLatestMarginCheckBuy().isPresent()))
					{
						return 0;
					}

					boolean shouldIncludeMarginCheck = plugin.getConfig().marginCheckLoss();
					boolean shouldUseRemainingGeLimit = plugin.getConfig().geLimitProfit();
					return item2.getPotentialProfit(shouldIncludeMarginCheck, shouldUseRemainingGeLimit).orElse(0) - item1.getPotentialProfit(shouldIncludeMarginCheck, shouldUseRemainingGeLimit).orElse(0);
				});
				break;
			case "roi":
				result.sort((item1, item2) -> {
					if ((item1.getLatestMarginCheckBuy().isPresent() && item1.getLatestMarginCheckSell().isPresent()) && (!item2.getLatestMarginCheckSell().isPresent() || !item2.getLatestMarginCheckBuy().isPresent()))
					{
						return -1;
					}

					if ((item2.getLatestMarginCheckBuy().isPresent() && item2.getLatestMarginCheckSell().isPresent()) && (!item1.getLatestMarginCheckSell().isPresent() || !item1.getLatestMarginCheckBuy().isPresent()))
					{
						return 1;
					}

					if ((!item2.getLatestMarginCheckBuy().isPresent() || !item2.getLatestMarginCheckSell().isPresent()) && (!item1.getLatestMarginCheckSell().isPresent() || !item1.getLatestMarginCheckBuy().isPresent()))
					{
						return 0;
					}

					int item1ProfitEach = item1.getLatestMarginCheckSell().get().getPrice() - item1.getLatestMarginCheckBuy().get().getPrice();
					int item2ProfitEach = item2.getLatestMarginCheckSell().get().getPrice() - item2.getLatestMarginCheckBuy().get().getPrice();

					float item1roi = (float) item1ProfitEach / item1.getLatestMarginCheckBuy().get().getPrice() * 100;
					float item2roi = (float) item2ProfitEach / item2.getLatestMarginCheckBuy().get().getPrice() * 100;

					return Float.compare(item1roi, item2roi);
				});
				break;
		}
		return result;
	}

	//Clears all other items, if the item in the offer setup slot is presently available on the panel
	public void highlightItem(Optional<FlippingItem> item)
	{
		SwingUtilities.invokeLater(() -> {
			paginator.setPageNumber(1);
			itemHighlighted = true;
			if (item.isPresent()) {
				rebuild(Collections.singletonList(item.get()));
			}
			else {
				rebuild(new ArrayList<>());
			}
		});
	}

	//This is run whenever the PlayerVar containing the GE offer slot changes to its empty value (-1)
	// or if the GE is closed/history tab opened
	public void dehighlightItem()
	{
		if (!itemHighlighted)
		{
			return;
		}
		itemHighlighted = false;
		rebuild(plugin.getTradesForCurrentView());
	}

	/**
	 * Checks if a FlippingItem's margins (buy and sell price) are outdated and updates the tooltip.
	 * This method is called in FlippingPlugin every second by the scheduler.
	 */
	public void updateTimerDisplays()
	{
		for (FlippingItemPanel activePanel : activePanels)
		{
			activePanel.updateTimerDisplays();
		}
	}

	/**
	 * Searches the active item panels for matching item names.
	 */
	private void updateSearch()
	{
		String lookup = searchBar.getText().toLowerCase();

		//Just so we don't mess with the highlight.
		if (isItemHighlighted())
		{
			searchBar.setEditable(true);
			return;
		}

		//When the clear button is pressed, this is run.
		if (Strings.isNullOrEmpty(lookup))
		{
			rebuild(plugin.getTradesForCurrentView());
			return;
		}

		ArrayList<FlippingItem> result = new ArrayList<>();
		for (FlippingItem item : plugin.getTradesForCurrentView())
		{
			//Contains makes it a little more forgiving when searching.
			if (item.getItemName().toLowerCase().contains(lookup))
			{
				result.add(item);
			}
		}

		if (result.isEmpty())
		{
			searchBar.setIcon(IconTextField.Icon.ERROR);
			searchBar.setEditable(true);
			rebuild(plugin.getTradesForCurrentView());
			return;
		}

		searchBar.setIcon(IconTextField.Icon.SEARCH);
		rebuild(result);
	}

	public void refreshPricesForFlippingItemPanel(int itemId) {
		for (FlippingItemPanel panel:activePanels) {
			if (panel.getFlippingItem().getItemId() == itemId) {
				panel.refreshProperties();
			}
		}
	}
}
