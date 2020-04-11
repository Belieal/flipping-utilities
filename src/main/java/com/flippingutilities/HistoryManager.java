package com.flippingutilities;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import net.runelite.api.events.GrandExchangeOfferChanged;

/**
 * Manages the history for an item. This class is responsible for figuring out how much profit a user made for
 * an item along with tracking how many items they bought since the last ge limit refresh and when the
 * next ge limit refresh for this an item will be.
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
	@Getter
	private List<OfferInfo> standardizedOffers = new ArrayList<>();

	@Getter
	private Instant nextGeLimitRefresh;

	//the number of items bought since the last ge limit reset.
	@Getter
	private int itemsBoughtThisLimitWindow;


	/**
	 * This method takes in every new offer that comes and updates the standardized offer list along with
	 * other properties related to the history of an item such as how many items were bought since the last
	 * ge limit refresh and how when the ge limit will reset again.
	 *
	 * @param newOffer the OfferInfo object created from the {@link GrandExchangeOfferChanged} event that
	 *                 onGrandExchangeOfferChanged (in FlippingPlugin) receives
	 */
	public void updateHistory(OfferInfo newOffer)
	{
		storeStandardizedOffer(newOffer);
		updateGeProperties();

	}

	/**
	 * Receives an offer, turns it into a standardized offer, and adds it to the standardized offer list.
	 * Standardizing an offer refers to making it reflect the quantity bought/sold since last offer rather
	 * than the current amount bought/sold overall in the trade as is the default information in the OfferInfo
	 * constructed from a grandExchangeOfferChanged event.
	 *
	 * @param newOffer the OfferInfo object created from the {@link GrandExchangeOfferChanged} event that
	 *                 onGrandExchangeOfferChanged (in FlippingPlugin) receives. It is crucial to note that
	 *                 This OfferInfo object contains the current quantity bought/sold for the trade currently.
	 */
	private void storeStandardizedOffer(OfferInfo newOffer)
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

			//if the offer is complete, clear the history for that slot.
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

	/**
	 * Updates when the ge limit will refresh and how many items have been bought since the last
	 * ge limit refresh.
	 */
	private void updateGeProperties()
	{
		OfferInfo mostRecentOffer = standardizedOffers.get(standardizedOffers.size() - 1);
		if (!mostRecentOffer.isBuy())
		{
			return;
		}
		// when the time of the last offer (most recent offer) is greater than nextGeLimitRefresh,
		// you know the ge limits have refreshed. Since this is the first offer after the ge limits
		// have refreshed, the next refresh will be four hours after this offer's buy time.
		if (nextGeLimitRefresh == null || mostRecentOffer.getTime().compareTo(nextGeLimitRefresh) > 0)
		{
			nextGeLimitRefresh = mostRecentOffer.getTime().plus(4, ChronoUnit.HOURS);
			itemsBoughtThisLimitWindow = mostRecentOffer.getQuantity();
		}
		//if the last offer (most recent offer) is before the next ge limit refresh, add its quantity to the
		//amount bought this limit window.
		else
		{
			itemsBoughtThisLimitWindow += mostRecentOffer.getQuantity();
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
	private int getValueOfTrades(List<OfferInfo> tradeList, int itemLimit)
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

	/**
	 * This is to prevent old values from remaining for items that a user has bought and whose
	 * refresh times have already passed. If the user buys the item again, the values will be up to date,
	 * so this method wouldn't be needed, but there is no guarantee the user buys the item again after the
	 * limit refreshes. This method should be called periodically to ensure no old values will remain.
	 */
	public void validateGeProperties()
	{
		if (Instant.now().compareTo(nextGeLimitRefresh) >= 0 && !(nextGeLimitRefresh==null))
		{
			nextGeLimitRefresh = null;
			itemsBoughtThisLimitWindow = 0;
		}
	}

}
