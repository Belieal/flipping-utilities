package com.flippingutilities.ui.statistics;

import com.flippingutilities.OfferEvent;
import com.flippingutilities.ui.utilities.UIUtilities;
import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.QuantityFormatter;

public class OfferPanel extends JPanel
{
	private JLabel title;
	private String action;
	private OfferEvent offer;

	public OfferPanel(OfferEvent offer)
	{
		setLayout(new BorderLayout());
		this.offer = offer;
		this.action = offer.isBuy() ? "Bought" : "Sold";
		this.title = new JLabel(QuantityFormatter.formatNumber(offer.getCurrentQuantityInTrade()) + " " + action
			+ " " + "(" + UIUtilities.formatDurationTruncated(offer.getTime()) + " ago)", SwingConstants.CENTER);

		title.setOpaque(true);
		title.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		title.setFont(FontManager.getRunescapeSmallFont());
		title.setForeground(offer.isBuy() ? UIUtilities.OUTDATED_COLOR : ColorScheme.GRAND_EXCHANGE_PRICE);

		JPanel body = new JPanel(new DynamicGridLayout(2, 2, 0, 2));
		body.setBorder(new EmptyBorder(0, 2, 1, 2));

		JLabel priceLabel = new JLabel("Price:");
		JLabel priceVal = new JLabel(QuantityFormatter.formatNumber(offer.getPrice()) + " gp", SwingConstants.RIGHT);

		JLabel totalPriceLabel = new JLabel("Total:");
		JLabel totalPriceVal = new JLabel(QuantityFormatter.formatNumber(offer.getPrice() * offer.getCurrentQuantityInTrade()) + " gp", SwingConstants.RIGHT);

		JLabel[] descriptions = {priceLabel, totalPriceLabel};
		JLabel[] vals = {priceVal, totalPriceVal};

		for (int i = 0; i < descriptions.length; i++)
		{
			JLabel descriptionLabel = descriptions[i];
			JLabel valLabel = vals[i];

			descriptionLabel.setFont(FontManager.getRunescapeSmallFont());
			descriptionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			valLabel.setFont(FontManager.getRunescapeSmallFont());
			valLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

			JPanel infoPanel = new JPanel(new BorderLayout());
			infoPanel.add(descriptionLabel, BorderLayout.WEST);
			infoPanel.add(valLabel, BorderLayout.EAST);
			body.add(infoPanel);
		}

		add(title, BorderLayout.NORTH);
		add(body, BorderLayout.CENTER);
	}


	public void updateTimeDisplay()
	{
		title.setText(QuantityFormatter.formatNumber(offer.getCurrentQuantityInTrade()) + " " + action
			+ " " + "(" + UIUtilities.formatDurationTruncated(offer.getTime()) + " ago)");
	}
}