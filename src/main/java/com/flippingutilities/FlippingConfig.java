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

package com.flippingutilities;

import com.flippingutilities.controller.FlippingPlugin;
import com.flippingutilities.ui.uiutilities.CustomColors;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Units;
import net.runelite.client.ui.ColorScheme;

import java.awt.*;

@ConfigGroup(FlippingPlugin.CONFIG_GROUP)
public interface FlippingConfig extends Config
{
	@ConfigItem(
		keyName = "roiGradientMax",
		name = "Set ROI gradient range limit",
		description = "Set the limit of the range before the gradient is bright green"
	)
	@Units(Units.PERCENT)
	default int roiGradientMax()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "marginCheckLoss",
		name = "Account for margin check loss",
		description = "Subtract the loss from margin checking the item when calculating the total profit"
	)
	default boolean marginCheckLoss()
	{
		return true;
	}

	@ConfigItem(
		keyName = "twelveHourFormat",
		name = "12 hour format",
		description = "Shows times in a 12 hour format (AM/PM)"
	)
	default boolean twelveHourFormat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "remainingGELimitProfit",
		name = "Calculate potential profit from remaining GE limit",
		description = "If unchecked, the potential profit will be calculated from total GE limit"
	)
	default boolean geLimitProfit()
	{
		return false;
	}

	@ConfigItem(
		keyName = "tradeStagnationTime",
		name = "Set trade stagnation time",
		description = "Set how long before the offer slot activity timer indicates that a trade has become stagnant"
	)
	@Units(Units.MINUTES)
	default int tradeStagnationTime()
	{
		return 15;
	}

	@ConfigItem(
		keyName = "slotTimersEnabled",
		name = "toggle slot timers",
		description = "Have a timer on active GE slots that will show the last time an offer came for the slot. This is useful" +
			"for knowing whether you should change your offer's price"
	)
	default boolean slotTimersEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "verboseView",
		name = "toggle verbose view",
		description = "show items in the flipping tab with all their tracked info like buy/sell price, roi, potential" +
			"profit, etc"
	)
	default boolean verboseViewEnabled() { return true; }

	@ConfigItem(
			keyName = "slotTimerBuyTextColor",
			name = "slot timer buy text color",
			description = "the color of the buy text on the slot timers"
	)
	default Color slotTimerBuyColor() {
		return ColorScheme.GRAND_EXCHANGE_LIMIT;
	}

	@ConfigItem(
			keyName = "slotTimerSellTextColor",
			name = "slot timer sell text color",
			description = "the color of the sell text on the slot timers"
	)
	default Color slotTimerSellColor() {
		return CustomColors.VIBRANT_YELLOW;
	}

	@ConfigItem(
			keyName = "wikiAutoRefresh",
			name = "wiki data auto refresh",
			description = "make the wiki data auto refresh"
	)
	default boolean wikiAutoRefresh() {
		return true;
	}
}
