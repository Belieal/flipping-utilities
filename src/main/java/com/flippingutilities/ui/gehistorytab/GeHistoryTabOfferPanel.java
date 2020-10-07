package com.flippingutilities.ui.gehistorytab;

import com.flippingutilities.OfferEvent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.api.GrandExchangeOfferState;

public class GeHistoryTabOfferPanel extends JPanel
{
	public GeHistoryTabOfferPanel(OfferEvent offer) {
		add(new JLabel(offer.getState() == GrandExchangeOfferState.BOUGHT? "Bought: ": "Sold: "));
		add(new JLabel(Integer.toString(offer.getItemId())));
		add(new JLabel("price:\n" + Integer.toString(offer.getPrice())));
		add(new JLabel("quantity: " + Integer.toString(offer.getCurrentQuantityInTrade())));
		setBorder(new EmptyBorder(0, 0, 0, 50));
	}
}
