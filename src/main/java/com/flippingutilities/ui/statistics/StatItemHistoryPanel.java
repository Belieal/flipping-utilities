
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

package com.flippingutilities.ui.statistics;

import com.flippingutilities.OfferInfo;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

public class StatItemHistoryPanel extends JPanel
{

	private JLabel timeSince = new JLabel("", SwingConstants.CENTER);
	private JLabel price = new JLabel();
	private JLabel stateAndQuantityLabel = new JLabel("x ", SwingConstants.RIGHT);

	/**
	 * Definitely not finished, don't look plz =)
	 *
	 * @param boughtOffer
	 * @param soldOffer
	 * @param itemCountFlipped
	 */
	StatItemHistoryPanel(OfferInfo boughtOffer, OfferInfo soldOffer, int itemCountFlipped)
	{
		setLayout(new GridBagLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel[] labelList = {timeSince, stateAndQuantityLabel};

		for (JLabel label : labelList)
		{
			label.setFont(FontManager.getRunescapeSmallFont());
			label.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);
		}

		GridBagConstraints constraints = new GridBagConstraints();

		constraints.fill = GridBagConstraints.REMAINDER;
		constraints.weightx = 1;
		constraints.gridx = 0;
		constraints.gridy = 0;
		add(timeSince, constraints);

		constraints.fill = GridBagConstraints.VERTICAL;
		constraints.weightx = 0.5;
		constraints.gridy++;
		//add(buyPrice);

		constraints.gridy++;
		//add(sellPrice);

		constraints.gridx = 1;
		constraints.gridy = 1;
		//add(profitLabel);

		constraints.gridy++;
		add(stateAndQuantityLabel);
	}

	public void updateDisplays()
	{
	}

}

