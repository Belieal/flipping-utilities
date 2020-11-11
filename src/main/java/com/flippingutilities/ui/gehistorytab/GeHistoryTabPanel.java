package com.flippingutilities.ui.gehistorytab;

import com.flippingutilities.OfferEvent;
import com.flippingutilities.ui.utilities.UIUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;

/**
 * The panel that holds a view of the items in the trade history tab in the ge. This is so that users can manually add
 * trades they did while not using runelite (those trades will be in the GE trade history tab, assuming they are complete
 * and not too long ago).
 */
public class GeHistoryTabPanel extends JPanel
{
	public JPanel geHistoryTabOffersPanel = new JPanel();

	public GeHistoryTabPanel() {
		geHistoryTabOffersPanel.setBorder((new EmptyBorder(0, 0, 0, 6)));
		geHistoryTabOffersPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BorderLayout());
		add(createTitlePanel(), BorderLayout.NORTH);
		add(createOfferContainer(), BorderLayout.CENTER);
	}

	private JPanel createTitlePanel() {
		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		titlePanel.setBorder(new EmptyBorder(5,5,5,5));
		JLabel titleText = new JLabel("Grand Exchange History", SwingConstants.CENTER);
		titleText.setFont(new Font("Verdana", Font.BOLD, 15));
		titlePanel.add(titleText, BorderLayout.CENTER);
 		return titlePanel;
	}

	private JPanel createOfferContainer() {
		JPanel geHistoryTabOfferContainer = new JPanel(new BorderLayout());
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.add(geHistoryTabOffersPanel, BorderLayout.NORTH);

		JScrollPane scrollWrapper = new JScrollPane(wrapper);
		scrollWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollWrapper.getVerticalScrollBar().setPreferredSize(new Dimension(5, 0));
		scrollWrapper.getVerticalScrollBar().setBorder(new EmptyBorder(0, 0, 0, 0));

		geHistoryTabOfferContainer.add(scrollWrapper, BorderLayout.CENTER);
		geHistoryTabOfferContainer.setBorder(new EmptyBorder(5, 0, 0, 0));
		geHistoryTabOfferContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		return geHistoryTabOfferContainer;
	}

	public void rebuild(List<OfferEvent> offers) {
		SwingUtilities.invokeLater(() ->
		{
			geHistoryTabOffersPanel.removeAll();
			List<GeHistoryTabOfferPanel> offerPanels = offers.stream().map(o -> new GeHistoryTabOfferPanel(o)).collect(Collectors.toList());
			UIUtilities.stackPanelsVertically((List) offerPanels, geHistoryTabOffersPanel, 4);
			revalidate();
			repaint();
		});
	}
}
