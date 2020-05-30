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

import com.flippingutilities.FlippingPlugin;
import com.flippingutilities.ui.flipping.FlippingPanel;
import com.flippingutilities.ui.statistics.StatsPanel;
import com.flippingutilities.ui.utilities.SettingsPanel;
import com.flippingutilities.ui.utilities.UIUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.ComboBoxListRenderer;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

public class MasterPanel extends PluginPanel
{
	@Getter
	private JComboBox<String> accountSelector;

	/**
	 * THe master panel is always present. The components added to it are components that should always be visible
	 * regardless of whether you are looking at the flipping panel or the statistics panel. The tab group to switch
	 * between the flipping and stats panel, the account selector dropdown menu, and the settings button are all examples
	 * of components that are always present, hence they are on the master panel.
	 *
	 * @param flippingPanel FlippingPanel represents the main tool of the plugin.
	 * @param statPanel     StatPanel represents useful performance statistics to the user.
	 */
	public MasterPanel(FlippingPlugin plugin, FlippingPanel flippingPanel, StatsPanel statPanel)
	{
		super(false);

		setLayout(new BorderLayout());

		JPanel mainDisplay = new JPanel();
		accountSelector = createAccountSelector(plugin::changeView);
		SettingsPanel settingsPanel = createSettingsPanel();
		JDialog modal = createSettingsModal(this, settingsPanel);
		JLabel settingsButton = createSettingsButton(() -> {
			modal.setVisible(true);
			settingsPanel.rebuild();
		});
		MaterialTabGroup tabSelector = createTabSelector(mainDisplay, flippingPanel, statPanel);
		JPanel header = createHeader(accountSelector, settingsButton, tabSelector);
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
	private JPanel createHeader(JComboBox accountSelector, JLabel settingsButton, MaterialTabGroup tabSelector)
	{
		JPanel topOfHeader = new JPanel(new BorderLayout());
		topOfHeader.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		topOfHeader.add(accountSelector, BorderLayout.CENTER);
		topOfHeader.add(settingsButton, BorderLayout.EAST);

		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		header.add(topOfHeader, BorderLayout.NORTH);
		header.add(tabSelector, BorderLayout.CENTER);
		return header;
	}

	/**
	 * This is the dropdown at the top of the header which allows the user to select which account they want to view.
	 * Its only set to visible if the user has more than once account with a trading history.
	 *
	 * @param onItemSelectionCallback the callback that fires when a user selects a display name from the dropdown.
	 * @return the account selector.
	 */
	private JComboBox createAccountSelector(Consumer<String> onItemSelectionCallback)
	{
		JComboBox viewSelectorDropdown = new JComboBox();
		viewSelectorDropdown.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		viewSelectorDropdown.setFocusable(false);
		viewSelectorDropdown.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		viewSelectorDropdown.setRenderer(new ComboBoxListRenderer());
		viewSelectorDropdown.setToolTipText("Select which of your account's trades list you want to view");
		viewSelectorDropdown.addItemListener(event ->
		{
			if (event.getStateChange() == ItemEvent.SELECTED)
			{

				String selectedDisplayName = (String) event.getItem();

				if (selectedDisplayName == null)
				{
					return;
				}
				else
				{
					onItemSelectionCallback.accept(selectedDisplayName);
				}
			}
		});
		return viewSelectorDropdown;
	}

	private JLabel createSettingsButton(Runnable callback)
	{
		JLabel button = new JLabel(UIUtilities.SETTINGS_ICON);
		button.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		button.setPreferredSize(UIUtilities.ICON_SIZE);
		button.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
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
	 * @param flippingPanel
	 * @param statsPanel
	 * @return
	 */
	private MaterialTabGroup createTabSelector(JPanel mainDisplay, JPanel flippingPanel, JPanel statsPanel)
	{
		MaterialTabGroup tabGroup = new MaterialTabGroup(mainDisplay);
		MaterialTab flippingTab = new MaterialTab("Flipping", tabGroup, flippingPanel);
		MaterialTab statTab = new MaterialTab("Statistics", tabGroup, statsPanel);

		tabGroup.setBorder(new EmptyBorder(5, 0, 2, 0));
		tabGroup.addTab(flippingTab);
		tabGroup.addTab(statTab);

		// Initialize with flipping tab open.
		tabGroup.select(flippingTab);
		return tabGroup;
	}

	private SettingsPanel createSettingsPanel()
	{
		SettingsPanel settingsPanel = new SettingsPanel(250, 400);
		settingsPanel.addSection("Account Selector");

		JComboBox accountDropdown = new JComboBox();
		settingsPanel.addDynamicOption("Account Selector",
			new JLabel("Choose an account to delete"),
			accountDropdown,
			() -> getViewSelectorItems().forEach(accountDropdown::addItem)
		);
		settingsPanel.addSection("Other Settings");
		settingsPanel.addSection("Other Settings7");
		settingsPanel.addSection("Other Settings5");
		settingsPanel.addSection("Other Settings1");
		settingsPanel.addSection("Other Settings2");
		return settingsPanel;
	}


	private JDialog createSettingsModal(Component parent, SettingsPanel settingsPanel)
	{
		JDialog modal = new JDialog();
		modal.add(settingsPanel);
		modal.setLocationRelativeTo(parent);
		return modal;
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
}
