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

package com.flippingutilities.ui.uiutilities;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

//have to extend MaterialTabGroup just to make it compatible with MaterialTab...not actually using any functionality
//from the parent. The difference between this TabGroup and MaterialTabGroup is just that this uses a card layout
//cause its faster when switching between different views
@Slf4j
public class FastTabGroup extends MaterialTabGroup
{
	/* The panel on which the tab's content or the view will be displayed on. */
	private final JPanel display;
	/* A list of all the tabs contained in this group. */
	private final List<MaterialTab> tabs = new ArrayList<>();
	@Getter
	private String lastSelectedTab;
	private boolean currentlyShowingView = false;

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

	/**
	 * A "view" is just a Jpanel. It just doesn't have a tab that can be selected by the user to display it. As such, a view
	 * is displayed programatically when it needs to be. For example, the GeHistoryTabPanel is a "view" and doesn't have
	 * a tab. It is displayed only when the user looks at their ge history.
	 * @param panel the view to add to the main display panel
	 * @param name the name of the view. This name is so that the view can be shown using it.
	 */
	public void addView(JPanel panel, String name) {
		display.add(panel, name);
	}

	public boolean select(MaterialTab selectedTab)
	{
		// If the OnTabSelected returned false, exit the method to prevent tab switching
		if (!selectedTab.select())
		{
			return false;
		}
		currentlyShowingView = false;
		lastSelectedTab = selectedTab.getText();
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

	public void unselectAll() {
		tabs.forEach(tab-> tab.unselect());
	}

	public void showView(String name) {
		currentlyShowingView = true;
		unselectAll();
		CardLayout cardLayout = (CardLayout) display.getLayout();
		cardLayout.show(display, name);
	}

	public void revertToSafeDisplay() {
		if (currentlyShowingView) {

			selectPreviouslySelectedTab();
		}
	}

	public void selectPreviouslySelectedTab() {
		for (MaterialTab tab: tabs) {
			if (tab.getText().equals(lastSelectedTab)) {
				select(tab);
				break;
			}
		}
	}
}
