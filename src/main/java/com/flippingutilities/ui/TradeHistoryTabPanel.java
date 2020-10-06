package com.flippingutilities.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * The panel that holds a view of the items in the trade history tab in the ge. This is so that users can manually add
 * trades they did while not using runelite (those trades will be in the GE trade history tab, assuming they are complete
 * and not too long ago).
 */
public class TradeHistoryTabPanel extends JPanel
{
	public TradeHistoryTabPanel() {
		String fonts[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();

		for ( int i = 0; i < fonts.length; i++ )
		{
			System.out.println(fonts[i]);
		}
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setLayout(new BorderLayout());
		add(createTitlePanel(), BorderLayout.NORTH);
	}

	private JPanel createTitlePanel() {
		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setBorder(new EmptyBorder(5,5,5,5));
		JLabel titleText = new JLabel("Grand Exchange History", SwingConstants.CENTER);
		titleText.setFont(new Font("Verdana", Font.BOLD, 15));
		titlePanel.add(titleText, BorderLayout.CENTER);
 		return titlePanel;
	}



}
