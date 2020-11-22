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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * This class is the representation of an item that a user is flipping. It contains information about the
 * margin of the item (buying and selling price), the latest buy and sell times, and the history of the item
 * which is all of the offers that make up the trade history of that item. This history is managed by the
 * {@link HistoryManager} and is used to get the profits for this item, how many more of it you can buy
 * until the ge limit refreshes, and when the next ge limit refreshes.
 * <p>
 * This class is the model behind a FlippingItemPanel as its data is used to create the contents
 * of a panel which is then displayed.
 */
@AllArgsConstructor
public class FlippingItem
{
	@SerializedName("name")
	@Getter
	@Setter
	private String itemName;

	@SerializedName("tGL")
	@Getter
	@Setter
	private int totalGELimit;

	@SerializedName("h")
	@Getter
	@Setter
	private HistoryManager history = new HistoryManager();

	@SerializedName("fB")
	@Getter
	private String flippedBy;

	//whether the item should be on the flipping panel or not.
	@SerializedName("vFPI")
	@Getter
	@Setter
	private Boolean validFlippingPanelItem;

	@Getter
	@Setter
	private boolean favorite;

	private transient Optional<OfferEvent> latestMarginCheckBuy;

	private transient Optional<OfferEvent> latestMarginCheckSell;

	private transient Optional<OfferEvent> latestBuy;

	private transient Optional<OfferEvent> latestSell;

	@Getter
	@Setter
	private transient Boolean expand;

	public FlippingItem(String itemName, int totalGeLimit, String flippedBy)
	{
		this.itemName = itemName;
		this.totalGELimit = totalGeLimit;
		this.flippedBy = flippedBy;
	}

	public FlippingItem clone()
	{
		return new FlippingItem(
				itemName,
				totalGELimit,
				history.clone(),
				flippedBy,
				validFlippingPanelItem,
				favorite,
				getLatestMarginCheckBuy().map(o -> o.clone()),
				getLatestMarginCheckSell().map(o -> o.clone()),
				getLatestBuy().map(o -> o.clone()),
				getLatestSell().map(o -> o.clone()),
				expand);
	}

	/**
	 * This method updates the history of a FlippingItem. This history is used to calculate profits,
	 * next ge limit refresh, and how many items were bought during this limit window.
	 *
	 * @param newOffer the new offer that just came in
	 */
	public void updateHistory(OfferEvent newOffer)
	{
		history.updateHistory(newOffer);
	}

	/**
	 * Updates the latest margin check/buy/sell offers. Technically, we don't need this and we can just
	 * query the history manager, but this saves us from querying the history manager which would have
	 * to search through the offers.
	 *
	 * @param newOffer new offer just received
	 */
	public void updateLatestProperties(OfferEvent newOffer)
	{
		if (newOffer.isBuy())
		{
			if (newOffer.isMarginCheck())
			{
				latestMarginCheckBuy = Optional.of(newOffer);
			}
			latestBuy = Optional.of(newOffer);
		}
		else
		{
			if (newOffer.isMarginCheck())
			{
				latestMarginCheckSell = Optional.of(newOffer);
			}
			latestSell = Optional.of(newOffer);
		}
	}

	/**
	 * combines two flipping items together (this only make sense if they are for the same item) by adding
	 * their histories together and retaining the other properties of the latest active item.
	 *
	 * @return merged flipping item
	 */
	public static FlippingItem merge(FlippingItem item1, FlippingItem item2)
	{
		if (item1 == null)
		{
			return item2;
		}

		if (item1.getLatestActivityTime().compareTo(item2.getLatestActivityTime()) >= 0)
		{
			item1.getHistory().getCompressedOfferEvents().addAll(item2.getHistory().getCompressedOfferEvents());
			item1.setFavorite(item1.isFavorite() || item2.isFavorite());
			return item1;
		}
		else
		{
			item2.getHistory().getCompressedOfferEvents().addAll(item1.getHistory().getCompressedOfferEvents());
			item2.setFavorite(item2.isFavorite() || item1.isFavorite());
			return item2;
		}
	}

	public long currentProfit(List<OfferEvent> tradeList)
	{
		return history.currentProfit(tradeList);
	}

	public long getFlippedCashFlow(List<OfferEvent> tradeList, boolean getExpense)
	{
		return history.getFlippedCashFlow(tradeList, getExpense);
	}

