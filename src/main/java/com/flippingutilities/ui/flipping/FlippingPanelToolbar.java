package com.flippingutilities.ui.flipping;

import com.flippingutilities.Flip;
import com.flippingutilities.ui.utilities.UIUtilities;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;

public class FlippingPanelToolbar extends JPanel
{
	private Map<String, ImageIcon[]> BUTTON_STATES = new HashMap<>();
	private String buttonCurrentlyPressed;
	private JLabel[] toolbarButtons;

	FlippingPanelToolbar(Consumer<String> onButtonSelect) {
		createButtonStates();
		createToolBarButtons();
		addButtons(onButtonSelect,);
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		setBorder(new EmptyBorder(0, 0, 5, 0));
	}

	private void attachMouseListener(JLabel toolbarButton, Consumer<String> onButtonPress) {
		toolbarButton.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e))
				{
					if (!toolbarButton.getName().equals(buttonCurrentlyPressed)) {
						buttonCurrentlyPressed = toolbarButton.getName();
						toolbarButton.setIcon(BUTTON_STATES.get(toolbarButton.getName())[2]);
						onButtonPress.accept(buttonCurrentlyPressed);
						deselectOtherButtons(toolbarButton);

					}
					else {

					}

				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (!toolbarButton.getName().equals(buttonCurrentlyPressed)) {
					toolbarButton.setIcon(BUTTON_STATES.get(toolbarButton.getName())[1]);
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				if (!toolbarButton.getName().equals(buttonCurrentlyPressed)) {
					toolbarButton.setIcon(BUTTON_STATES.get(toolbarButton.getName())[0]);
				}
			}
		});
	}

	private void createButtonStates() {
		ImageIcon[] RECENT_BUTTON_STATES = {UIUtilities.SORT_BY_RECENT_OFF_ICON, UIUtilities.SORT_BY_RECENT_HALF_ON_ICON, UIUtilities.SORT_BY_RECENT_ON_ICON};
		ImageIcon[] FAVORITE_BUTTON_STATES = {UIUtilities.STAR_OFF_ICON, UIUtilities.STAR_HALF_ON_ICON, UIUtilities.STAR_ON_ICON};
		ImageIcon[] PROFIT_BUTTON_STATES = {UIUtilities.SORT_BY_PROFIT_OFF_ICON, UIUtilities.SORT_BY_PROFIT_HALF_ON_ICON, UIUtilities.SORT_BY_PROFIT_ON_ICON};
		ImageIcon[] ROI_BUTTON_STATES = {UIUtilities.SORT_BY_ROI_OFF_ICON, UIUtilities.SORT_BY_ROI_HALF_ON_ICON, UIUtilities.SORT_BY_ROI_ON_ICON};

		BUTTON_STATES.put("recent", RECENT_BUTTON_STATES);
		BUTTON_STATES.put("favorite", FAVORITE_BUTTON_STATES);
		BUTTON_STATES.put("profit", PROFIT_BUTTON_STATES);
		BUTTON_STATES.put("roi", ROI_BUTTON_STATES);
	}

	private void createToolBarButtons() {
		JLabel sortByRecent = new JLabel(UIUtilities.SORT_BY_RECENT_OFF_ICON);
		sortByRecent.setName("recent");
		sortByRecent.setToolTipText("Sort by last traded time");

		JLabel sortByROI = new JLabel(UIUtilities.SORT_BY_ROI_OFF_ICON);
		sortByROI.setName("roi");
		sortByROI.setToolTipText("Sort by ROI");

		JLabel sortByProfit = new JLabel(UIUtilities.SORT_BY_PROFIT_OFF_ICON);
		sortByProfit.setName("profit");
		sortByProfit.setToolTipText("Sort by potential profit");

		JLabel favoriteModifier = new JLabel(UIUtilities.STAR_OFF_ICON);
		favoriteModifier.setName("favorite");
		favoriteModifier.setToolTipText("view your favorite items");

		JLabel[] buttons = {sortByRecent, sortByROI, sortByProfit, favoriteModifier};
		toolbarButtons = buttons;
	}


	private void addButtons(Consumer<String> onButtonPress, Runnable ) {
		JLabel sortByRecent = new JLabel(UIUtilities.SORT_BY_RECENT_OFF_ICON);
		sortByRecent.setName("recent");
		sortByRecent.setToolTipText("Sort by last traded time");

		JLabel sortByROI = new JLabel(UIUtilities.SORT_BY_ROI_OFF_ICON);
		sortByROI.setName("roi");
		sortByROI.setToolTipText("Sort by ROI");

		JLabel sortByProfit = new JLabel(UIUtilities.SORT_BY_PROFIT_OFF_ICON);
		sortByProfit.setName("profit");
		sortByProfit.setToolTipText("Sort by potential profit");

		JLabel favoriteModifier = new JLabel(UIUtilities.STAR_OFF_ICON);
		favoriteModifier.setName("favorite");
		favoriteModifier.setToolTipText("view your favorite items");

		JLabel[] toolbarButtons = {sortByRecent, sortByROI, sortByProfit, favoriteModifier};

		for (int i = 0; i < toolbarButtons.length; i++)
		{
			attachMouseListener(toolbarButtons[i], onButtonPress);
			JPanel buttonPanel = new JPanel();
			buttonPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
			buttonPanel.add(toolbarButtons[i]);
			//don't set right border on last item
			if (i != toolbarButtons.length - 1)
			{
				buttonPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, ColorScheme.BRAND_ORANGE));
			}
			add(buttonPanel);
		}
	}

	private void deselectOtherButtons(JLabel toolBarButton) {
		for (int i = 0; i < toolbarButtons.length;i++) {
			if (toolbarButtons[i] != toolBarButton) {
				toolbarButtons[i].setIcon(BUTTON_STATES.get(toolbarButtons[i])[0]);
			}
		}
	}
}
