package com.flippingutilities;


import lombok.Data;
import lombok.EqualsAndHashCode;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.http.api.ge.GrandExchangeTrade;

/**
 * This class stores information from a {@link GrandExchangeOfferChanged} event.
 * A {@link GrandExchangeOfferChanged} event has all of the information needed already, but passing it
 * around when current methods like addFlipTrade, addToTradesList, and updateFlip expect a
 * {@Link GrandExchangeTrade} would require a lot of changes in existing code. Instead, by subclassing
 * GrandExchangeTrade and adding two new fields that are needed for future changes, backwards
 * compatability is maintained, while allowing new work using the additional info stored in this class.
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class OfferInfo extends GrandExchangeTrade
{

	//new fields that weren't in GrandExchangeTrade
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

	public OfferInfo clone()
	{
		OfferInfo clonedOffer = new OfferInfo();
		clonedOffer.setBuy(isBuy());
		clonedOffer.setItemId(getItemId());
		clonedOffer.setQuantity(getQuantity());
		clonedOffer.setTime(getTime()); //need to clone the instant object too. Currently this is just a reference to the old one.
		clonedOffer.setSlot(slot);
		clonedOffer.setState(state);
		clonedOffer.setPrice(getPrice());
		return clonedOffer;
	}

	public String toString()
	{
		return "buy: " + isBuy() + " quantity: " + getQuantity() + " price each: " + getPrice() +
			" slot " + slot + " state " + state;
	}
}

