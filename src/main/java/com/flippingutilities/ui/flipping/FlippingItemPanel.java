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
import com.flippingutilities.OfferEvent;
import com.flippingutilities.ui.utilities.UIUtilities;
import static com.flippingutilities.ui.utilities.UIUtilities.DELETE_ICON;
import static com.flippingutilities.ui.utilities.UIUtilities.ICON_SIZE;
import static com.flippingutilities.ui.utilities.UIUtilities.STAR_HALF_ON_ICON;
import static com.flippingutilities.ui.utilities.UIUtilities.STAR_OFF_ICON;
import static com.flippingutilities.ui.utilities.UIUtilities.STAR_ON_ICON;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.QuantityFormatter;

/**
 * Represents an instance of one of the many panels on the FlippingPanel. It is used to display information such as
 * the margin check prices of the flipping item, the ge limit left, when the ge limit will refresh, the ROI, etc.
 */
@Slf4j
public class FlippingItemPanel extends JPanel
{
	private static final String NUM_FORMAT = "%,d";

	@Getter
	private final FlippingItem flippingItem;
	private FlippingPlugin plugin;
	JLabel priceCheckBuyVal = new JLabel();
	JLabel priceCheckSellVal = new JLabel();
	JLabel latestBuyPriceVal = new JLabel();
	JLabel latestSellPriceVal = new JLabel();
	JLabel profitEachVal = new JLabel();
	JLabel potentialProfitVal = new JLabel();

	JLabel roiLabel = new JLabel();
	JLabel limitLabel = new JLabel();

	JLabel priceCheckBuyText = new JLabel("Should buy at: ");
	JLabel priceCheckSellText = new JLabel("Should sell at: ");
	JLabel latestBuyPriceText = new JLabel("Last buy price: ");
	JLabel latestSellPriceText = new JLabel("Last sell price: ");
	JLabel profitEachText = new JLabel("Profit each: ");
	JLabel profitTotalText = new JLabel("Potential profit: ");
	JLabel roiText = new JLabel("ROI:", JLabel.CENTER);
	JLabel geLimitText = new JLabel("GE limit:",JLabel.CENTER);

	JPanel itemInfo;
	JPanel timeInfoPanel;

	JLabel priceCheckBuyTimeVal = new JLabel();
	JLabel priceCheckSellTimeVal = new JLabel();
	JLabel latestBuyTimeVal = new JLabel();
	JLabel latestSellTimeVal = new JLabel();

	JLabel geRefreshLabel = new JLabel();
	JLabel geRefreshAtLabel = new JLabel();

	JLabel latestPcBuyAt = new JLabel();
	JLabel latestPcSellAt = new JLabel();
	JLabel latestBoughtAt = new JLabel();
	JLabel latestSoldAt = new JLabel();


	FlippingItemPanel(final FlippingPlugin plugin, AsyncBufferedImage itemImage, final FlippingItem flippingItem, Runnable onDeleteCallback)
	{
		this.flippingItem = flippingItem;
		this.plugin = plugin;
		flippingItem.validateGeProperties();
		setBackground(UIUtilities.DARK_GRAY);
		setLayout(new BorderLayout());
		setToolTipText("Flipped by " + flippingItem.getFlippedBy());

		setDescriptionLabels();
		setValueLabels();
		updateTimerDisplays();

		JPanel titlePanel = createTitlePanel(createItemIcon(itemImage), createDeleteButton(onDeleteCallback), createItemNameLabel(), createFavoriteIcon());
		itemInfo = createItemInfoPanel();
		timeInfoPanel = createTimeInfoPanel();
		timeInfoPanel.setVisible(false);
		add(titlePanel, BorderLayout.NORTH);
		add(itemInfo, BorderLayout.CENTER);
		add(timeInfoPanel, BorderLayout.SOUTH);

		//if it is enabled, the itemInfo panel is visible by default so no reason to check it
		if (!plugin.getConfig().verboseViewEnabled())
		{
			collapse();
		}

		//if user has "overridden" the config option by expanding/collapsing that item, use what they set instead of the config value.
		if (flippingItem.getExpand() != null)
		{
			if (flippingItem.getExpand())
			{
				expand();
			}
			else
			{
				collapse();
			}
		}
	}

