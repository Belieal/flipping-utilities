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

package com.flippingutilities.ui.utilities;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

//have to extend MaterialTabGroup just to make it compatible with MaterialTab...not actually using any functionality
//from the parent. The difference between this TabGroup and MaterialTabGroup is just that this uses a card layout
//cause its faster when switching between different views
@Slf4j
public class FastTabGroup extends MaterialTabGroup
{
	/* The panel on which the content tab's content will be displayed on. */
	private final JPanel display;
	/* A list of all the tabs contained in this group. */
	private final List<MaterialTab> tabs = new ArrayList<>();

	public FastTabGroup(JPanel display)
	{
		this.display = display;
		this.display.setLayout(new CardLayout());
		setLayout(new FlowLayout(FlowLayout.CENTER, 8, 0));
		setOpaque(false);
	}

	public void addTab(MaterialTab tab)
	{
		tabs.add(tab);
		display.add(tab.getContent(), tab.getText());
		add(tab, BorderLayout.NORTH);
	}

	public boolean select(MaterialTab selectedTab)
	{
		// If the OnTabSelected returned false, exit the method to prevent tab switching
		if (!selectedTab.select())
		{
			return false;
		}

		CardLayout cardLayout = (CardLayout) display.getLayout();
		cardLayout.show(display, selectedTab.getText());

		//Unselect all other tabs
		for (MaterialTab tab : tabs)
		{
			if (!tab.equals(selectedTab))
			{
				tab.unselect();
			}
		}
		return true;
	}
}
