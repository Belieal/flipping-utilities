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

import com.google.common.base.Strings;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.ImageUtil;

@Slf4j
public class FlippingPanel extends PluginPanel
{
	@Getter
	private static final String WELCOME_PANEL = "WELCOME_PANEL";
	private static final String ITEMS_PANEL = "ITEMS_PANEL";
	private static final int DEBOUNCE_DELAY_MS = 250;
	private static final ImageIcon RESET_ICON;
	private static final ImageIcon RESET_HOVER_ICON;
	private static final Dimension ICON_SIZE = new Dimension(32, 32);
	private static final Border TOP_PANEL_BORDER = new CompoundBorder(
		BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.BRAND_ORANGE),
		BorderFactory.createEmptyBorder());

	static
	{
		final BufferedImage resetIcon = ImageUtil
			.getResourceStreamFromClass(FlippingPlugin.class, "/reset.png");
		RESET_ICON = new ImageIcon(resetIcon);
		RESET_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(resetIcon, 0.53f));
	}

	@Inject
	private ClientThread clientThread;
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

	//So we can keep track what items are shown on the panel.
	private ArrayList<FlippingItemPanel> activePanels = new ArrayList<>();


	@Inject
	public FlippingPanel(final FlippingPlugin plugin, final ItemManager itemManager, ClientThread clientThread, ScheduledExecutorService executor)
	{
		super(false);

		this.plugin = plugin;
		this.itemManager = itemManager;
		this.clientThread = clientThread;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1;
		constraints.gridx = 0;
		constraints.gridy = 0;

		//Contains the main results panel and top panel
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

		//Title at the top of the plugin panel.
		JLabel title = new JLabel("Flipping Utilities", SwingConstants.CENTER);
		title.setForeground(Color.WHITE);

		//Search bar beneath the title.
		searchBar.setIcon(IconTextField.Icon.SEARCH);
		searchBar.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
		searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchBar.setBorder(BorderFactory.createMatteBorder(0, 5, 5, 5, ColorScheme.DARKER_GRAY_COLOR.darker()));
		searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchBar.setMinimumSize(new Dimension(0, 35));
		searchBar.addActionListener(e -> executor.execute(this::updateSearch));
		searchBar.addClearListener(e -> updateSearch());
		searchBar.addKeyListener(key ->
		{
			if (runningRequest != null)
			{
				runningRequest.cancel(false);
			}
			runningRequest = executor.schedule((Runnable) this::updateSearch, DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);
		});

		//Contains a greeting message when the items panel is empty.
		JPanel welcomeWrapper = new JPanel(new BorderLayout());
		welcomeWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		PluginErrorPanel welcomePanel = new PluginErrorPanel();
		welcomeWrapper.add(welcomePanel, BorderLayout.NORTH);

		welcomePanel.setContent("Flipping Utilities",
			"For items to show up, margin check an item.");

		//Clears the config and resets the items panel.
		final JLabel resetIcon = new JLabel(RESET_ICON);
		resetIcon.setToolTipText("Reset trade history");
		resetIcon.setPreferredSize(ICON_SIZE);
		resetIcon.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e))
				{
					plugin.resetTradeHistory();
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

		//Top panel that holds the plugin title and reset button.
		final JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		topPanel.add(new Box.Filler(ICON_SIZE, ICON_SIZE, ICON_SIZE), BorderLayout.WEST);
		topPanel.add(title, BorderLayout.CENTER);
		topPanel.add(resetIcon, BorderLayout.EAST);
		topPanel.add(searchBar, BorderLayout.SOUTH);
		topPanel.setBorder(TOP_PANEL_BORDER);

		centerPanel.add(scrollWrapper, ITEMS_PANEL);
		centerPanel.add(welcomeWrapper, WELCOME_PANEL);

		//To switch between greeting and items panels
		cardLayout.show(centerPanel, WELCOME_PANEL);

		container.add(topPanel, BorderLayout.NORTH);
		container.add(centerPanel, BorderLayout.CENTER);

		add(container, BorderLayout.CENTER);
	}

	private void initializeFlippingPanel(ArrayList<FlippingItem> flippingItems)
	{
		if (flippingItems == null)
		{
			return;
		}
		//Reset active panel list.
		activePanels.clear();

		if (flippingItems.size() == 0)
		{
			cardLayout.show(centerPanel, WELCOME_PANEL);
		}
		else
		{
			cardLayout.show(centerPanel, ITEMS_PANEL);
		}


		SwingUtilities.invokeLater(() ->
		{
			int index = 0;
			for (FlippingItem item : flippingItems)
			{
				FlippingItemPanel newPanel = new FlippingItemPanel(plugin, itemManager, item);
				activePanels.add(newPanel);
				newPanel.clearButton.addMouseListener(new MouseAdapter()
				{
					@Override
					public void mouseClicked(MouseEvent e)
					{
						if (e.getButton() == MouseEvent.BUTTON1)
						{
							deletePanel(newPanel);
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
			}
		});
	}

	public void rebuildFlippingPanel(ArrayList<FlippingItem> flippingItems)
	{
		flippingItemsPanel.removeAll();
		if (flippingItems == null)
		{
			return;
		}
		else
		{
			initializeFlippingPanel(flippingItems);
		}

		revalidate();
		repaint();
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
			rebuildFlippingPanel(itemToHighlight);
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

		rebuildFlippingPanel(plugin.getTradesList());
		itemHighlighted = false;
		plugin.setPrevHighlight(0);
	}

	public ArrayList<FlippingItem> findItemPanel(int itemId)
	{
		ArrayList<FlippingItem> result = new ArrayList<>();

		for (FlippingItem item : plugin.getTradesList())
		{
			if (item.getItemId() == itemId)
			{
				result.add(item);
				//We only expect one result
				break;
			}
		}

		return result;
	}

	//Updates tooltips on prices to show how long ago the latest margin check was.
	public void updateTimes()
	{
		for (FlippingItemPanel activePanel : activePanels)
		{
			activePanel.checkOutdatedPriceTimes();
		}
	}

	public void updateGELimit()
	{
		SwingUtilities.invokeLater(() ->
		{
			for (FlippingItemPanel activePanel : activePanels)
			{
				activePanel.updateGELimits();
			}
		});
	}

	public void deletePanel(FlippingItemPanel itemPanel)
	{
		if (!activePanels.contains(itemPanel))
		{
			return;
		}
		ArrayList<FlippingItem> tradeList = plugin.getTradesList();
		tradeList.remove(itemPanel.getFlippingItem());

		rebuildFlippingPanel(tradeList);
		plugin.updateConfig();
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
			rebuildFlippingPanel(plugin.getTradesList());
			return;
		}

		ArrayList<FlippingItem> result = new ArrayList<>();
		for (FlippingItem item : plugin.getTradesList())
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
			rebuildFlippingPanel(plugin.getTradesList());
			return;
		}

		searchBar.setIcon(IconTextField.Icon.SEARCH);
		rebuildFlippingPanel(result);
	}
}
