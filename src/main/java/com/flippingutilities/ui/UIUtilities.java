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


import com.flippingutilities.FlippingPlugin;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.ImageIcon;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.QuantityFormatter;

/**
 * This class contains various methods that the UI uses to format their visuals.
 */

public class UIUtilities
{

	public static final Color OUTDATED_COLOR = new Color(250, 74, 75);
	public static final Color PROFIT_COLOR = new Color(255, 175, 55);

	private static final NumberFormat PRECISE_DECIMAL_FORMATTER = new DecimalFormat(
		"#,###.###",
		DecimalFormatSymbols.getInstance(Locale.ENGLISH)
	);
	private static final NumberFormat DECIMAL_FORMATTER = new DecimalFormat(
		"#,###.#",
		DecimalFormatSymbols.getInstance(Locale.ENGLISH)
	);

	public static final ImageIcon OPEN_ICON;
	public static final ImageIcon CLOSE_ICON;

	static
	{
		final BufferedImage openIcon = ImageUtil
			.getResourceStreamFromClass(FlippingPlugin.class, "/small_open_arrow.png");
		CLOSE_ICON = new ImageIcon(openIcon);
		OPEN_ICON = new ImageIcon(ImageUtil.rotateImage(openIcon, Math.toRadians(90)));
	}

	/**
	 * This method formats a string time from an instant from the time, as specified by the parameter, until now.
	 *
	 * @param fromInstant The start of the duration
	 * @return A formatted string.
	 */
	public static String formatDuration(Instant fromInstant)
	{
		if (fromInstant != null)
		{
			long toInstant = Instant.now().getEpochSecond();

			//Time since trade was done.
			long timeAgo = (toInstant - fromInstant.getEpochSecond());

			String result = timeAgo + (timeAgo == 1 ? " second" : " seconds");
			if (timeAgo > 60)
			{
				//Seconds to minutes.
				long timeAgoMinutes = timeAgo / 60;
				result = timeAgoMinutes + (timeAgoMinutes == 1 ? " minute" : " minutes");

				if (timeAgoMinutes > 60)
				{
					//Minutes to hours
					int timeAgoHours = (int) (timeAgoMinutes / 60);
					result = timeAgoHours + (timeAgoHours == 1 ? " hour" : " hours");
					if (timeAgoHours > 24)
					{
						int timeAgoDays = timeAgoHours / 24;
						result = timeAgoDays + (timeAgoDays == 1 ? " day" : " days");
					}
				}
			}
			return result;
		}
		else
		{
			return "";
		}
	}

	/**
	 * This method formats a time instant into a string of hours and minutes.
	 *
	 * @param time             The time that needs to be formatted
	 * @param twelveHourFormat Determines if the returned string should be formatted with 12-hour or 24-hour format
	 * @param includeDate      Include the day and month in the format string.
	 * @return A formatted string of hours and minutes dependent on user config (hh:mm a  or  HH:mm)
	 */
	public static String formatTime(Instant time, boolean twelveHourFormat, boolean includeDate)
	{
		DateTimeFormatter timeFormatter;
		String pattern = "";

		if (includeDate)
		{
			//Example:	04 Sep
			pattern = "dd MMM ";
		}

		if (twelveHourFormat)
		{
			//Example:	2:53 PM
			pattern += "hh:mm a";
		}
		else
		{
			//Example:	14:53
			pattern += "HH:mm";
		}
		timeFormatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault());
		return timeFormatter.format(time);
	}

	/**
	 * This method calculates the color based on a red-yellow-green gradient.
	 *
	 * @param percentage  The percentage representing the value that needs to be gradiated.
	 * @param gradientMax The max value representation before the gradient tops out on green.
	 * @return A color representing a value on a red-yellow-green gradient.
	 */
	public static Color gradiatePercentage(float percentage, int gradientMax)
	{
		if (percentage < gradientMax * 0.5)
		{
			return (percentage <= 0) ? Color.RED :
				ColorUtil.colorLerp(Color.RED, Color.YELLOW, percentage / gradientMax * 2);
		}
		else
		{
			return (percentage >= gradientMax) ? Color.GREEN :
				ColorUtil.colorLerp(Color.YELLOW, Color.GREEN, percentage / gradientMax * 0.5);
		}
	}

	/**
	 * Functionally the same as {@link QuantityFormatter#quantityToRSDecimalStack(int, boolean)},
	 * except this allows for formatting longs.
	 *
	 * @param quantity Long to format
	 * @param precise  If true, allow thousandths precision if {@code quantity} is larger than 1 million.
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

		return format.format(Long.divideUnsigned(quantity, (long) Math.pow(10, (Long.divideUnsigned(power, 3)) * 3)))
			+ new String[] {"", "K", "M", "B"}[(int) (power / 3)];
	}
}