	/**
	 * Creates the panel which contains all the info about the item like its price check prices, limit
	 * remaining, etc.
	 * @return
	 */
	private JPanel createItemInfoPanel()
	{
		JPanel itemInfo = new JPanel(new DynamicGridLayout(8, 1));
		itemInfo.setBackground(getBackground());

		JPanel priceCheckBuyPanel = new JPanel(new BorderLayout());
		JPanel priceCheckSellPanel = new JPanel(new BorderLayout());
		JPanel latestBuyPanel = new JPanel(new BorderLayout());
		JPanel latestSellPanel = new JPanel(new BorderLayout());
		JPanel profitEachPanel = new JPanel(new BorderLayout());
		JPanel potentialProfitPanel = new JPanel(new BorderLayout());

		JPanel[] panels = {priceCheckBuyPanel, priceCheckSellPanel, latestBuyPanel, latestSellPanel, profitEachPanel, potentialProfitPanel};
		JLabel[] descriptionLabels = {priceCheckBuyText, priceCheckSellText, latestBuyPriceText, latestSellPriceText, profitEachText, profitTotalText};
		JLabel[] valueLabels = {priceCheckBuyVal, priceCheckSellVal, latestBuyPriceVal, latestSellPriceVal,profitEachVal, potentialProfitVal};


		for (int i=0;i<panels.length;i++) {
			panels[i].setBackground(UIUtilities.DARK_GRAY);
			panels[i].setBorder(new EmptyBorder(4,8,6,8));
			panels[i].add(descriptionLabels[i], BorderLayout.WEST);
			panels[i].add(valueLabels[i], BorderLayout.EAST);
			itemInfo.add(panels[i]);
			if (i == panels.length-1) {
				panels[i].setBorder(new EmptyBorder(4,8,0,8));
			}
		}

		itemInfo.add(createGeLimitRefreshTimeAndRoiPanel());

		return itemInfo;
	}

	/**
	 * holds the ge limit remaining on the left, the ge refresh timer in the center, and the roi on the right.
	 * @return
	 */
	private JPanel createGeLimitRefreshTimeAndRoiPanel() {
		JPanel geLimitPanel = new JPanel(new DynamicGridLayout(2,1,0,5));
		geLimitPanel.setBorder(new EmptyBorder(0,0,0,10));
		geLimitPanel.setBackground(UIUtilities.DARK_GRAY);
		geLimitPanel.add(geLimitText);
		geLimitPanel.add(limitLabel);

		JPanel roiPanel = new JPanel(new DynamicGridLayout(2,1,0,5));
		roiPanel.setBackground(UIUtilities.DARK_GRAY);
		roiPanel.setBorder(new EmptyBorder(0,15,0,0));
		roiPanel.add(roiText);
		roiPanel.add(roiLabel);

		//hold the ge limit timer and the text that shows the local time the limit will refresh at
		JPanel geRefreshTimePanel = new JPanel(new DynamicGridLayout(2,1,0, 2));
		geRefreshTimePanel.setBorder(new EmptyBorder(5,0,5,0));
		geRefreshTimePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		geRefreshTimePanel.add(geRefreshLabel);
		geRefreshTimePanel.add(geRefreshAtLabel);


		//holds the ge limit remaining on the left, the ge refresh timer in the center, and the roi on the right.
		JPanel geLimitRefreshTimeAndRoiPanel = new JPanel(new BorderLayout());
		geLimitRefreshTimeAndRoiPanel.setBorder(new EmptyBorder(10,8,6,8));
		geLimitRefreshTimeAndRoiPanel.setBackground(UIUtilities.DARK_GRAY);
		geLimitRefreshTimeAndRoiPanel.add(geRefreshTimePanel, BorderLayout.CENTER);
		geLimitRefreshTimeAndRoiPanel.add(geLimitPanel, BorderLayout.WEST);
		geLimitRefreshTimeAndRoiPanel.add(roiPanel, BorderLayout.EAST);

		MouseAdapter geRefreshLabelsListener = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (timeInfoPanel.isVisible()) {
					timeInfoPanel.setVisible(false);
				}
				else {
					timeInfoPanel.setVisible(true);
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				if (!timeInfoPanel.isVisible()) {
					geRefreshTimePanel.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
				}
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if (!timeInfoPanel.isVisible()) {
					geRefreshTimePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
				}
			}
		};

