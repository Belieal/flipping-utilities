package com.flippingutilities.ui.statistics;

import com.flippingutilities.OfferInfo;
import com.flippingutilities.ui.utilities.UIUtilities;
import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.QuantityFormatter;

public class OfferPanel extends JPanel
{
	private JLabel title;
	private String action;
	private OfferInfo offer;

	public OfferPanel(OfferInfo offer)
	{
		JPanel panel = new JPanel();

		this.offer = offer;
		this.action = offer.isBuy()? "Bought": "Sold";
		this.title = new JLabel(QuantityFormatter.formatNumber(offer.getCurrentQuantityInTrade()) + " " + action
			+ " " + "(" + UIUtilities.formatDurationTruncated(offer.getTime()) + " ago)");

		title.setOpaque(true);
		title.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JPanel body = new JPanel(new BorderLayout());
		body.setBorder(new EmptyBorder(0, 2, 1, 2));

		JLabel priceLabel = new JLabel("Price Each:");
		JLabel priceVal = new JLabel(QuantityFormatter.formatNumber(offer.getPrice()) + " gp", SwingConstants.RIGHT);

		body.add(priceLabel, BorderLayout.WEST);
		body.add(priceVal, BorderLayout.EAST);

		panel.add(title, BorderLayout.NORTH);
		panel.add(body, BorderLayout.CENTER);

	}


	public void updateTimeDisplay()
	{
		title.setText(QuantityFormatter.formatNumber(offer.getCurrentQuantityInTrade()) + " " + action
			+ " " + "(" + UIUtilities.formatDurationTruncated(offer.getTime()) + " ago)");
	}

}
