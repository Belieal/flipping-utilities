/*
 * Copyright (c) 2020, Belieal <https://github.com/Belieal>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.flippingutilities.ui;

import com.flippingutilities.FlippingItem;
import com.flippingutilities.FlippingPlugin;
import com.flippingutilities.ui.flipping.FlippingPanel;
import com.flippingutilities.ui.statistics.StatsPanel;
import com.flippingutilities.ui.utilities.FastTabGroup;
import com.flippingutilities.ui.utilities.UIUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.ComboBoxListRenderer;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.util.LinkBrowser;

@Slf4j
public class MasterPanel extends PluginPanel
{
	@Getter
	private JComboBox<String> accountSelector;

	@Getter
	private JLabel settingsButton;

	private FlippingPlugin plugin;

	private FastTabGroup tabGroup;

	private static String FLIPPING_TAB_NAME = "Flipping";
	private static String STATISTICS_TAB_NAME = "Statistics";

	private MaterialTab flippingTab;
	private MaterialTab statisticsTab;

	/**
	 * THe master panel is always present. The components added to it are components that should always be visible
	 * regardless of whether you are looking at the flipping panel or the statistics panel. The tab group to switch
	 * between the flipping and stats panel, the account selector dropdown menu, and the settings button are all examples
	 * of components that are always present, hence they are on the master panel.
	 *
	 * @param flippingPanel FlippingPanel represents the main tool of the plugin.
	 * @param statPanel     StatPanel represents useful performance statistics to the user.
	 */
	public MasterPanel(FlippingPlugin plugin, FlippingPanel flippingPanel, StatsPanel statPanel, SettingsPanel settingsPanel)
	{
		super(false);

		this.plugin = plugin;

		setLayout(new BorderLayout());

		JPanel mainDisplay = new JPanel();

		accountSelector = accountSelector();
		JDialog modal = UIUtilities.createModalFromPanel(this, settingsPanel);
		modal.setTitle("Settings");
		settingsButton = settingsButton(() ->
		{
			modal.setVisible(true);
			settingsPanel.rebuild();
		});

		tabGroup = tabSelector(mainDisplay, flippingPanel, statPanel);
		JPanel header = Header(accountSelector, settingsButton, tabGroup);
		add(header, BorderLayout.NORTH);
		add(mainDisplay, BorderLayout.CENTER);
	}

	/**
	 * The header is at the top of the panel. It is the component that contains the account selector dropdown, the
	 * settings button to the right of the dropdown, and the tab selector which allows a user to select either the
	 * flipping or stats tab.
	 *
	 * @param accountSelector the account selector dropdown
	 * @param settingsButton  a button which opens up a modal for altering settings
	 * @param tabSelector     a tab group with allows a user to select either the flipping or stats tab to view.
	 * @return a jpanel representing the header.
	 */
	private JPanel Header(JComboBox accountSelector, JLabel settingsButton, MaterialTabGroup tabSelector)
	{
		JPanel accountSelectorPanel = new JPanel(new BorderLayout());
		accountSelectorPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		accountSelectorPanel.add(accountSelector, BorderLayout.CENTER);

		JPanel tabGroupArea = new JPanel(new BorderLayout());
		tabGroupArea.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		tabGroupArea.add(tabSelector, BorderLayout.CENTER);
		tabGroupArea.add(settingsButton, BorderLayout.EAST);
		tabGroupArea.add(sponsorPanel(), BorderLayout.NORTH);

		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		header.add(accountSelector, BorderLayout.NORTH);
		header.add(tabGroupArea, BorderLayout.CENTER);

		return header;
	}

	private JPanel sponsorPanel() {
		JLabel sponsorText = new JLabel("Help fund plugin development", JLabel.CENTER);
		sponsorText.setFont(FontManager.getRunescapeSmallFont());
		sponsorText.setToolTipText("Click me");
		Font font = sponsorText.getFont();
		Map attributes = font.getAttributes();
		attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		sponsorText.setFont(font.deriveFont(attributes));
		Color defaultColor = sponsorText.getForeground();

		JPanel sponsorPanel = new JPanel();
		sponsorPanel.setBorder(new EmptyBorder(0,10,0,0));
		sponsorPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		sponsorPanel.add(sponsorText);
		sponsorPanel.add(new JLabel(UIUtilities.HEART_ICON));

		final JMenuItem patreonLink = new JMenuItem("Patreon Link");
		patreonLink.addActionListener(e -> LinkBrowser.browse("https://www.patreon.com/FlippingUtilities"));
		final JMenuItem paypalLink = new JMenuItem("Paypal Link");
		paypalLink.addActionListener(e -> LinkBrowser.browse("http://paypal.com/donate?hosted_button_id=BPCG76L7B7ZAY"));

		final JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.setBorder(new EmptyBorder(5, 5, 5, 5));
		popupMenu.add(patreonLink);
		popupMenu.add(paypalLink);
		sponsorPanel.setComponentPopupMenu(popupMenu);

		sponsorText.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e) {
				popupMenu.show(e.getComponent(), e.getX(), e.getY());
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				sponsorText.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				sponsorText.setForeground(defaultColor);
			}
		});

		return sponsorPanel;
	}

	/**
	 * This is the dropdown at the top of the header which allows the user to select which account they want to view.
	 * Its only set to visible if the user has more than once account with a trading history.
	 *
	 * @return the account selector.
	 */
	private JComboBox accountSelector()
	{
		JComboBox viewSelectorDropdown = new JComboBox();
		viewSelectorDropdown.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		viewSelectorDropdown.setFocusable(false);
		viewSelectorDropdown.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		viewSelectorDropdown.setRenderer(new ComboBoxListRenderer());
		viewSelectorDropdown.setToolTipText("Select which of your account's trades list you want to view");
		viewSelectorDropdown.addItemListener(event ->
		{
			if (event.getStateChange() == ItemEvent.SELECTED)
			{
				String selectedName = (String) event.getItem();
				plugin.changeView(selectedName);
			}
		});

		return viewSelectorDropdown;
	}

	/**
	 * This is the button that you click on to view the setting modal. It is only visible if the account selector is
	 * visible.
	 *
	 * @param callback the callback executed when the button is clicked.
	 * @return the settings button
	 */
	private JLabel settingsButton(Runnable callback)
	{
		JLabel button = new JLabel(UIUtilities.SETTINGS_ICON);
		button.setToolTipText("Open Settings Panel");
		button.setPreferredSize(UIUtilities.ICON_SIZE);
		button.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				callback.run();
			}
		});
		return button;
	}

	/**
	 * Adds the tabs for the flipping panel and stats panel onto the main display panel. These tabs can then
	 * be clicked to view the flipping/stats panel
	 *
	 * @param mainDisplay   the panel on which the tabs will be put and on which either the flipping or stats panel will be
	 *                      rendered
	 * @return
	 */
	private FastTabGroup tabSelector(JPanel mainDisplay, JPanel flippingPanel, JPanel statPanel)
	{
		FastTabGroup tabGroup = new FastTabGroup(mainDisplay);
		flippingTab = new MaterialTab(FLIPPING_TAB_NAME, tabGroup, flippingPanel);
		statisticsTab = new MaterialTab(STATISTICS_TAB_NAME, tabGroup, statPanel);
		tabGroup.setBorder(new EmptyBorder(0, 35, 2, 0));
		tabGroup.addTab(flippingTab);
		tabGroup.addTab(statisticsTab);
		// Initialize with flipping tab open.
		tabGroup.select(flippingTab);
		return tabGroup;
	}

	public Set<String> getViewSelectorItems()
	{
		Set<String> items = new HashSet<>();
		for (int i = 0; i < accountSelector.getItemCount(); i++)
		{
			items.add(accountSelector.getItemAt(i));
		}
		return items;
	}

	public void addView(JPanel panel, String name) {
		tabGroup.addView(panel, name);
	}

	public void showView(String name) {
		tabGroup.showView(name);
	}

	public void selectPreviouslySelectedTab() {
		tabGroup.selectPreviouslySelectedTab();
	}

	/**
	 * There are certain views that should not be viewable unless the user is logged in because they require the
	 * currently logged in account. This method is used to revert back to a "safe" previously selected tab that is
	 * safe to view when an account is logged out.
	 */
	public void revertToSafeDisplay() {
		tabGroup.revertToSafeDisplay();
	}
}
