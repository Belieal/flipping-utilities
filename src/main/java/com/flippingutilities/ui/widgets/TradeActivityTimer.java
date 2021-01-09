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

import com.flippingutilities.controller.FlippingPlugin;
import com.flippingutilities.model.OfferEvent;
import com.flippingutilities.ui.uiutilities.CustomColors;
import java.awt.Color;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.flippingutilities.ui.uiutilities.TimeFormatters;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.util.ColorUtil;

@Slf4j
public class TradeActivityTimer
{
	//Spacing between the slot state text and the timer
	private static final String BUY_SPACER = "          ";
	private static final String SELL_SPACER = "          ";
	private static final int FONT_ID = 495;

	//The slot that this timer object embeds to
	@Getter
	private transient Widget slotWidget;
	//The widget of the text at the top of the offer slot
	private transient Widget slotStateWidget;
	//The state can be one of "Sell", "Buy" and "Empty", depending on the slot's offer state
	private transient String slotStateString;

	@Setter
	private transient FlippingPlugin plugin;
	@Setter
	private transient Client client;

	//Index of the slot widget from left to right, top to bottom. (0-7)
	private int slotIndex;
	private Instant lastUpdate;
	private Instant tradeStartTime;
	private OfferEvent currentOffer;
	//is true when we get an offer from when the account was logged out which means we don't know when it occurred.
	private transient boolean offerOccurredAtUnknownTime;

	public TradeActivityTimer(FlippingPlugin plugin, Client client, int slotIndex)
	{
		this.plugin = plugin;
		this.client = client;
		this.slotIndex = slotIndex;
	}

	public void setWidget(Widget slotWidget)
	{
		this.slotWidget = slotWidget;
		slotStateWidget = slotWidget.getChild(16);
		slotStateString = slotStateWidget.getText();
	}

	public void setCurrentOffer(OfferEvent offer)
	{
		if (offer.isBeforeLogin()) {
			offerOccurredAtUnknownTime = true;
			return;
		}

		offerOccurredAtUnknownTime = false;
		currentOffer = offer;
		lastUpdate = Instant.now();

		if (currentOffer.isStartOfOffer())
		{
			tradeStartTime = Instant.now();
		}
	}

	/**
	 * Updates the slot trade activity timer. It reassigns all widget field variables,
	 * as they sometimes get unloaded and therefore won't point to the right widget objects.
	 */
	public void updateTimer()
	{
		if (slotWidget == null)
		{
			return;
		}

		if (slotWidget.isHidden() || plugin.getCurrentlyLoggedInAccount() == null || currentOffer == null || offerOccurredAtUnknownTime || tradeStartTime == null)
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

		//Reassign widgets
		slotWidget = offerSlot;
		slotStateWidget = slotWidget.getChild(16);
		slotStateString = slotStateWidget.getText();

		if (!isSlotFilled())
		{
			//should i set current offer to null?
			//The slot hasn't been filled with an offer, so default to Jagex format.
			slotStateWidget.setText("Empty");
			slotStateWidget.setFontId(496);
			slotStateWidget.setXTextAlignment(1);
			return;
		}

		setText(createFormattedTimeString());
		slotStateWidget.setFontId(FONT_ID);
		slotStateWidget.setXTextAlignment(0);
	}

	/**
	 * Appends the offer state text widget with the up-to-date time string.
	 *
	 * @param timeString Formatted timer string.
	 */
	private void setText(String timeString)
	{
		String spacer;
		Color stateTextColor;

		//switching comps, going on mobile, etc can leave a stale offer in there, so we have to verify using the actual
		//offer from the client.
		GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
		GrandExchangeOffer clientOffer = offers[slotIndex];
		if (clientOffer.getState() == GrandExchangeOfferState.BOUGHT || clientOffer.getState() == GrandExchangeOfferState.BUYING || clientOffer.getState() == GrandExchangeOfferState.CANCELLED_BUY) {
			slotStateString = "Buy";
			spacer = BUY_SPACER;
			stateTextColor = plugin.getConfig().slotTimerBuyColor();
		}
		else
		{
			slotStateString = "Sell";
			spacer = SELL_SPACER;
			stateTextColor = plugin.getConfig().slotTimerSellColor();
		}

		Color timeColor = isSlotStagnant() ? CustomColors.OUTDATED_COLOR : Color.WHITE;

		if (clientOffer.getState() == GrandExchangeOfferState.CANCELLED_BUY || clientOffer.getState() == GrandExchangeOfferState.CANCELLED_SELL || clientOffer.getState() == GrandExchangeOfferState.BOUGHT || clientOffer.getState() == GrandExchangeOfferState.SOLD)
		{
			//Override to completion color
			timeColor = new Color(0, 180, 0);
		}

		if (timeString.length() > 9)
		{
			//Make sure we don't overflow the text
			timeString = "   --:--:--";
		}

		slotStateWidget.setText("  <html>" + ColorUtil.wrapWithColorTag(slotStateString, stateTextColor) + spacer + ColorUtil.wrapWithColorTag(timeString, timeColor) + "</html>");
	}

	/**
	 * Resets the offer state text to default Jagex format.
	 */
	public void resetToDefault()
	{
		try {
			if (!isSlotFilled())
			{
				slotStateWidget.setText("Empty");
			}
			else if (currentOffer.isBuy())
			{
				slotStateWidget.setText("Buy");
			}
			else if (!currentOffer.isBuy())
			{
				slotStateWidget.setText("Sell");
			}
			slotStateWidget.setFontId(496);
			slotStateWidget.setXTextAlignment(1);
		}
		catch (NullPointerException e) {
			log.info("npe when resetting slot visuals. This is ok");
		}
	}

	/**
	 * Finds whether the slot is filled with an offer or if it's empty.
	 *
	 * @return Returns true if the slot is filled and false if it's empty.
	 */
	private boolean isSlotFilled()
	{
		GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
		return offers[slotIndex].getItemId() != 0;
	}

	/**
	 * Returns whether the slot contains a stagnant offer as defined by the user config.
	 * Has to be run after createFormattedTimeString() else the timerBase won't be sufficiently up to date.
	 */
	private boolean isSlotStagnant()
	{
		return lastUpdate.isBefore(Instant.now().minus(plugin.getConfig().tradeStagnationTime(), ChronoUnit.MINUTES));
	}


	public String createFormattedTimeString()
	{
		if (currentOffer.isComplete())
		{
			return TimeFormatters.formatDuration(tradeStartTime, lastUpdate);
		}

		else
		{
			return TimeFormatters.formatDuration(lastUpdate, Instant.now());
		}
	}
}