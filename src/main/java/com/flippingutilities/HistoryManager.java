package com.flippingutilities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.events.GrandExchangeOfferChanged;

/**
 * manages the history for an item. This class is responsible for figuring out how much profit a user made for
 * an item.
 */
public class HistoryManager
{
	//contains the history for each slot so that when a new offer comes in for a slot, we can use the
	//slot history to figure out how many new items were bought/sold. When a offer with a state that is
	//complete (bought/sold/cancelled buy/cancelled sell) comes in, the history for that slot is removed
	//as the slot is now empty.
	private Map<Integer, List<OfferInfo>> slotHistory = new HashMap<>();

	//a list of standardizedOffers. A standardizedOffer is an offer with a quantity that represents the
	//quantity bought since the last offer. A regular offer just has info from an offerEvent, which gives
	//you the current quantity bought/sold overall in the trade.
	private List<OfferInfo> standardizedOffers = new ArrayList<>();

	/**
	 * @param newOffer the OfferInfo object created from the {@link GrandExchangeOfferChanged} event that
	 *                 onGrandExchangeOfferChanged (in FlippingPlugin) receives. It is crucial to note that
	 *                 This OfferInfo object contains the current quantity bought/sold for the trade currently, not the amount
	 *                 bought/sold since the last offer (to fix this, we "standardize" the offer in this method itself by comparing
	 *                 it to the last offer seen for that slot (provided it belongs to the same trade).
	 */
	public void updateHistory(OfferInfo newOffer)
	{
		int newOfferSlot = newOffer.getSlot();

		//if there are currently trades in progress in that slot
		if (slotHistory.containsKey(newOfferSlot))
		{

			List<OfferInfo> currentTradesForSlot = slotHistory.get(newOfferSlot);
			OfferInfo lastOffer = currentTradesForSlot.get(currentTradesForSlot.size() - 1);
			OfferInfo standardizedOffer = newOffer.standardizeOffer(lastOffer);
			standardizedOffers.add(standardizedOffer);
			currentTradesForSlot.add(newOffer);

			//if the offer is complete, delete the history for that slot.
			if (newOffer.isComplete())
			{
				slotHistory.remove(newOfferSlot);
			}
		}

		//its the first trade for that slot!
		else
		{
			//don't need to standardize as its quantity represents the quantity bought as its the first
			//trade in that slot.
			standardizedOffers.add(newOffer);

			//if the offer was a complete offer there's no need to add it to the slot history as a complete
			//offer means the slot history is over.
			if (!newOffer.isComplete())
			{
				slotHistory.put(newOfferSlot, new ArrayList<>(Arrays.asList(newOffer)));

			}
		}
	}

	//TODO
	//return a summary, not just the profit. A summary will include the profit, and the quantity of buys/sells
	//and the individual prices (only if they are different)

	/**
	 * Calculates profit for this item by looking at the standardizedOffers list, filtering out offers
	 * that are older than the earliest time, putting buy offers and sell offers into a different list, and then
	 * returning the difference in value of the sell list and the buy list.
	 *
	 * @param earliestTime the earliest time the user wants trades to impact profit for this item for.
	 * @return profit
	 */
	public int currentProfit(Instant earliestTime)
	{
		List<OfferInfo> buyList = new ArrayList<>();
		List<OfferInfo> sellList = new ArrayList<>();
		int numBoughtItems = 0;
		int numSoldItems = 0;

		for (OfferInfo standardizedOffer : standardizedOffers)
		{
			//later than the time the user selected (if they selected 4 hours, its all trades after 4 hours ago.
			if (standardizedOffer.getTime().compareTo(earliestTime) > 0)
			{
				if (standardizedOffer.isBuy())
				{
					numBoughtItems += standardizedOffer.getQuantity();
					buyList.add(standardizedOffer);
				}
				else
				{
					numSoldItems += standardizedOffer.getQuantity();
					sellList.add(standardizedOffer);
				}
			}
		}

		//if a user has only sold items, or a user has only bought items, return profit as 0 as there
		//is no way to determine how much profit was made until you have both a buy and a sell (so that you can
		// actually calculate a difference).
		if (numBoughtItems == 0 || numSoldItems == 0)
		{
			return 0;
		}

		int itemCountLimit = Math.min(numBoughtItems, numSoldItems);
		//return the value of the sell list - the value of the buy list. This is the profit.
		return getValueOfTrades(sellList, itemCountLimit) - getValueOfTrades(buyList, itemCountLimit);
	}

	/**
	 * Calculates the amount of money spent on either a buy or sell list, up to the amount of items
	 * specified by the limit.
	 *
	 * @param tradeList a buy or a sell list
	 * @param itemLimit the amount of items to calculate the value up until. This is for the case
	 *                  when a user has an unequal amount of buys/sells in which case you want to return the
	 *                  profit the items only up until the buys and sells are equal.
	 * @return the amount of money spent on the offer list, up to the amount of items specified by the
	 * limit
	 */
	public int getValueOfTrades(List<OfferInfo> tradeList, int itemLimit)
	{
		int itemsSeen = 0;
		int moneySpent = 0;

		for (OfferInfo offer : tradeList)
		{
			if (itemsSeen + offer.getQuantity() >= itemLimit)
			{
				moneySpent += (itemLimit - itemsSeen) * offer.getPrice();
				break;
			}
			else
			{
				moneySpent += offer.getQuantity() * offer.getPrice();
				itemsSeen += offer.getQuantity();
			}
		}

		return moneySpent;
	}
}
