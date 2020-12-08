package com.flippingutilities.ui.gehistorytab;

import com.flippingutilities.model.OfferEvent;
import com.flippingutilities.ui.uiutilities.Icons;
import com.flippingutilities.ui.uiutilities.TimeFormatters;
import com.flippingutilities.ui.uiutilities.UIUtilities;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

public class MatchingOffersPanel extends JPanel
{
	MatchingOffersPanel(List<OfferEvent> matchingOffers) {
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JPanel titlePanel = new JPanel(new BorderLayout());
		String plural = matchingOffers.size() == 1? "match":"matches";
		String titleText = matchingOffers.size() < 5? matchingOffers.size() + " " + plural + " in history": "At least 5 matches";
		JLabel titleTextLabel = new JLabel(titleText, SwingConstants.CENTER);
		titleTextLabel.setFont(FontManager.getRunescapeBoldFont());
		titlePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		titleTextLabel.setForeground(matchingOffers.size() == 0? ColorScheme.GRAND_EXCHANGE_PRICE: ColorScheme.GRAND_EXCHANGE_ALCH);
		JLabel collapsePanelIconLabel = new JLabel(Icons.OPEN_ICON);
		titlePanel.add(titleTextLabel, BorderLayout.CENTER);
		titlePanel.add(collapsePanelIconLabel, BorderLayout.EAST);
		titlePanel.setBorder(new EmptyBorder(3,0,0,0));
		titlePanel.setToolTipText("<html>Having a matching offer in your history means you made an offer in the past with the same quantity, price and buy/sell state.<br>" +
			"Having 0 matching offers means the plugin did not track the trade (perhaps you made it on mobile) and it's safe to add manually.<br>" +
			"If you have matching offers it still might be safe to manually add the offer but you have to decide whether the offer you are <br>" +
			"adding is one of the matching offers or not to make sure you are not adding an offer already tracked in your history.</html>");

		List<JPanel> matchingOfferPanels = matchingOffers.stream().map(o -> createMatchingOfferPanel(o)).collect(Collectors.toList());
		JPanel matchingOffersPanelBody = UIUtilities.stackPanelsVertically(matchingOfferPanels, 2);
		matchingOffersPanelBody.setVisible(false);

		titlePanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					if (matchingOffersPanelBody.isVisible())
					{
						matchingOffersPanelBody.setVisible(false);
						collapsePanelIconLabel.setIcon(Icons.OPEN_ICON);
					}
					else
					{
						matchingOffersPanelBody.setVisible(true);
						collapsePanelIconLabel.setIcon(Icons.CLOSE_ICON);
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				titlePanel.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				titlePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}
		});

		add(titlePanel, BorderLayout.NORTH);
		add(matchingOffersPanelBody, BorderLayout.CENTER);
	}

	JPanel createMatchingOfferPanel(OfferEvent offerEvent) {
		JPanel matchingOfferPanel = new JPanel(new BorderLayout());
		matchingOfferPanel.add(new JLabel("Same Offer Made:"), BorderLayout.WEST);
		matchingOfferPanel.add(new JLabel(TimeFormatters.formatDurationTruncated(offerEvent.getTime()) + " ago"), BorderLayout.EAST);
		return matchingOfferPanel;
	}
}
