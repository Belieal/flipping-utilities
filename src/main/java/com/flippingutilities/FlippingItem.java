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
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class FlippingItem
{
	private static int GE_RESET_TIME_SECONDS = 60 * 60 * 4;

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
	private Instant geLimitResetTime;

	@Getter
	@Setter
	private boolean isFrozen;

	private HistoryManager history;

	public void updateHistory(OfferInfo newOffer)
	{
		history.updateHistory(newOffer);
	}

	public int currentProfit(Instant earliestTime)
	{
		return history.currentProfit(earliestTime);
	}

	public int remainingGeLimit()
	{
		return totalGELimit - history.getItemsBoughtThisLimitWindow();
	}

	public void validateGeProperties() {
		history.validateGeProperties();
	}

	/**
	 * This method is used to update the margin of an item. As such it is only envoked when an offer
	 *
	 * @param newOffer the new offer just received.
	 */
	public void updateMargin(OfferInfo newOffer) {
		boolean tradeBuyState = newOffer.isBuy();
		int tradePrice = newOffer.getPrice();
		Instant tradeTime = newOffer.getTime();

		if (!isFrozen())
		{
			if (tradeBuyState)
			{
				latestSellPrice = tradePrice;
				latestSellTime = tradeTime;
			}
			else
			{
				latestBuyPrice = tradePrice;
				latestBuyTime = tradeTime;
			}
		}

	}
}
