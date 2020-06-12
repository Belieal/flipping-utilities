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
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.runelite.api.events.GrandExchangeOfferChanged;

/**
 * Manages the history for an item. This class is responsible for figuring out how much profit a user made for
 * an item along with tracking how many items they bought since the last ge limit refresh and when the
 * next ge limit refresh for this an item will be.
 */
@AllArgsConstructor
@NoArgsConstructor
public class HistoryManager
{
	//contains the history for each slot so that when a new offer comes in for a slot, we can use the
	//slot history to figure out how many new items were bought/sold. When a offer with a state that is
	//complete (bought/sold/cancelled buy/cancelled sell) comes in, the history for that slot is removed
	//as the slot is now empty.
	@SerializedName("sH")
	private Map<Integer, List<OfferInfo>> slotHistory = new HashMap<>();

	//a list of standardizedOffers. A standardizedOffer is an offer with a currentQuantityInTrade that represents the
	//currentQuantityInTrade bought since the last offer. A regular offer just has info from an offerEvent, which gives
	//you the current currentQuantityInTrade bought/sold overall in the trade.
	@SerializedName("sO")
	@Getter
	@Setter
	private List<OfferInfo> standardizedOffers = new ArrayList<>();

	@SerializedName("nGLR")
	@Getter
	private Instant nextGeLimitRefresh;

	//the number of items bought since the last ge limit reset.
	@SerializedName("iBTLW")
	@Getter
	private int itemsBoughtThisLimitWindow;

	public enum PanelSelection
	{
		FLIPPING,
		STATS,
		BOTH
	}

	public HistoryManager clone()
	{
		Map<Integer, List<OfferInfo>> newSlotHistory = new HashMap<>();
		for (int i : slotHistory.keySet())
		{
			newSlotHistory.put(i, clone(slotHistory.get(i)));
		}

		List<OfferInfo> newStandardizedOffers = clone(standardizedOffers);
		Instant newGeLimitRefresh = nextGeLimitRefresh == null ? null : Instant.ofEpochMilli(nextGeLimitRefresh.toEpochMilli());

		return new HistoryManager(newSlotHistory, newStandardizedOffers, newGeLimitRefresh, itemsBoughtThisLimitWindow);
	}

	//a utility to clone an offer list
	private List<OfferInfo> clone(List<OfferInfo> offers)
	{
		return offers.stream().map(OfferInfo::clone).collect(Collectors.toList());
	}

	/**
	 * This method takes in every new offer that comes and updates the standardized offer list along with
	 * other properties related to the history of an item such as how many items were bought since the last
	 * ge limit refresh and how when the ge limit will reset again. The standardized offer list is used to
	 * calculate profit for the item.
	 *
	 * @param newOffer the OfferInfo object created from the {@link GrandExchangeOfferChanged} event that
	 *                 onGrandExchangeOfferChanged (in FlippingPlugin) receives
	 */
	public void updateHistory(OfferInfo newOffer)
	{
		storeStandardizedOffer(newOffer);
		updateGeProperties(standardizedOffers.get(standardizedOffers.size() - 1));
		truncateOffers(standardizedOffers);
	}

