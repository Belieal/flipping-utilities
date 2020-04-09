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
	private int quantity;
	private int price;
	private Instant time;
	private int slot;
	private GrandExchangeOfferState state;

	public boolean isComplete()
	{
		return
			state == GrandExchangeOfferState.BOUGHT ||
				state == GrandExchangeOfferState.SOLD ||
				state == GrandExchangeOfferState.CANCELLED_BUY ||
				state == GrandExchangeOfferState.CANCELLED_SELL;
	}

	/**
	 * Returns an offerInfo object with the quantity sold/bought the amount of items sold/bought since
	 * the last event, rather than current quantity sold/bought overall in the trade. This makes it
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
		standardizedOffer.setQuantity(getQuantity() - lastOffer.getQuantity());
		return standardizedOffer;

	}

	//TODO actually clone the Instant object, as we are currently just passing that as the same reference.
	public OfferInfo clone()
	{
		OfferInfo clonedOffer = new OfferInfo(buy, itemId, quantity, price, time, slot, state);
		return clonedOffer;
	}
}

