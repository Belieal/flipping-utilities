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


import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.QuantityFormatter;

import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;

@Slf4j
public class FlippingItemPanel extends JPanel
{
	
	private static final ImageIcon OPEN_ICON;
	private static final ImageIcon CLOSE_ICON;
	
	private static final Dimension ICON_SIZE = new Dimension(32, 32);
	private static final String NUM_FORMAT = "%,d";
	private static final Color OUTDATED_COLOR = new Color(250, 74, 75);
	private static final Color PROFIT_COLOR = new Color(255, 175, 55);
	private static final String OUTDATED_STRING = "Price is outdated. ";
	
	private static final Border ITEM_INFO_BORDER = new CompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 0, 0, ColorScheme.DARK_GRAY_COLOR),
			BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR.darker(), 3));
	
	static {
		final BufferedImage openIcon = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/open-arrow.png");
		CLOSE_ICON = new ImageIcon(openIcon);
		OPEN_ICON = new ImageIcon(ImageUtil.rotateImage(openIcon, Math.toRadians(90)));
	}
	
	private int buyPrice;
	private int sellPrice;
	private int profitEach;
	private int profitTotal;
	private float ROI;
	private final int GELimit;
	private final FlippingItem flippingItem;
	private FlippingPlugin plugin;
	
	@Setter
	private boolean activeTimer;
	
	@Inject
	private FlippingConfig config;
	@Inject
	private ClientThread clientThread;
	
	/* Labels */
	JLabel buyPriceVal = new JLabel();
	JLabel sellPriceVal = new JLabel();
	JLabel profitEachVal = new JLabel();
	JLabel profitTotalVal = new JLabel();
	JLabel ROILabel = new JLabel();
	JLabel arrowIcon = new JLabel(OPEN_ICON);
	
	/* Panels */
	JPanel topPanel = new JPanel(new BorderLayout());
	JPanel itemInfo = new JPanel(new BorderLayout());
	JPanel leftInfoTextPanel = new JPanel(new GridLayout(7, 1));
	JPanel rightValuesPanel = new JPanel(new GridLayout(7, 1));
	
	FlippingItemPanel(final FlippingPlugin plugin, final ItemManager itemManager, final FlippingItem flippingItem) {
		this.flippingItem = flippingItem;
		this.buyPrice = this.flippingItem.getLatestBuyPrice();
		this.sellPrice = this.flippingItem.getLatestSellPrice();
		this.profitEach = sellPrice - buyPrice;
		this.ROI = calculateROI();
		this.plugin = plugin;
		
		final int itemID = flippingItem.getItemId();
		GELimit = flippingItem.getTotalGELimit();

		/* Assume one less traded item than limit as one was spent in margin checking the item.
		If the user wants, we calculate the total profit while taking into account
		the margin check loss. */
		this.profitTotal = (GELimit == 0) ? 0 : (GELimit - 1) * profitEach - (plugin.getConfig().marginCheckLoss() ? profitEach : 0);
		
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		
		Color background = getBackground();
		/* Item icon */
		AsyncBufferedImage itemImage = itemManager.getImage(itemID);
		JLabel itemIcon = new JLabel();
		itemIcon.setAlignmentX(Component.LEFT_ALIGNMENT);
		itemIcon.setPreferredSize(ICON_SIZE);
		if (itemImage != null) {
			itemImage.addTo(itemIcon);
		}
		
		/* Arrow icon */
		arrowIcon.setAlignmentX(Component.RIGHT_ALIGNMENT);
		arrowIcon.setPreferredSize(ICON_SIZE);
		
		/* Item name panel */
		JLabel itemName = new JLabel(flippingItem.getItemName(), SwingConstants.CENTER);
		
		itemName.setForeground(Color.WHITE);
		itemName.setFont(FontManager.getRunescapeBoldFont());
		
		topPanel.setBackground(background.darker());
		topPanel.add(itemIcon, BorderLayout.WEST);
		topPanel.add(itemName, BorderLayout.CENTER);
		//Pad east to make name line up with horizontal center of the whole panel.
		//Will probably replace this with an arrow to indicate collapsibility.
		topPanel.add(arrowIcon, BorderLayout.EAST);
		topPanel.setBorder(new EmptyBorder(2, 0, 2, 0));
		
		/* GE limit */
		JLabel limitLabel = new JLabel();
		
		/* Prices and profits info */
		leftInfoTextPanel.setBackground(background);
		rightValuesPanel.setBackground(background);
		
		/* Left labels */
		JLabel buyPriceText = new JLabel("Buy price each: ");
		JLabel sellPriceText = new JLabel("Sell price each: ");
		JLabel profitEachText = new JLabel("Profit each: ");
		JLabel profitTotalText = new JLabel("Total profit: ");
		limitLabel.setText("GE limit: " + String.format(NUM_FORMAT, GELimit));
		
		/* Right labels */
		buyPriceVal.setHorizontalAlignment(JLabel.RIGHT);
		sellPriceVal.setHorizontalAlignment(JLabel.RIGHT);
		profitEachVal.setHorizontalAlignment(JLabel.RIGHT);
		profitTotalVal.setHorizontalAlignment(JLabel.RIGHT);
		ROILabel.setHorizontalAlignment(JLabel.RIGHT);
		
		/* Left font colors */
		buyPriceText.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		sellPriceText.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		profitEachText.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		profitTotalText.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		limitLabel.setForeground(ColorScheme.GRAND_EXCHANGE_LIMIT);
		
		
		/* Tooltips */
		buyPriceText.setToolTipText("The buy price according to your latest margin check");
		sellPriceText.setToolTipText("The sell price according to your latest margin check");
		profitEachText.setToolTipText("The profit margin according to your latest margin check");
		profitTotalText.setToolTipText("The total profit according to your latest margin check and GE 4-hour limit");
		ROILabel.setToolTipText("Return on investment: Percentage of profit relative to gp invested");
		limitLabel.setToolTipText("The amount you can buy of this item every 4 hours.");
		
		profitEachVal.setToolTipText(profitEachText.getToolTipText());
		profitTotalVal.setToolTipText(profitTotalText.getToolTipText());
		
		/* Right profit labels font colors. */
		profitEachVal.setForeground(PROFIT_COLOR);
		profitTotalVal.setForeground(PROFIT_COLOR);
		
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
		rightValuesPanel.add(profitTotalVal);
		
		//Separate prices and profit with GE limit and ROI.
		leftInfoTextPanel.add(padLabel3);
		rightValuesPanel.add(padLabel4);
		
		/* GE limits and ROI labels */
		leftInfoTextPanel.add(limitLabel);
		rightValuesPanel.add(ROILabel);
		
		leftInfoTextPanel.setBorder(new EmptyBorder(2, 5, 2, 5));
		rightValuesPanel.setBorder(new EmptyBorder(2, 5, 2, 5));
		
		//Container for both left and right panels.
		itemInfo.setBackground(background);
		itemInfo.add(leftInfoTextPanel, BorderLayout.WEST);
		itemInfo.add(rightValuesPanel, BorderLayout.EAST);
		itemInfo.setBorder(ITEM_INFO_BORDER);
		
		add(topPanel, BorderLayout.NORTH);
		add(itemInfo, BorderLayout.CENTER);
	}
	
	//Creates, updates and sets the strings for the values to the right.
	public void buildPanelValues() {
		//Update latest price
		this.buyPrice = flippingItem.getLatestBuyPrice();
		this.sellPrice = flippingItem.getLatestSellPrice();
		
		int roiGradientMax = plugin.getConfig().roiGradientMax();
		
		updateProfits();
		SwingUtilities.invokeLater(() ->
		{
			buyPriceVal.setText((this.buyPrice == 0) ? "N/A" : String.format(NUM_FORMAT, this.buyPrice) + " gp");
			sellPriceVal.setText((this.sellPrice == 0) ? "N/A" : String.format(NUM_FORMAT, this.sellPrice) + " gp");
			
			profitEachVal.setText((this.buyPrice == 0 || this.sellPrice == 0) ? "N/A" : QuantityFormatter.quantityToRSDecimalStack(profitEach) + " gp");
			profitTotalVal.setText((this.buyPrice == 0 || this.sellPrice == 0) ? "N/A" : QuantityFormatter.quantityToRSDecimalStack(profitTotal) + " gp");
			
			ROILabel.setText("ROI:  " + ((buyPrice == 0 || sellPrice == 0 || profitEach <= 0) ? "N/A" : String.format("%.2f", ROI) + "%"));
		});
		
		//Color gradient red-yellow-green depending on ROI.
		if (ROI < roiGradientMax * 0.5) {
			Color gradientRedToYellow = ColorUtil.colorLerp(Color.RED, Color.YELLOW, ROI / roiGradientMax * 2);
			SwingUtilities.invokeLater(() -> ROILabel.setForeground(gradientRedToYellow));
		} else {
			Color gradientYellowToGreen = (ROI >= roiGradientMax) ? Color.GREEN : ColorUtil.colorLerp(Color.YELLOW, Color.GREEN, ROI / roiGradientMax * 0.5);
			SwingUtilities.invokeLater(() -> ROILabel.setForeground(gradientYellowToGreen));
		}
	}
	
	public void expand() {
		if (isCollapsed()) {
			arrowIcon.setIcon(OPEN_ICON);
			itemInfo.setVisible(true);
		}
	}
	
	public void collapse() {
		if (! isCollapsed()) {
			arrowIcon.setIcon(CLOSE_ICON);
			itemInfo.setVisible(false);
		}
	}
	
	public boolean isCollapsed() {
		return ! itemInfo.isVisible();
	}
	
	//Recalculates profits.
	public void updateProfits() {
		this.profitEach = sellPrice - buyPrice;
		this.profitTotal = GELimit != 0 ? GELimit * profitEach : 0;
		this.ROI = calculateROI();
	}
	
	//Calculates the return on investment percentage.
	private float calculateROI() {
		return Math.abs((float) profitEach / buyPrice * 100);
	}
	
	//Checks if prices are outdated and updates the tooltip.
	public void checkOutdatedPriceTimes() {
		if (! activeTimer) {
			//Panel is dead.
			return;
		}
		//Update time of latest price update.
		Instant latestBuyTime = flippingItem.getLatestBuyTime();
		Instant latestSellTime = flippingItem.getLatestSellTime();
		
		//Update price texts with the string formatter
		final String latestBuyString = formatTimeText(latestBuyTime);
		final String latestSellString = formatTimeText(latestSellTime);
		
		//As the config unit is in minutes.
		int latestBuyDuration = latestBuyTime != null ? (int) Duration.between(latestBuyTime, Instant.now()).toMinutes() : 0;
		int latestSellDuration = latestSellTime != null ? (int) Duration.between(latestSellTime, Instant.now()).toMinutes() : 0;
		
		//Check if, according to the user-defined settings, prices are outdated, else set default color.
		if (latestBuyDuration > plugin.getConfig().outOfDateWarning()) {
			SwingUtilities.invokeLater(() -> {
				buyPriceVal.setForeground(OUTDATED_COLOR);
				buyPriceVal.setToolTipText(OUTDATED_STRING + latestBuyString);
			});
			
		} else {
			SwingUtilities.invokeLater(() -> {
				buyPriceVal.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);
				buyPriceVal.setToolTipText(latestBuyString);
			});
			
		}
		//Sell value
		if (latestSellDuration > plugin.getConfig().outOfDateWarning()) {
			SwingUtilities.invokeLater(() -> {
				sellPriceVal.setForeground(OUTDATED_COLOR);
				sellPriceVal.setToolTipText(OUTDATED_STRING + latestSellString);
			});
			
		} else {
			SwingUtilities.invokeLater(() -> {
				sellPriceVal.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);
				sellPriceVal.setToolTipText(latestSellString);
			});
		}
		
	}
	
	//Helper to construct the price time tooltip string.
	//If there's a method somewhere that does this already, please tell me. :)
	private String formatTimeText(Instant timeInstant) {
		if (timeInstant != null) {
			int timeDuration = (int) Duration.between(timeInstant, Instant.now()).toSeconds();
			String result = timeDuration + ((timeDuration == 1) ? " second old" : " seconds old");
			
			if (timeDuration > 60) {
				//Seconds to minutes.
				int timeDurationMinutes = timeDuration / 60;
				result = timeDurationMinutes + ((timeDurationMinutes == 1) ? " minute old" : " minutes old");
				
				if (timeDurationMinutes > 60) {
					//Minutes to hours
					int timeDurationHours = timeDurationMinutes / 60;
					result = timeDurationHours + ((timeDurationHours == 1) ? " hour old" : " hours old");
				}
			}
			return result;
		} else {
			return "";
		}
	}
}
