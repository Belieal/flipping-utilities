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
	@SerializedName("id")
	@Getter
	private final int itemId;

	@SerializedName("name")
	@Getter
	private final String itemName;

	@SerializedName("tGL")
	@Getter
	@Setter
	private int totalGELimit;

	@SerializedName("mCBP")
	@Getter
	private int marginCheckBuyPrice;

	@SerializedName("mCSP")
	@Getter
	private int marginCheckSellPrice;

	@SerializedName("mCBT")
	@Getter
	private Instant marginCheckBuyTime;

	@SerializedName("mCST")
	@Getter
	private Instant marginCheckSellTime;

	@SerializedName("lBT")
	@Getter
	private Instant latestBuyTime;

	@SerializedName("lST")
	@Getter
	private Instant latestSellTime;

	//An activity is described as a completed offer event.
	@SerializedName("lAT")
	@Getter
	@Setter
	private Instant latestActivityTime;

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

	@Getter
	@Setter
	private transient Boolean expand;

	public FlippingItem(int itemId, String itemName, int totalGeLimit, String flippedBy)
	{
		this.itemId = itemId;
		this.itemName = itemName;
		this.totalGELimit = totalGeLimit;
		this.flippedBy = flippedBy;
	}

	//utility for cloning an instant...
	private Instant ci(Instant i)
	{
		if (i == null)
		{
			return null;
		}
		return Instant.ofEpochMilli(i.toEpochMilli());
	}

	public FlippingItem clone()
	{
		return new FlippingItem(itemId, itemName, totalGELimit, marginCheckBuyPrice, marginCheckSellPrice,
			ci(marginCheckBuyTime), ci(marginCheckSellTime), ci(latestBuyTime), ci(latestSellTime), ci(latestActivityTime),
			history.clone(), flippedBy, validFlippingPanelItem, favorite, expand);
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
	 * Updates the latest buy/sell times of an item. This will be used to display an overlay on
	 * GE slots to show whether an item is active or not.
	 *
	 * @param newOffer new offer just received
	 */
	public void updateLatestTimes(OfferEvent newOffer)
	{
		if (newOffer.isBuy())
		{
			latestBuyTime = newOffer.getTime();
		}
		else
		{
			latestSellTime = newOffer.getTime();
		}

		latestActivityTime = newOffer.getTime();
	}

	/**
	 * This method is used to update the margin of an item. As such it is only invoked when an offer is a
	 * margin check. It is invoked by FlippingPlugin's updateFlippingItem method in the plugin class which itself is only
	 * invoked when an offer is a margin check.
	 *
	 * @param newOffer the new offer just received.
	 */
	public void updateMargin(OfferEvent newOffer)
	{
		int tradePrice = newOffer.getPrice();
		Instant tradeTime = newOffer.getTime();

		if (newOffer.isBuy())
		{
			marginCheckSellPrice = tradePrice;
			marginCheckSellTime = tradeTime;
		}
		else
		{
			marginCheckBuyPrice = tradePrice;
			marginCheckBuyTime = tradeTime;
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
			marginCheckSellPrice = 0;
			marginCheckSellTime = null;
			marginCheckBuyPrice = 0;
			marginCheckBuyTime = null;
		}
	}

	//generated to string from intellij. I made it not create a representation of the history cause it would be too
	//long and you typically don't want to see that.
	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder("FlippingItem{");
		sb.append("itemId=").append(itemId);
		sb.append(", itemName='").append(itemName).append('\'');
		sb.append(", totalGELimit=").append(totalGELimit);
		sb.append(", marginCheckBuyPrice=").append(marginCheckBuyPrice);
		sb.append(", marginCheckSellPrice=").append(marginCheckSellPrice);
		sb.append(", marginCheckBuyTime=").append(marginCheckBuyTime);
		sb.append(", marginCheckSellTime=").append(marginCheckSellTime);
		sb.append(", latestBuyTime=").append(latestBuyTime);
		sb.append(", latestSellTime=").append(latestSellTime);
		sb.append(", latestActivityTime=").append(latestActivityTime);
		sb.append(", madeBy='").append(flippedBy).append('\'');
		sb.append('}');
		return sb.toString();
	}

	public int getPotentialProfit(boolean includeMarginCheck, boolean currentGeLimit)
	{
		int profitEach = marginCheckSellPrice - marginCheckBuyPrice;
		if (remainingGeLimit() == 0)
		{
			return 0;
		}
		int geLimit = currentGeLimit ? remainingGeLimit() : totalGELimit;
		int profitTotal = geLimit * profitEach;
		if (includeMarginCheck)
		{
			profitTotal -= profitEach;
		}
		return profitTotal;
	}

	public List<OfferEvent> getOfferMatches(OfferEvent offerEvent, int limit)
	{
		return history.getOfferMatches(offerEvent, limit);
	}
}
