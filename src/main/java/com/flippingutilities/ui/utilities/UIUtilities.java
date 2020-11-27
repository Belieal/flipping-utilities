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

import com.flippingutilities.FlippingItem;
import com.flippingutilities.FlippingPlugin;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.QuantityFormatter;
import okhttp3.HttpUrl;

/**
 * This class contains various methods that the UI uses to format their visuals.
 */
@Slf4j
public class UIUtilities
{

	public static final Color OUTDATED_COLOR = new Color(250, 74, 75);
	public static final Color PROFIT_COLOR = new Color(255, 175, 55);
	public static final Color DARK_GRAY_ALT_ROW_COLOR = new Color(35, 35, 35);
	public static final Color DARK_GRAY = new Color(27, 27, 27);

	public static final Color VIBRANT_YELLOW = new Color(230, 220, 6);

	private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
	private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

	private static final String[] PARENTHESIS_EXCLUDED_WORDS = {"empty", "sk", "lg", "dark", "dusk", "light", "unf", "uncharged", "wound", "full", "inactive"};
	private static final NumberFormat PRECISE_DECIMAL_FORMATTER = new DecimalFormat(
		"#,###.###",
		DecimalFormatSymbols.getInstance(Locale.ENGLISH)
	);
	private static final NumberFormat DECIMAL_FORMATTER = new DecimalFormat(
		"#,###.#",
		DecimalFormatSymbols.getInstance(Locale.ENGLISH)
	);

	public static final Dimension ICON_SIZE = new Dimension(32, 32);
	public static final int TOOLBAR_BUTTON_SIZE = 20;

	public static final ImageIcon OPEN_ICON;
	public static final ImageIcon CLOSE_ICON;

	public static final ImageIcon RESET_ICON;
	public static final ImageIcon RESET_HOVER_ICON;

	public static final ImageIcon DELETE_ICON;

	public static final ImageIcon SETTINGS_ICON;

	public static final ImageIcon ACCOUNT_ICON;

	public static final ImageIcon DELETE_BUTTON;

	public static final ImageIcon HIGHLIGHT_DELETE_BUTTON;

	public static final ImageIcon STAR_ON_ICON;
	public static final ImageIcon STAR_HALF_ON_ICON;
	public static final ImageIcon STAR_OFF_ICON;


	public static final ImageIcon SORT_BY_RECENT_OFF_ICON;
	public static final ImageIcon SORT_BY_RECENT_ON_ICON;
	public static final ImageIcon SORT_BY_RECENT_HALF_ON_ICON;


	public static final ImageIcon SORT_BY_ROI_OFF_ICON;
	public static final ImageIcon SORT_BY_ROI_ON_ICON;
	public static final ImageIcon SORT_BY_ROI_HALF_ON_ICON;

	public static final ImageIcon SORT_BY_PROFIT_OFF_ICON;
	public static final ImageIcon SORT_BY_PROFIT_ON_ICON;
	public static final ImageIcon SORT_BY_PROFIT_HALF_ON_ICON;

	public static final ImageIcon ARROW_LEFT;
	public static final ImageIcon ARROW_RIGHT;
	public static final ImageIcon ARROW_LEFT_HOVER;
	public static final ImageIcon ARROW_RIGHT_HOVER;

	static
	{
		final BufferedImage openIcon = ImageUtil
			.getResourceStreamFromClass(FlippingPlugin.class, "/small_open_arrow.png");
		CLOSE_ICON = new ImageIcon(openIcon);
		OPEN_ICON = new ImageIcon(ImageUtil.rotateImage(openIcon, Math.toRadians(90)));

		final BufferedImage resetIcon = ImageUtil
			.getResourceStreamFromClass(FlippingPlugin.class, "/reset.png");
		RESET_ICON = new ImageIcon(resetIcon);
		RESET_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(resetIcon, 0.53f));

		final BufferedImage deleteIcon = ImageUtil
			.getResourceStreamFromClass(FlippingPlugin.class, "/delete_icon.png");
		DELETE_ICON = new ImageIcon(deleteIcon);

		final BufferedImage settingsIcon = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/settings_icon.png");
		SETTINGS_ICON = new ImageIcon(settingsIcon);

		final BufferedImage accountIcon = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/gnome.png");
		ACCOUNT_ICON = new ImageIcon(accountIcon);

		final BufferedImage deleteButton = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/deleteButton.png");
		DELETE_BUTTON = new ImageIcon(deleteButton);

		final BufferedImage highlightDeleteButton = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/highlightDeleteButton.png");
		HIGHLIGHT_DELETE_BUTTON = new ImageIcon(highlightDeleteButton);


