package com.flippingutilities.ui.gehistorytab;

import com.flippingutilities.OfferEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.client.ui.ColorScheme;

public class GeHistoryTabOfferPanel extends JPanel
{
	public GeHistoryTabOfferPanel(OfferEvent offer) {
		setLayout(new BorderLayout());
		//check box or some button should trigger calllback in plugin that also sets madeby attribute on offer
		JCheckBox checkBox = new JCheckBox();
		checkBox.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		checkBox.setFocusPainted(false);
		add(checkBox, BorderLayout.WEST);

		add(createInfoPanel(offer), BorderLayout.CENTER);
	}

	public JPanel createInfoPanel(OfferEvent offer) {
		JPanel infoPanel = new JPanel(new BorderLayout());
		JLabel itemNameLabel = new JLabel(offer.getItemName());

		infoPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		infoPanel.setBackground(ColorScheme.BRAND_ORANGE);
		infoPanel.add(new JLabel(offer.getState() == GrandExchangeOfferState.BOUGHT? "Bought: ": "Sold: "));
		infoPanel.add(new JLabel(Integer.toString(offer.getItemId())));
		infoPanel.add(new JLabel("price:\n" + offer.getPrice()));
		infoPanel.add(new JLabel("quantity: " + offer.getCurrentQuantityInTrade()));
		return infoPanel;
	}
}
