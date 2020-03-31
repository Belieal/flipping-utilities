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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.QuantityFormatter;

@Slf4j
public class FlippingItemPanel extends JPanel
{

	private static final ImageIcon OPEN_ICON;
	private static final ImageIcon CLOSE_ICON;
	private static final ImageIcon DELETE_ICON;

	private static final Dimension ICON_SIZE = new Dimension(32, 32);
	private static final String NUM_FORMAT = "%,d";
	private static final Color OUTDATED_COLOR = new Color(250, 74, 75);
	private static final Color PROFIT_COLOR = new Color(255, 175, 55);
	private static final String OUTDATED_STRING = "Price is outdated. ";

	private static final Border ITEM_INFO_BORDER = new CompoundBorder(
		BorderFactory.createMatteBorder(0, 0, 0, 0, ColorScheme.DARK_GRAY_COLOR),
		BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR.darker(), 3));

	static
	{
		final BufferedImage openIcon = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/open-arrow.png");
		CLOSE_ICON = new ImageIcon(openIcon);
		OPEN_ICON = new ImageIcon(ImageUtil.rotateImage(openIcon, Math.toRadians(90)));

		final BufferedImage deleteIcon = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/delete_icon.png");
		DELETE_ICON = new ImageIcon(deleteIcon);
	}

	private int buyPrice;
	private int sellPrice;
	private int profitEach;
	private int profitTotal;
	private float ROI;
	@Getter
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
	JLabel limitLabel = new JLabel();
	JLabel ROILabel = new JLabel();
	JLabel arrowIcon = new JLabel(OPEN_ICON);
	JButton clearButton = new JButton(DELETE_ICON);
	JCheckBox marginFreezer = new JCheckBox();

	/* Panels */
	JPanel topPanel = new JPanel(new BorderLayout());
	JPanel itemInfo = new JPanel(new BorderLayout());
	JPanel leftInfoTextPanel = new JPanel(new GridLayout(8, 1));
	JPanel rightValuesPanel = new JPanel(new GridLayout(8, 1));

	FlippingItemPanel(final FlippingPlugin plugin, final ItemManager itemManager, final FlippingItem flippingItem)
	{
		this.flippingItem = flippingItem;
		this.buyPrice = this.flippingItem.getLatestBuyPrice();
		this.sellPrice = this.flippingItem.getLatestSellPrice();
		this.plugin = plugin;

		final int itemID = flippingItem.getItemId();

		updateProfits();

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		Color background = getBackground();
		/* Item icon */
		AsyncBufferedImage itemImage = itemManager.getImage(itemID);
		JLabel itemIcon = new JLabel();
		itemIcon.setAlignmentX(Component.LEFT_ALIGNMENT);
		itemIcon.setPreferredSize(ICON_SIZE);
		if (itemImage != null)
		{
			itemImage.addTo(itemIcon);
		}

		/* Arrow icon */
		arrowIcon.setAlignmentX(Component.RIGHT_ALIGNMENT);
		arrowIcon.setPreferredSize(ICON_SIZE);

		/* Clear button */
		clearButton.setPreferredSize(ICON_SIZE);
		clearButton.setFont(FontManager.getRunescapeBoldFont());
		clearButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		clearButton.setBorder(null);
		clearButton.setBorderPainted(false);
		clearButton.setContentAreaFilled(false);
		clearButton.setVisible(false);

		/* Margin freezer */
		marginFreezer.setBorder(BorderFactory.createEmptyBorder());
		marginFreezer.setBackground(ColorScheme.LIGHT_GRAY_COLOR);
		marginFreezer.setToolTipText("freeze margin");
		marginFreezer.setSelected(flippingItem.isFrozen());
		marginFreezer.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					if (flippingItem.isFrozen())
					{

						flippingItem.setFrozen(false);
					}
					else
					{
						flippingItem.setFrozen(true);
					}
				}
			}
		});


		JPanel itemClearPanel = new JPanel(new BorderLayout());
		itemClearPanel.setBackground(background.darker());

		itemClearPanel.add(itemIcon, BorderLayout.WEST);
		itemClearPanel.add(clearButton, BorderLayout.EAST);

		/* Item name panel */
		JLabel itemName = new JLabel(flippingItem.getItemName(), SwingConstants.CENTER);

		itemName.setForeground(Color.WHITE);
		itemName.setFont(FontManager.getRunescapeBoldFont());
		itemName.setPreferredSize(new Dimension(0, 0)); //Make sure the item name fits

		topPanel.setBackground(background.darker());
		topPanel.add(itemClearPanel, BorderLayout.WEST);
		topPanel.add(itemName, BorderLayout.CENTER);
		topPanel.add(arrowIcon, BorderLayout.EAST);
		topPanel.setBorder(new EmptyBorder(2, 1, 2, 1));
		topPanel.addMouseListener(new MouseAdapter()
		{

			@Override
			public void mousePressed(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1 && !itemClearPanel.contains(e.getPoint()))
				{
					if (isCollapsed())
					{
						expand();
					}
					else
					{
						collapse();
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				itemIcon.setVisible(false);
				clearButton.setVisible(true);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				//Mouse is hovering over icon
				if (topPanel.contains(e.getPoint()))
				{
					return;
				}
				clearButton.setVisible(false);
				itemIcon.setVisible(true);
			}
		});

		/* Prices and profits info */
		leftInfoTextPanel.setBackground(background);
		rightValuesPanel.setBackground(background);

		/* Left labels */
		JLabel buyPriceText = new JLabel("Buy price each: ");
		JLabel sellPriceText = new JLabel("Sell price each: ");
		JLabel profitEachText = new JLabel("Profit each: ");
		JLabel profitTotalText = new JLabel("Total profit: ");

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
		ROILabel.setToolTipText("<html>Return on investment:<br>Percentage of profit relative to gp invested</html>");
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
		JLabel padLabel5 = new JLabel(" ");

		/* Left info labels */
		leftInfoTextPanel.add(buyPriceText);
		leftInfoTextPanel.add(sellPriceText);
		leftInfoTextPanel.add(marginFreezer);
		leftInfoTextPanel.add(padLabel1);
		leftInfoTextPanel.add(profitEachText);
		leftInfoTextPanel.add(profitTotalText);

		/* Right value labels */
		rightValuesPanel.add(buyPriceVal);
		rightValuesPanel.add(sellPriceVal);
		rightValuesPanel.add(padLabel2);
		rightValuesPanel.add(padLabel3);
		rightValuesPanel.add(profitEachVal);
		rightValuesPanel.add(profitTotalVal);

		//Separate prices and profit with GE limit and ROI.
		leftInfoTextPanel.add(padLabel4);
		rightValuesPanel.add(padLabel5);

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

		buildPanelValues();
		updateGELimits();
		checkOutdatedPriceTimes();
		setActiveTimer(true);

		add(topPanel, BorderLayout.NORTH);
		add(itemInfo, BorderLayout.CENTER);
	}

	//Creates, updates and sets the strings for the values to the right.
	public void buildPanelValues()
	{
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
		if (ROI < roiGradientMax * 0.5)
		{
			Color gradientRedToYellow = ColorUtil.colorLerp(Color.RED, Color.YELLOW, ROI / roiGradientMax * 2);
			SwingUtilities.invokeLater(() -> ROILabel.setForeground(gradientRedToYellow));
		}
		else
		{
			Color gradientYellowToGreen = (ROI >= roiGradientMax) ? Color.GREEN : ColorUtil.colorLerp(Color.YELLOW, Color.GREEN, ROI / roiGradientMax * 0.5);
			SwingUtilities.invokeLater(() -> ROILabel.setForeground(gradientYellowToGreen));
		}
	}

	public void expand()
	{
		if (isCollapsed())
		{
			arrowIcon.setIcon(OPEN_ICON);
			itemInfo.setVisible(true);
		}
	}

	public void collapse()
	{
		if (!isCollapsed())
		{
			arrowIcon.setIcon(CLOSE_ICON);
			itemInfo.setVisible(false);
		}
	}

	public boolean isCollapsed()
	{
		return !itemInfo.isVisible();
	}

	//Recalculates profits.
	public void updateProfits()
	{
		this.profitEach = sellPrice - buyPrice;

		/*
		If the user wants, we calculate the total profit while taking into account
		the margin check loss. */
		this.profitTotal = (flippingItem.getRemainingGELimit() == 0) ? 0
			: flippingItem.getRemainingGELimit() * profitEach - (plugin.getConfig().marginCheckLoss() ? profitEach : 0);
		this.ROI = calculateROI();
	}

	//Calculates the return on investment percentage.
	private float calculateROI()
	{
		return Math.abs((float) profitEach / buyPrice * 100);
	}

	//Checks if prices are outdated and updates the tooltip.
	public void checkOutdatedPriceTimes()
	{
		if (!activeTimer)
		{
			//Panel is dead.
			return;
		}
		//Update time of latest price update.
		Instant latestBuyTime = flippingItem.getLatestBuyTime();
		Instant latestSellTime = flippingItem.getLatestSellTime();

		//Update price texts with the string formatter
		final String latestBuyString = formatPriceTimeText(latestBuyTime) + " old";
		final String latestSellString = formatPriceTimeText(latestSellTime) + " old";

		//As the config unit is in minutes.
		final int latestBuyTimeAgo = latestBuyTime != null ? (int) (Instant.now().getEpochSecond() - latestBuyTime.getEpochSecond()) : 0;
		final int latestSellTimeAgo = latestSellTime != null ? (int) (Instant.now().getEpochSecond() - latestSellTime.getEpochSecond()) : 0;

		//Check if, according to the user-defined settings, prices are outdated, else set default color.
		if (latestBuyTimeAgo != 0 && latestBuyTimeAgo / 60 > plugin.getConfig().outOfDateWarning())
		{
			SwingUtilities.invokeLater(() ->
			{
				buyPriceVal.setForeground(OUTDATED_COLOR);
				buyPriceVal.setToolTipText("<html>" + OUTDATED_STRING + "<br>" + latestBuyString + "</html>");
			});

		}
		else
		{
			SwingUtilities.invokeLater(() ->
			{
				buyPriceVal.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);
				buyPriceVal.setToolTipText(latestBuyString);
			});

		}
		//Sell value
		if (latestSellTimeAgo != 0 && latestSellTimeAgo / 60 > plugin.getConfig().outOfDateWarning())
		{
			SwingUtilities.invokeLater(() ->
			{
				sellPriceVal.setForeground(OUTDATED_COLOR);
				sellPriceVal.setToolTipText("<html>" + OUTDATED_STRING + "<br>" + latestSellString + "</html>");
			});

		}
		else
		{
			SwingUtilities.invokeLater(() ->
			{
				sellPriceVal.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);
				sellPriceVal.setToolTipText(latestSellString);
			});
		}

	}

	//Helper to construct the price time tooltip string.
	//If there's a method somewhere that does this already, please tell me. :)
	//TODO: Refactor using something other than Instant (LocalTime?)
	private String formatPriceTimeText(Instant timeInstant)
	{
		if (timeInstant != null)
		{
			//Time since trade was done.
			long timeAgo = (Instant.now().getEpochSecond() - timeInstant.getEpochSecond());

			String result = timeAgo + (timeAgo == 1 ? " second" : " seconds");
			if (timeAgo > 60)
			{
				//Seconds to minutes.
				long timeAgoMinutes = timeAgo / 60;
				result = timeAgoMinutes + (timeAgoMinutes == 1 ? " minute" : " minutes");

				if (timeAgoMinutes > 60)
				{
					//Minutes to hours
					long timeAgoHours = timeAgoMinutes / 60;
					result = timeAgoHours + (timeAgoHours == 1 ? " hour" : " hours");
				}
			}
			return result;
		}
		else
		{
			return "";
		}
	}

	private String formatGELimitResetTime(Instant time)
	{
		DateTimeFormatter timeFormatter;
		if (plugin.getConfig().twelveHourFormat())
		{
			timeFormatter = DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.systemDefault());
		}
		else
		{
			timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
		}
		return timeFormatter.format(time);
	}

	public void updateGELimits()
	{
		SwingUtilities.invokeLater(() ->
		{
			if (flippingItem.getGeLimitResetTime() != null && flippingItem.getGeLimitResetTime().isBefore(Instant.now()))
			{
				flippingItem.resetGELimit();
			}

			limitLabel.setText("GE limit: " + String.format(NUM_FORMAT, flippingItem.getRemainingGELimit()));
			if (flippingItem.getGeLimitResetTime() == null)
			{
				limitLabel.setToolTipText("None has been bought in the past 4 hours.");
			}
			else
			{
				final long remainingSeconds = flippingItem.getGeLimitResetTime().getEpochSecond() - Instant.now().getEpochSecond();
				final long remainingMinutes = remainingSeconds / 60 % 60;
				final long remainingHours = remainingSeconds / 3600 % 24;
				String timeString = String.format("%02d:%02d ", remainingHours, remainingMinutes) + (remainingHours > 1 ? "hours" : "hour");

				limitLabel.setToolTipText("<html>" + "GE limit is reset in " + timeString + "."
					+ "<br>This will be at " + formatGELimitResetTime(flippingItem.getGeLimitResetTime()) + ".<html>");
			}
		});
	}
}
