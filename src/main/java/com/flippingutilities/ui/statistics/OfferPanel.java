package com.flippingutilities.ui.statistics;

import com.flippingutilities.OfferInfo;
import com.flippingutilities.ui.utilities.UIUtilities;
import java.awt.BorderLayout;
import java.util.Arrays;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.QuantityFormatter;

public class OfferPanel extends JPanel
{
	private JLabel title;
	private String action;
	private OfferInfo offer;

	public OfferPanel(OfferInfo offer)
	{
		setLayout(new BorderLayout());
		this.offer = offer;
		this.action = offer.isBuy()? "Bought": "Sold";
		this.title = new JLabel(QuantityFormatter.formatNumber(offer.getCurrentQuantityInTrade()) + " " + action
			+ " " + "(" + UIUtilities.formatDurationTruncated(offer.getTime()) + " ago)", SwingConstants.CENTER);

		title.setOpaque(true);
		title.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		title.setBorder(new EmptyBorder(3,0,2,0));

		JPanel body = new JPanel(new BorderLayout());
		body.setBorder(new EmptyBorder(0, 2, 1, 2));

		JLabel priceLabel = new JLabel("Price Each:");
		JLabel priceVal = new JLabel(QuantityFormatter.formatNumber(offer.getPrice()) + " gp", SwingConstants.RIGHT);

		Arrays.asList(title, priceLabel, priceVal).forEach(label -> {
			label.setFont(FontManager.getRunescapeSmallFont());
			label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		});

		body.add(priceLabel, BorderLayout.WEST);
		body.add(priceVal, BorderLayout.EAST);

		add(title, BorderLayout.NORTH);
		add(body, BorderLayout.CENTER);
	}


	public void updateTimeDisplay()
	{
		title.setText(QuantityFormatter.formatNumber(offer.getCurrentQuantityInTrade()) + " " + action
			+ " " + "(" + UIUtilities.formatDurationTruncated(offer.getTime()) + " ago)");
	}

}