	/**
	 * Receives an offer, turns it into a standardized offer, and adds it to the standardized offer list.
	 * Standardizing an offer refers to making it reflect the currentQuantityInTrade bought/sold since last offer rather
	 * than the current amount bought/sold overall in the trade as is the default information in the OfferInfo
	 * constructed from a grandExchangeOfferChanged event.
	 *
	 * @param newOffer the OfferInfo object created from the {@link GrandExchangeOfferChanged} event that
	 *                 onGrandExchangeOfferChanged (in FlippingPlugin) receives. It is crucial to note that
	 *                 This OfferInfo object contains the current currentQuantityInTrade bought/sold for the trade currently.
	 */
	public void storeStandardizedOffer(OfferInfo newOffer)
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
			//don't need to standardize as its currentQuantityInTrade represents the currentQuantityInTrade bought as its the first
			//trade in that slot.
			newOffer.setQuantitySinceLastOffer(newOffer.getCurrentQuantityInTrade());
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
	private void updateGeProperties(OfferInfo mostRecentOffer)
	{
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
			itemsBoughtThisLimitWindow = mostRecentOffer.getQuantitySinceLastOffer();
		}
		//if the last offer (most recent offer) is before the next ge limit refresh, add its currentQuantityInTrade to the
		//amount bought this limit window.
		else
		{
			itemsBoughtThisLimitWindow += mostRecentOffer.getQuantitySinceLastOffer();
		}

	}

	public void truncateOffers(List<OfferInfo> offers)
	{
		OfferInfo mostRecentOffer = offers.get(offers.size() - 1);

		//do not go through the process of truncation if the offer is the only offer in that trade as it will
		//have no past offers to truncate. Also don't start truncation if the offer is not complete
		if (mostRecentOffer.getQuantitySinceLastOffer() == mostRecentOffer.getCurrentQuantityInTrade() || !mostRecentOffer.isComplete())
		{
			return;
		}

		//getting the profit still relies on "quantitySinceLastOffer" and since we are deleting all
		//"last offers" for a trade, we have to set the "quantitySinceLastOffer" equal to the amount
		//bought/sold in the entire trade to get accurate profit results.
		mostRecentOffer.setQuantitySinceLastOffer(mostRecentOffer.getCurrentQuantityInTrade());

		//size is minus 2 to get the second to last item in the list
		for (int i = offers.size() - 2; i > -1; i--)
		{
			OfferInfo aPreviousOffer = offers.get(i);
			if (aPreviousOffer.getSlot() == mostRecentOffer.getSlot() && aPreviousOffer.isBuy() == mostRecentOffer.isBuy())
			{
				//if it belongs to the same slot and its complete, it must belong to a previous trade given that
				//the most recent offer was for the same slot and was also complete.
				if (aPreviousOffer.isComplete())
				{
					return;
				}

				else
				{
					offers.remove(i);
				}
			}
		}
	}

	/**
	 * Gets the latest trade update by the slot index parameter and buy state.
	 * Can return null if tradeList doesn't contain any offers that contain the slotIndex or buyState.
	 *
	 * @param tradeList List of offers to get the latest trade time from
	 * @param slotIndex Slot the trade needs to have been traded from
	 * @param buyState  The state of the offers to get
	 * @return Returns an instant of the latest trade update by slot, or null if no trades were found.
	 */

	public Instant getLatestTradeUpdateBySlot(List<OfferInfo> tradeList, int slotIndex, boolean buyState, boolean completedOffer)
	{
		Collections.reverse(tradeList);

		OfferInfo result = tradeList.stream()
			//Check that trade states, slotIndices and completed parameters match.
			.filter(item -> (item.isBuy() == buyState) && (item.getSlot() == slotIndex) && (!completedOffer || item.isComplete()))
			.findFirst()
			.orElse(null);


		if (result == null)
		{
			return null;
		}

		return result.getTime();
	}

	/**
	 * Calculates profit for a list of trades made with this item by counting the expenses and revenues
	 * accrued over these trades and figuring out the difference in value.
	 *
	 * @param tradeList The list of trades whose total profits will be calculated.
	 * @return profit
	 */
	public long currentProfit(List<OfferInfo> tradeList)
	{
		//return the value of the sell list - the value of the buy list. This is the profit.
		return getCashflow(tradeList, false) - getCashflow(tradeList, true);
	}

	/**
	 * This method finds the value of a list of offers. The boolean parameter determines if we calculate
	 * from buyList or sellList.
	 *
	 * @param tradeList  The list of standardized offers whose cashflow we want the value of.
	 * @param getExpense Options parameter that calculates, if true, the total expenses accrued
	 *                   and, if false, the total revenues accrued from the trades.
	 * @return Returns a long value based on the boolean parameter provided.
	 */
	public long getCashflow(List<OfferInfo> tradeList, boolean getExpense)
	{
		return getValueOfTrades(getSaleList(tradeList, getExpense), countItemsFlipped(tradeList));
	}

	/**
	 * Gets the currentQuantityInTrade of flipped items that has been done in a list of offers.
	 * The currentQuantityInTrade flipped is determined by the lowest of either number of items bought or sold.
	 *
	 * @param tradeList The list of items that the item count is based on
	 * @return An integer representing the total currentQuantityInTrade of items flipped in the list of offers
	 */
	public int countItemsFlipped(List<OfferInfo> tradeList)
	{
		int numBoughtItems = 0;
		int numSoldItems = 0;

		for (OfferInfo offer : tradeList)
		{
			if (!offer.isValidStatOffer())
			{
				continue;
			}

			if (offer.isBuy())
			{
				numBoughtItems += offer.getQuantitySinceLastOffer();
			}
			else
			{
				numSoldItems += offer.getQuantitySinceLastOffer();
			}
		}

		return Math.min(numBoughtItems, numSoldItems);
	}

	/**
	 * Gets the list of trades of either buy or sell states from a list of trades.
	 *
	 * @param tradeList The list of trades that will be checked.
	 * @param buyState  true will return offers that have been bought and false will return offers that have been sold.
	 * @return A list of items either sold or bought over a period of time.
	 */
	public ArrayList<OfferInfo> getSaleList(List<OfferInfo> tradeList, boolean buyState)
	{
		ArrayList<OfferInfo> results = new ArrayList<>();

		for (OfferInfo offer : tradeList)
		{
			if (offer.isBuy() == buyState && offer.isValidStatOffer())
			{
				results.add(offer);
			}
		}

		return results;
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
	private long getValueOfTrades(List<OfferInfo> tradeList, int itemLimit)
	{
		int itemsSeen = 0;
		long moneySpent = 0;


		for (OfferInfo offer : tradeList)
		{
			if (!offer.isValidStatOffer())
			{
				continue;
			}

			if (itemsSeen + offer.getQuantitySinceLastOffer() >= itemLimit)
			{
				moneySpent += (itemLimit - itemsSeen) * offer.getPrice();
				break;
			}
			else
			{
				moneySpent += offer.getQuantitySinceLastOffer() * offer.getPrice();
				itemsSeen += offer.getQuantitySinceLastOffer();
			}

		}

		return moneySpent;
	}

	/**
	 * Returns the history of the item that were traded between earliestTime and now.
	 *
	 * @param earliestTime the earliest time that trades from the trade history are added to the resulting list.
	 * @return A list of offers that were within the interval of earliestTime and now.
	 */
	public ArrayList<OfferInfo> getIntervalsHistory(Instant earliestTime)
	{
		ArrayList<OfferInfo> result = new ArrayList<>();

		for (OfferInfo offer : standardizedOffers)
		{
			if (offer.getTime().isAfter(earliestTime) && offer.isValidStatOffer())
			{
				result.add(offer);
			}
		}

		return result;
	}

	/**
	 * This is to prevent old values from remaining for items that a user has bought and whose
	 * refresh times have already passed. If the user buys the item again, the values will be up to date,
	 * so this method wouldn't be needed, but there is no guarantee the user buys the item again after the
	 * limit refreshes. This method should be called periodically to ensure no old values will remain.
	 */
	public void validateGeProperties()
	{
		if (nextGeLimitRefresh == null)
		{
			return;
		}

		if (Instant.now().compareTo(nextGeLimitRefresh) >= 0)
		{
			nextGeLimitRefresh = null;
			itemsBoughtThisLimitWindow = 0;
		}
	}

	public boolean hasValidOffers(PanelSelection panelSelection)
	{
		boolean result = false;

		switch (panelSelection)
		{
			case FLIPPING:
				result = standardizedOffers.stream().anyMatch(OfferInfo::isValidFlippingOffer);
				break;

			case STATS:
				result = standardizedOffers.stream().anyMatch(OfferInfo::isValidStatOffer);
				break;

			case BOTH:
				result = standardizedOffers.stream().anyMatch(offer -> offer.isValidFlippingOffer() && offer.isValidStatOffer());
				break;
		}

		return result;
	}

	public void invalidateOffers(PanelSelection panelSelection)
	{
		invalidateOffers(panelSelection, standardizedOffers);
	}

	public void invalidateOffers(PanelSelection panelSelection, List<OfferInfo> offerList)
	{
		switch (panelSelection)
		{
			case FLIPPING:
				offerList.forEach(offer -> offer.setValidFlippingOffer(false));
				break;

			case STATS:
				offerList.forEach(offer -> offer.setValidStatOffer(false));
				break;

			case BOTH:
				offerList.forEach(offer ->
				{
					offer.setValidFlippingOffer(false);
					offer.setValidStatOffer(false);
				});
				break;
		}

		truncateInvalidOffers();
	}

	public void truncateInvalidOffers()
	{
		if (nextGeLimitRefresh == null)
		{
			standardizedOffers.removeIf(offer -> !offer.isValidFlippingOffer() && !offer.isValidStatOffer());
			return;
		}

		Instant startOfRefresh = nextGeLimitRefresh.minus(4, ChronoUnit.HOURS);

		standardizedOffers.removeIf(offer -> !offer.isValidFlippingOffer() && !offer.isValidStatOffer() &&
			(offer.getTime().isAfter(nextGeLimitRefresh) || offer.getTime().isBefore(startOfRefresh)));
	}

	/**
	 * Creates Flips from offers. Flips represent a buy trade followed by a sell trade. A trade is a collection
	 * of offers from the empty offer to the completed offer. A completed offer marks the end of a trade.
	 *
	 * @param earliestTime the time after which trades should be looked at
	 * @return flips
	 */
	public List<Flip> getFlips(Instant earliestTime)
	{
		ArrayList<OfferInfo> intervalHistory = getIntervalsHistory(earliestTime);

		//group offers based on which account those offers belong to (this is really only relevant when getting the flips
		//of the account wide tradelist as you don't want to match offers from diff accounts.
		Map<String, List<OfferInfo>> groupedOffers = intervalHistory.stream().collect(Collectors.groupingBy(OfferInfo::getMadeBy));

		//take each offer list and create flips out of them, then put those flips into one list.
		List<Flip> flips = new ArrayList<>();
		groupedOffers.values().forEach(offers -> flips.addAll(createFlips(offers)));

		flips.sort(Comparator.comparing(Flip::getTime));
		Collections.reverse(flips);

		return flips;
	}

	/**
	 * Creates flips out of a list of offers. It does this by first pairing margin check offers together and then
	 * pairing regular offers together.
	 *
	 * @param offers the offer list
	 * @return flips
	 */
	public List<Flip> createFlips(List<OfferInfo> offers)
	{
		List<OfferInfo>[] subLists = partition(
			offers.stream().map(OfferInfo::clone).collect(Collectors.toList()),
			o -> o.isMarginCheck() && o.isBuy(),
			o -> o.isMarginCheck() && !o.isBuy(),
			o -> !o.isMarginCheck() && o.isComplete() && o.isBuy(),
			o -> !o.isMarginCheck() && o.isComplete() && !o.isBuy());

		List<OfferInfo> buyMarginChecks = subLists[0];
		List<OfferInfo> sellMarginChecks = subLists[1];
		List<OfferInfo> nonMarginCheckBuys = subLists[2];
		List<OfferInfo> nonMarginCheckSells = subLists[3];

		ArrayList<Flip> flips = new ArrayList<>();

		List<OfferInfo> unPairedMarginChecks = new ArrayList<>();
		List<Flip> flipsFromMarginChecks = pairMarginChecks(buyMarginChecks, sellMarginChecks, unPairedMarginChecks);

		unPairedMarginChecks.forEach(offer ->
		{
			if (offer.isBuy())
			{
				nonMarginCheckBuys.add(offer);
			}
			else
			{
				nonMarginCheckSells.add(offer);
			}
		});

		//we sort the offers because we added the unpaired margin checks back to the offer list and it should be
		//placed in the appropriate place in the list so it doesn't get matched with an offer from many days ago or something.
		nonMarginCheckBuys.sort(Comparator.comparing(OfferInfo::getTime));
		nonMarginCheckSells.sort(Comparator.comparing(OfferInfo::getTime));

		flips.addAll(flipsFromMarginChecks);
		flips.addAll(combineToFlips(nonMarginCheckBuys, nonMarginCheckSells));

		return flips;

	}

	/**
	 * We need to pair margin check offers together because we don't want them to be paired with a regular offer in the case
	 * of an uneven quantity of items bought/sold. Pairing margin checks is tricky...A "whole" margin check is defined as a
	 * buy margin check offer followed by a sell margin check offer. However, when flipping, one often insta
	 * buys an item just to see its optimal sell price and likewise they might randomly insta sell an item to see
	 * its optimal buy price. These "half" margin checks may not be followed by a corresponding buy/sell margin check offer
	 * to make it a "whole" margin check. As such, if we are grouping margin check offers together to create flips,
	 * if a user has done some of these "half" margin checks, we have to be careful not to accidently group them with a
	 * buy/sell margin check offer that actually has its corresponding buy/sell margin check offer that makes it a whole
	 * margin check. This can result in REALLY inaccurate Flips as half margin check (lets say its a margin check buy offer)
	 * from a day before can be matched with a sell margin check offer from another day (when the margin's are totally
	 * different). And since that buy margin check offer was erroneously matched to that sell margin check offer the
	 * buy offer that was actually supposed to be matched to it, might match with some sell margin check offer that
	 * IT doesn't correspond to, etc.
	 *
	 * @param buys      a list of buy margin check offers
	 * @param sells     a list of sell margin check offers
	 * @param remainder an empty list to be populated with margin check offers that don't have companion buy/sell offers.
	 * @return a list of flips created from "whole" margin checks.
	 */
	public List<Flip> pairMarginChecks(List<OfferInfo> buys, List<OfferInfo> sells, List<OfferInfo> remainder)
	{
		List<Flip> flips = new ArrayList<>();
		int buyIdx;
		int sellIdx = 0;
		for (buyIdx = 0; buyIdx < buys.size(); buyIdx++)
		{

			if (sellIdx == sells.size())
			{
				break;
			}

			OfferInfo buy = buys.get(buyIdx);
			OfferInfo sell = sells.get(sellIdx);

			//just a subjective heuristic i am using to determine whether a buy margin check has a companion sell margin
			//check. Chances are, if there's a 1 minute difference
			long millisBetweenBuyAndSell = Duration.between(buy.getTime(), sell.getTime()).toMillis();
			if (millisBetweenBuyAndSell >= 0 && millisBetweenBuyAndSell < 60000) //60k milliseconds is a minute
			{
				flips.add(new Flip(buy.getPrice(), sell.getPrice(), sell.getCurrentQuantityInTrade(), sell.getTime(), sell.isMarginCheck()));
				sellIdx++;
			}

			//if the buy is more than 1 minute before the sell, its probably not for that sell.
			else if (millisBetweenBuyAndSell >= 0 && !(millisBetweenBuyAndSell < 60000))
			{
				remainder.add(buy);
			}

			//if the sell comes before the buy its a stand alone insta sell (a "half" margin check")
			else if (millisBetweenBuyAndSell < 0)
			{
				remainder.add(sell);
				sellIdx++;
				buyIdx--; //stay on this buy offer
			}

		}

		//if the sells were exhausted, it won't add anything as "i" will be equal to sells.size. The same applies with
		//the buys
		remainder.addAll(sells.subList(sellIdx, sells.size()));
		remainder.addAll(buys.subList(buyIdx, buys.size()));
		return flips;
	}

	/**
	 * Creates flips based on the buy and sell list. It does this by going through the sell list and the buy list
	 * and only moving onto the next sell offer when the current sell offer is exhausted (seen more items bought than it
	 * has items sold). This ensures that a flip is only created on a completed sell offer
	 *
	 * @param buys  the buy offers
	 * @param sells the sell offers
	 * @return a list of Flips based on the buy and sell list.
	 */
	private ArrayList<Flip> combineToFlips(List<OfferInfo> buys, List<OfferInfo> sells)
	{

		ArrayList<Flip> flips = new ArrayList<>();

		int buyIdx = 0;
		for (OfferInfo sell : sells)
		{
			int numBuysSeen = 0;
			int totalRevenue = 0;
			while (buyIdx < buys.size())
			{
				OfferInfo buy = buys.get(buyIdx);
				numBuysSeen += buy.getCurrentQuantityInTrade();

				if (numBuysSeen >= sell.getCurrentQuantityInTrade())
				{
					int leftOver = numBuysSeen - sell.getCurrentQuantityInTrade();
					int amountTaken = buy.getCurrentQuantityInTrade() - leftOver;
					totalRevenue += amountTaken * buy.getPrice();
					buy.setCurrentQuantityInTrade(leftOver);
					flips.add(new Flip(totalRevenue / sell.getCurrentQuantityInTrade(), sell.getPrice(), sell.getCurrentQuantityInTrade(), sell.getTime(), sell.isMarginCheck() && buy.isMarginCheck()));
					break;
				}
				else
				{
					totalRevenue += buy.getCurrentQuantityInTrade() * buy.getPrice();
					buyIdx++;
				}
			}
		}

		return flips;
	}


	/**
	 * Partition a list of items into n sublists based on n conditions passed in. Perhaps this should be a static method?
	 * The first condition puts items that meet its criteria in the first arraylist in the sublists array, the nth
	 * conditions puts the items in the nth arraylist in the sublists array.
	 *
	 * @param items      to partition into sub lists
	 * @param conditions conditions to partition on
	 * @return
	 */
	private <T> List<T>[] partition(List<T> items, Predicate<T>... conditions)
	{
		List<T>[] subLists = new ArrayList[conditions.length];

		IntStream.range(0, subLists.length).forEach(i -> subLists[i] = new ArrayList<>());

		for (T item : items)
		{
			for (int i = 0; i < conditions.length; i++)
			{
				if (conditions[i].test(item))
				{
					subLists[i].add(item);
				}
			}
		}
		return subLists;
	}
}