		//i have to attach it to everything cause otherwise it only listens to the specific area not covered by some other component.
		geRefreshTimePanel.addMouseListener(geRefreshLabelsListener);
		geRefreshLabel.addMouseListener(geRefreshLabelsListener);
		geRefreshAtLabel.addMouseListener(geRefreshLabelsListener);

		return geLimitRefreshTimeAndRoiPanel;
	}

	private JPanel createTimeInfoPanel() {
		JPanel timeInfoPanel = new JPanel(new DynamicGridLayout(5, 1));
		timeInfoPanel.setBackground(getBackground());

		JPanel priceCheckBuyTimePanel = new JPanel(new BorderLayout());
		JPanel priceCheckSellTimePanel = new JPanel(new BorderLayout());
		JPanel latestBuyTimePanel = new JPanel(new BorderLayout());
		JPanel latestSellTimePanel = new JPanel(new BorderLayout());

		JLabel priceCheckBuyTimeText = new JLabel("Time since PC buy: ");
		JLabel priceCheckSellTimeText = new JLabel("Time since PC sell: ");
		JLabel latestBuyTimeText = new JLabel("Time since last buy: ");
		JLabel latestSellTimeText = new JLabel("Time since last sell: ");

		JPanel[] panels = {priceCheckBuyTimePanel, priceCheckSellTimePanel, latestBuyTimePanel, latestSellTimePanel};
		JLabel[] descriptionLabels = {priceCheckBuyTimeText, priceCheckSellTimeText, latestBuyTimeText, latestSellTimeText};
		JLabel[] timerValueLabels = {priceCheckBuyTimeVal,priceCheckSellTimeVal,latestBuyTimeVal,latestSellTimeVal};
		JLabel[] dateLabels = {latestPcBuyAt, latestPcSellAt, latestBoughtAt, latestSoldAt};

		for (int i=0;i<panels.length;i++) {
			descriptionLabels[i].setFont(plugin.getFont());
			descriptionLabels[i].setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
			timerValueLabels[i].setHorizontalAlignment(JLabel.CENTER);
			dateLabels[i].setHorizontalAlignment(JLabel.CENTER);
			dateLabels[i].setFont(plugin.getFont());
			dateLabels[i].setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
			timerValueLabels[i].setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
			timerValueLabels[i].setFont(FontManager.getRunescapeBoldFont());
			timerValueLabels[i].setOpaque(true);
			timerValueLabels[i].setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
			panels[i].setBackground(UIUtilities.DARK_GRAY);
			panels[i].setBorder(new EmptyBorder(4,8,8,8));
			panels[i].add(descriptionLabels[i], BorderLayout.WEST);
			panels[i].add(createTimerAndDatePanel(timerValueLabels[i], dateLabels[i]), BorderLayout.EAST);
			timeInfoPanel.add(panels[i]);
		}
		return timeInfoPanel;
	}

	private JPanel createTimerAndDatePanel(JLabel timerLabel, JLabel dateLabel) {
		JPanel timerAndDatePanel = new JPanel(new DynamicGridLayout(2,1,0,2));
		timerAndDatePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		timerAndDatePanel.setBorder(new EmptyBorder(5,5,5,5));
		timerAndDatePanel.add(timerLabel);
		timerAndDatePanel.add(dateLabel);
		return timerAndDatePanel;
	}



	private void setValueLabels() {

		Arrays.asList(priceCheckBuyVal, priceCheckSellVal, latestBuyPriceVal, latestSellPriceVal, profitEachVal, potentialProfitVal, roiLabel, limitLabel).
				forEach(label -> {
					label.setHorizontalAlignment(JLabel.RIGHT);
					label.setFont(plugin.getFont());
					if (label == limitLabel) {
						limitLabel.setHorizontalAlignment(JLabel.CENTER);
						label.setForeground(ColorScheme.GRAND_EXCHANGE_LIMIT);
					}
					if (label == roiLabel) {
						roiLabel.setHorizontalAlignment(JLabel.CENTER);
					}
				});

		geRefreshLabel.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		geRefreshLabel.setFont(FontManager.getRunescapeBoldFont());
		geRefreshLabel.setHorizontalAlignment(JLabel.CENTER);
		geRefreshLabel.setToolTipText("This is a timer displaying how much time is left before the GE limit refreshes for this item");

		geRefreshAtLabel.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		geRefreshAtLabel.setFont(FontManager.getRunescapeSmallFont());
		geRefreshAtLabel.setHorizontalAlignment(JLabel.CENTER);
		geRefreshAtLabel.setToolTipText("This shows the local time when the ge limit will refresh");

		roiLabel.setToolTipText("<html>Return on investment:<br>Percentage of profit relative to gp invested</html>");

		Optional<OfferEvent> latestMarginCheckBuy = flippingItem.getLatestMarginCheckBuy();
		Optional<OfferEvent> latestMarginCheckSell = flippingItem.getLatestMarginCheckSell();

		Optional<OfferEvent> latestBuy = flippingItem.getLatestBuy();
		Optional<OfferEvent> latestSell = flippingItem.getLatestSell();


		Optional<Integer> profitEach = flippingItem.getCurrentProfitEach();

		Optional<Integer> potentialProfit = flippingItem.getPotentialProfit(plugin.getConfig().marginCheckLoss(), plugin.getConfig().geLimitProfit());

		Optional<Float> roi =  flippingItem.getCurrentRoi();

		priceCheckBuyVal.setText(latestMarginCheckSell.isPresent() ? String.format(NUM_FORMAT, latestMarginCheckSell.get().getPrice()) + " gp":"N/A");
		priceCheckSellVal.setText(latestMarginCheckBuy.isPresent() ? String.format(NUM_FORMAT, latestMarginCheckBuy.get().getPrice()) + " gp" : "N/A");

		latestBuyPriceVal.setText(latestBuy.isPresent() ? String.format(NUM_FORMAT, latestBuy.get().getPrice()) + " gp" : "N/A");
		latestSellPriceVal.setText(latestSell.isPresent() ? String.format(NUM_FORMAT, latestSell.get().getPrice()) + " gp" : "N/A");

		profitEachVal.setText(profitEach.isPresent()? QuantityFormatter.quantityToRSDecimalStack(profitEach.get()) + " gp": "N/A");
		potentialProfitVal.setText(potentialProfit.isPresent() ? QuantityFormatter.quantityToRSDecimalStack(potentialProfit.get()) + " gp": "N/A");

		roiLabel.setText(roi.isPresent()? String.format("%.2f", roi.get()) + "%" : "N/A");
		//Color gradient red-yellow-green depending on ROI.
		roiLabel.setForeground(UIUtilities.gradiatePercentage(roi.orElse(0F), plugin.getConfig().roiGradientMax()));

		latestPcBuyAt.setText(latestMarginCheckBuy.isPresent()? UIUtilities.formatTime(latestMarginCheckBuy.get().getTime(), true, true):"N/A");
		latestPcSellAt.setText(latestMarginCheckSell.isPresent()? UIUtilities.formatTime(latestMarginCheckSell.get().getTime(), true, true):"N/A");
		latestBoughtAt.setText(latestBuy.isPresent()? UIUtilities.formatTime(latestBuy.get().getTime(), true, true):"N/A");
		latestSoldAt.setText(latestSell.isPresent()? UIUtilities.formatTime(latestSell.get().getTime(), true, true):"N/A");

		if (flippingItem.getTotalGELimit() > 0) {
			limitLabel.setText(String.format(NUM_FORMAT, flippingItem.getRemainingGeLimit()));
		} else {
			limitLabel.setText("Unknown");
		}
	}

	private void setDescriptionLabels() {
		Arrays.asList(priceCheckBuyText, priceCheckSellText, latestBuyPriceText, latestSellPriceText, profitEachText, profitTotalText, geLimitText, roiText).
				forEach(label -> {
					label.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
					label.setFont(plugin.getFont());
				});

		/* Tooltips */
		priceCheckBuyText.setToolTipText("The buy price according to your latest margin check. This is the price you insta sold the item for");
		priceCheckSellText.setToolTipText("The sell price according to your latest margin check. This is the price you insta bought the item for");
		latestBuyPriceText.setToolTipText("The last price you bought this item for");
		latestSellPriceText.setToolTipText("The last price you sold this item for");
		profitEachText.setToolTipText("The profit margin according to your latest margin check");
		profitTotalText.setToolTipText("The potential profit according to your latest margin check and GE 4-hour limit");
	}

	/**
	 * Creates the title panel which holds the item icon, delete button (shows up only when you hover over the item icon),
	 * the item name label, and the favorite button.
	 *
	 * @param itemIcon
	 * @param deleteButton
	 * @param itemNameLabel
	 * @param favoriteButton
	 * @return
	 */
	private JPanel createTitlePanel(JLabel itemIcon, JButton deleteButton, JLabel itemNameLabel, JLabel favoriteButton)
	{
		JPanel itemClearPanel = new JPanel(new BorderLayout());
		itemClearPanel.setBackground(getBackground());
		itemClearPanel.add(itemIcon, BorderLayout.WEST);
		itemClearPanel.add(deleteButton, BorderLayout.EAST);

		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setComponentPopupMenu(UIUtilities.createGeTrackerLinksPopup(flippingItem));
		titlePanel.setBackground(getBackground());
		titlePanel.add(itemClearPanel, BorderLayout.WEST);
		titlePanel.add(itemNameLabel, BorderLayout.CENTER);
		titlePanel.add(favoriteButton, BorderLayout.EAST);
		titlePanel.setBorder(new EmptyBorder(2, 1, 2, 1));
		titlePanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				itemIcon.setVisible(false);
				deleteButton.setVisible(true);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				deleteButton.setVisible(false);
				itemIcon.setVisible(true);
			}
		});
		return titlePanel;
	}

	/**
	 * Creates the image icon located on the title panel
	 *
	 * @param itemImage the image of the item as given by the ItemManager
	 * @return
	 */
	private JLabel createItemIcon(AsyncBufferedImage itemImage)
	{
		JLabel itemIcon = new JLabel();
		itemIcon.setAlignmentX(Component.LEFT_ALIGNMENT);
		itemIcon.setPreferredSize(ICON_SIZE);
		if (itemImage != null)
		{
			itemImage.addTo(itemIcon);
		}
		return itemIcon;
	}

	/**
	 * Creates the delete button located on the title panel which shows up when you hover over the image icon.
	 *
	 * @param onDeleteCallback the callback to be run when the delete button is pressed.
	 * @return
	 */
	private JButton createDeleteButton(Runnable onDeleteCallback)
	{
		JButton clearButton = new JButton(DELETE_ICON);
		clearButton.setPreferredSize(ICON_SIZE);
		clearButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		clearButton.setBorder(null);
		clearButton.setBorderPainted(false);
		clearButton.setContentAreaFilled(false);
		clearButton.setVisible(false);
		clearButton.setToolTipText("Delete item");
		clearButton.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					flippingItem.setValidFlippingPanelItem(false);
					onDeleteCallback.run();
				}
			}
		});
		return clearButton;
	}

	/**
	 * Creates the item name label that is located on the title panel. The item name label can be clicked on to
	 * expand or collapse the itemInfo panel
	 *
	 * @return
	 */
	private JLabel createItemNameLabel()
	{
		JLabel itemNameLabel = new JLabel(flippingItem.getItemName(), SwingConstants.CENTER);
		itemNameLabel.setFont(FontManager.getRunescapeBoldFont());
		itemNameLabel.setPreferredSize(new Dimension(0, 0)); //Make sure the item name fits
		itemNameLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (isCollapsed())
				{
					expand();
					flippingItem.setExpand(true);
				}
				else
				{
					collapse();
					plugin.search(flippingItem.getItemName());
					flippingItem.setExpand(false);
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (isCollapsed())
				{
					itemNameLabel.setText("Expand");
				}
				else
				{
					itemNameLabel.setText("Collapse");
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				itemNameLabel.setText(flippingItem.getItemName());
			}
		});
		return itemNameLabel;
	}

	/**
	 * Creates the favorite icon used for favoriting items.
	 *
	 * @return
	 */
	private JLabel createFavoriteIcon()
	{
		JLabel favoriteIcon = new JLabel();
		favoriteIcon.setIcon(flippingItem.isFavorite() ? STAR_ON_ICON : STAR_OFF_ICON);
		favoriteIcon.setAlignmentX(Component.RIGHT_ALIGNMENT);
		favoriteIcon.setPreferredSize(new Dimension(24, 24));
		favoriteIcon.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (flippingItem.isFavorite())
				{
					if (plugin.getAccountCurrentlyViewed().equals(plugin.ACCOUNT_WIDE))
					{
						plugin.setFavoriteOnAllAccounts(flippingItem, false);
					}
					flippingItem.setFavorite(false);
					favoriteIcon.setIcon(STAR_OFF_ICON);
				}
				else
				{
					if (plugin.getAccountCurrentlyViewed().equals(plugin.ACCOUNT_WIDE))
					{
						plugin.setFavoriteOnAllAccounts(flippingItem, true);
					}
					flippingItem.setFavorite(true);
					favoriteIcon.setIcon(STAR_ON_ICON);
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (!flippingItem.isFavorite())
				{
					favoriteIcon.setIcon(STAR_HALF_ON_ICON);
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				if (!flippingItem.isFavorite())
				{
					favoriteIcon.setIcon(STAR_OFF_ICON);
				}
			}
		});

		return favoriteIcon;
	}

	public void expand()
	{
		if (isCollapsed())
		{
			itemInfo.setVisible(true);
		}
	}

	public void collapse()
	{
		if (!isCollapsed())
		{
			itemInfo.setVisible(false);
		}
	}

	public boolean isCollapsed()
	{
		return !itemInfo.isVisible();
	}

	/**
	 * Refresh properties that could be stale due to a new offer event. Other properties like the
	 * price check buy and sell price don't have to be refreshed like this cause offers that would cause
	 * them to change already trigger rebuilds of the flipping panel which will reconstruct this panel anyway.
	 */
	public void refreshProperties() {
		latestBuyPriceVal.setText(flippingItem.getLatestBuy().isPresent() ?
				String.format(NUM_FORMAT, flippingItem.getLatestBuy().get().getPrice()) + " gp" : "N/A");
		latestSellPriceVal.setText(flippingItem.getLatestSell().isPresent() ?
				String.format(NUM_FORMAT, flippingItem.getLatestSell().get().getPrice()) + " gp" : "N/A");

		Optional<Integer> potentialProfit = flippingItem.getPotentialProfit(plugin.getConfig().marginCheckLoss(), plugin.getConfig().geLimitProfit());
		potentialProfitVal.setText(potentialProfit.isPresent() ? QuantityFormatter.quantityToRSDecimalStack(potentialProfit.get()) + " gp" : "N/A");

		if (flippingItem.getTotalGELimit() > 0) {
			limitLabel.setText(String.format(NUM_FORMAT, flippingItem.getRemainingGeLimit()));
		} else {
			limitLabel.setText("Unknown");
		}
	}

	public void updateTimerDisplays() {

		flippingItem.validateGeProperties();

		geRefreshLabel.setText(flippingItem.getGeLimitResetTime() == null?
				UIUtilities.formatDuration(Duration.ZERO):
				UIUtilities.formatDuration(Instant.now(), flippingItem.getGeLimitResetTime()));

		//need to update this so it can be reset when the timer runs down.
		if (flippingItem.getTotalGELimit() > 0) {
			limitLabel.setText(String.format(NUM_FORMAT, flippingItem.getRemainingGeLimit()));
		} else {
			limitLabel.setText("Unknown");
		}

		geRefreshAtLabel.setText(flippingItem.getGeLimitResetTime() == null? "Now": UIUtilities.formatTime(flippingItem.getGeLimitResetTime(), true, false));

		setTimeString(flippingItem.getLatestMarginCheckBuy(), priceCheckBuyTimeVal);
		setTimeString(flippingItem.getLatestMarginCheckSell(), priceCheckSellTimeVal);
		setTimeString(flippingItem.getLatestBuy(), latestBuyTimeVal);
		setTimeString(flippingItem.getLatestSell(), latestSellTimeVal);
	}

	private void setTimeString(Optional<OfferEvent> offerEvent, JLabel timeLabel) {
		if (!offerEvent.isPresent()) {
			timeLabel.setText("N/A");
		}
		else {
			//if difference is more than a day don't show it as HH:MM:SS
			if (Instant.now().getEpochSecond() - offerEvent.get().getTime().getEpochSecond() > 86400) {
				timeLabel.setText(UIUtilities.formatDurationTruncated(offerEvent.get().getTime()));
			}
			else {
				timeLabel.setText(UIUtilities.formatDuration(offerEvent.get().getTime()));
			}
		}
	}

}
