package com.flippingutilities.ui;

import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

/**
 * The panel that holds a view of the items in the trade history tab in the ge. This is so that users can manually add
 * trades they did while not using runelite (those trades will be in the GE trade history tab, assuming they are complete
 * and not too long ago).
 */
public class TradeHistoryTabPanel extends JPanel
{
	public TradeHistoryTabPanel() {
		setSize(500, 500);
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
	}

}
