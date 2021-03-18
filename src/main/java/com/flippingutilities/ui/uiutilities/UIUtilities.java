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

import com.flippingutilities.ui.flipping.FlippingPanel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.QuantityFormatter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class contains various methods that the UI uses to format their visuals.
 */
@Slf4j
public class UIUtilities
{
	private static final NumberFormat PRECISE_DECIMAL_FORMATTER = new DecimalFormat(
		"#,###.###",
		DecimalFormatSymbols.getInstance(Locale.ENGLISH)
	);
	private static final NumberFormat DECIMAL_FORMATTER = new DecimalFormat(
		"#,###.#",
		DecimalFormatSymbols.getInstance(Locale.ENGLISH)
	);

	/**
	 * This method calculates the red-yellow-green gradient factored by the percentage or max gradient.
	 *
	 * @param percentage  The percentage representing the value that needs to be gradiated.
	 * @param gradientMax The max value representation before the gradient tops out on green.
	 * @return A color representing a value on a red-yellow-green gradient.
	 */
	public static Color gradiatePercentage(float percentage, int gradientMax)
	{
		if (percentage < gradientMax * 0.5)
		{
			return (percentage <= 0) ? Color.RED
				: ColorUtil.colorLerp(Color.RED, Color.YELLOW, percentage / gradientMax * 2);
		}
		else
		{
			return (percentage >= gradientMax) ? ColorScheme.GRAND_EXCHANGE_PRICE
				: ColorUtil.colorLerp(Color.YELLOW, ColorScheme.GRAND_EXCHANGE_PRICE, percentage / gradientMax * 0.5);
		}
	}

	/**
	 * Functionally the same as {@link QuantityFormatter#quantityToRSDecimalStack(int, boolean)},
	 * except this allows for formatting longs.
	 *
	 * @param quantity Long to format
	 * @param precise  If true, allow thousandths precision if {@code currentQuantityInTrade} is larger than 1 million.
	 *                 Otherwise have at most a single decimal
	 * @return Formatted number string.
	 */
	public static synchronized String quantityToRSDecimalStack(long quantity, boolean precise)
	{
		if (Long.toString(quantity).length() <= 4)
		{
			return QuantityFormatter.formatNumber(quantity);
		}

		long power = (long) Math.log10(quantity);

		// Output thousandths for values above a million
		NumberFormat format = precise && power >= 6
			? PRECISE_DECIMAL_FORMATTER
			: DECIMAL_FORMATTER;

		return format.format(quantity / Math.pow(10, (Long.divideUnsigned(power, 3)) * 3))
			+ new String[] {"", "K", "M", "B", "T"}[(int) (power / 3)];
	}

	public static JDialog createModalFromPanel(Component parent, JPanel panel)
	{
		JDialog modal = new JDialog();
		modal.setSize(new Dimension(panel.getSize()));
		modal.add(panel);
		modal.setLocationRelativeTo(parent);
		return modal;
	}

	public static JPanel stackPanelsVertically(List<JPanel> panels, int gap) {
		JPanel mainPanel = new JPanel();
		stackPanelsVertically(panels, mainPanel, gap);
		return mainPanel;
	}

	//make this take a supplier to supply it with the desired margin wrapper.
	public static void stackPanelsVertically(List<JPanel> panels, JPanel mainPanel, int vGap)
	{
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

		int index = 0;
		for (JPanel panel : panels)
		{
			if (index == 0)
			{
				mainPanel.add(panel);
				index++;
			}
			else
			{
				mainPanel.add(Box.createVerticalStrut(vGap));
				mainPanel.add(panel);
			}
		}
	}

	public static String colorText(String s, Color color) {
		return String.format("<span style='color:%s;'>%s</span>",ColorUtil.colorToHexCode(color), s);
	}

	public static String buildWikiLink(int itemId) {
		//https://prices.runescape.wiki/osrs/item/2970
		return "https://prices.runescape.wiki/osrs/item/" + itemId;
	}

	public static IconTextField createSearchBar(ScheduledExecutorService executor, Runnable onSearch) {
		final Future<?>[] runningRequest = {null};
		IconTextField searchBar = new IconTextField();
		searchBar.setIcon(IconTextField.Icon.SEARCH);
		searchBar.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 32));
		searchBar.setBackground(ColorScheme.DARK_GRAY_COLOR);
		searchBar.setBorder(BorderFactory.createMatteBorder(0, 5, 7, 5, ColorScheme.DARKER_GRAY_COLOR.darker()));
		searchBar.setHoverBackgroundColor(ColorScheme.DARKER_GRAY_HOVER_COLOR);
		searchBar.setMinimumSize(new Dimension(0, 35));
		searchBar.addActionListener(e -> executor.execute(onSearch));
		searchBar.addClearListener(onSearch);
		searchBar.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (runningRequest[0] != null)
				{
					runningRequest[0].cancel(false);
				}
				runningRequest[0] = executor.schedule(onSearch, 250, TimeUnit.MILLISECONDS);
			}
		});
		return searchBar;
	}
}
