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

package com.flippingutilities.ui.statistics;

import com.flippingutilities.FlippingItem;
import com.flippingutilities.FlippingPlugin;
import com.flippingutilities.HistoryManager;
import com.flippingutilities.OfferEvent;
import com.flippingutilities.ui.utilities.Paginator;
import com.flippingutilities.ui.utilities.UIUtilities;
import static com.flippingutilities.ui.utilities.UIUtilities.RESET_HOVER_ICON;
import static com.flippingutilities.ui.utilities.UIUtilities.RESET_ICON;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.text.StyleContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.ComboBoxListRenderer;
import net.runelite.client.util.QuantityFormatter;

@Slf4j
public class StatsPanel extends JPanel
{
	private static final String[] TIME_INTERVAL_STRINGS = {"Past Hour", "Past 4 Hours", "Past 12 Hours", "Past Day", "Past Week", "Past Month", "Session", "All"};
	private static final String[] SORT_BY_STRINGS = {"Most Recent", "Most Total Profit", "Most Profit Each", "Highest ROI", "Highest Quantity"};
	private static final Dimension ICON_SIZE = new Dimension(16, 16);

	private static final Border TOP_PANEL_BORDER = new CompoundBorder(
		BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.BRAND_ORANGE),
		BorderFactory.createEmptyBorder(5, 2, 1, 8));

	private static final Border TOTAL_PROFIT_CONTAINER_BORDER = new CompoundBorder(
		BorderFactory.createMatteBorder(0, 0, 2, 0, ColorScheme.LIGHT_GRAY_COLOR),
		BorderFactory.createEmptyBorder(2, 5, 5, 5));

	private static final Font BIG_PROFIT_FONT = StyleContext.getDefaultStyleContext()
		.getFont(FontManager.getRunescapeBoldFont().getName(), Font.PLAIN, 28);

	private FlippingPlugin plugin;
	private ItemManager itemManager;

	//Holds the buttons that control time intervals
	private JPanel topPanel = new JPanel(new BorderLayout());

	//Holds the sub info labels.
	private JPanel subInfoContainer = new JPanel();

	private JPanel statItemContainer = new JPanel(new GridBagLayout());

	//Constraints for statItemContainer.
	private final GridBagConstraints constraints = new GridBagConstraints();

	//Combo box that selects the time interval that startOfInterval contains.
	private JComboBox<String> timeIntervalDropdown = new JComboBox<>(TIME_INTERVAL_STRINGS);

	//Sorting selector
	private JComboBox<String> sortBox = new JComboBox<>(SORT_BY_STRINGS);

	//Represents the total profit made in the selected time interval.
	private JLabel totalProfitVal = new JLabel();

	//Sets the visible state for subinfo
	private JLabel arrowIcon = new JLabel(UIUtilities.OPEN_ICON);

	/* Subinfo text labels */
	private final JLabel hourlyProfitText = new JLabel("Hourly Profit: ");
	private final JLabel roiText = new JLabel("ROI: ");
	private final JLabel totalRevenueText = new JLabel("Total Revenue: ");
	private final JLabel totalExpenseText = new JLabel("Total Expense: ");
	private final JLabel totalQuantityText = new JLabel("Total Items Flipped: ");
	private final JLabel totalFlipsText = new JLabel("Total Flips Made: ");
	private final JLabel mostCommonFlipText = new JLabel("Most Common: ");
	private final JLabel sessionTimeText = new JLabel("Session Time: ");

	private final JLabel[] textLabelArray = {hourlyProfitText, roiText, totalRevenueText, totalExpenseText, totalQuantityText, totalFlipsText, mostCommonFlipText, sessionTimeText};

	/* Subinfo value labels */
	private final JLabel hourlyProfitVal = new JLabel("", SwingConstants.RIGHT);
	private final JLabel roiVal = new JLabel("", SwingConstants.RIGHT);
	private final JLabel totalRevenueVal = new JLabel("", SwingConstants.RIGHT);
	private final JLabel totalExpenseVal = new JLabel("", SwingConstants.RIGHT);
	private final JLabel totalQuantityVal = new JLabel("", SwingConstants.RIGHT);
	private final JLabel totalFlipsVal = new JLabel("", SwingConstants.RIGHT);
	private final JLabel mostCommonFlipVal = new JLabel("", SwingConstants.RIGHT);
	private final JLabel sessionTimeVal = new JLabel("", SwingConstants.RIGHT);

	private final JLabel[] valLabelArray = {hourlyProfitVal, roiVal, totalRevenueVal, totalExpenseVal, totalQuantityVal, totalFlipsVal, mostCommonFlipVal, sessionTimeVal};

	private final JPanel hourlyProfitPanel = new JPanel(new BorderLayout());
	private final JPanel roiPanel = new JPanel(new BorderLayout());
	private final JPanel totalRevenuePanel = new JPanel(new BorderLayout());
	private final JPanel totalExpensePanel = new JPanel(new BorderLayout());
	private final JPanel totalQuantityPanel = new JPanel(new BorderLayout());
	private final JPanel totalFlipsPanel = new JPanel(new BorderLayout());
	private final JPanel mostCommonFlipPanel = new JPanel(new BorderLayout());
	private final JPanel sessionTimePanel = new JPanel(new BorderLayout());

	private final JPanel[] subInfoPanelArray = {hourlyProfitPanel, roiPanel, totalRevenuePanel, totalExpensePanel, totalQuantityPanel, totalFlipsPanel, mostCommonFlipPanel, sessionTimePanel};

	//Data acquired from history manager of all items
	private long totalProfit;
	private long totalExpenses;
	private long totalRevenues;
	private long totalQuantity;
	private int totalFlips;
	private String mostCommonItemName;
	private int mostFlips;

	//Contains the unix time of the start of the interval.
	@Getter
	private Instant startOfInterval = Instant.now();

	@Getter
	private String selectedSort;

	private ArrayList<StatItemPanel> activePanels = new ArrayList<>();

	@Getter
	JLabel resetIcon;

	@Getter
	private Set<String> expandedItems = new HashSet<>();
	@Getter
	private Set<String> expandedTradeHistories = new HashSet<>();

	private Paginator paginator;

	/**
	 * The statistics panel shows various stats about trades the user has made over a selectable time interval.
	 * This represents the front-end Statistics Tab.
	 * It is shown when it has been selected by the tab manager.
	 *
	 * @param plugin      Used to access the config and list of trades.
	 * @param itemManager Accesses the RuneLite item cache.
	 */
	public StatsPanel(final FlippingPlugin plugin, final ItemManager itemManager)
	{
		super(false);

		this.plugin = plugin;
		this.itemManager = itemManager;

		setLayout(new BorderLayout());

		//Constraints for statItems later on.
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1;
		constraints.gridx = 0;
		constraints.gridy = 0;

		timeIntervalDropdown.setRenderer(new ComboBoxListRenderer());
		timeIntervalDropdown.setFocusable(false);
		timeIntervalDropdown.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		timeIntervalDropdown.addItemListener(event ->
		{
			if (event.getStateChange() == ItemEvent.SELECTED)
			{
				String interval = (String) event.getItem();
				setTimeInterval(interval);
			}
		});
		timeIntervalDropdown.setToolTipText("Specify the time span you would like to see the statistics of");

		//Icon that resets all the panels currently shown in the time span.
		resetIcon = new JLabel(RESET_ICON);
		resetIcon.setToolTipText("Reset Statistics");
		resetIcon.setPreferredSize(ICON_SIZE);
		resetIcon.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e))
				{
					//Display warning message
					final int result = JOptionPane.showOptionDialog(resetIcon, "<html>Are you sure you want to reset the statistics?" +
							"<br>This only resets the statistics within the time span</html>",
						"Are you sure?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
						null, new String[] {"Yes", "No"}, "No");

					//If the user pressed "Yes"
					if (result == JOptionPane.YES_OPTION)
					{
						resetPanel();
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

		//Adds option to completely reset the trade history.
		final JMenuItem clearMenuOption = new JMenuItem("Reset all panels");
		clearMenuOption.addActionListener(e ->
		{
			resetPanel();
			plugin.getFlippingPanel().resetPanel();
			rebuild(plugin.getTradesForCurrentView());
			plugin.getFlippingPanel().rebuild(plugin.getTradesForCurrentView());
		});

		final JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.setBorder(new EmptyBorder(5, 5, 5, 5));
		popupMenu.add(clearMenuOption);

		resetIcon.setComponentPopupMenu(popupMenu);

		//Since the combobox can't be resized using setBorder, we have to use a wrapper.
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBorder(new EmptyBorder(0, 2, 0, 11));
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		wrapper.add(timeIntervalDropdown, BorderLayout.CENTER);

		//Holds the time interval selector beneath the tab manager.
		topPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		topPanel.setBorder(TOP_PANEL_BORDER);
		topPanel.add(new JLabel("Time Span:"), BorderLayout.WEST);
		topPanel.add(wrapper, BorderLayout.CENTER);
		topPanel.add(resetIcon, BorderLayout.EAST);

		//Title text for the big total profit label.
		final JLabel profitText = new JLabel("Total Profit: ", SwingConstants.CENTER);
		profitText.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);
		profitText.setFont(FontManager.getRunescapeBoldFont());

		//Profit total over the selected time interval
		totalProfitVal.setFont(BIG_PROFIT_FONT);
		totalProfitVal.setHorizontalAlignment(SwingConstants.CENTER);
		totalProfitVal.setToolTipText("");

		arrowIcon.setPreferredSize(ICON_SIZE);

		//Make sure the profit label is centered
		JLabel padLabel = new JLabel();
		padLabel.setPreferredSize(ICON_SIZE);

		//Formats the profit text and value.
		JPanel profitTextAndVal = new JPanel(new BorderLayout());
		profitTextAndVal.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		profitTextAndVal.add(totalProfitVal, BorderLayout.CENTER);
		profitTextAndVal.add(profitText, BorderLayout.NORTH);

		//Contains the total profit information.
		JPanel totalProfitContainer = new JPanel(new BorderLayout());
		totalProfitContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		totalProfitContainer.setBorder(TOTAL_PROFIT_CONTAINER_BORDER);

		//totalProfitContainer.add(, BorderLayout.NORTH);
		totalProfitContainer.add(profitTextAndVal, BorderLayout.CENTER);
		totalProfitContainer.add(arrowIcon, BorderLayout.EAST);
		totalProfitContainer.add(padLabel, BorderLayout.WEST);

		//Controls the collapsible sub info function
		MouseAdapter collapseOnClick = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e))
				{
					if (subInfoContainer.isVisible())
					{
						//Collapse sub info
						arrowIcon.setIcon(UIUtilities.CLOSE_ICON);
						subInfoContainer.setVisible(false);
					}
					else
					{
						//Expand sub info
						arrowIcon.setIcon(UIUtilities.OPEN_ICON);
						subInfoContainer.setVisible(true);
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				totalProfitContainer.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
				profitTextAndVal.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				totalProfitContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
				profitTextAndVal.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
			}
		};

		//Since the totalProfitVal's tooltip consumes the mouse event
		totalProfitContainer.addMouseListener(collapseOnClick);
		totalProfitVal.addMouseListener(collapseOnClick);

		/* Subinfo represents the less-used general historical stats */
		subInfoContainer.setLayout(new DynamicGridLayout(subInfoPanelArray.length, 1));

		//All labels should already be sorted in their arrays.
		for (int i = 0; i < subInfoPanelArray.length; i++)
		{
			textLabelArray[i].setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);

			subInfoPanelArray[i].add(textLabelArray[i], BorderLayout.WEST);
			subInfoPanelArray[i].add(valLabelArray[i], BorderLayout.EAST);
		}

		subInfoContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		subInfoContainer.setBorder(new EmptyBorder(9, 5, 5, 5));

		//To ensure the item's name won't wrap the whole panel.
		mostCommonFlipVal.setMaximumSize(new Dimension(145, 0));

		sessionTimePanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isRightMouseButton(e))
				{
					//Display warning message
					final int result = JOptionPane.showOptionDialog(resetIcon, "Are you sure you want to reset the session time?",
						"Are you sure?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
						null, new String[] {"Yes", "No"}, "No");

					//If the user pressed "Yes"
					if (result == JOptionPane.YES_OPTION)
					{
						plugin.handleSessionTimeReset();
						rebuild(plugin.getTradesForCurrentView());
					}
				}
			}
		});

		sessionTimeVal.setText(UIUtilities.formatDuration(plugin.getAccumulatedTimeForCurrentView()));
		sessionTimeVal.setPreferredSize(new Dimension(200, 0));
		sessionTimeVal.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);
		sessionTimePanel.setToolTipText("Right-click to reset session timer");

		//Wraps the total profit labels.
		JPanel totalProfitWrapper = new JPanel(new BorderLayout());
		totalProfitWrapper.add(totalProfitContainer, BorderLayout.NORTH);
		totalProfitWrapper.add(subInfoContainer, BorderLayout.SOUTH);
		totalProfitWrapper.setBorder(new EmptyBorder(5, 5, 5, 5));

		//Holds all the main content of the panel.
		JPanel contentWrapper = new JPanel(new BorderLayout());
		contentWrapper.add(totalProfitWrapper, BorderLayout.NORTH);

		/* The following represents the formatting behind the StatItems that appear at the bottom of the page.
		 These are designed similarly to the FlippingItemPanels and contains information about individual flips. */
		/* Sorting selector */
		JPanel sortPanel = new JPanel(new BorderLayout());

		JLabel sortLabel = new JLabel("Sort by: ");

		sortBox.setSelectedItem("Most Recent");
		sortBox.setRenderer(new ComboBoxListRenderer());
		sortBox.setMinimumSize(new Dimension(0, 35));
		sortBox.setFocusable(false);
		sortBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		sortBox.addActionListener(event ->
		{
			selectedSort = (String) sortBox.getSelectedItem();

			if (selectedSort == null)
			{
				return;
			}
			rebuild(plugin.getTradesForCurrentView());
		});

		sortPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		sortPanel.setBorder(new EmptyBorder(10, 5, 2, 5));

		sortPanel.add(sortLabel, BorderLayout.WEST);
		sortPanel.add(sortBox, BorderLayout.CENTER);

		statItemContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		JPanel statItemWrapper = new JPanel(new BorderLayout());
		statItemWrapper.add(statItemContainer, BorderLayout.NORTH);
		statItemWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);


		JScrollPane scrollWrapper = new JScrollPane(statItemWrapper);
		scrollWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		scrollWrapper.setBorder(new EmptyBorder(3, 5, 0, 5));
		scrollWrapper.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
		scrollWrapper.getVerticalScrollBar().setBorder(new EmptyBorder(0, 3, 0, 0));

		//itemContainer holds the StatItems along with its sorting selector.
		JPanel itemContainer = new JPanel(new BorderLayout());
		itemContainer.add(sortPanel, BorderLayout.NORTH);
		itemContainer.add(scrollWrapper, BorderLayout.CENTER);
		//itemContainer.setBorder(BorderFactory.createMatteBorder(1, 1, 0, 1, ColorScheme.BRAND_ORANGE));

		paginator = new Paginator(() -> SwingUtilities.invokeLater(() -> {
			Instant rebuildStart = Instant.now();
			rebuildStatItemContainer(plugin.getTradesForCurrentView());
			revalidate();
			repaint();
			log.info("page change took {}", Duration.between(rebuildStart, Instant.now()).toMillis());
		}));
		paginator.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		paginator.setBorder(new EmptyBorder(0, 0, 0, 10));
		JPanel itemContainerGroup = new JPanel(new BorderLayout());

		itemContainerGroup.setBorder(BorderFactory.createMatteBorder(10, 5, 15,5, ColorScheme.DARK_GRAY_COLOR));
		itemContainerGroup.add(itemContainer, BorderLayout.CENTER);
		itemContainerGroup.add(paginator, BorderLayout.SOUTH);
		contentWrapper.add(itemContainerGroup, BorderLayout.CENTER);

		add(contentWrapper, BorderLayout.CENTER);
		add(topPanel, BorderLayout.NORTH);
	}

	/**
	 * Removes old stat items and builds new ones based on the passed trade list.
	 * Items are initialized with their sub info containers collapsed.
	 *
	 * @param flippingItems The list of flipping items that get shown on the stat panel.
	 */
	public void rebuild(List<FlippingItem> flippingItems)
	{
		//Remove old stats
		activePanels = new ArrayList<>();

		SwingUtilities.invokeLater(() ->
		{
			Instant rebuildStart = Instant.now();
			rebuildStatItemContainer(flippingItems);
			updateDisplays(flippingItems);
			revalidate();
			repaint();
			log.info("stats panel rebuild took {}", Duration.between(rebuildStart, Instant.now()).toMillis());
		});
	}

	public void rebuildStatItemContainer(List<FlippingItem> flippingItems) {
		List<FlippingItem> sortedItems = sortTradeList(flippingItems);
		List<FlippingItem> itemsThatShouldHavePanels = sortedItems.stream().filter(item -> item.getIntervalHistory(startOfInterval).stream().anyMatch(OfferEvent::isValidStatOffer)).collect(Collectors.toList());
		paginator.updateTotalPages(itemsThatShouldHavePanels.size());
		List<FlippingItem> itemsOnCurrentPage = paginator.getCurrentPageItems(itemsThatShouldHavePanels);
		statItemContainer.removeAll();
		int index = 0;
		for (FlippingItem item : itemsOnCurrentPage)
		{
			ArrayList<OfferEvent> itemTradeHistory = new ArrayList<>(item.getIntervalHistory(startOfInterval));

			//Make sure the item has stats we can use
			if (itemTradeHistory.isEmpty())
			{
				continue;
			}

			StatItemPanel newPanel = new StatItemPanel(plugin, itemManager, item);

			if (index++ > 0)
			{
				JPanel marginWrapper = new JPanel(new BorderLayout());
				marginWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
				marginWrapper.setBorder(new EmptyBorder(5, 0, 0, 0));
				marginWrapper.add(newPanel, BorderLayout.NORTH);
				statItemContainer.add(marginWrapper, constraints);
			}
			else
			{
				//First item in the wrapper
				statItemContainer.add(newPanel, constraints);
			}
			activePanels.add(newPanel);
			constraints.gridy++;
		}
	}



	/**
	 * Updates the display of the total profit value along with the display of sub panels
	 *
	 * @param tradesList
	 */
	public void updateDisplays(List<FlippingItem> tradesList)
	{
		subInfoContainer.removeAll();

		boolean useAltColor = true;
		for (JPanel panel : subInfoPanelArray)
		{
			panel.setBorder(new EmptyBorder(4, 2, 4, 2));
			subInfoContainer.add(panel);
			panel.setBackground(useAltColor ? UIUtilities.DARK_GRAY_ALT_ROW_COLOR : ColorScheme.DARKER_GRAY_COLOR);

			useAltColor = !useAltColor;
		}

		if (!Objects.equals(timeIntervalDropdown.getSelectedItem(), "Session"))
		{
			subInfoContainer.remove(sessionTimePanel);
			subInfoContainer.remove(hourlyProfitPanel);
		}

		totalProfit = 0;
		totalExpenses = 0;
		totalRevenues = 0;
		totalQuantity = 0;
		totalFlips = 0;
		mostCommonItemName = null;
		mostFlips = 0;

		for (FlippingItem item : tradesList)
		{
			if (!item.hasValidOffers(HistoryManager.PanelSelection.STATS))
			{
				continue;
			}

			List<OfferEvent> intervalHistory = item.getIntervalHistory(startOfInterval);
			totalProfit += item.currentProfit(intervalHistory);
			totalExpenses += item.getFlippedCashFlow(startOfInterval, true);
			totalRevenues += item.getFlippedCashFlow(startOfInterval, false);
			totalQuantity += item.countItemsFlipped(intervalHistory);
			int flips = item.getFlips(startOfInterval).size();
			totalFlips += flips;
			if (mostCommonItemName == null || mostFlips < flips)
			{
				mostFlips =flips;
				mostCommonItemName = item.getItemName();
			}
		}

		updateTotalProfitDisplay();
		updateSubInfoFont();
		if (Objects.equals(timeIntervalDropdown.getSelectedItem(), "Session"))
		{
			Duration accumulatedTime = plugin.getAccumulatedTimeForCurrentView();
			updateSessionTimeDisplay(accumulatedTime);
			updateHourlyProfitDisplay(accumulatedTime);
		}
		updateRoiDisplay();
		updateRevenueAndExpenseDisplay();
		updateTotalQuantityDisplay();
		updateTotalFlipsDisplay();
		updateMostCommonFlip();
	}

	/**
	 * Responsible for updating the total profit label at the very top.
	 * Sets the new total profit value from the items in tradesList from {@link FlippingPlugin#getTradesForCurrentView()}.
	 */
	private void updateTotalProfitDisplay()
	{
		if (plugin.getTradesForCurrentView() == null)
		{
			totalProfitVal.setText("0");
			totalProfitVal.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
			totalProfitVal.setToolTipText("Total Profit: 0 gp");
			return;
		}

		totalProfitVal.setText(((totalProfit >= 0) ? "" : "-") + UIUtilities.quantityToRSDecimalStack(Math.abs(totalProfit), true) + " gp");
		totalProfitVal.setToolTipText("Total Profit: " + QuantityFormatter.formatNumber(totalProfit) + " gp");

		//Reproduce the RuneScape stack size colors
		if (totalProfit < 0)
		{
			//]-inf, 0[
			totalProfitVal.setForeground(UIUtilities.OUTDATED_COLOR);
		}
		else if (totalProfit <= 100000)
		{
			//[0,100k[
			totalProfitVal.setForeground(Color.YELLOW);
		}
		else if (totalProfit <= 10000000)
		{
			//[100k,10m[
			totalProfitVal.setForeground(Color.WHITE);
		}
		else
		{
			//[10m,inf[
			totalProfitVal.setForeground(Color.GREEN);
		}
	}

	/**
	 * Updates the hourly profit value display. Also checks and sets the font color according to profit/loss.
	 */
	private void updateHourlyProfitDisplay(Duration accumulatedTime)
	{
		String profitString;
		double divisor = accumulatedTime.toMillis() / 1000 * 1.0 / (60 * 60);

		if (divisor != 0)
		{
			profitString = UIUtilities.quantityToRSDecimalStack((long) (totalProfit / divisor), true);
		}
		else
		{
			profitString = "0";
		}

		hourlyProfitVal.setText(profitString + " gp/hr");
		hourlyProfitVal.setForeground(totalProfit >= 0 ? ColorScheme.GRAND_EXCHANGE_PRICE : UIUtilities.OUTDATED_COLOR);
		hourlyProfitPanel.setToolTipText("Hourly profit as determined by the session time");
	}

	/**
	 * Updates the total ROI value display. Also checks and sets the font color according to profit/loss.
	 */
	private void updateRoiDisplay()
	{
		float roi = (float) totalProfit / totalExpenses * 100;

		if (totalExpenses == 0)
		{
			roiVal.setText("0.00%");
			roiVal.setForeground(Color.RED);
			return;
		}
		else
		{
			roiVal.setText(String.format("%.2f", (float) totalProfit / totalExpenses * 100) + "%");
		}

		roiVal.setForeground(UIUtilities.gradiatePercentage(roi, plugin.getConfig().roiGradientMax()));
		roiPanel.setToolTipText("<html>Return on investment:<br>Percentage of profit relative to gp invested</html>");
	}

	/**
	 * Updates both the revenue and expense display along with setting their font colors.
	 */
	private void updateRevenueAndExpenseDisplay()
	{
		totalRevenueVal.setText(UIUtilities.quantityToRSDecimalStack(totalRevenues, true) + " gp");
		totalRevenueVal.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		totalRevenueVal.setToolTipText("Total amount of gp collected");

		totalExpenseVal.setText(UIUtilities.quantityToRSDecimalStack(totalExpenses, true) + " gp");
		totalExpenseVal.setForeground(UIUtilities.OUTDATED_COLOR);
		totalExpensePanel.setToolTipText("Total amount of gp invested");
	}

	private void updateTotalQuantityDisplay()
	{
		totalQuantityVal.setText(QuantityFormatter.formatNumber(totalQuantity));
		totalQuantityVal.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		totalQuantityPanel.setToolTipText("Total quantity of items flipped");
	}

	private void updateTotalFlipsDisplay()
	{
		totalFlipsVal.setText(QuantityFormatter.formatNumber(totalFlips));
		totalFlipsVal.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		totalFlipsPanel.setToolTipText("<html>Total amount of flips completed" +
			"<br>Does not count margin checks</html>");
	}

	private void updateMostCommonFlip()
	{
		if (mostCommonItemName == null || mostFlips == 0)
		{
			mostCommonFlipVal.setText("None");
			mostCommonFlipVal.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			return;
		}
		mostCommonFlipVal.setText(mostCommonItemName);
		mostCommonFlipVal.setToolTipText("Flipped " + mostFlips + (mostFlips == 1 ? " time" : " times"));
		mostCommonFlipVal.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		mostCommonFlipPanel.setToolTipText("<html>Most commonly flipped item determined by the item with most flips completed" +
			"<br>Does not count margin checks</html>");
	}

	/**
	 * This is called every second by the executor service in FlippingPlugin
	 */
	public void updateTimeDisplay()
	{
		activePanels.forEach(StatItemPanel::updateTimeLabels);
	}

	/**
	 * This is called by updateSessionTime in FlippingPlugin, which itself is called every second by
	 * the executor service.
	 *
	 * @param accumulatedTime The total time the user has spent flipping since the client started up.
	 */
	public void updateSessionTimeDisplay(Duration accumulatedTime)
	{
		sessionTimeVal.setText(UIUtilities.formatDuration(accumulatedTime));
	}

	/**
	 * Designates the panel for deletion by changing its FlippingItem's stored offers stat validity state to false.
	 * This means the panel will not be built upon the next rebuild calls of StatPanel.
	 *
	 * @param itemPanel The panel which holds the FlippingItem to be terminated.
	 * @param reset     Determines if this was called by the resetPanel method so that the interval is set to all
	 */
	public void deletePanel(StatItemPanel itemPanel, boolean reset)
	{
		if (!activePanels.contains(itemPanel))
		{
			return;
		}

		FlippingItem item = itemPanel.getFlippingItem();

		item.invalidateOffers(HistoryManager.PanelSelection.STATS, item.getIntervalHistory(reset ? Instant.EPOCH : startOfInterval));
		plugin.truncateTradeList();
	}

	/**
	 * Designates every panel that is currently shown on the StatPanel to be terminated.
	 * Read deletePanel method's doc for information on how this is done.
	 */
	public void resetPanel()
	{
		for (StatItemPanel itemPanel : activePanels)
		{
			deletePanel(itemPanel, true);
		}
	}

	/**
	 * Gets called every time the time interval combobox has its selection changed.
	 * Sets the start interval of the profit calculation.
	 *
	 * @param selectedInterval The string from TIME_INTERVAL_STRINGS that is selected in the time interval combobox
	 */
	public void setTimeInterval(String selectedInterval)
	{
		if (selectedInterval == null)
		{
			return;
		}

		Instant timeNow = Instant.now();

		switch (selectedInterval)
		{
			case "Past Hour":
				startOfInterval = timeNow.minus(1, ChronoUnit.HOURS);
				break;
			case "Past 4 Hours":
				startOfInterval = timeNow.minus(4, ChronoUnit.HOURS);
				break;
			case "Past 12 Hours":
				startOfInterval = timeNow.minus(12, ChronoUnit.HOURS);
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
				startOfInterval = plugin.getStartOfSessionForCurrentView();
				break;
			case "All":
				startOfInterval = Instant.EPOCH;
				break;
			default:
				break;
		}
		paginator.setPageNumber(1);
		rebuild(plugin.getTradesForCurrentView());
	}

	/**
	 * Chooses the font that is used for the sub information based on user config.
	 */
	private void updateSubInfoFont()
	{
		Font font = null;
		switch (plugin.getConfig().subInfoFontStyle())
		{
			case SMALL_FONT:
				font = FontManager.getRunescapeSmallFont();
				break;

			case REGULAR_FONT:
				font = FontManager.getRunescapeFont();
				break;

			case BOLD_FONT:
				font = FontManager.getRunescapeBoldFont();
				break;
		}

		for (int i = 0; i < textLabelArray.length; i++)
		{
			textLabelArray[i].setFont(font);
			valLabelArray[i].setFont(font);
		}
	}

	/**
	 * Clones and sorts the to-be-built tradeList items according to the selectedSort string.
	 *
	 * @param tradeList The soon-to-be drawn tradeList whose items are getting sorted.
	 * @return Returns a cloned and sorted tradeList as specified by the selectedSort string.
	 */
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
				result.sort(Comparator.comparing(FlippingItem::getLatestActivityTime));
				break;

			case "Most Total Profit":
				result.sort((item1, item2) -> {
					ArrayList<OfferEvent> intervalHistory1 = item1.getIntervalHistory(startOfInterval);
					ArrayList<OfferEvent> intervalHistory2 = item2.getIntervalHistory(startOfInterval);

					long totalExpense1 = item1.getFlippedCashFlow(intervalHistory1, true);
					long totalRevenue1 = item1.getFlippedCashFlow(intervalHistory1, false);

					long totalExpense2 = item2.getFlippedCashFlow(intervalHistory2, true);
					long totalRevenue2 = item2.getFlippedCashFlow(intervalHistory2, false);

					if ((totalExpense1 != 0 && totalRevenue1 != 0) && (totalExpense2 ==0 || totalRevenue2 == 0)) {
						return 1;
					}

					if ((totalExpense1 == 0 || totalRevenue1 == 0) && (totalExpense2 != 0 && totalRevenue2 != 0)) {
						return -1;
					}

					if ((totalExpense1 == 0 || totalRevenue1 == 0) && (totalExpense2 == 0 || totalRevenue2 == 0)) {
						return 0;
					}

					return Long.compare(item1.currentProfit(intervalHistory1), item2.currentProfit(intervalHistory2));
				});
				break;

			case "Most Profit Each":
				result.sort(Comparator.comparing(item ->
				{
					ArrayList<OfferEvent> intervalHistory = item.getIntervalHistory(startOfInterval);
					int quantity = item.countItemsFlipped(intervalHistory);

					if (quantity == 0)
					{
						return 0;
					}

					return (int) item.currentProfit(intervalHistory) / quantity;
				}));
				break;
			case "Highest ROI":
				result.sort((item1, item2) ->
				{
					ArrayList<OfferEvent> intervalHistory1 = item1.getIntervalHistory(startOfInterval);
					ArrayList<OfferEvent> intervalHistory2 = item2.getIntervalHistory(startOfInterval);

					long totalExpense1 = item1.getFlippedCashFlow(intervalHistory1, true);
					long totalRevenue1 = item1.getFlippedCashFlow(intervalHistory1, false);

					long totalExpense2 = item2.getFlippedCashFlow(intervalHistory2, true);
					long totalRevenue2 = item2.getFlippedCashFlow(intervalHistory2, false);

					if ((totalExpense1 != 0 && totalRevenue1 != 0) && (totalExpense2 ==0 || totalRevenue2 == 0)) {
						return 1;
					}

					if ((totalExpense1 == 0 || totalRevenue1 == 0) && (totalExpense2 != 0 && totalRevenue2 != 0)) {
						return -1;
					}

					if ((totalExpense1 == 0 || totalRevenue1 == 0) && (totalExpense2 == 0 || totalRevenue2 == 0)) {
						return 0;
					}

					return Float.compare((float) item1.currentProfit(intervalHistory1) / totalExpense1, (float) item2.currentProfit(intervalHistory2) / totalExpense2);
				});
				break;

			case "Highest Quantity":
				result.sort(Comparator.comparing(item -> item.countItemsFlipped(item.getIntervalHistory(startOfInterval))));
				break;

			default:
				throw new IllegalStateException("Unexpected value: " + selectedSort);
		}
		Collections.reverse(result);

		return result;
	}

	public void setSelectedTimeInterval(String interval)
	{
		if (interval == null)
		{
			timeIntervalDropdown.setSelectedItem("Session");
		}
		else
		{
			timeIntervalDropdown.setSelectedItem(interval);
		}
	}

}
