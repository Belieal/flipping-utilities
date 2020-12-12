package com.flippingutilities.ui.uiutilities;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

@Slf4j
public class Paginator extends JPanel
{
	@Getter
	@Setter
	private int pageNumber = 1;
	private int totalPages;
	@Getter
	private JLabel statusText;
	@Setter
	private JLabel arrowRight;
	@Setter
	private JLabel arrowLeft;
	Runnable onPageChange;
	@Setter
	private int pageSize = 20;

	public Paginator(Runnable onPageChange)
	{
		this.onPageChange = onPageChange;
		this.statusText = new JLabel("Page 1 of 1", SwingUtilities.CENTER);
		this.statusText.setFont(FontManager.getRunescapeBoldFont());
		this.arrowLeft = new JLabel(Icons.ARROW_LEFT);
		this.arrowRight = new JLabel(Icons.ARROW_RIGHT);
		this.arrowRight.setForeground(Color.blue);
		setLayout(new FlowLayout());
		add(arrowLeft);
		add(statusText);
		add(arrowRight);
		setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		setBorder(new EmptyBorder(3, 0, 0, 0));
		arrowLeft.addMouseListener(onMouse(false));
		arrowRight.addMouseListener(onMouse(true));
	}

	public void updateTotalPages(int numItems)
	{
		if (numItems <= pageSize) {
			totalPages = 1;
		}
		else {
			totalPages = (int) Math.ceil((float)numItems/ pageSize);
		}

		statusText.setText(String.format("Page %d of %d", pageNumber, totalPages));
	}

	private MouseAdapter onMouse(boolean isIncrease)
	{
		return new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (isIncrease)
				{
					if (pageNumber < totalPages)
					{
						pageNumber++;
						try
						{
							onPageChange.run();
						}
						catch (Exception exc)
						{
							log.info("couldn't increase page number cause callback failed");
							pageNumber--;
						}
					}
				}
				else
				{
					if (pageNumber > 1)
					{
						pageNumber--;
						try
						{
							onPageChange.run();
						}
						catch (Exception exc)
						{
							log.info("couldn't decrease page number cause callback failed");
							pageNumber++;
						}

					}
				}
				statusText.setText(String.format("Page %d of %d", pageNumber, totalPages));
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (isIncrease)
				{
					arrowRight.setIcon(Icons.ARROW_RIGHT_HOVER);
				}
				else
				{
					arrowLeft.setIcon(Icons.ARROW_LEFT_HOVER);
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				if (isIncrease)
				{
					arrowRight.setIcon(Icons.ARROW_RIGHT);
				}
				else
				{
					arrowLeft.setIcon(Icons.ARROW_LEFT);
				}
			}
		};
	}

	public <T> List<T> getCurrentPageItems(List<T> items)
	{
		List<T> pageItems = new ArrayList<>();
		int startIndex = (pageNumber - 1) * pageSize;
		int endIndex = Math.min(startIndex + pageSize, items.size());
		for (int i = startIndex; i < endIndex; i++)
		{
			pageItems.add(items.get(i));
		}
		return pageItems;
	}
}
