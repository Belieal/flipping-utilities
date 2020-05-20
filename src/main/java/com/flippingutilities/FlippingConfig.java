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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Units;

@ConfigGroup(FlippingPlugin.CONFIG_GROUP)
public interface FlippingConfig extends Config
{
	enum Fonts
	{
		SMALL_FONT,
		REGULAR_FONT,
		BOLD_FONT
	}

	@ConfigItem(
		keyName = "outOfDateWarning",
		name = "Set how long before prices are outdated",
		description = "Set how long before warning that prices are outdated"
	)
	@Units(Units.MINUTES)
	default int outOfDateWarning()
	{
		return 30;
	}

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
		keyName = "subInfoFont",
		name = "Set sub info font",
		description = "Choose the font for sub information on the panel"
	)
	default Fonts subInfoFontStyle()
	{
		return Fonts.SMALL_FONT;
	}

	@ConfigItem(
		keyName = "multiAccTracking",
		name = "Enable multi account tracking",
		description = "<html>Enabling this feature gives you access to a dropdown"
			+ "<br>which allows you to see each of your accounts flips and profits in isolation."
			+ "<br>This is very useful if you flip on multiple accounts.</html>"
	)
	default boolean multiAccTracking()
	{
		return false;
	}
}
