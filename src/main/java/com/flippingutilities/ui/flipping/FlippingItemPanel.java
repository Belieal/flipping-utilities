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
import com.flippingutilities.ui.utilities.UIUtilities;
import static com.flippingutilities.ui.utilities.UIUtilities.DELETE_ICON;
import static com.flippingutilities.ui.utilities.UIUtilities.ICON_SIZE;
import static com.flippingutilities.ui.utilities.UIUtilities.STAR_HALF_ON_ICON;
import static com.flippingutilities.ui.utilities.UIUtilities.STAR_OFF_ICON;
import static com.flippingutilities.ui.utilities.UIUtilities.STAR_ON_ICON;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.QuantityFormatter;

@Slf4j
public class FlippingItemPanel extends JPanel
{
	private static final String NUM_FORMAT = "%,d";
	private static final String OUTDATED_STRING = "Price is outdated. ";
	private static final Border ITEM_INFO_BORDER = new CompoundBorder(
		BorderFactory.createMatteBorder(0, 0, 0, 0, ColorScheme.DARK_GRAY_COLOR),
		BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR.darker(), 3));

	@Getter
	private final FlippingItem flippingItem;
	private FlippingPlugin plugin;

	/* Labels */
	JLabel buyPriceVal = new JLabel();
	JLabel sellPriceVal = new JLabel();
	JLabel profitEachVal = new JLabel();
	JLabel potentialProfitVal = new JLabel();
	JLabel limitLabel = new JLabel();
	JLabel roiLabel = new JLabel();

	/* Panels */
	JPanel titlePanel = new JPanel(new BorderLayout());
	JPanel itemInfo = new JPanel(new BorderLayout());

	FlippingItemPanel(final FlippingPlugin plugin, AsyncBufferedImage itemImage, final FlippingItem flippingItem, Runnable onDeleteCallback)
	{
		this.flippingItem = flippingItem;
		this.plugin = plugin;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setToolTipText("Flipped by " + flippingItem.getFlippedBy());

		JLabel favoriteIcon = new JLabel();
		favoriteIcon.setIcon(flippingItem.isFavorite()? STAR_ON_ICON:STAR_OFF_ICON);
		favoriteIcon.setAlignmentX(Component.RIGHT_ALIGNMENT);
		favoriteIcon.setPreferredSize(new Dimension(24, 24));
		favoriteIcon.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (flippingItem.isFavorite()) {
					if (plugin.getAccountCurrentlyViewed().equals(plugin.ACCOUNT_WIDE)) {
						plugin.setFavoriteOnAllAccounts(flippingItem, false);
					}
					flippingItem.setFavorite(false);
					favoriteIcon.setIcon(STAR_OFF_ICON);
				}
				else {
					if (plugin.getAccountCurrentlyViewed().equals(plugin.ACCOUNT_WIDE)) {
						plugin.setFavoriteOnAllAccounts(flippingItem, true);
					}
					flippingItem.setFavorite(true);
					favoriteIcon.setIcon(STAR_ON_ICON);
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (!flippingItem.isFavorite()) {
					favoriteIcon.setIcon(STAR_HALF_ON_ICON);
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				if (!flippingItem.isFavorite()) {
					favoriteIcon.setIcon(STAR_OFF_ICON);
				}
			}
		});

		Color background = getBackground();

		/* Item icon */
		JLabel itemIcon = new JLabel();
		itemIcon.setAlignmentX(Component.LEFT_ALIGNMENT);
		itemIcon.setPreferredSize(ICON_SIZE);
		if (itemImage != null)
		{
			itemImage.addTo(itemIcon);
		}

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

		JPanel itemClearPanel = new JPanel(new BorderLayout());
		itemClearPanel.setBackground(background.darker());

		itemClearPanel.add(itemIcon, BorderLayout.WEST);
		itemClearPanel.add(clearButton, BorderLayout.EAST);

		/* Item name panel */
		JLabel itemNameLabel = new JLabel(flippingItem.getItemName(), SwingConstants.CENTER);
		itemNameLabel.setForeground(Color.WHITE);
		itemNameLabel.setFont(FontManager.getRunescapeBoldFont());
		itemNameLabel.setPreferredSize(new Dimension(0, 0)); //Make sure the item name fits
		itemNameLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (isCollapsed()) {
					expand();
					flippingItem.setExpand(true);
				}
				else {
					collapse();
					flippingItem.setExpand(false);
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (isCollapsed()) {
					itemNameLabel.setText("Expand");
				}
				else {
					itemNameLabel.setText("Collapse");
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				itemNameLabel.setText(flippingItem.getItemName());
			}
		});

		titlePanel.setComponentPopupMenu(UIUtilities.createGeTrackerLinksPopup(flippingItem));
		titlePanel.setBackground(background.darker());
		titlePanel.add(itemClearPanel, BorderLayout.WEST);
		titlePanel.add(itemNameLabel, BorderLayout.CENTER);
		titlePanel.add(favoriteIcon, BorderLayout.EAST);
		titlePanel.setBorder(new EmptyBorder(2, 1, 2, 1));
		titlePanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				itemIcon.setVisible(false);
				clearButton.setVisible(true);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				clearButton.setVisible(false);
				itemIcon.setVisible(true);
			}
		});

		/* Prices and profits info */
		JPanel leftInfoTextPanel = new JPanel(new GridLayout(7, 1));
		JPanel rightValuesPanel = new JPanel(new GridLayout(7, 1));
		leftInfoTextPanel.setBackground(background);
		rightValuesPanel.setBackground(background);

		/* Left labels */
		JLabel buyPriceText = new JLabel("Buy price each: ");
		JLabel sellPriceText = new JLabel("Sell price each: ");
		JLabel profitEachText = new JLabel("Profit each: ");
		JLabel profitTotalText = new JLabel("Potential profit: ");

		/* Right labels */
		buyPriceVal.setHorizontalAlignment(JLabel.RIGHT);
		sellPriceVal.setHorizontalAlignment(JLabel.RIGHT);
		profitEachVal.setHorizontalAlignment(JLabel.RIGHT);
		potentialProfitVal.setHorizontalAlignment(JLabel.RIGHT);
		roiLabel.setHorizontalAlignment(JLabel.RIGHT);

		/* Left font colors */
		buyPriceText.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		sellPriceText.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		profitEachText.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		profitTotalText.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		limitLabel.setForeground(ColorScheme.GRAND_EXCHANGE_LIMIT);

		/* Tooltips */
		buyPriceText.setToolTipText("The buy price according to your latest margin check. This is the price you insta sold the item for");
		sellPriceText.setToolTipText("The sell price according to your latest margin check. This is the price you insta bought the item for");
		profitEachText.setToolTipText("The profit margin according to your latest margin check");
		profitTotalText.setToolTipText(
			"The potential profit according to your latest margin check and GE 4-hour limit");
		roiLabel.setToolTipText(
			"<html>Return on investment:<br>Percentage of profit relative to gp invested</html>");
		limitLabel.setToolTipText("The amount you can buy of this item every 4 hours.");

		profitEachVal.setToolTipText(profitEachText.getToolTipText());
		potentialProfitVal.setToolTipText(profitTotalText.getToolTipText());

		/* Right profit labels font colors. */
		profitEachVal.setForeground(UIUtilities.PROFIT_COLOR);
		potentialProfitVal.setForeground(UIUtilities.PROFIT_COLOR);

		//To space out the pricing and profit labels
		JLabel padLabel1 = new JLabel(" ");
		JLabel padLabel2 = new JLabel(" ");
		JLabel padLabel3 = new JLabel(" ");
		JLabel padLabel4 = new JLabel(" ");

		/* Left info labels */
		leftInfoTextPanel.add(buyPriceText);
		leftInfoTextPanel.add(sellPriceText);
		leftInfoTextPanel.add(padLabel1);
		leftInfoTextPanel.add(profitEachText);
		leftInfoTextPanel.add(profitTotalText);

		/* Right value labels */
		rightValuesPanel.add(buyPriceVal);
		rightValuesPanel.add(sellPriceVal);
		rightValuesPanel.add(padLabel2);
		rightValuesPanel.add(profitEachVal);
		rightValuesPanel.add(potentialProfitVal);

		//Separate prices and profit with GE limit and ROI.
		leftInfoTextPanel.add(padLabel3);
		rightValuesPanel.add(padLabel4);

		/* GE limits and ROI labels */
		leftInfoTextPanel.add(limitLabel);
		rightValuesPanel.add(roiLabel);

		leftInfoTextPanel.setBorder(new EmptyBorder(2, 5, 2, 5));
		rightValuesPanel.setBorder(new EmptyBorder(2, 5, 2, 5));

		//Container for both left and right panels.
		itemInfo.setBackground(background);
		itemInfo.add(leftInfoTextPanel, BorderLayout.WEST);
		itemInfo.add(rightValuesPanel, BorderLayout.EAST);
		itemInfo.setBorder(ITEM_INFO_BORDER);

		//if it is enabled, the itemInfo panel is visible by default so no reason to check it
		if (!plugin.getConfig().verboseViewEnabled()) {
			collapse();
		}

		//if user has "overridden" the config option by expanding/collapsing that item, use what they set instead of the config value.
		if (flippingItem.getExpand() != null) {
			if (flippingItem.getExpand()) {
				expand();
			}
			else {
				collapse();
			}
		}

		buildPanelValues();
		updateGePropertiesDisplay();
		updatePriceOutdatedDisplay();

		add(titlePanel, BorderLayout.NORTH);
		add(itemInfo, BorderLayout.CENTER);
	}

	//Creates, updates and sets the strings for the values to the right.
	public void buildPanelValues()
	{
		//Update latest price
		int buyPrice = flippingItem.getMarginCheckBuyPrice();
		int sellPrice = flippingItem.getMarginCheckSellPrice();
		int profitEach = sellPrice - buyPrice;
		int potentialProfit = flippingItem.getPotentialProfit(plugin.getConfig().marginCheckLoss(), plugin.getConfig().geLimitProfit());
		float roi = (float) profitEach / buyPrice * 100;

		buyPriceVal
			.setText((buyPrice == 0) ? "N/A" : String.format(NUM_FORMAT, buyPrice) + " gp");
		sellPriceVal.setText(
			(sellPrice == 0) ? "N/A" : String.format(NUM_FORMAT, sellPrice) + " gp");

		profitEachVal.setText((buyPrice == 0 || sellPrice == 0) ? "N/A"
			: QuantityFormatter.quantityToRSDecimalStack(profitEach) + " gp");
		potentialProfitVal.setText((buyPrice == 0 || sellPrice == 0 || potentialProfit < 0) ? "N/A" : QuantityFormatter
			.quantityToRSDecimalStack(potentialProfit) + " gp");

		roiLabel.setText("ROI:  " + ((buyPrice == 0 || sellPrice == 0) ? "N/A"
			: String.format("%.2f", roi) + "%"));

		//Color gradient red-yellow-green depending on ROI.
		roiLabel.setForeground(UIUtilities.gradiatePercentage(roi, plugin.getConfig().roiGradientMax()));
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
	 * Checks if a FlippingItem's margins (buy and sell price) are outdated and updates the tooltip.
	 * This is called in two places:
	 * On initialization of a FlippingItemPanel and in FlippingPlugin by the scheduler which call its
	 * every second.
	 */
	public void updatePriceOutdatedDisplay()
	{
		//Update time of latest price update.
		Instant latestBuyTime = flippingItem.getMarginCheckBuyTime();
		Instant latestSellTime = flippingItem.getMarginCheckSellTime();

		//Update price texts with the string formatter
		final String latestBuyString = UIUtilities.formatDurationTruncated(latestBuyTime) + " old";
		final String latestSellString = UIUtilities.formatDurationTruncated(latestSellTime) + " old";

		//As the config unit is in minutes.
		final int latestBuyTimeAgo =
			latestBuyTime != null ? (int) (Instant.now().getEpochSecond() - latestBuyTime
				.getEpochSecond()) : 0;
		final int latestSellTimeAgo =
			latestSellTime != null ? (int) (Instant.now().getEpochSecond() - latestSellTime
				.getEpochSecond()) : 0;

		//Check if, according to the user-defined settings, prices are outdated, else set default color.
		if (latestBuyTimeAgo != 0 && latestBuyTimeAgo / 60 > plugin.getConfig().outOfDateWarning())
		{
			buyPriceVal.setForeground(UIUtilities.OUTDATED_COLOR);
			buyPriceVal.setToolTipText("<html>" + OUTDATED_STRING + "<br>" + latestBuyString + "</html>");
		}
		else
		{
			buyPriceVal.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);
			buyPriceVal.setToolTipText(latestBuyString);
		}
		//Sell value
		if (latestSellTimeAgo != 0 && latestSellTimeAgo / 60 > plugin.getConfig().outOfDateWarning())
		{
			sellPriceVal.setForeground(UIUtilities.OUTDATED_COLOR);
			sellPriceVal
				.setToolTipText("<html>" + OUTDATED_STRING + "<br>" + latestSellString + "</html>");
		}
		else
		{
			sellPriceVal.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);
			sellPriceVal.setToolTipText(latestSellString);
		}

	}

	/**
	 * uses the properties of the FlippingItem to show the ge limit and refresh time display.
	 * Places where this is called:
	 * on initialization of a FlippingItemPanel
	 * In {@link FlippingPanel#updateActivePanelsGePropertiesDisplay} which itself is envoked in two places in
	 * the FlippingPlugin, a background thread, and every time an offer comes in.
	 */
	public void updateGePropertiesDisplay()
	{
		flippingItem.validateGeProperties();
		boolean unknownLimit = false;

		//New items can show as having a total GE limit of 0.
		if (flippingItem.getTotalGELimit() > 0)
		{
			limitLabel.setText("GE limit: " + String.format(NUM_FORMAT, flippingItem.remainingGeLimit()));
		}
		else
		{
			limitLabel.setText("GE limit: Unknown");
			unknownLimit = true;
		}

		String tooltipText = "";

		if (unknownLimit)
		{
			tooltipText = "<html>This item's total GE limit is unknown.<br>";
		}

		if (flippingItem.getGeLimitResetTime() == null)
		{
			tooltipText += "<html>None has been bought in the past 4 hours.</html>";
		}
		else
		{
			final long remainingSeconds = flippingItem.getGeLimitResetTime().getEpochSecond() - Instant.now().getEpochSecond();
			final long remainingMinutes = remainingSeconds / 60 % 60;
			final long remainingHours = remainingSeconds / 3600 % 24;
			String timeString = String.format("%02d:%02d ", remainingHours, remainingMinutes)
				+ (remainingHours > 1 ? "hours" : "hour");

			tooltipText += "<html>GE limit is reset in " + timeString + "."
				+ "<br>This will be at " + UIUtilities.formatTime(flippingItem.getGeLimitResetTime(), plugin.getConfig().twelveHourFormat(), false)
				+ ".</html>";
		}
		limitLabel.setToolTipText(tooltipText);
	}

}
