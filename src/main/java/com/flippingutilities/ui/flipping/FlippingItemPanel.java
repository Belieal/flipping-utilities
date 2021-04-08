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

import com.flippingutilities.controller.FlippingPlugin;
import com.flippingutilities.model.FlippingItem;
import com.flippingutilities.model.OfferEvent;
import com.flippingutilities.model.Section;
import com.flippingutilities.ui.uiutilities.*;
import com.flippingutilities.utilities.WikiItemMargins;
import com.flippingutilities.utilities.WikiRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.QuantityFormatter;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;

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

	//All the labels that hold the actual values for these properties.
	JLabel wikiBuyVal = new JLabel();
	JLabel wikiSellVal = new JLabel();
	JLabel wikiBuyTimeVal = new JLabel();
	JLabel wikiSellTimeVal = new JLabel();
	JLabel instaSellVal = new JLabel();
	JLabel instaBuyVal = new JLabel();
	JLabel latestBuyPriceVal = new JLabel();
	JLabel latestSellPriceVal = new JLabel();
	JLabel profitEachVal = new JLabel();
	JLabel potentialProfitVal = new JLabel();
	JLabel roiLabelVal = new JLabel();
	JLabel geLimitVal = new JLabel();
	JLabel geRefreshCountdownLabel = new JLabel();
	JLabel wikiRequestCountDownTimer = new JLabel();
	//local time the ge limit will reset
	JLabel geRefreshAtLabel = new JLabel();

	//description labels
	JLabel wikiBuyText = new JLabel("Wiki insta buy: ");
	JLabel wikiSellText = new JLabel("Wiki insta sell: ");
	JLabel wikiBuyTimeText = new JLabel("Wiki insta buy age: ");
	JLabel wikiSellTimeText = new JLabel("Wiki insta sell age: ");
	JLabel instaSellText = new JLabel("Last insta sell: ");
	JLabel instaBuyText = new JLabel("Last insta buy: ");
	JLabel latestBuyPriceText = new JLabel("Last buy price: ");
	JLabel latestSellPriceText = new JLabel("Last sell price: ");
	JLabel profitEachText = new JLabel("Profit each: ");
	JLabel potentialProfitText = new JLabel("Potential profit: ");
	JLabel roiText = new JLabel("ROI:", JLabel.CENTER);
	JLabel geLimitText = new JLabel("GE limit:",JLabel.CENTER);

	JPanel itemInfo;

	JLabel searchCodeLabel;
	JLabel refreshIconLabel = new JLabel();

	WikiRequest wikiRequest;
	Instant timeOfRequestCompletion;

	FlippingItemPanel(final FlippingPlugin plugin, AsyncBufferedImage itemImage, final FlippingItem flippingItem)
	{
		this.flippingItem = flippingItem;
		this.plugin = plugin;
		flippingItem.validateGeProperties();
		setBackground(CustomColors.DARK_GRAY);
		setLayout(new BorderLayout());
		setBorder(new CompoundBorder(
				new EmptyBorder(5,5,0,0),
				new MatteBorder(0, 0, 5, 5, ColorScheme.DARKER_GRAY_COLOR.darker())));
		setToolTipText("Flipped by " + flippingItem.getFlippedBy());

		styleDescriptionLabels();
		styleValueLabels();
		setValueLabels();
		updateTimerDisplays();

		JPanel titlePanel = createTitlePanel(createItemIcon(itemImage), createItemNameLabel(), createFavoriteIcon());
		itemInfo = createItemInfoPanel();
		add(titlePanel, BorderLayout.NORTH);
		add(itemInfo, BorderLayout.CENTER);
		add(createBottomPanel(), BorderLayout.SOUTH);

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
		JPanel itemInfo = new JPanel();
		itemInfo.setLayout(new BoxLayout(itemInfo, BoxLayout.Y_AXIS));
		itemInfo.setBackground(getBackground());
		itemInfo.setBorder(new EmptyBorder(20,6,8,8));
		List<Section> sections = plugin.getDataHandler().viewAccountWideData().getFlippingItemPanelSections();
		for (Section section : sections) {
			itemInfo.add(createSectionPanel(section));
			itemInfo.add(Box.createVerticalStrut(5));
		}
		return itemInfo;
	}

	private JPanel createSectionPanel(Section section) {
		JPanel sectionPanel = new JPanel(new BorderLayout());
		sectionPanel.setBackground(getBackground());

		JLabel arrowIconLabel = new JLabel(section.isDefaultExpanded()? Icons.OPEN_ICON : Icons.CLOSE_ICON);
		arrowIconLabel.setVerticalAlignment(JLabel.NORTH);
		arrowIconLabel.setFont(FontManager.getRunescapeBoldFont());
		arrowIconLabel.setBorder(new EmptyBorder(0,0,0,5));

		sectionPanel.add(arrowIconLabel, BorderLayout.WEST);

		JPanel sectionItemsPanel = new JPanel();
		sectionItemsPanel.setLayout(new BoxLayout(sectionItemsPanel, BoxLayout.Y_AXIS));
		sectionItemsPanel.setBackground(getBackground());
		if (!section.isDefaultExpanded()) {
			arrowIconLabel.setText(section.getName());
			sectionItemsPanel.setVisible(false);
		}

		sectionPanel.add(sectionItemsPanel, BorderLayout.CENTER);
		arrowIconLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (sectionItemsPanel.isVisible()) {
					sectionItemsPanel.setVisible(false);
					arrowIconLabel.setIcon(Icons.CLOSE_ICON);
					arrowIconLabel.setText(section.getName());
				}
				else {
					sectionItemsPanel.setVisible(true);
					arrowIconLabel.setIcon(Icons.OPEN_ICON);
					arrowIconLabel.setText("");
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				super.mouseEntered(e);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				super.mouseExited(e);
			}
		});

		List<String> labelsToShow = new ArrayList<>();
		for (String labelName: section.getLabels().keySet()) {
			if (section.getLabels().get(labelName)) {
				labelsToShow.add(labelName);
			}
		}
		boolean isFirstInPair = true;
		for (String labelName : labelsToShow) {
			JPanel panel = createPanelForSectionLabel(labelName);
			if (isFirstInPair) {
				panel.setBorder(new EmptyBorder(6,0,3,0));
			}
			else {
				panel.setBorder(new EmptyBorder(2,0,8,0));
			}
			isFirstInPair = !isFirstInPair;
			sectionItemsPanel.add(panel);
		}

		return sectionPanel;
	}

	private JPanel createPanelForSectionLabel(String labelName) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(getBackground());
		JLabel descriptionLabel;
		JLabel valueLabel;
		switch (labelName) {
			case Section.WIKI_BUY_PRICE:
				descriptionLabel = wikiBuyText;
				valueLabel = wikiBuyVal;
				break;
			case Section.WIKI_SELL_PRICE:
				descriptionLabel = wikiSellText;
				valueLabel = wikiSellVal;
				break;
			case Section.PRICE_CHECK_BUY_PRICE:
				descriptionLabel = instaSellText;
				valueLabel = instaSellVal;
				makePropertyPanelEditable(panel, instaSellVal, instaSellText);
				break;
			case Section.PRICE_CHECK_SELL_PRICE:
				descriptionLabel = instaBuyText;
				valueLabel = instaBuyVal;
				makePropertyPanelEditable(panel, instaBuyVal, instaBuyText);
				break;
			case Section.LATEST_BUY_PRICE:
				descriptionLabel = latestBuyPriceText;
				valueLabel = latestBuyPriceVal;
				makePropertyPanelEditable(panel, latestBuyPriceVal, latestBuyPriceText);
				break;
			case Section.LATEST_SELL_PRICE:
				descriptionLabel = latestSellPriceText;
				valueLabel = latestSellPriceVal;
				makePropertyPanelEditable(panel, latestSellPriceVal, latestSellPriceText);
				break;
			case Section.PROFIT_EACH:
				descriptionLabel = profitEachText;
				valueLabel = profitEachVal;
				break;
			case Section.POTENTIAL_PROFIT:
				descriptionLabel = potentialProfitText;
				valueLabel = potentialProfitVal;
				break;
			case Section.ROI:
				descriptionLabel = roiText;
				valueLabel = roiLabelVal;
				break;
			case Section.REMAINING_GE_LIMIT:
				descriptionLabel = geLimitText;
				valueLabel = geLimitVal;
				break;
			case Section.GE_LIMIT_REFRESH_TIMER:
				//no description or value label for this. The timer is a panel itself.
				return createGeTimerPanel();
			default:
				//this should never be reached
				return new JPanel();
		}
		panel.add(descriptionLabel, BorderLayout.WEST);
		panel.add(valueLabel, BorderLayout.EAST);
		return panel;
	}

	private JPanel createBottomPanel() {
		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.setBackground(CustomColors.DARK_GRAY);
		bottomPanel.setBorder(new EmptyBorder(8,8,8,8));

		JLabel searchIconLabel = new JLabel(Icons.SEARCH);
		searchIconLabel.setToolTipText("Click to search realtime prices for this item!");
		searchIconLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				log.info(UIUtilities.buildWikiLink(flippingItem.getItemId()));
				LinkBrowser.browse(UIUtilities.buildWikiLink(flippingItem.getItemId()));
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				searchIconLabel.setIcon(Icons.SEARCH_HOVER);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				searchIconLabel.setIcon(Icons.SEARCH);
			}
		});

		TextField searchCodeTextField = new TextField(10);

		JPanel searchCodePanel = new JPanel();
		searchCodePanel.setBorder(new EmptyBorder(0,0,0,4));
		searchCodePanel.setBackground(CustomColors.DARK_GRAY);
		searchCodePanel.setPreferredSize(new Dimension(0,20));
		searchCodeLabel = new JLabel("<html> quick search code: " + UIUtilities.colorText(flippingItem.getFavoriteCode(), CustomColors.VIBRANT_YELLOW) + "</html>", JLabel.CENTER);
		if (flippingItem.isFavorite()) {
			searchCodeLabel.setText("<html> quick search code: " + UIUtilities.colorText(flippingItem.getFavoriteCode(), ColorScheme.GRAND_EXCHANGE_PRICE) + "</html>");
		}
		else {
			searchCodeLabel.setText("<html> quick search code: " + UIUtilities.colorText("N/A", CustomColors.VIBRANT_YELLOW) + "</html>");
		}
		searchCodeLabel.setToolTipText("<html>If you have favorited this item, you can type the search code when you are <br>" +
				"searching for items in the ge to populate your ge results with any item with this code</html>");
		searchCodeLabel.setFont(FontManager.getRunescapeSmallFont());

		searchCodePanel.add(searchCodeLabel);

		final boolean[] isHighlighted = {false};
		MouseListener l = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (!flippingItem.isFavorite()) {
					JOptionPane.showMessageDialog(searchCodeLabel, "<html>Item is not favorited.<br> Favorite the item to be able to use/edit the quick search code</html>");
					return;
				}

				if (isHighlighted[0]) {
					searchCodePanel.remove(searchCodeTextField);
					searchCodePanel.add(searchCodeLabel);
					isHighlighted[0] = false;
				}
				else {
					searchCodePanel.remove(searchCodeLabel);
					searchCodePanel.add(searchCodeTextField);
					isHighlighted[0] = true;
				}
				repaint();
				revalidate();
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				searchCodePanel.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				searchCodePanel.setBackground(getBackground());
			}
		};
		searchCodePanel.addMouseListener(l);
		searchCodeLabel.addMouseListener(l);

		searchCodeTextField.setBackground(ColorScheme.DARK_GRAY_COLOR);
		searchCodeTextField.setText(flippingItem.getFavoriteCode());
		searchCodeTextField.addActionListener(e -> {
			isHighlighted[0] = false;
			if (plugin.getAccountCurrentlyViewed().equals(FlippingPlugin.ACCOUNT_WIDE)) {
				plugin.setFavoriteCodeOnAllAccounts(flippingItem, searchCodeTextField.getText());
			}
			else {
				plugin.markAccountTradesAsHavingChanged(plugin.getAccountCurrentlyViewed());
			}

			flippingItem.setFavoriteCode(searchCodeTextField.getText());

			searchCodeLabel.setText("<html> quick search code: " + UIUtilities.colorText(flippingItem.getFavoriteCode(), CustomColors.VIBRANT_YELLOW) + "</html>");

			searchCodePanel.remove(searchCodeTextField);
			searchCodePanel.add(searchCodeLabel);
			repaint();
			revalidate();
		});

		JPanel refreshIconPanel = new JPanel();
		refreshIconPanel.setLayout(new BoxLayout(refreshIconPanel, BoxLayout.X_AXIS));
		refreshIconPanel.setBackground(getBackground());

		refreshIconLabel.setIcon(Icons.REFRESH);
		refreshIconLabel.setDisabledIcon(Icons.REFRESH_HOVER);

		refreshIconPanel.add(refreshIconLabel);
		refreshIconPanel.add(Box.createHorizontalStrut(2));
		refreshIconPanel.add(wikiRequestCountDownTimer);

		bottomPanel.add(refreshIconPanel, BorderLayout.WEST);
		bottomPanel.add(searchIconLabel, BorderLayout.EAST);
		bottomPanel.add(searchCodePanel, BorderLayout.CENTER);
		return bottomPanel;
	}

	private void makePropertyPanelEditable(JPanel propertyPanel, JLabel valueLabel, JLabel descriptionLabel) {
		final boolean[] isHighlighted = {false};
		TextField textField = new TextField(10);
		textField.setBackground(ColorScheme.DARK_GRAY_COLOR);
		String currentText = valueLabel.getText();
		String textWithoutGp = currentText.substring(0, currentText.length()-3);
		textField.setText(textWithoutGp);
		textField.addActionListener((e1 -> {
			isHighlighted[0] = false;
			try {
				int num = Integer.parseInt(textField.getText().replace(",", ""));
				if (num <= 0) {
					JOptionPane.showMessageDialog(this,"You cannot input zero or a negative number");
					return;
				}
				valueLabel.setText(String.format(NUM_FORMAT, num) + " gp");
				OfferEvent dummyOffer;
				if (valueLabel == instaSellVal) {
					dummyOffer = OfferEvent.dummyOffer(false, true, num, flippingItem.getItemId(), flippingItem.getItemName());
					flippingItem.setLatestInstaSell(Optional.of(dummyOffer));
				}
				else if (valueLabel == instaBuyVal){
					dummyOffer = OfferEvent.dummyOffer(true, true, num, flippingItem.getItemId(), flippingItem.getItemName());
					flippingItem.setLatestInstaBuy(Optional.of(dummyOffer));
				}
				else if (valueLabel == latestBuyPriceVal){
					dummyOffer = OfferEvent.dummyOffer(true, false, num, flippingItem.getItemId(), flippingItem.getItemName());
					flippingItem.setLatestBuy(Optional.of(dummyOffer));
				}
				else {
					dummyOffer = OfferEvent.dummyOffer(false, false, num, flippingItem.getItemId(), flippingItem.getItemName());
					flippingItem.setLatestSell(Optional.of(dummyOffer));
				}

				setValueLabels();
			}
			catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(this, "You need to input a number");
				return;
			}
			propertyPanel.remove(textField);
			propertyPanel.add(valueLabel, BorderLayout.EAST);
			revalidate();
			repaint();
		}));
		MouseAdapter m = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (isHighlighted[0]) {
					isHighlighted[0] = false;
					propertyPanel.remove(textField);
					propertyPanel.add(valueLabel, BorderLayout.EAST);
				}
				else {
					isHighlighted[0] = true;
					propertyPanel.remove(valueLabel);
					propertyPanel.add(textField, BorderLayout.EAST);
				}
				revalidate();
				repaint();
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				propertyPanel.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				propertyPanel.setBackground(CustomColors.DARK_GRAY);
			}
		};
		propertyPanel.addMouseListener(m);
		descriptionLabel.addMouseListener(m);
	}

	private JPanel createGeTimerPanel() {
		JPanel geRefreshTimePanel = new JPanel(new DynamicGridLayout(2,1,0, 2));
		geRefreshTimePanel.setBorder(new EmptyBorder(8,0,5,30));
		geRefreshTimePanel.setBackground(CustomColors.DARK_GRAY);
		geRefreshTimePanel.add(geRefreshCountdownLabel);
		geRefreshTimePanel.add(geRefreshAtLabel);
		return geRefreshTimePanel;
	}

	private void styleValueLabels() {
		Arrays.asList(latestBuyPriceVal, latestSellPriceVal, instaSellVal, instaBuyVal, profitEachVal, potentialProfitVal,
				roiLabelVal, geLimitVal).
				forEach(label -> {
					label.setHorizontalAlignment(JLabel.RIGHT);
					label.setFont(plugin.getFont());
				});

		instaSellVal.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);
		instaBuyVal.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);

		profitEachVal.setForeground(CustomColors.PROFIT_COLOR);
		potentialProfitVal.setForeground(CustomColors.PROFIT_COLOR);

		geRefreshCountdownLabel.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		geRefreshCountdownLabel.setFont(FontManager.getRunescapeBoldFont());
		geRefreshCountdownLabel.setHorizontalAlignment(JLabel.CENTER);
		geRefreshCountdownLabel.setToolTipText("This is a timer displaying how much time is left before the GE limit refreshes for this item");
		geRefreshCountdownLabel.setBorder(new EmptyBorder(0,0,0,20));

		geRefreshAtLabel.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		geRefreshAtLabel.setFont(FontManager.getRunescapeSmallFont());
		geRefreshAtLabel.setHorizontalAlignment(JLabel.CENTER);
		geRefreshAtLabel.setToolTipText("This shows the local time when the ge limit will refresh");
		geRefreshAtLabel.setBorder(new EmptyBorder(0,0,0,20));

		roiLabelVal.setToolTipText("<html>Return on investment:<br>Percentage of profit relative to gp invested</html>");

		wikiBuyVal.setFont(CustomFonts.SMALLER_RS_BOLD_FONT);
		wikiBuyVal.setForeground(Color.WHITE);
		wikiSellVal.setFont(CustomFonts.SMALLER_RS_BOLD_FONT);
		wikiSellVal.setForeground(Color.WHITE);

		wikiRequestCountDownTimer.setAlignmentY(JLabel.TOP);
		wikiRequestCountDownTimer.setFont(new Font(Font.SERIF, Font.PLAIN, 9));

		JPopupMenu popup = new JPopupMenu();
		popup.add(createWikiHoverTimePanel());
		UIUtilities.addPopupOnHover(wikiBuyVal, popup);
		UIUtilities.addPopupOnHover(wikiSellVal, popup);
	}

	private void styleDescriptionLabels() {
		Arrays.asList(wikiBuyText, wikiSellText, latestBuyPriceText, latestSellPriceText, instaSellText, instaBuyText, profitEachText, potentialProfitText, geLimitText, roiText).
				forEach(label -> {
					label.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
					label.setFont(plugin.getFont());
				});

		/* Tooltips */
		instaSellText.setToolTipText("The buy price according to your latest margin check. This is the price you insta sold the item for");
		instaBuyText.setToolTipText("The sell price according to your latest margin check. This is the price you insta bought the item for");
		latestBuyPriceText.setToolTipText("The last price you bought this item for");
		latestSellPriceText.setToolTipText("The last price you sold this item for");
		profitEachText.setToolTipText("The profit margin according to your latest margin check");
		potentialProfitText.setToolTipText("The potential profit according to your latest margin check and GE 4-hour limit");
		geLimitText.setToolTipText("Remaining ge limit");

		if (flippingItem.getTotalGELimit() <= 0) {
			geLimitText.setText("Bought:");
			geLimitText.setToolTipText("Item has unknown limit, so this just displays how many you have bought in a 4 hour window");
		}
	}

	/**
	 * Creates the title panel which holds the item icon, delete button (shows up only when you hover over the item icon),
	 * the item name label, and the favorite button.
	 *
	 * @param itemIcon
	 * @param itemNameLabel
	 * @param favoriteButton
	 * @return
	 */
	private JPanel createTitlePanel(JLabel itemIcon, JLabel itemNameLabel, JLabel favoriteButton)
	{
		CustomizationPanel customizationPanel = new CustomizationPanel(plugin);
		JDialog customizationModal = UIUtilities.createModalFromPanel(this, customizationPanel);

		JLabel customizeLabel = new JLabel("customize look", JLabel.CENTER);
		Color c = customizeLabel.getForeground();
		customizeLabel.setFont(FontManager.getRunescapeSmallFont());
		Font font=new Font(customizeLabel.getFont().getName(),Font.ITALIC,customizeLabel.getFont().getSize());
		customizeLabel.setFont(font);
		UIUtilities.makeLabelUnderlined(customizeLabel);
		customizeLabel.setHorizontalAlignment(JLabel.CENTER);
		customizeLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				customizationPanel.rebuild(plugin.getDataHandler().viewAccountWideData().getFlippingItemPanelSections());
				customizationModal.setVisible(true);
				customizationModal.pack();
				customizationModal.setLocation(getLocationOnScreen().x - customizationModal.getWidth() - 10 , getLocationOnScreen().y - customizationModal.getHeight()/2);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				customizeLabel.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				customizeLabel.setForeground(c);
			}
		});

		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setBackground(getBackground());
		titlePanel.add(itemIcon, BorderLayout.WEST);
		titlePanel.add(itemNameLabel, BorderLayout.CENTER);
		titlePanel.add(favoriteButton, BorderLayout.EAST);
		titlePanel.add(customizeLabel, BorderLayout.SOUTH);
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
		Icon itemIcon = new ImageIcon(itemImage);
		JLabel itemIconLabel = new JLabel(itemIcon);
		itemIconLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		itemIconLabel.setPreferredSize(Icons.ICON_SIZE);

		itemIconLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				flippingItem.setValidFlippingPanelItem(false);
				if (!plugin.getAccountCurrentlyViewed().equals(FlippingPlugin.ACCOUNT_WIDE)) {
					plugin.markAccountTradesAsHavingChanged(plugin.getAccountCurrentlyViewed());
				}
				plugin.getFlippingPanel().rebuild(plugin.viewTradesForCurrentView());
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				itemIconLabel.setIcon(Icons.DELETE_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				itemIconLabel.setIcon(itemIcon);
			}
		});

		return itemIconLabel;
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
		favoriteIcon.setIcon(flippingItem.isFavorite() ? Icons.STAR_ON_ICON : Icons.STAR_OFF_ICON);
		favoriteIcon.setAlignmentX(Component.RIGHT_ALIGNMENT);
		favoriteIcon.setPreferredSize(new Dimension(24, 24));
		favoriteIcon.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (plugin.getAccountCurrentlyViewed().equals(FlippingPlugin.ACCOUNT_WIDE))
				{
					plugin.setFavoriteOnAllAccounts(flippingItem, !flippingItem.isFavorite());
				}
				else {
					plugin.markAccountTradesAsHavingChanged(plugin.getAccountCurrentlyViewed());
				}
				flippingItem.setFavorite(!flippingItem.isFavorite());
				favoriteIcon.setIcon(flippingItem.isFavorite()? Icons.STAR_ON_ICON:Icons.STAR_OFF_ICON);

				if (flippingItem.isFavorite()) {
					searchCodeLabel.setText("<html> quick search code: " + UIUtilities.colorText(flippingItem.getFavoriteCode(), ColorScheme.GRAND_EXCHANGE_PRICE) + "</html>");
				}
				else {
					searchCodeLabel.setText("<html> quick search code: " + UIUtilities.colorText("N/A", CustomColors.VIBRANT_YELLOW) + "</html>");
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (!flippingItem.isFavorite())
				{
					favoriteIcon.setIcon(Icons.STAR_HALF_ON_ICON);
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				if (!flippingItem.isFavorite())
				{
					favoriteIcon.setIcon(Icons.STAR_OFF_ICON);
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

	public void setValueLabels() {
		Optional<OfferEvent> latestMarginCheckBuy = flippingItem.getLatestInstaBuy();
		Optional<OfferEvent> latestMarginCheckSell = flippingItem.getLatestInstaSell();

		Optional<OfferEvent> latestBuy = flippingItem.getLatestBuy();
		Optional<OfferEvent> latestSell = flippingItem.getLatestSell();

		Optional<Integer> profitEach = flippingItem.getCurrentProfitEach();
		Optional<Integer> potentialProfit = flippingItem.getPotentialProfit(plugin.getConfig().marginCheckLoss(), plugin.getConfig().geLimitProfit());

		Optional<Float> roi =  flippingItem.getCurrentRoi();

		instaSellVal.setText(latestMarginCheckSell.isPresent() ? String.format(NUM_FORMAT, latestMarginCheckSell.get().getPrice()) + " gp":"N/A");
		instaBuyVal.setText(latestMarginCheckBuy.isPresent() ? String.format(NUM_FORMAT, latestMarginCheckBuy.get().getPrice()) + " gp" : "N/A");

		latestBuyPriceVal.setText(latestBuy.isPresent() ? String.format(NUM_FORMAT, latestBuy.get().getPrice()) + " gp" : "N/A");
		latestSellPriceVal.setText(latestSell.isPresent() ? String.format(NUM_FORMAT, latestSell.get().getPrice()) + " gp" : "N/A");

		profitEachVal.setText(profitEach.isPresent()? QuantityFormatter.quantityToRSDecimalStack(profitEach.get()) + " gp": "N/A");
		potentialProfitVal.setText(potentialProfit.isPresent() ? QuantityFormatter.quantityToRSDecimalStack(potentialProfit.get()) + " gp": "N/A");

		roiLabelVal.setText(roi.isPresent()? String.format("%.2f", roi.get()) + "%" : "N/A");
		//Color gradient red-yellow-green depending on ROI.
		roiLabelVal.setForeground(UIUtilities.gradiatePercentage(roi.orElse(0F), plugin.getConfig().roiGradientMax()));

		if (flippingItem.getTotalGELimit() > 0) {
			geLimitVal.setText(String.format(NUM_FORMAT, flippingItem.getRemainingGeLimit()));
		} else {
			geLimitVal.setText(String.format(NUM_FORMAT, flippingItem.getItemsBoughtThisLimitWindow()));
			//can't have potential profit if the limit is unknown
			potentialProfitVal.setText("N/A");
		}
		updateWikiLabels(plugin.getLastWikiRequest(), plugin.getTimeOfLastWikiRequest());
	}

	public void updateTimerDisplays() {
		flippingItem.validateGeProperties();

		geRefreshCountdownLabel.setText(flippingItem.getGeLimitResetTime() == null?
				TimeFormatters.formatDuration(Duration.ZERO):
				TimeFormatters.formatDuration(Instant.now(), flippingItem.getGeLimitResetTime()));

		//need to update this so it can be reset when the timer runs down.
		if (flippingItem.getTotalGELimit() > 0) {
			geLimitVal.setText(String.format(NUM_FORMAT, flippingItem.getRemainingGeLimit()));
		} else {
			geLimitVal.setText(String.format(NUM_FORMAT, flippingItem.getItemsBoughtThisLimitWindow()));
		}

		geRefreshAtLabel.setText(flippingItem.getGeLimitResetTime() == null? "Now": TimeFormatters.formatTime(flippingItem.getGeLimitResetTime(), true, false));
	}

	public void updateWikiLabels(WikiRequest wr, Instant requestCompletionTime) {
		timeOfRequestCompletion = requestCompletionTime;
		wikiRequest = wr;

		if (wikiRequest == null) {
			wikiBuyVal.setText("N/A");
			wikiSellVal.setText("N/A");
			return;
		}

		WikiItemMargins wikiItemInfo = wikiRequest.getData().get(flippingItem.getItemId());
		wikiBuyVal.setText(wikiItemInfo.getHigh()==0? "No data":QuantityFormatter.formatNumber(wikiItemInfo.getHigh()) + " gp");
		wikiSellVal.setText(wikiItemInfo.getLow()==0? "No data":QuantityFormatter.formatNumber(wikiItemInfo.getLow()) + " gp");
		updateWikiTimeLabels();
	}

	public void updateWikiTimeLabels() {
		//can be called before wikiRequest is set cause is is called in the repeating task which can start before the
		//request is completed
		if (wikiRequest == null) {
			wikiBuyTimeVal.setText("Request not made yet");
			wikiSellTimeVal.setText("Request not made yet");
			wikiRequestCountDownTimer.setText("N/A");
			return;
		}
		//probably don't need this. Should always be non null if wikiRequest is not null
		if (timeOfRequestCompletion != null) {
			long secondsSinceLastRequestCompleted = Instant.now().getEpochSecond() - timeOfRequestCompletion.getEpochSecond();
			if (secondsSinceLastRequestCompleted >= 60) {
				wikiRequestCountDownTimer.setText("0");
				refreshIconLabel.setEnabled(true);
			}
			else {
				refreshIconLabel.setEnabled(false);
				wikiRequestCountDownTimer.setText(String.valueOf(60 - secondsSinceLastRequestCompleted));
			}
		}

		WikiItemMargins wikiItemInfo = wikiRequest.getData().get(flippingItem.getItemId());
		if (wikiItemInfo.getHighTime() == 0) {
			wikiBuyTimeVal.setText("No data");
		}
		else {
			wikiBuyTimeVal.setText(TimeFormatters.formatDuration(Instant.ofEpochSecond(wikiItemInfo.getHighTime())));
		}
		if (wikiItemInfo.getLowTime() == 0) {
			wikiBuyTimeVal.setText("No data");
		}
		else {
			wikiSellTimeVal.setText(TimeFormatters.formatDuration(Instant.ofEpochSecond(wikiItemInfo.getLowTime())));
		}
	}

	//panel that is shown when someone hovers over the wiki buy/sell value labels
	private JPanel createWikiHoverTimePanel() {
		wikiBuyTimeText.setFont(FontManager.getRunescapeSmallFont());
		wikiBuyTimeText.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		wikiSellTimeText.setFont(FontManager.getRunescapeSmallFont());
		wikiSellTimeText.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);

		wikiSellTimeVal.setFont(FontManager.getRunescapeSmallFont());
		wikiBuyTimeVal.setFont(FontManager.getRunescapeSmallFont());

		JPanel wikiTimePanel = new JPanel();
		wikiTimePanel.setLayout(new BoxLayout(wikiTimePanel, BoxLayout.Y_AXIS));
		wikiTimePanel.setBorder(new EmptyBorder(5,5,5,5));

		JPanel buyTimePanel = new JPanel(new BorderLayout());
		buyTimePanel.add(wikiBuyTimeText, BorderLayout.WEST);
		buyTimePanel.add(wikiBuyTimeVal, BorderLayout.EAST);

		JPanel sellTimePanel = new JPanel(new BorderLayout());
		sellTimePanel.add(wikiSellTimeText, BorderLayout.WEST);
		sellTimePanel.add(wikiSellTimeVal, BorderLayout.EAST);

		wikiTimePanel.add(buyTimePanel);
		wikiTimePanel.add(Box.createVerticalStrut(5));
		wikiTimePanel.add(sellTimePanel);

		return wikiTimePanel;
	}

}
