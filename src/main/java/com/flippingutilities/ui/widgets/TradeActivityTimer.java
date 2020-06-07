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

package com.flippingutilities.ui.widgets;

import com.flippingutilities.FlippingItem;
import com.flippingutilities.FlippingPlugin;
import com.flippingutilities.OfferInfo;
import com.flippingutilities.ui.UIUtilities;
import java.awt.Color;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.ColorUtil;

@Slf4j
public class TradeActivityTimer
{
	//Spacing between the slot state text and the timer
	private static final String BUY_SPACER = "          ";
	private static final String SELL_SPACER = "          ";
	private static final int PROGRESS_BAR_COMPLETED_COLOR = 24320;
	private static final int PROGRESS_BAR_CANCELLED_COLOR = 9371648;

	//AKA RunescapeFont (non-bold)
	private static final int FONT_ID = 495;

	//The slot that this timer object embeds to
	@Setter
	private Widget slotWidget;
	private ClientThread clientThread;
	private FlippingPlugin plugin;
	private ScheduledExecutorService executor;
	private Client client;
	//Index of the slot widget from left to right, top to bottom. (0-7)
	private int slotIndex;

	//Future for the repeating timer updates.
	//Call interrupt(true) on it to cancel all independent timer updates.
	private ScheduledFuture<?> repeatingTimerUpdates;

	//The widget of the text at the top of the offer slot
	private Widget slotStateWidget;
	//The item icon is only shown if there's an offer filling the offer slot
	private Widget itemIconWidget;
	//The state can be one of "Sell", "Buy" and "Empty", depending on the slot's offer state
	private String slotStateString;

	//When the offer was first detected and timer drawn.
	private Instant offerStartTime = Instant.EPOCH;
	private Instant timerBase = Instant.now();

	public TradeActivityTimer(Widget slotWidget, ClientThread clientThread, int slotIndex, FlippingPlugin plugin, ScheduledExecutorService executor, Client client)
	{
		this.slotWidget = slotWidget;
		this.clientThread = clientThread;
		this.slotIndex = slotIndex;
		this.plugin = plugin;
		this.executor = executor;
		this.client = client;

		slotStateString = slotWidget.getChild(16).getText();

		//Begin independent timer updates
		repeatingTimerUpdates = startTimerUpdates();
	}

	/**
	 * Calls updates for the timer display every second.
	 *
	 * @return A future that can be cancelled to halt independent updates.
	 */
	public ScheduledFuture<?> startTimerUpdates()
	{
		return executor.scheduleAtFixedRate(() ->
			clientThread.invokeLater(this::updateTimer), 0, 500, TimeUnit.MILLISECONDS);
	}

	/**
	 * Cancels all timer updates. Should be used on shutdown of the plugin to make sure we don't mess with other plugins.
	 */
	public void stopTimerUpdates()
	{
		repeatingTimerUpdates.cancel(true);
	}

	/**
	 * Updates the slot trade activity timer. It reassigns all widget field variables,
	 * as they sometimes get unloaded and therefore won't point to the right widget objects.
	 */
	public void updateTimer()
	{
		//Don't need to update if the timer won't be visible to the user.
		if (slotWidget == null || slotWidget.isHidden())
		{
			return;
		}

		//Reload offerSlot widget in case it got unloaded previously
		Widget offerSlot = client.getWidget(WidgetID.GRAND_EXCHANGE_GROUP_ID, 5).getStaticChildren()[slotIndex + 1];

		//Ideally this shouldn't be triggered, but just in case.
		if (offerSlot == null)
		{
			return;
		}

		slotStateWidget = slotWidget.getChild(16);

		if (slotStateString.equals("Empty") && !slotStateWidget.getText().equals("Empty"))
		{
			//The timer has been assigned to a new offer, update the draw time.
			offerStartTime = Instant.now();
		}

		//Reassign widgets
		slotWidget = offerSlot;
		itemIconWidget = slotWidget.getChild(18);
		slotStateString = slotStateWidget.getText();

		if (!isSlotFilled())
		{
			//The slot hasn't been filled with an offer, so default to Jagex format.
			resetToEmpty();
			return;
		}

		//Look for the stored trade in the history.
		FlippingItem flippingItem = getItemFromWidget(new ArrayList<>(plugin.getTradesForCurrentView()));

		if (plugin.getCurrentlyLoggedInAccount() == null)
		{
			return;
		}

		if (flippingItem == null && plugin.getAccountCache().get(plugin.getCurrentlyLoggedInAccount()).getLastOffers().get(slotIndex) == null)
		{
			//Again, this shouldn't happen since the trade will always be created before the timer.
			//However, it may happen if the user's trades weren't saved properly or they just installed the plugin
			//with ongoing offers.
			return;
		}

		setText(createFormattedTimeString(flippingItem), isSlotStagnant(), isSlotComplete());
		slotStateWidget.setFontId(FONT_ID);
		slotStateWidget.setXTextAlignment(0);
	}