		final BufferedImage starOn = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/toolbar-icons/star-gold.png");
		final BufferedImage sortByRecentOn = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/toolbar-icons/clock-gold.png");
		final BufferedImage sortByRoiOn = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/toolbar-icons/roi-gold.png");
		final BufferedImage sortByProfitOn = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/toolbar-icons/profit-gold.png");
		final BufferedImage starOff = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/toolbar-icons/star_off_white.png");
		final BufferedImage sortByRecentOff = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/toolbar-icons/clock_white.png");
		final BufferedImage sortByRoiOff = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/toolbar-icons/thick_roi_white.png");
		final BufferedImage sortByProfitOff = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/toolbar-icons/potential_profit_white.png");

		STAR_ON_ICON = new ImageIcon(starOn.getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));
		SORT_BY_RECENT_ON_ICON = new ImageIcon(sortByRecentOn.getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));
		SORT_BY_ROI_ON_ICON = new ImageIcon(sortByRoiOn.getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));
		SORT_BY_PROFIT_ON_ICON = new ImageIcon(sortByProfitOn.getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));

		STAR_HALF_ON_ICON = new ImageIcon(starOff.getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));
		SORT_BY_PROFIT_HALF_ON_ICON = new ImageIcon(sortByProfitOff.getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));
		SORT_BY_RECENT_HALF_ON_ICON = new ImageIcon(sortByRecentOff.getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));
		SORT_BY_ROI_HALF_ON_ICON = new ImageIcon(sortByRoiOff.getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));

		STAR_OFF_ICON = new ImageIcon(ImageUtil.alphaOffset(starOff, 0.53f).getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));
		SORT_BY_RECENT_OFF_ICON = new ImageIcon(ImageUtil.alphaOffset(sortByRecentOff, 0.53f).getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));
		SORT_BY_ROI_OFF_ICON = new ImageIcon(ImageUtil.alphaOffset(sortByRoiOff, 0.53f).getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));
		SORT_BY_PROFIT_OFF_ICON = new ImageIcon(ImageUtil.alphaOffset(sortByProfitOff, 0.53f).getScaledInstance(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE, Image.SCALE_SMOOTH));

		final BufferedImage arrowLeft = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/left-arrow.png");
		ARROW_LEFT = new ImageIcon(ImageUtil.alphaOffset(arrowLeft,0.70f));

		final BufferedImage arrowRight = ImageUtil.getResourceStreamFromClass(FlippingPlugin.class, "/right-arrow.png");
		ARROW_RIGHT = new ImageIcon(ImageUtil.alphaOffset(arrowRight,0.70f));

		ARROW_LEFT_HOVER = new ImageIcon(arrowLeft);
		ARROW_RIGHT_HOVER = new ImageIcon(arrowRight);
	}

	/**
	 * Formats a duration into HH:MM:SS
	 *
	 * @param duration
	 * @return Formatted (HH:MM:SS) string
	 */
	public static String formatDuration(Duration duration)
	{
		long seconds = duration.toMillis() / 1000;
		return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
	}


	/**
	 * Formats an instant into a duration between the parameter and now.
	 *
	 * @return Formatted (HH:MM:SS) string
	 */
	public static String formatDuration(Instant fromInstant)
	{
		return formatDuration(Duration.between(fromInstant, Instant.now()));
	}

	/**
	 * Formats the duration between one instant to an end instant.
	 *
	 * @param startInstant Start of duration
	 * @param endInstant   End of duration
	 * @return Formatted (HH:MM:SS) string
	 */
	public static String formatDuration(Instant startInstant, Instant endInstant)
	{
		return formatDuration(Duration.between(startInstant, endInstant));
	}

	/**
	 * This method formats a string time from an instant from the time, as specified by the parameter, until now.
	 * It truncates the time representation to the greatest unit. For example, 65 seconds will become a minute
	 * (not a minute and 5 seconds), 70 minutes will become an hour (not an hour and 10 minutes), etc. This is to
	 * save space on the panels.
	 *
	 * @param fromInstant The start of the duration
	 * @return A formatted string.
	 */
	public static String formatDurationTruncated(Instant fromInstant)
	{
		if (fromInstant != null)
		{
			long toInstant = Instant.now().getEpochSecond();

			//Time since trade was done.
			long timeAgo = (toInstant - fromInstant.getEpochSecond());

			String result = timeAgo + (timeAgo == 1 ? " second" : " seconds");
			if (timeAgo >= 60)
			{
				//Seconds to minutes.
				long timeAgoMinutes = timeAgo / 60;
				result = timeAgoMinutes + (timeAgoMinutes == 1 ? " minute" : " minutes");

				if (timeAgoMinutes >= 60)
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

	/**
	 * Builds the url required for opening the OSRS Exchange page for the item.
	 * <p>
	 * Example of a lobster's URL:
	 * http://services.runescape.com/m=itemdb_oldschool/Runescape/viewitem?obj=6416
	 *
	 * @param itemId The item to be opened on the Exchange.
	 * @return Returns the full URL for opening in the browser.
	 */
	private static String buildOsrsExchangeUrl(int itemId)
	{
		String url = new HttpUrl.Builder()
			.scheme("http")
			.host("services.runescape.com")
			.addPathSegment("m=itemdb_oldschool")
			.addPathSegment("Runescape")
			.addPathSegment("viewitem")
			.addQueryParameter("obj", String.valueOf(itemId))
			.build()
			.toString();

		log.info("Opening OSRS Exchange: " + url);
		return url;
	}

	/**
	 * This method builds the https://platinumtokens.com (PT) url from the given itemName.
	 * PT takes a slugged (Dragon dagger(p++) -> dragon-dagger-p-plus-plus) as its item query parameter.
	 * This method therefore also slugs the item's name, however it's not perfect. There are some continuity errors
	 * in the slug format used by the site (Rune armour set (sk) WON'T redirect to the item as the (sk) is
	 * slugged as -sk even though every single other item on the website, with parentheses, are slugged like -s-k).
	 * Thankfully, this just means the user will be directed to the base URL, so wouldn't be too disruptive for  the user.
	 * <p>
	 * Example of an item's url (Dragon dagger(p++)):
	 * https://platinumtokens.com/item/dragon-dagger-p-plus-plus
	 *
	 * @param itemName The item's name to be opened on PT
	 * @return Returns the URL for the item on PT
	 */
	private static String buildPlatinumTokensUrl(String itemName)
	{
		//Determine if item name contains parentheses.
		String[] splitString = itemName.split("\\(");
		boolean containsParentheses = splitString.length != 1;
		if (containsParentheses)
		{
			if (Arrays.stream(PARENTHESIS_EXCLUDED_WORDS).parallel().noneMatch(itemName::contains))
			{
				//Every character inside parentheses need to be slugged.
				itemName = splitString[0] + splitString[1].replace("", "-");
			}
		}

		//'+' is slugged to "plus"
		itemName = itemName.replace("+", "plus");

		//All whitespaces are replaced with slugs
		String noWhitespace = WHITESPACE.matcher(itemName).replaceAll("-");
		//Normalize any characters not expected
		String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
		//Remove all remaining parentheses or other symbols and check that we don't have any double slugs.
		String slug = NONLATIN.matcher(normalized).replaceAll("").replace("--", "-");

		if (containsParentheses)
		{
			if (Arrays.stream(PARENTHESIS_EXCLUDED_WORDS).parallel().noneMatch(itemName::contains))
			{
				//If we removed the parentheses earlier, we're guaranteed to have a trailing slug
				slug = slug.substring(0, slug.length() - 1);
			}
		}

		//Build the url
		String url = new HttpUrl.Builder()
			.scheme("https")
			.host("platinumtokens.com")
			.addPathSegment("item")
			.addPathSegment(slug.toLowerCase(Locale.ENGLISH))
			.build()
			.toString();

		log.info("Opening Platinum Tokens: " + url);
		return url;
	}

	public static JPopupMenu createGeTrackerLinksPopup(FlippingItem flippingItem)
	{
		final JMenuItem openOsrsGe = new JMenuItem("Open in OSRS Exchange");
		openOsrsGe.addActionListener(e -> LinkBrowser.browse(UIUtilities.buildOsrsExchangeUrl(flippingItem.getItemId())));

		//Opens the item's Platinum Tokens page
		final JMenuItem openPlatinumTokens = new JMenuItem("Open in PlatinumTokens.com");
		openPlatinumTokens.addActionListener(e -> LinkBrowser.browse(UIUtilities.buildPlatinumTokensUrl(flippingItem.getItemName())));

		final JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.setBorder(new EmptyBorder(5, 5, 5, 5));
		popupMenu.add(openOsrsGe);
		popupMenu.add(openPlatinumTokens);

		return popupMenu;
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
		GridBagConstraints constraints = new GridBagConstraints();
		mainPanel.setLayout(new GridBagLayout());

		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1;
		constraints.gridx = 0;
		constraints.gridy = 0;

		int index = 0;
		for (JPanel panel : panels)
		{
			if (index++ > 0)
			{
				JPanel marginWrapper = new JPanel(new BorderLayout());
				marginWrapper.add(panel, BorderLayout.NORTH);
				marginWrapper.setBorder(new EmptyBorder(vGap,0,0,0));
				mainPanel.add(marginWrapper, constraints);
			}
			else
			{
				mainPanel.add(panel, constraints);
			}

			constraints.gridy++;
		}
	}
}