	public long getFlippedCashFlow(Instant earliestTime, boolean getExpense)
	{
		return history.getFlippedCashFlow(getIntervalHistory(earliestTime), getExpense);
	}

	public long getTotalCashFlow(List<OfferEvent> tradeList, boolean getExpense)
	{
		return history.getTotalCashFlow(tradeList, getExpense);
	}

	public int countItemsFlipped(List<OfferEvent> tradeList)
	{
		return history.countItemsFlipped(tradeList);
	}

	public ArrayList<OfferEvent> getIntervalHistory(Instant earliestTime)
	{
		return history.getIntervalsHistory(earliestTime);
	}

	public int remainingGeLimit()
	{
		return totalGELimit - history.getItemsBoughtThisLimitWindow();
	}

	public Instant getGeLimitResetTime()
	{
		return history.getNextGeLimitRefresh();
	}

	public void validateGeProperties()
	{
		history.validateGeProperties();
	}

	public List<Flip> getFlips(Instant earliestTime)
	{
		return history.getFlips(earliestTime);
	}

	public boolean hasValidOffers()
	{
		return history.hasValidOffers();
	}

	public void invalidateOffers(ArrayList<OfferEvent> offerList)
	{
		history.invalidateOffers(offerList);
	}

	public void setValidFlippingPanelItem(boolean isValid)
	{
		validFlippingPanelItem = isValid;
		if (!isValid)
		{
			latestMarginCheckBuy = Optional.empty();
			latestMarginCheckSell = Optional.empty();
		}
	}

	public Optional<Integer> getPotentialProfit(boolean includeMarginCheck, boolean shouldUseRemainingGeLimit)
	{
		if (!getLatestMarginCheckBuy().isPresent() || !getLatestMarginCheckSell().isPresent()) {
			return Optional.empty();
		}

		int profitEach = getLatestMarginCheckSell().get().getPrice() - getLatestMarginCheckSell().get().getPrice();
		int remainingGeLimit = remainingGeLimit();
		int geLimit = shouldUseRemainingGeLimit ? remainingGeLimit : totalGELimit;
		int profitTotal = geLimit * profitEach;
		if (includeMarginCheck)
		{
			profitTotal -= profitEach;
		}
		return Optional.of(profitTotal);
	}

	public List<OfferEvent> getOfferMatches(OfferEvent offerEvent, int limit)
	{
		return history.getOfferMatches(offerEvent, limit);
	}

	public Instant getLatestActivityTime()
	{
		return history.getCompressedOfferEvents().get(history.getCompressedOfferEvents().size()-1).getTime();
	}

	public int getItemId() {
		return history.getCompressedOfferEvents().get(history.getCompressedOfferEvents().size()-1).getItemId();
	}

	public Optional<OfferEvent> getLatestMarginCheckSell() {
		if (latestMarginCheckSell == null) {
			latestMarginCheckSell = history.getLatestOfferThatMatchesPredicate(offer -> offer.isBuy() & offer.isMarginCheck());
		}
		return latestMarginCheckSell;
	}

	public Optional<OfferEvent> getLatestMarginCheckBuy() {
		if (latestMarginCheckBuy == null) {
			latestMarginCheckBuy = history.getLatestOfferThatMatchesPredicate(offer -> !offer.isBuy() & offer.isMarginCheck());
		}
		return latestMarginCheckBuy;
	}

	public Optional<OfferEvent> getLatestBuy() {
		if (latestBuy == null) {
			latestBuy = history.getLatestOfferThatMatchesPredicate(offer -> offer.isBuy());
		}
		return latestBuy;
	}

	public Optional<OfferEvent> getLatestSell() {
		if (latestSell == null) {
			latestSell = history.getLatestOfferThatMatchesPredicate(offer -> !offer.isBuy());
		}
		return latestSell;
	}

	public Optional<Float> getCurrentRoi() {
		return getCurrentProfitEach().isPresent() && getLatestMarginCheckBuy().isPresent()?
				Optional.of((float)getCurrentProfitEach().get() / getLatestMarginCheckBuy().get().getPrice() * 100) : Optional.empty();
	}

	public Optional<Integer> getCurrentProfitEach() {
		return getLatestMarginCheckBuy().isPresent() && getLatestMarginCheckSell().isPresent()?
				Optional.of(getLatestMarginCheckBuy().get().getPrice() - getLatestMarginCheckSell().get().getPrice()) : Optional.empty();
	}
}
