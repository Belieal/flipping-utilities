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


import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.events.GrandExchangeOfferChanged;

/**
 * This class stores information from a {@link GrandExchangeOfferChanged} event.
 * A {@link GrandExchangeOfferChanged} event has all of the information needed already, but passing it
 * around when current methods like addFlipTrade, addToTradesList, and updateFlip expect a
 * {@Link GrandExchangeTrade} would require a lot of changes in existing code. Instead, by subclassing
 * GrandExchangeTrade and adding two new fields that are needed for future changes, backwards
 * compatability is maintained, while allowing new work using the additional info stored in this class.
 */

@Data
@AllArgsConstructor
public class OfferInfo
{
	private boolean buy;
	private int itemId;
	private int currentQuantityInTrade;
	private int price;
	private Instant time;
	private int slot;
	private GrandExchangeOfferState state;
	private int tickArrivedAt;
	private int ticksSinceFirstOffer;
	private int totalQuantityInTrade;
	private int quantitySinceLastOffer;

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
	 * Returns an offerInfo object with the currentQuantityInTrade sold/bought the amount of items sold/bought since
	 * the last event, rather than current currentQuantityInTrade sold/bought overall in the trade. This makes it
	 * easier to calculate the profit.
	 * This value could be set from outside and a clone does not need to be returned, but since references
	 * to the same offerInfo are used throughout the code, avoiding mutation is best.
	 *
	 * @param lastOffer the last offer from that slot.
	 * @return a standardized offer
	 */
	public OfferInfo standardizeOffer(OfferInfo lastOffer)
	{
		OfferInfo standardizedOffer = clone();
		standardizedOffer.setQuantitySinceLastOffer(getCurrentQuantityInTrade() - lastOffer.getCurrentQuantityInTrade());
		return standardizedOffer;
	}

	//TODO actually clone the Instant object, as we are currently just passing that as the same reference.
	public OfferInfo clone()
	{
		return new OfferInfo(buy, itemId, currentQuantityInTrade, price, time, slot, state, tickArrivedAt, ticksSinceFirstOffer, totalQuantityInTrade, quantitySinceLastOffer);
	}

	public boolean equals(Object other)
	{
		if (other == this)
		{
			return true;
		}

		if (!(other instanceof OfferInfo))
		{
			return false;
		}

		OfferInfo otherOffer = (OfferInfo) other;

		return getState() == otherOffer.getState() && getCurrentQuantityInTrade() == otherOffer.getCurrentQuantityInTrade();
	}
}

