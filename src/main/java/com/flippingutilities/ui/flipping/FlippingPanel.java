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

import com.flippingutilities.FlippingItem;
import com.flippingutilities.FlippingPlugin;
import com.flippingutilities.HistoryManager;
import static com.flippingutilities.ui.utilities.UIUtilities.ICON_SIZE;
import static com.flippingutilities.ui.utilities.UIUtilities.RESET_HOVER_ICON;
import static com.flippingutilities.ui.utilities.UIUtilities.RESET_ICON;
import com.google.common.base.Strings;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
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
		BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.BRAND_ORANGE),
		BorderFactory.createEmptyBorder(4, 0, 0, 0));

	private final FlippingPlugin plugin;
	private final ItemManager itemManager;

	//Main item panel that holds all the shown items.
	private final JPanel flippingItemsPanel = new JPanel();

	private final IconTextField searchBar = new IconTextField();
	private Future<?> runningRequest = null;

	//Constraints for items in the item panel.
	private final GridBagConstraints constraints = new GridBagConstraints();
	public final CardLayout cardLayout = new CardLayout();

	@Getter
	public final JPanel centerPanel = new JPanel(cardLayout);

	//Keeps track of all items currently displayed on the panel.
	private ArrayList<FlippingItemPanel> activePanels = new ArrayList<>();

	@Getter
	JLabel resetIcon;

	public FlippingPanel(final FlippingPlugin plugin, final ItemManager itemManager, ScheduledExecutorService executor)
	{
		super(false);

		this.plugin = plugin;
		this.itemManager = itemManager;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1;
		constraints.gridx = 0;
		constraints.gridy = 0;

		//Contains the main content panel and top panel
		JPanel container = new JPanel();
		container.setLayout(new BorderLayout(0, 5));
		container.setBorder(new EmptyBorder(0, 0, 5, 0));
		container.setBackground(ColorScheme.DARK_GRAY_COLOR);

		//Holds all the item panels
		flippingItemsPanel.setLayout(new GridBagLayout());
		flippingItemsPanel.setBorder((new EmptyBorder(0, 5, 0, 3)));
		flippingItemsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.add(flippingItemsPanel, BorderLayout.NORTH);

		JScrollPane scrollWrapper = new JScrollPane(wrapper);
		scrollWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollWrapper.getVerticalScrollBar().setPreferredSize(new Dimension(5, 0));
		scrollWrapper.getVerticalScrollBar().setBorder(new EmptyBorder(0, 0, 0, 0));

		//Search bar beneath the tab manager.
		searchBar.setIcon(IconTextField.Icon.SEARCH);
		searchBar.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
		searchBar.setBackground(ColorScheme.DARK_GRAY_COLOR);
		searchBar.setBorder(BorderFactory.createMatteBorder(0, 5, 5, 5, ColorScheme.DARKER_GRAY_COLOR.darker()));
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

		welcomePanel.setContent("Flipping Utilities",
			"For items to show up, margin check an item.");

		//Clears the config and resets the items panel.
		resetIcon = new JLabel(RESET_ICON);
		resetIcon.setToolTipText("Reset trade history");
		resetIcon.setPreferredSize(ICON_SIZE);
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
						null, new String[] {"Yes", "No"}, "No");

					//If the user pressed "Yes"
					if (result == JOptionPane.YES_OPTION)
					{
						resetPanel();
						cardLayout.show(centerPanel, FlippingPanel.getWELCOME_PANEL());
						rebuild(plugin.getTradesForCurrentView());
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				resetIcon.setIcon(RESET_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				resetIcon.setIcon(RESET_ICON);
			}
		});

		final JMenuItem clearMenuOption = new JMenuItem("Reset all panels");
		clearMenuOption.addActionListener(e ->
		{
			resetPanel();
			plugin.getStatPanel().resetPanel();
			rebuild(plugin.getTradesForCurrentView());
			plugin.getStatPanel().rebuild(plugin.getTradesForCurrentView());
		});

		final JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.setBorder(new EmptyBorder(5, 5, 5, 5));
		popupMenu.add(clearMenuOption);

		resetIcon.setComponentPopupMenu(popupMenu);

		//Top panel that holds the plugin title and reset button.
		final JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		topPanel.add(resetIcon, BorderLayout.EAST);
		topPanel.add(searchBar, BorderLayout.CENTER);
		topPanel.setBorder(TOP_PANEL_BORDER);

		centerPanel.add(scrollWrapper, ITEMS_PANEL);
		centerPanel.add(welcomeWrapper, WELCOME_PANEL);

		//To switch between greeting and items panels
		cardLayout.show(centerPanel, WELCOME_PANEL);
		container.add(topPanel, BorderLayout.NORTH);
		container.add(centerPanel, BorderLayout.CENTER);

		add(container, BorderLayout.CENTER);

	}

	public void rebuild(List<FlippingItem> flippingItems)
	{
		//Reset active panel list.
		activePanels.clear();

		SwingUtilities.invokeLater(() ->
		{
			flippingItemsPanel.removeAll();

			if (flippingItems == null || flippingItems.size() == 0)
			{
				cardLayout.show(centerPanel, WELCOME_PANEL);
				return;
			}

			cardLayout.show(centerPanel, ITEMS_PANEL);

			int index = 0;
			for (FlippingItem item : flippingItems)
			{
				if (!item.isValidFlippingPanelItem())
				{
					continue;
				}

				FlippingItemPanel newPanel = new FlippingItemPanel(plugin, itemManager, item);

				newPanel.clearButton.addMouseListener(new MouseAdapter()
				{
					@Override
					public void mouseClicked(MouseEvent e)
					{
						if (e.getButton() == MouseEvent.BUTTON1)
						{
							deleteItemPanel(newPanel);
							rebuild(plugin.getTradesForCurrentView());
						}
					}
				});

				if (index++ > 0)
				{
					JPanel marginWrapper = new JPanel(new BorderLayout());
					marginWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
					marginWrapper.setBorder(new EmptyBorder(4, 0, 0, 0));
					marginWrapper.add(newPanel, BorderLayout.NORTH);
					flippingItemsPanel.add(marginWrapper, constraints);
				}
				else
				{
					flippingItemsPanel.add(newPanel, constraints);
				}
				constraints.gridy++;
				activePanels.add(newPanel);
			}

			if (activePanels.isEmpty())
			{
				cardLayout.show(centerPanel, WELCOME_PANEL);
			}

			revalidate();
			repaint();
		});
	}

	@Getter
	@Setter
	private boolean itemHighlighted = false;

	//Clears all other items, if the item in the offer setup slot is presently available on the panel
	public void highlightItem(int itemId)
	{
		if (itemHighlighted)
		{
			return;
		}

		ArrayList<FlippingItem> itemToHighlight = new ArrayList<>(findItemPanel(itemId));

		if (!itemToHighlight.isEmpty())
		{
			rebuild(itemToHighlight);
			itemHighlighted = true;
		}
	}

	//This is run whenever the PlayerVar containing the GE offer slot changes to its empty value (-1)
	// or if the GE is closed.
	public void dehighlightItem()
	{
		if (!itemHighlighted)
		{
			return;
		}

		rebuild(plugin.getTradesForCurrentView());
		itemHighlighted = false;
		plugin.setPrevHighlight(0);
	}

	public ArrayList<FlippingItem> findItemPanel(int itemId)
	{
		//We only expect one item.
		return plugin.getTradesForCurrentView().stream()
			.filter(item -> item.getItemId() == itemId && item.isValidFlippingPanelItem())
			.collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Checks if a FlippingItem's margins (buy and sell price) are outdated and updates the tooltip.
	 * This method is called in FlippingPLugin every second by the scheduler.
	 */
	public void updateActivePanelsPriceOutdatedDisplay()
	{
		for (FlippingItemPanel activePanel : activePanels)
		{
			activePanel.updatePriceOutdatedDisplay();
		}
	}

	/**
	 * uses the properties of the FlippingItem to show the ge limit and refresh time display. This is invoked
	 * in the FlippingPlugin in two places:
	 * <p>
	 * 1. Everytime an offer comes in (in onGrandExchangeOfferChanged) and the user
	 * is currently looking at either the account wide trade list or trades list of the account currently
	 * logged in
	 * <p>
	 * 2. In a background thread every second, as initiated in the startUp() method of the FlippingPlugin.
	 */
	public void updateActivePanelsGePropertiesDisplay()
	{
		for (FlippingItemPanel activePanel : activePanels)
		{
			activePanel.updateGePropertiesDisplay();
		}
	}

	private void deleteItemPanel(FlippingItemPanel itemPanel)
	{
		if (!activePanels.contains(itemPanel))
		{
			return;
		}

		//itemPanel.getFlippingItem().invalidateOffers(HistoryManager.PanelSelection.FLIPPING);
		FlippingItem item = itemPanel.getFlippingItem();
		item.setValidFlippingPanelItem(false);
	}

	public void resetPanel()
	{
		for (FlippingItemPanel itemPanel : activePanels)
		{
			deleteItemPanel(itemPanel);
		}

		plugin.truncateTradeList();
		setItemHighlighted(false);
	}

	//Searches the active item panels for matching item names.
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

}
