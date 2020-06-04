package com.flippingutilities.ui;

import com.flippingutilities.FlippingPlugin;
import com.flippingutilities.ui.utilities.UIUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

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

	private JPanel accountSelectionPanel()
	{
		JPanel accountSelectionPanel = new JPanel();
		accountSelectionPanel.setLayout(new BoxLayout(accountSelectionPanel, BoxLayout.Y_AXIS));
		accountSelectionPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		accountSelectionPanel.setPreferredSize(new Dimension(170, 120));
		return accountSelectionPanel;
	}

	private JLabel accountLabel(String name)
	{
		JLabel accountLabel = new JLabel(UIUtilities.ACCOUNT_ICON, JLabel.LEFT);
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
		SwingUtilities.invokeLater(() -> {
			accountSelectionPanel.removeAll();
			settingsBasePanel.removeAll();
			accountLabels.clear();
			Set<String> accountsWithHistory = new HashSet<>(plugin.getAccountCache().keySet());
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

	private JLabel accountDeleteButton()
	{
		JLabel deleteButton = new JLabel(UIUtilities.DELETE_BUTTON);
		deleteButton.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{

			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				deleteButton.setIcon(UIUtilities.HIGHLIGHT_DELETE_BUTTON);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				deleteButton.setIcon(UIUtilities.DELETE_BUTTON);
			}
		});

		return deleteButton;
	}


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
