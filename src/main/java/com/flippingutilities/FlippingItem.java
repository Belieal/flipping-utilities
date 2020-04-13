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

import com.flippingutilities.ui.flipping.FlippingItemPanel;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is the representation of an item that a user is flipping. It contains information about the
 * margin of the item (buying and selling price), the latest buy and sell times, and the history of the item
 * which is all of the offers that make up the trade history of that item. This history is managed by the
 * {@link HistoryManager} and is used to get the profits for this item, how many more of it you can buy
 * until the ge limit refreshes, and when the next ge limit refreshes.
 * <p>
 * This class is the model behind a {@link FlippingItemPanel} as its data is used to create the contents
 * of a panel which is then displayed.
 */
@Slf4j
public class FlippingItem
{
	@Getter
	private final int itemId;

	@Getter
	private final String itemName;

	@Getter
	private final int totalGELimit;

	@Getter
	private int latestBuyPrice;

	@Getter
	private int latestSellPrice;

	@Getter
	private Instant latestBuyTime;

	@Getter
	private Instant latestSellTime;

	@Getter
	@Setter
	private boolean isFrozen = false;

	@Getter
	private boolean sellPriceNeedsUpdate = true;

	@Getter
	private boolean buyPriceNeedsUpdate = true;

	private HistoryManager history = new HistoryManager();


	public FlippingItem(int itemId, String itemName, int totalGeLimit)
	{
		this.itemId = itemId;
		this.itemName = itemName;
		this.totalGELimit = totalGeLimit;
	}


	public void updateHistory(OfferInfo newOffer)
	{
		history.updateHistory(newOffer);
	}

	public long currentProfit(Instant earliestTime)
	{
		return history.currentProfit(earliestTime);
	}

	public long getTotalExpenses()
	{
		return history.getTotalExpenses();
	}

	public long getTotalRevenues()
	{
		return history.getTotalRevenues();
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

	/**
	 * This method is used to update the margin of an item. As such it is only invoked when an offer is a
	 * margin check. It is invoked by updateFlippingItem in the plugin class which itself is only
	 * invoked when an offer is a margin check.
	 *
	 * @param newOffer the new offer just received.
	 */
	public void updateMargin(OfferInfo newOffer)
	{
		boolean tradeBuyState = newOffer.isBuy();
		int tradePrice = newOffer.getPrice();
		Instant tradeTime = newOffer.getTime();

		if (!(isFrozen))
		{
			if (tradeBuyState)
			{
				latestSellPrice = tradePrice;
				latestSellTime = tradeTime;
				sellPriceNeedsUpdate = false;
			}
			else
			{
				latestBuyPrice = tradePrice;
				latestBuyTime = tradeTime;
				buyPriceNeedsUpdate = false;
			}
		}

	}

	/**
	 * This Method is responsible for freezing an item's margin. When an item is to be frozen, buyPriceNeedsUpdate and
	 * sellPriceNeeds update are set to true, and isFrozen is set to false. isFrozen is set to false so that
	 * updateMargin will update the margins and so that the components that rely on a FlippingItem can easily
	 * see that it is frozen. BuyPriceNeedsUpdate and sellPriceNeedsUpdate are set to true, so that in
	 * {@link FlippingPlugin#updateFlippingItem(FlippingItem, OfferInfo)} when an item is being updated, the margin
	 * is only frozen again if BOTH the sell price and buy price are updated.
	 *
	 * @param freeze whether the item should have it's margin frozen or not
	 */
	public void freezeMargin(boolean freeze)
	{
		if (freeze)
		{
			isFrozen = true;
			buyPriceNeedsUpdate = false;
			sellPriceNeedsUpdate = false;
		}
		else
		{
			isFrozen = false;
			buyPriceNeedsUpdate = true;
			sellPriceNeedsUpdate = true;
		}
	}
}
