package com.flippingutilities.ui.gehistorytab;

import com.flippingutilities.OfferEvent;
import com.flippingutilities.ui.utilities.UIUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.function.BiConsumer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;

public class GeHistoryTabOfferPanel extends JPanel
{
	private int offerId;
	private BiConsumer<Integer, Boolean> onCheckBoxChangeCallback;

	public GeHistoryTabOfferPanel(OfferEvent offer, int offerId, BiConsumer<Integer, Boolean> onCheckBoxChangeCallback) {
		this.offerId = offerId;
		this.onCheckBoxChangeCallback = onCheckBoxChangeCallback;
		setLayout(new BorderLayout());
		JCheckBox checkBox = new JCheckBox();
		checkBox.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		checkBox.setFocusPainted(false);
		checkBox.addItemListener(itemEvent -> onCheckBoxChangeCallback.accept(offerId, itemEvent.getStateChange() == itemEvent.SELECTED));
		add(checkBox, BorderLayout.WEST);
		add(createInfoPanel(offer), BorderLayout.CENTER);
	}

	public JPanel createInfoPanel(OfferEvent offer) {
		JPanel infoPanel = new JPanel(new BorderLayout());
		infoPanel.setBorder(new EmptyBorder(6,0,0,3));
		JLabel itemNameLabel = new JLabel(offer.getItemName(), SwingConstants.CENTER);

		JPanel offerDetailsPanel = new JPanel(new DynamicGridLayout(3, 1, 0, 0));

//		label.setFont(FontManager.getRunescapeSmallFont());
//		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel statePanel = new JPanel(new BorderLayout());
		JLabel leftStateLabel = new JLabel("State:");
		JLabel rightStateLabel = new JLabel(offer.getState() == GrandExchangeOfferState.BOUGHT? "Bought": "Sold");
		statePanel.add(leftStateLabel, BorderLayout.WEST);
		statePanel.add(rightStateLabel, BorderLayout.EAST);

		JPanel quantityPanel = new JPanel(new BorderLayout());
		JLabel leftQuantityLabel = new JLabel("Quantity:");
		JLabel rightQuantityLabel = new JLabel(String.valueOf(offer.getCurrentQuantityInTrade()));
		quantityPanel.add(leftQuantityLabel, BorderLayout.WEST);
		quantityPanel.add(rightQuantityLabel, BorderLayout.EAST);

		JPanel pricePanel = new JPanel(new BorderLayout());
		JLabel leftPriceLabel = new JLabel("Price Ea:");
		JLabel rightPriceLabel = new JLabel(UIUtilities.quantityToRSDecimalStack(offer.getPrice(), false));
		pricePanel.add(leftPriceLabel, BorderLayout.WEST);
		pricePanel.add(rightPriceLabel, BorderLayout.EAST);

		offerDetailsPanel.add(statePanel);
		offerDetailsPanel.add(quantityPanel);
		offerDetailsPanel.add(pricePanel);


		infoPanel.add(itemNameLabel, BorderLayout.NORTH);
		infoPanel.add(offerDetailsPanel, BorderLayout.CENTER);
		return infoPanel;
	}
}
