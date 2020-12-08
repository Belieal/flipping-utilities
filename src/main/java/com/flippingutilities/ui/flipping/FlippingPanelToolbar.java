package com.flippingutilities.ui.flipping;

import com.flippingutilities.FlippingPlugin;
import com.flippingutilities.ui.uiutilities.Icons;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;

@Slf4j
public class FlippingPanelToolbar extends JPanel
{
	private Map<String, ImageIcon[]> BUTTON_STATES = new HashMap<>();
	private String buttonCurrentlyPressed;
	private JLabel[] toolbarButtons;
	private FlippingPlugin plugin;
	private FlippingPanel panel;

	FlippingPanelToolbar(FlippingPanel panel, FlippingPlugin plugin) {
		this.plugin = plugin;
		this.panel = panel;
		createButtonStates();
		createToolBarButtons();
		addButtons();
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		setBorder(new EmptyBorder(0, 0, 5, 0));
	}

	private void attachMouseListener(JLabel toolbarButton) {
		toolbarButton.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e))
				{
					//pressed on a non selected button
					if (!toolbarButton.getName().equals(buttonCurrentlyPressed)) {
						buttonCurrentlyPressed = toolbarButton.getName();
						//set button to "on" visual state
						toolbarButton.setIcon(BUTTON_STATES.get(toolbarButton.getName())[2]);
						panel.setSelectedSort(toolbarButton.getName());
						panel.getPaginator().setPageNumber(1);
						panel.rebuild(plugin.getTradesForCurrentView());
						deselectOtherButtons(toolbarButton);
					}
					else {
						//pressed on the already selected button, this means that there should no longer be any selected
						//button. Rebuild the flipping panel while sorting by recent just to "reset" the panel, and from
						//then on, don't apply any sort.
						panel.setSelectedSort("recent");
						panel.getPaginator().setPageNumber(1);
						panel.rebuild(plugin.getTradesForCurrentView());
						panel.setSelectedSort(null);
						buttonCurrentlyPressed = null;
						//set button to "off" visual state
						toolbarButton.setIcon(BUTTON_STATES.get(toolbarButton.getName())[0]);
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
		ImageIcon[] RECENT_BUTTON_STATES = {Icons.SORT_BY_RECENT_OFF_ICON, Icons.SORT_BY_RECENT_HALF_ON_ICON, Icons.SORT_BY_RECENT_ON_ICON};
		ImageIcon[] FAVORITE_BUTTON_STATES = {Icons.STAR_OFF_ICON, Icons.STAR_HALF_ON_ICON, Icons.STAR_ON_ICON};
		ImageIcon[] PROFIT_BUTTON_STATES = {Icons.SORT_BY_PROFIT_OFF_ICON, Icons.SORT_BY_PROFIT_HALF_ON_ICON, Icons.SORT_BY_PROFIT_ON_ICON};
		ImageIcon[] ROI_BUTTON_STATES = {Icons.SORT_BY_ROI_OFF_ICON, Icons.SORT_BY_ROI_HALF_ON_ICON, Icons.SORT_BY_ROI_ON_ICON};

		BUTTON_STATES.put("recent", RECENT_BUTTON_STATES);
		BUTTON_STATES.put("favorite", FAVORITE_BUTTON_STATES);
		BUTTON_STATES.put("profit", PROFIT_BUTTON_STATES);
		BUTTON_STATES.put("roi", ROI_BUTTON_STATES);
	}

	private void createToolBarButtons() {
		JLabel sortByRecent = new JLabel(Icons.SORT_BY_RECENT_OFF_ICON);
		sortByRecent.setName("recent");
		sortByRecent.setToolTipText("Sort by last traded time");

		JLabel sortByROI = new JLabel(Icons.SORT_BY_ROI_OFF_ICON);
		sortByROI.setName("roi");
		sortByROI.setToolTipText("Sort by ROI");

		JLabel sortByProfit = new JLabel(Icons.SORT_BY_PROFIT_OFF_ICON);
		sortByProfit.setName("profit");
		sortByProfit.setToolTipText("Sort by potential profit");

		JLabel favoriteModifier = new JLabel(Icons.STAR_OFF_ICON);
		favoriteModifier.setName("favorite");
		favoriteModifier.setToolTipText("view your favorite items");

		JLabel[] buttons = {sortByRecent, sortByROI, sortByProfit, favoriteModifier};
		toolbarButtons = buttons;
	}


	private void addButtons() {
		for (int i = 0; i < toolbarButtons.length; i++)
		{
			attachMouseListener(toolbarButtons[i]);
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
			if (!toolbarButtons[i].getName().equals(toolBarButton.getName())) {
				toolbarButtons[i].setIcon(BUTTON_STATES.get(toolbarButtons[i].getName())[0]);
			}
		}
	}
}
