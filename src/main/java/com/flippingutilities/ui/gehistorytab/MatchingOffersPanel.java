package com.flippingutilities.ui.gehistorytab;

import com.flippingutilities.OfferEvent;
import com.flippingutilities.ui.utilities.UIUtilities;
import static com.flippingutilities.ui.utilities.UIUtilities.CLOSE_ICON;
import static com.flippingutilities.ui.utilities.UIUtilities.OPEN_ICON;
import java.awt.BorderLayout;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

public class MatchingOffersPanel extends JPanel
{

	MatchingOffersPanel(List<OfferEvent> matchingOffers) {
		setLayout(new BorderLayout());


		JPanel titlePanel = new JPanel(new BorderLayout());
		String plural = matchingOffers.size() == 1? "match":"matches";
		String titleText = matchingOffers.size() < 5? matchingOffers.size() + " " + plural + " in history": "At least 5 matches in history";
		JLabel titleTextLabel = new JLabel(titleText);
		titleTextLabel.setForeground(matchingOffers.size() == 0? ColorScheme.GRAND_EXCHANGE_PRICE: ColorScheme.GRAND_EXCHANGE_ALCH);
		JLabel collapsePanelIconLabel = new JLabel(OPEN_ICON);
		titlePanel.add(titleTextLabel, BorderLayout.CENTER);
		titlePanel.add(collapsePanelIconLabel, BorderLayout.EAST);


		List<JPanel> matchingOfferPanels = matchingOffers.stream().map(o -> createMatchingOfferPanel(o)).collect(Collectors.toList());
		JPanel matchingOffersPanelBody = UIUtilities.stackPanelsVertically(matchingOfferPanels, 2);

		add(titlePanel, BorderLayout.NORTH);
		add(matchingOffersPanelBody, BorderLayout.CENTER);
	}


	JPanel createMatchingOfferPanel(OfferEvent offerEvent) {
		JPanel matchingOfferPanel = new JPanel(new BorderLayout());
		matchingOfferPanel.add(new JLabel("Made:"), BorderLayout.WEST);
		matchingOfferPanel.add(new JLabel(UIUtilities.formatDurationTruncated(offerEvent.getTime()) + " ago"), BorderLayout.EAST);
		return matchingOfferPanel;
	}
}