	/**
	 * Appends the offer state text widget with the up-to-date timer.
	 *
	 * @param timeString      Formatted timer string.
	 * @param tradeIsStagnant Designates whether to treat the offer as being stagnant.
	 * @param tradeIsComplete Designates whether to treat the offer to be complete.
	 */
	private void setText(String timeString, boolean tradeIsStagnant, boolean tradeIsComplete)
	{
		String spacer;
		if (slotStateString.contains("Buy"))
		{
			slotStateString = "Buy";
			spacer = BUY_SPACER;
		}
		else
		{
			slotStateString = "Sell";
			spacer = SELL_SPACER;
		}

		Color color = tradeIsStagnant ? UIUtilities.OUTDATED_COLOR : Color.WHITE;

		if (tradeIsComplete)
		{
			//Override to completion color
			color = new Color(0, 180, 0);
		}

		System.out.println(timeString);

		if (timeString.length() > 9)
		{
			//Make sure we don't overflow the text
			timeString = "   --:--:--";
		}

		slotStateWidget.setText("  <html>" + slotStateString + spacer + ColorUtil.wrapWithColorTag(timeString, color) + "</html>");
	}

	/**
	 * Finds and returns the flippingItem based on the widgetIcon itemId.
	 * Will return null if no items are found.
	 *
	 * @return The found flippingItem from the tradeList or null
	 */
	public FlippingItem getItemFromWidget(List<FlippingItem> tradeList)
	{
		int widgetItemId = itemIconWidget.getItemId();

		Optional<FlippingItem> foundItem = tradeList.stream()
			.filter(item -> item.getItemId() == widgetItemId)
			.findFirst();

		return foundItem.orElse(null);
	}

	/**
	 * Resets the offer state text to default Jagex format.
	 */
	private void resetToEmpty()
	{
		slotStateWidget.setText("Empty");
		slotStateWidget.setFontId(496);
		slotStateWidget.setXTextAlignment(1);
	}

	/**
	 * Finds whether the slot is filled with an offer or if it's empty.
	 *
	 * @return Returns true if the slot is filled and false if it's empty.
	 */
	private boolean isSlotFilled()
	{
		//Child index 0 is exclusively visible on empty slots
		return slotWidget.getChild(0).isHidden();
	}

	/**
	 * Returns whether the slot has a completed offer in it.
	 * Based on the color of the progress bar of the slot.
	 */
	private boolean isSlotComplete()
	{
		int progressBarColor = slotWidget.getChild(22).getTextColor();
		return progressBarColor == PROGRESS_BAR_COMPLETED_COLOR || progressBarColor == PROGRESS_BAR_CANCELLED_COLOR;
	}

	/**
	 * Returns whether the slot contains a stagnant offer as defined by the user config.
	 * Has to be run after createFormattedTimeString() else the timerBase won't be sufficiently up to date.
	 */
	private boolean isSlotStagnant()
	{
		return timerBase.isBefore(Instant.now().minus(plugin.getConfig().tradeStagnationTime(), ChronoUnit.MINUTES));
	}

	/**
	 * Creates and returns a formatted time string based on (HH:MM:SS).
	 *
	 * @param flippingItem The item in the slot that the timer is based upon.
	 * @return Returns a formatted timer string.
	 */
	private String createFormattedTimeString(FlippingItem flippingItem)
	{
		if (!plugin.getAccountCache().get(plugin.getCurrentlyLoggedInAccount()).getLastOffers().containsKey(slotIndex))
		{
			timerBase = Instant.now();
			return UIUtilities.formatDuration(timerBase);
		}

		OfferInfo latestOffer = plugin.getAccountCache().get(plugin.getCurrentlyLoggedInAccount()).getLastOffers().get(slotIndex);

		//Get the first offer recorded of this item in this slot.
		if (flippingItem == null)
		{
			timerBase = offerStartTime;
			return UIUtilities.formatDuration(offerStartTime);
		}

		if (latestOffer.getSlot() == slotIndex && latestOffer.getItemId() == flippingItem.getItemId()
			&& !latestOffer.isComplete() && latestOffer.getCurrentQuantityInTrade() == 0)
		{

			offerStartTime = latestOffer.getTime();
		}

		Instant latestTradeTime = flippingItem.getLatestTradeUpdateBySlot(slotIndex, slotStateString.contains("Buy"), isSlotComplete());

		//Fallback in case we didn't find a time stored.
		//Happens when no offers have been recorded previously
		if (latestTradeTime == null)
		{
			timerBase = offerStartTime;
			return UIUtilities.formatDuration(offerStartTime);
		}

		else if (isSlotComplete())
		{
			return UIUtilities.formatDuration(offerStartTime, latestTradeTime);
		}
		else
		{
			timerBase = latestTradeTime.isAfter(offerStartTime) ? latestTradeTime : offerStartTime;
			return UIUtilities.formatDuration(timerBase);
		}
	}
}
