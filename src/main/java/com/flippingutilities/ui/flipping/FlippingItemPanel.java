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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.util.Optional;
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
import net.runelite.client.ui.ColorScheme;
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
	private static final String OUTDATED_STRING = "Price is outdated. ";
	private static final Border ITEM_INFO_BORDER = new CompoundBorder(
		BorderFactory.createMatteBorder(0, 0, 0, 0, ColorScheme.DARK_GRAY_COLOR),
		BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR.darker(), 3));
	@Getter
	private final FlippingItem flippingItem;
	private FlippingPlugin plugin;
	JLabel priceCheckBuyVal;
	JLabel priceCheckSellVal;
	JLabel latestBuyPriceVal;
	JLabel latestSellPriceVal;
	JLabel profitEachVal;
	JLabel potentialProfitVal;
	JLabel roiLabel;
	JLabel limitLabel;
	JPanel itemInfo;

	FlippingItemPanel(final FlippingPlugin plugin, AsyncBufferedImage itemImage, final FlippingItem flippingItem, Runnable onDeleteCallback)
	{
		this.flippingItem = flippingItem;
		this.plugin = plugin;
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setLayout(new BorderLayout());
		setToolTipText("Flipped by " + flippingItem.getFlippedBy());

		JPanel titlePanel = createTitlePanel(createItemIcon(itemImage), createDeleteButton(onDeleteCallback), createItemNameLabel(), createFavoriteIcon());
		itemInfo = createItemInfoPanel(createLeftLabelsPanel(), createRightLabelsPanel());
		add(titlePanel, BorderLayout.NORTH);
		add(itemInfo, BorderLayout.CENTER);

		updateGePropertiesDisplay();
		updatePriceOutdatedDisplay();

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
	 * Creates the panel which holds the right and left labels panels. Those panels hold values such as the buy price,
	 * sell price, roi, etc.
	 *
	 * @param leftLabelsPanel  Holds names of of values such as "Buy price:", "Sell price:"
	 * @param rightLabelsPanel Holds values such as "100,000".
	 * @return
	 */
	private JPanel createItemInfoPanel(JPanel leftLabelsPanel, JPanel rightLabelsPanel)
	{
		JPanel itemInfo = new JPanel(new BorderLayout());
		itemInfo.setBackground(getBackground());
		itemInfo.add(leftLabelsPanel, BorderLayout.WEST);
		itemInfo.add(rightLabelsPanel, BorderLayout.EAST);
		itemInfo.setBorder(ITEM_INFO_BORDER);
		return itemInfo;
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
		itemClearPanel.setBackground(getBackground().darker());
		itemClearPanel.add(itemIcon, BorderLayout.WEST);
		itemClearPanel.add(deleteButton, BorderLayout.EAST);

		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setComponentPopupMenu(UIUtilities.createGeTrackerLinksPopup(flippingItem));
		titlePanel.setBackground(getBackground().darker());
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
		itemNameLabel.setForeground(Color.WHITE);
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

	/**
	 * Creates the panel which holds the labels on the right side of the FlippingItemPanel. These are labels that have
	 * corresponding values for the left labels. For example, the left label might be "buy price" and the corresponding
	 * label on this panel would be "100,000".
	 *
	 * @return right labels panel
	 */
	private JPanel createRightLabelsPanel()
	{
		JPanel rightValuesPanel = new JPanel(new GridLayout(10, 1));
		rightValuesPanel.setBackground(getBackground());

		priceCheckBuyVal = new JLabel();
		priceCheckSellVal = new JLabel();
		latestBuyPriceVal = new JLabel();
		latestSellPriceVal = new JLabel();
		profitEachVal = new JLabel();
		potentialProfitVal = new JLabel();
		roiLabel = new JLabel();

		priceCheckBuyVal.setHorizontalAlignment(JLabel.RIGHT);
		priceCheckSellVal.setHorizontalAlignment(JLabel.RIGHT);
		latestBuyPriceVal.setHorizontalAlignment(JLabel.RIGHT);
		latestSellPriceVal.setHorizontalAlignment(JLabel.RIGHT);
		profitEachVal.setHorizontalAlignment(JLabel.RIGHT);
		potentialProfitVal.setHorizontalAlignment(JLabel.RIGHT);
		roiLabel.setHorizontalAlignment(JLabel.RIGHT);

		roiLabel.setToolTipText("<html>Return on investment:<br>Percentage of profit relative to gp invested</html>");

		profitEachVal.setForeground(UIUtilities.PROFIT_COLOR);
		potentialProfitVal.setForeground(UIUtilities.PROFIT_COLOR);

		Optional<OfferEvent> latestMarginCheckBuy = flippingItem.getLatestMarginCheckBuy();
		Optional<OfferEvent> latestMarginCheckSell = flippingItem.getLatestMarginCheckSell();
		Optional<Integer> latestBuyPrice = flippingItem.getl
		Optional<Integer> latestSellPrice = flippingItem.getLatestPrice(false);
		Optional<Integer> profitEach =
			latestMarginCheckBuy.isPresent() && latestMarginCheckSell.isPresent()?
				Optional.of(latestMarginCheckBuy.get().getPrice() - latestMarginCheckSell.get().getPrice()) : Optional.empty();
		int potentialProfit = flippingItem.getPotentialProfit(plugin.getConfig().marginCheckLoss(), plugin.getConfig().geLimitProfit());
		float roi = (float) profitEach / latestMarginCheckBuyPrice * 100;

		priceCheckBuyVal
			.setText(!latestMarginCheckBuy.isPresent() ? "N/A" : String.format(NUM_FORMAT, latestMarginCheckBuy.get().getPrice()) + " gp");
		priceCheckSellVal.
			setText(!latestMarginCheckSell.isPresent() ? "N/A" : String.format(NUM_FORMAT, latestMarginCheckSell.get().getPrice()) + " gp");

		latestBuyPriceVal.setText(latestBuyPrice.isPresent() ? String.format(NUM_FORMAT, latestBuyPrice.get()) + " gp" : "N/A");
		latestSellPriceVal.setText(latestSellPrice.isPresent() ? String.format(NUM_FORMAT, latestSellPrice.get()) + " gp" : "N/A");

		profitEachVal.setText((latestMarginCheckBuyPrice == 0 || latestMarginCheckSellPrice == 0) ? "N/A"
			: QuantityFormatter.quantityToRSDecimalStack(profitEach) + " gp");
		potentialProfitVal.setText((latestMarginCheckBuyPrice == 0 || latestMarginCheckSellPrice == 0 || potentialProfit < 0) ? "N/A" : QuantityFormatter
			.quantityToRSDecimalStack(potentialProfit) + " gp");

		roiLabel.setText("ROI:  " + ((latestMarginCheckBuyPrice == 0 || latestMarginCheckSellPrice == 0) ? "N/A"
			: String.format("%.2f", roi) + "%"));

		//Color gradient red-yellow-green depending on ROI.
		roiLabel.setForeground(UIUtilities.gradiatePercentage(roi, plugin.getConfig().roiGradientMax()));

		JLabel padLabel1 = new JLabel(" ");
		JLabel padLabel2 = new JLabel(" ");
		JLabel padLabel3 = new JLabel(" ");

		rightValuesPanel.add(priceCheckBuyVal);
		rightValuesPanel.add(priceCheckSellVal);
		rightValuesPanel.add(padLabel1);
		rightValuesPanel.add(latestBuyPriceVal);
		rightValuesPanel.add(latestSellPriceVal);
		rightValuesPanel.add(padLabel2);
		rightValuesPanel.add(profitEachVal);
		rightValuesPanel.add(potentialProfitVal);
		rightValuesPanel.add(padLabel3);
		rightValuesPanel.add(roiLabel);

		rightValuesPanel.setBorder(new EmptyBorder(2, 5, 2, 5));
		return rightValuesPanel;
	}

	/**
	 * Creates the panel which holds the labels on the left side of the FlippingItemPanel. These are labels such as
	 * "buy price", "sell price", etc.
	 *
	 * @return left labels panel
	 */
	private JPanel createLeftLabelsPanel()
	{
		JPanel leftInfoTextPanel = new JPanel(new GridLayout(10, 1));
		leftInfoTextPanel.setBackground(getBackground());
		/* Left labels */
		JLabel priceCheckBuyText = new JLabel("Price check buy: ");
		JLabel priceCheckSellText = new JLabel("Price check sell: ");
		JLabel latestBuyPrice = new JLabel("Last buy price: ");
		JLabel latestSellPrice = new JLabel("Last sell price: ");
		JLabel profitEachText = new JLabel("Profit each: ");
		JLabel profitTotalText = new JLabel("Potential profit: ");
		limitLabel = new JLabel();
		/* Left font colors */
		priceCheckBuyText.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		priceCheckSellText.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		latestBuyPrice.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		latestSellPrice.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		profitEachText.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		profitTotalText.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		limitLabel.setForeground(ColorScheme.GRAND_EXCHANGE_LIMIT);
		/* Tooltips */
		priceCheckBuyText.setToolTipText("The buy price according to your latest margin check. This is the price you insta sold the item for");
		priceCheckSellText.setToolTipText("The sell price according to your latest margin check. This is the price you insta bought the item for");
		profitEachText.setToolTipText("The profit margin according to your latest margin check");
		profitTotalText.setToolTipText("The potential profit according to your latest margin check and GE 4-hour limit");
		limitLabel.setToolTipText("The amount you can buy of this item every 4 hours.");

		JLabel padLabel1 = new JLabel(" ");
		JLabel padLabel2 = new JLabel(" ");
		JLabel padLabel3 = new JLabel(" ");

		leftInfoTextPanel.add(priceCheckBuyText);
		leftInfoTextPanel.add(priceCheckSellText);
		leftInfoTextPanel.add(padLabel1);
		leftInfoTextPanel.add(latestBuyPrice);
		leftInfoTextPanel.add(latestSellPrice);
		leftInfoTextPanel.add(padLabel2);
		leftInfoTextPanel.add(profitEachText);
		leftInfoTextPanel.add(profitTotalText);
		leftInfoTextPanel.add(padLabel3);
		leftInfoTextPanel.add(limitLabel);

		leftInfoTextPanel.setBorder(new EmptyBorder(2, 5, 2, 5));
		return leftInfoTextPanel;
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
	 * On initialization of a FlippingItemPanel and in FlippingPlugin by the scheduler which calls it
	 * every second.
	 */
	public void updatePriceOutdatedDisplay()
	{
		//Update time of latest price update.
		Instant latestBuyTime = flippingItem.getLatestMarginCheckBuyTime();
		Instant latestSellTime = flippingItem.getLatestMarginCheckSellTime();

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
			priceCheckBuyVal.setForeground(UIUtilities.OUTDATED_COLOR);
			priceCheckBuyVal.setToolTipText("<html>" + OUTDATED_STRING + "<br>" + latestBuyString + "</html>");
		}
		else
		{
			priceCheckBuyVal.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);
			priceCheckBuyVal.setToolTipText(latestBuyString);
		}
		//Sell value
		if (latestSellTimeAgo != 0 && latestSellTimeAgo / 60 > plugin.getConfig().outOfDateWarning())
		{
			priceCheckSellVal.setForeground(UIUtilities.OUTDATED_COLOR);
			priceCheckSellVal
				.setToolTipText("<html>" + OUTDATED_STRING + "<br>" + latestSellString + "</html>");
		}
		else
		{
			priceCheckSellVal.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);
			priceCheckSellVal.setToolTipText(latestSellString);
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
