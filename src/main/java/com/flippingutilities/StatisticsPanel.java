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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledExecutorService;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.text.StyleContext;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.ComboBoxListRenderer;
import net.runelite.client.util.QuantityFormatter;

public class StatisticsPanel extends JPanel
{
	private static final String[] TIME_INTERVAL_STRINGS = {"Past Hour", "Past Day", "Past Week", "Past Month", "Session", "All"};
	private static final String[] SORT_BY_STRINGS = {"Most Recent", "Most Profit Total", "Most Profit Each", "Highest ROI", "Highest Quantity"};
	private static final Border TOP_PANEL_BORDER = new CompoundBorder(
		BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.BRAND_ORANGE),
		BorderFactory.createEmptyBorder(4, 6, 2, 6));

	private static final Font BIG_PROFIT_FONT = StyleContext.getDefaultStyleContext()
		.getFont(FontManager.getRunescapeBoldFont().getName(), Font.PLAIN, 28);

	private FlippingPlugin plugin;

	//Holds the buttons that control time intervals
	private JPanel topPanel = new JPanel(new BorderLayout());
	//Wraps the total profit labels.
	private JPanel profitWrapper = new JPanel(new BorderLayout());
	//Holds all the main content of the panel.
	private JPanel contentWrapper = new JPanel(new BorderLayout());
	//Represents the total profit made in the selected time interval.
	private JLabel totalProfitLabel = new JLabel();

	private HistoryManager historyManager = new HistoryManager();

	//Contains the unix time of the start of the interval.
	private Instant startOfInterval = Instant.EPOCH;

	/**
	 * The statistics panel shows various stats about trades the user has made over a selectable time interval.
	 * This represents the front-end Statistics Tab.
	 * It is shown when it has been selected by the tab manager.
	 *
	 * @param plugin      Used to access the config and list of trades.
	 * @param itemManager Accesses the RuneLite item cache.
	 * @param executor    For repeated method calls, required by periodic update methods.
	 */
	public StatisticsPanel(final FlippingPlugin plugin, final ItemManager itemManager, ScheduledExecutorService executor)
	{
		super(false);

		this.plugin = plugin;

		setLayout(new BorderLayout());

		//Combo box that selects the time interval that startOfInterval contains.
		JComboBox<String> timeIntervalList = new JComboBox<>(TIME_INTERVAL_STRINGS);
		timeIntervalList.setSelectedItem("All");
		timeIntervalList.setRenderer(new ComboBoxListRenderer());
		timeIntervalList.setMinimumSize(new Dimension(0, 35));
		timeIntervalList.setFocusable(false);
		timeIntervalList.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		timeIntervalList.addActionListener(event ->
		{
			String selectedInterval = (String) timeIntervalList.getSelectedItem();

			if (selectedInterval == null)
			{
				return;
			}
			setTimeInterval(selectedInterval);
			updateProfits();
		});

		//Holds the time interval selector beneath the tab manager.
		topPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		topPanel.setBorder(TOP_PANEL_BORDER);
		topPanel.add(new JLabel("Time Interval: "), BorderLayout.WEST);
		topPanel.add(timeIntervalList, BorderLayout.CENTER);

		//Title text for the big total profit label.
		final JLabel profitText = new JLabel("Total profit: ");
		profitText.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);

		//Profit total over the selected time interval
		totalProfitLabel.setFont(BIG_PROFIT_FONT);

		//Contains the main profit information.
		JPanel profitContainer = new JPanel(new GridBagLayout());
		profitContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		profitContainer.setBorder(new EmptyBorder(2, 5, 5, 5));

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.REMAINDER;
		constraints.weightx = 1;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.insets = new Insets(0, 0, 8, 0);

		profitContainer.add(profitText, constraints);

		/* Subinfo labels */
		constraints.gridy = 1;
		profitContainer.add(totalProfitLabel, constraints);

		final JLabel profitPerHourLabel = new JLabel("Hourly profit: ");
		final JLabel roiLabel = new JLabel("Return on investment: ");
		final JLabel tradesMadeLabel = new JLabel("Trades made: ");
		final JLabel profitPerTradeLabel = new JLabel("Avg. Profit/trade: ");

		profitPerHourLabel.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);
		roiLabel.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);
		tradesMadeLabel.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);
		profitPerTradeLabel.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);

		constraints.gridy = 2;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 0.5;
		constraints.insets = new Insets(5, 0, 0, 0);
		profitContainer.add(profitPerHourLabel, constraints);
		constraints.gridy = 3;
		profitContainer.add(roiLabel, constraints);
		constraints.gridy = 4;
		profitContainer.add(tradesMadeLabel, constraints);
		constraints.gridy = 5;
		profitContainer.add(profitPerTradeLabel, constraints);

		profitWrapper.add(profitContainer, BorderLayout.NORTH);
		profitWrapper.setBorder(new EmptyBorder(5, 5, 5, 5));

		JPanel sortPanel = new JPanel(new BorderLayout());
		sortPanel.add(new JLabel("Sort by: "), BorderLayout.WEST);

		JComboBox<String> sortBox = new JComboBox<>(SORT_BY_STRINGS);
		sortBox.setSelectedItem("Most Recent");
		sortBox.setRenderer(new ComboBoxListRenderer());
		sortBox.setMinimumSize(new Dimension(0, 35));
		sortBox.setFocusable(false);
		sortBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		sortBox.addActionListener(event ->
		{
			String selectedSort = (String) sortBox.getSelectedItem();

			if (selectedSort == null)
			{
				return;
			}

			setSortBy(selectedSort);
		});

		sortPanel.add(sortBox, BorderLayout.CENTER);
		sortPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		sortPanel.setBorder(new EmptyBorder(10, 5, 10, 5));

		JPanel itemContainer = new JPanel(new BorderLayout());
		itemContainer.add(sortPanel, BorderLayout.NORTH);

		contentWrapper.add(profitWrapper, BorderLayout.NORTH);
		contentWrapper.add(itemContainer, BorderLayout.CENTER);

		add(contentWrapper, BorderLayout.CENTER);
		add(topPanel, BorderLayout.NORTH);

	}

	/**
	 * Updates all profit labels on the stat panel using their respective update methods.
	 * Gets called on startup, after the tradesList has been initialized and after every new registered trade.
	 */
	//New trade registered, update the profit labels and add/update profit item.
	public void updateProfits()
	{
		updateTotalProfit();
	}

	/**
	 * Responsible for updating the total profit label at the very top.
	 * Sets the new total profit value from the items in tradesList from {@link FlippingPlugin}.
	 */
	//TODO: As QuantityFormatter.quantityToRSDecimalStack() doesn't take longs as parameter,
	// a new format method is needed that support longs.
	private void updateTotalProfit()
	{
		if (plugin.getTradesList() == null)
		{
			totalProfitLabel.setText("0");
			totalProfitLabel.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
			totalProfitLabel.setToolTipText("Total Profit: 0 gp");
			return;
		}

		SwingUtilities.invokeLater(() ->
		{
			long totalProfit = 0;

			for (FlippingItem item : plugin.getTradesList())
			{
				totalProfit += item.currentProfit(startOfInterval);
			}

			totalProfitLabel.setText(QuantityFormatter.quantityToRSDecimalStack((int) totalProfit, true) + " gp");
			totalProfitLabel.setToolTipText("Total Profit: " + QuantityFormatter.formatNumber(totalProfit) + " gp");
			totalProfitLabel.setForeground(totalProfit >= 0 ? ColorScheme.GRAND_EXCHANGE_PRICE : ColorScheme.PROGRESS_ERROR_COLOR);
		});
	}

	/**
	 * Gets called every time the time interval combobox has its selection changed.
	 * Sets the start interval of the profit calculation.
	 *
	 * @param selectedInterval The string from TIME_INTERVAL_STRINGS that is selected in the time interval combobox
	 */
	private void setTimeInterval(String selectedInterval)
	{
		Instant timeNow = Instant.now();
		switch (selectedInterval)
		{
			case "Past Hour":
				startOfInterval = timeNow.minus(1, ChronoUnit.HOURS);
				break;
			case "Past Day":
				startOfInterval = timeNow.minus(1, ChronoUnit.DAYS);
				break;
			//Apparently Instant doesn't support weeks and months.
			case "Past Week":
				startOfInterval = timeNow.minus(7, ChronoUnit.DAYS);
				break;
			case "Past Month":
				startOfInterval = timeNow.minus(30, ChronoUnit.DAYS);
				break;
			case "Session":
				//Use session time.
				break;
			case "All":
				startOfInterval = Instant.EPOCH;
				break;
			default:
				break;
		}
	}

	/**
	 * Gets called every time the sort by combobox has its selection changed.
	 * Sorts the profit items according to the selected string.
	 *
	 * @param selectedSort The string from SORT_BY_STRINGS that is selected in the sort by combobox
	 */
	//TODO: Hook this up
	private void setSortBy(String selectedSort)
	{
		switch (selectedSort)
		{
			case "Most Recent":
				break;
			case "Most Profit Total":
				break;
			case "Most Profit Each":
				break;
			case "Highest ROI":
				break;
			case "Highest Quantity":
				break;
		}
	}
}
