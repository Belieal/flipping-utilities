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

import com.flippingutilities.ui.flipping.FlippingPanel;
import com.flippingutilities.ui.statistics.StatisticsPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import javax.inject.Inject;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

public class TabManager extends PluginPanel
{


	private JLabel whichTradesListLabel = new JLabel("Account wide list");

	/**
	 * This manages the tab navigation bar at the top of the panel.
	 * Once a tab is selected, the corresponding panel will be displayed below
	 * along with indication of what tab is selected.
	 *
	 * @param flippingPanel FlippingPanel represents the main tool of the plugin.
	 * @param statPanel     StatPanel represents useful performance statistics to the user.
	 */
	@Inject
	public TabManager(FlippingPanel flippingPanel, StatisticsPanel statPanel)
	{
		super(false);

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());

		whichTradesListLabel.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		whichTradesListLabel.setHorizontalAlignment(SwingConstants.CENTER);

		JPanel display = new JPanel();
		JPanel top = new JPanel(new BorderLayout());

		top.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		top.setBorder(new EmptyBorder(5,0,0,0));
		top.add(whichTradesListLabel, BorderLayout.NORTH);

		MaterialTabGroup tabGroup = new MaterialTabGroup(display);
		MaterialTab flippingTab = new MaterialTab("Flipping", tabGroup, flippingPanel);
		MaterialTab statTab = new MaterialTab("Statistics", tabGroup, statPanel);

		tabGroup.setBorder(new EmptyBorder(5, 0, 2, 0));
		tabGroup.addTab(flippingTab);
		tabGroup.addTab(statTab);

		// Initialize with flipping tab open.
		tabGroup.select(flippingTab);
		top.add(tabGroup, BorderLayout.CENTER);

		add(top, BorderLayout.NORTH);
		add(display, BorderLayout.CENTER);
	}

	public void setWhichTradesListDisplay(String username) {
		whichTradesListLabel.setText(String.format("%s's list",username));
	}
}
