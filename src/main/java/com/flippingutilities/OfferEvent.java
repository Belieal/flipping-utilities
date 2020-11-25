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


import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.events.GrandExchangeOfferChanged;

/**
 * This class stores information from a {@link GrandExchangeOfferChanged} event and is populated with
 * extra information such as ticksSinceFirstOffer and quantitySinceLastOffer based on previous offers
 * belonging to the same trade as it.
 */
@Data
@AllArgsConstructor
public class OfferEvent
{
	@SerializedName("b")
	private boolean buy;
	@SerializedName("id")
	private int itemId;
	@SerializedName("cQIT")
	private int currentQuantityInTrade;
	@SerializedName("p")
	private int price;
	@SerializedName("t")
	private Instant time;
	@SerializedName("s")
	private int slot;
	@SerializedName("st")
	private GrandExchangeOfferState state;
	@SerializedName("tAA")
	private int tickArrivedAt;
	@SerializedName("tSFO")
	private int ticksSinceFirstOffer;
	@SerializedName("tQIT")
	private int totalQuantityInTrade;
	//States that determine if the offer is appurtenant to the current scope of the panel.
	//The states change dependent on user-selected removals.
	@SerializedName("vSQ")
	private boolean validOfferEvent;
	/**
	 * a offer always belongs to a flipping item. Every flipping item was flipped by an account and only one account and
	 * has a flipped by attribute. So, the reason this attribute is here is because during the process of creating
	 * the account wide trade list, we merge flipping items flipped by several different accounts into one. Thus, in that
	 * case, a flipping item would have been flipped by multiple accounts so each offer needs to be marked to
	 * differentiate offer. This functionality is currently only used in getFlips as, when getting the flips for the
	 * account wide list, you don't want to match offers from different accounts!
	 */
	private transient String madeBy;

	private transient boolean beforeLogin;
	//only used in theGeHistoryTabOfferPanel cause i don't want to pass the itemmanager down that far just to resolve item name from an id.
	private transient String itemName;

	/**
	 * Returns a boolean representing that the offer is a complete offer. A complete offer signifies
	 * the end of that trade, thus the end of the slot's history. The HistoryManager uses this to decide when
	 * to clear the history for a slot.
	 *
	 * @return boolean value representing that the offer is a complete offer
	 */
	public boolean isComplete()
	{
		return
			state == GrandExchangeOfferState.BOUGHT ||
				state == GrandExchangeOfferState.SOLD ||
				state == GrandExchangeOfferState.CANCELLED_BUY ||
				state == GrandExchangeOfferState.CANCELLED_SELL;
	}

	/**
	 * when an offer is complete, two events are generated: a buying/selling event and a bought/sold event.
	 * this method identifies the redundant buying/selling event before the bought/sold event.
	 */
	public boolean isRedundantEventBeforeOfferCompletion()
	{
		return (state == GrandExchangeOfferState.BUYING || state == GrandExchangeOfferState.SELLING) && currentQuantityInTrade == totalQuantityInTrade;
	}

	/**
	 * A margin check is defined as an offer that is either a BOUGHT or SOLD offer and has a currentQuantityInTrade of 1. This
	 * resembles the typical margin check process wherein you buy an item (currentQuantityInTrade of 1) for a high press, and then
	 * sell that item (currentQuantityInTrade of 1), to figure out the optimal buying and selling prices.
	 *
	 * @return boolean value representing whether the offer is a margin check or not
	 */
	public boolean isMarginCheck()
	{
		return (state == GrandExchangeOfferState.BOUGHT || state == GrandExchangeOfferState.SOLD) && totalQuantityInTrade == 1
			&& ticksSinceFirstOffer <= 2;
	}

	/**
	 * We get an event for every empty slot on logic
	 *
	 * @return whether this OfferEvent was caused by an empty slot
	 */
	public boolean isCausedByEmptySlot()
	{
		return (itemId == 0 || state == GrandExchangeOfferState.EMPTY);
	}

	/**
	 * When we first place an offer for a slot we get an offer event that has a quantity traded of 0. This offer marks
	 * the tick the offer was placed. The reason we need to also check if it wasn't a complete offer is because you can
	 * cancel a buy or a sell, and provided you didn't buy or sell anything, the quantity in the offer can be 0, but its
	 * not the start of the offer.
	 *
	 * @return boolean value representing whether the offer is a start of a trade.
	 */
	public boolean isStartOfOffer()
	{
		return currentQuantityInTrade == 0 && !isComplete();
	}

	public OfferEvent clone()
	{
		return new OfferEvent(buy,
			itemId,
			currentQuantityInTrade,
			price,
			time,
			slot,
			state,
			tickArrivedAt,
			ticksSinceFirstOffer,
			totalQuantityInTrade,
			validOfferEvent,
			madeBy,
			beforeLogin,
			itemName);
	}

	public boolean equals(Object other)
	{
		if (other == this)
		{
			return true;
		}

		if (!(other instanceof OfferEvent))
		{
			return false;
		}

		OfferEvent otherOffer = (OfferEvent) other;

		return isDuplicate(otherOffer)
			&& tickArrivedAt == otherOffer.tickArrivedAt
			&& ticksSinceFirstOffer == otherOffer.ticksSinceFirstOffer
			&& time.equals(otherOffer.time)
			&& validOfferEvent == otherOffer.validOfferEvent;
	}

	/**
	 * This checks whether the given OfferEvent is a "duplicate" of this OfferEvent. Some fields such as
	 * tickArrivedAt are omitted because even if they are different, the given offer is still redundant due to all
	 * the other information being the same and should be screened out by screenOfferEvent in FlippingPlugin, where
	 * this method is used.
	 *
	 * @param other the OfferEvent being compared.
	 * @return whether or not the given offer event is redundant
	 */
	public boolean isDuplicate(OfferEvent other)
	{
		return state == other.getState()
			&& currentQuantityInTrade == other.getCurrentQuantityInTrade()
			&& slot == other.getSlot()
			&& totalQuantityInTrade == other.getTotalQuantityInTrade() && itemId == other.getItemId()
			&& price == other.getPrice();
	}

	public static OfferEvent fromGrandExchangeEvent(GrandExchangeOfferChanged event)
	{
		GrandExchangeOffer offer = event.getOffer();

		boolean isBuy = offer.getState() == GrandExchangeOfferState.BOUGHT
			|| offer.getState() == GrandExchangeOfferState.CANCELLED_BUY
			|| offer.getState() == GrandExchangeOfferState.BUYING;

		return new OfferEvent(
			isBuy,
			offer.getItemId(),
			offer.getQuantitySold(),
			offer.getQuantitySold() == 0 ? 0 : offer.getSpent() / offer.getQuantitySold(),
			Instant.now().truncatedTo(ChronoUnit.SECONDS),
			event.getSlot(),
			offer.getState(),
			0,
			0,
			offer.getTotalQuantity(),
			true,
			null,
			false,
			null);
	}

	/**
	 * Sets the ticks since the first offer event of the trade that this offer event belongs to. The ticks since first
	 * offer event is used to determine whether an offer event is a margin check or not.
	 *
	 * @param lastOfferForSlot the last offer event for the slot this offer event belongs to
	 */
	public void setTicksSinceFirstOffer(OfferEvent lastOfferForSlot)
	{
		int tickDiffFromLastOffer = Math.abs(tickArrivedAt - lastOfferForSlot.getTickArrivedAt());
		ticksSinceFirstOffer = tickDiffFromLastOffer + lastOfferForSlot.getTicksSinceFirstOffer();
	}
}

