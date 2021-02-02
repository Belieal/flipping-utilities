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

package com.flippingutilities.ui.settings;

import com.flippingutilities.controller.FlippingPlugin;
import com.flippingutilities.ui.uiutilities.Icons;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class is a panel which allows a user to select which account to modify. Currently, a user can only delete their
 * account. This panel is displayed in a modal that pops up when a user clicks on the settings button in the master panel.
 */
public class SettingsPanel extends JPanel
{
	int WIDTH = 700;

	int HEIGHT = 520;

	int ACCOUNT_LABEL_HEIGHT = 45;

	FlippingPlugin plugin;

	JPanel accountSelectionPanel;

	JPanel settingsBasePanel;

	List<JLabel> accountLabels = new ArrayList<>();

	JLabel selectedAccountLabel;


	public SettingsPanel(FlippingPlugin plugin)
	{
		this.plugin = plugin;
		setLayout(new BorderLayout());
		setSize(WIDTH, HEIGHT);
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		accountSelectionPanel = accountSelectionPanel();

		JScrollPane accountScroller = new JScrollPane(accountSelectionPanel);

		settingsBasePanel = new JPanel();
		settingsBasePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());

		add(accountScroller, BorderLayout.WEST);
		add(new JScrollPane(settingsBasePanel), BorderLayout.CENTER);
	}

	/**
	 * This is the panel that holds the vertically stacked account labels.
	 *
	 * @return
	 */
	private JPanel accountSelectionPanel()
	{
		JPanel accountSelectionPanel = new JPanel();
		accountSelectionPanel.setLayout(new BoxLayout(accountSelectionPanel, BoxLayout.Y_AXIS));
		accountSelectionPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		accountSelectionPanel.setPreferredSize(new Dimension(170, 120));
		return accountSelectionPanel;
	}

	/**
	 * Creates the account labels that a user can click on to select which account's settings they want to change. These
	 * labels are in the account selection panel.
	 *
	 * @param name name of the account
	 * @return
	 */
	private JLabel accountLabel(String name)
	{
		JLabel accountLabel = new JLabel(Icons.ACCOUNT_ICON, JLabel.LEFT);
		accountLabel.setOpaque(true);
		accountLabel.setMaximumSize(new Dimension(170, ACCOUNT_LABEL_HEIGHT));
		accountLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		accountLabel.setText(name);
		accountLabel.setFont(FontManager.getRunescapeBoldFont());
		accountLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				setSelectedAccountLabel(accountLabel);
				revalidate();
				repaint();
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				accountLabel.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
				accountLabel.setSize(accountLabel.getWidth(), ACCOUNT_LABEL_HEIGHT + 10);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				if (accountLabel != selectedAccountLabel)
				{
					accountLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
					accountLabel.setSize(accountLabel.getWidth(), ACCOUNT_LABEL_HEIGHT);
				}
			}
		});
		return accountLabel;
	}

	public void rebuild()
	{
		SwingUtilities.invokeLater(() ->
		{
			accountSelectionPanel.removeAll();
			settingsBasePanel.removeAll();
			accountLabels.clear();
			Set<String> accountsWithHistory = new HashSet<>(plugin.getCurrentDisplayNames());
			for (String name : accountsWithHistory)
			{
				JLabel accountLabel = accountLabel(name);
				accountLabels.add(accountLabel);
				accountSelectionPanel.add(accountLabel);
				accountSelectionPanel.add(Box.createRigidArea(new Dimension(0, 2)));
			}
			if (!accountLabels.isEmpty())
			{
				setSelectedAccountLabel(accountLabels.get(0));
			}
			repaint();
			revalidate();
		});
	}

	private void setSelectedAccountLabel(JLabel accountLabel)
	{
		selectedAccountLabel = accountLabel;
		accountLabel.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);

		for (JLabel label : accountLabels)
		{
			if (label != accountLabel)
			{
				label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			}
		}

		settingsBasePanel.removeAll();
		settingsBasePanel.add(optionsPanel(accountLabel.getText()));
	}

	/**
	 * Creates the button which deletes the associated account if pressed. This button is located on the options panel.
	 */
	private JLabel accountDeleteButton()
	{
		JLabel deleteButton = new JLabel(Icons.DELETE_BUTTON);
		deleteButton.setToolTipText("Deletes the file that stores all of this account's trades. This cannot" +
			"be undone!");
		deleteButton.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				String selectedAccountName = selectedAccountLabel.getText();
				if (selectedAccountName.equals(plugin.getCurrentlyLoggedInAccount()))
				{
					JOptionPane.showMessageDialog(null, "You cannot delete a currently logged in account", "Alert", JOptionPane.ERROR_MESSAGE);
				}

				else
				{
					int result = JOptionPane.showOptionDialog(deleteButton, "Are you sure you want to delete this account?",
						"Are you sure?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
						null, new String[] {"Yes", "No"}, "No");

					if (result == JOptionPane.YES_OPTION)
					{
						plugin.deleteAccount(selectedAccountName);
						rebuild();
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				deleteButton.setIcon(Icons.HIGHLIGHT_DELETE_BUTTON);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				deleteButton.setIcon(Icons.DELETE_BUTTON);
			}
		});

		return deleteButton;
	}


	/**
	 * Creates a panel that represents all the settings options for the account with the given name. Currently, the
	 * only option is the ability to delete the account.
	 *
	 * @param name the name of the account the options are for.
	 * @return
	 */
	private JPanel optionsPanel(String name)
	{
		JPanel basePanel = new JPanel(new BorderLayout());
		basePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());

		JLabel title = new JLabel("currently viewing " + name, SwingConstants.CENTER);
		title.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
		title.setFont(new Font("Roboto", Font.ITALIC + Font.BOLD, 10));
		title.setBorder(new EmptyBorder(0, 0, 10, 0));

		JLabel deleteButton = accountDeleteButton();

		JPanel optionBody = new JPanel();
		optionBody.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		optionBody.setPreferredSize(new Dimension(500, 400));

		basePanel.add(title, BorderLayout.NORTH);
		basePanel.add(optionBody, BorderLayout.CENTER);
		basePanel.add(deleteButton, BorderLayout.SOUTH);

		return basePanel;
	}
}
