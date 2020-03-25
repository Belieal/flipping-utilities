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
import net.runelite.http.api.ge.GrandExchangeTrade;

@Slf4j
@AllArgsConstructor
public class FlippingItem
{
	private static int GE_RESET_TIME_SECONDS = 60 * 60 * 4;

	@Getter
	private ArrayList<GrandExchangeTrade> tradeHistory;

	@Getter
	private final int itemId;

	@Getter
	private final String itemName;

	@Getter
	private final int totalGELimit;

	@Getter
	private int remainingGELimit;

	@Getter
	@Setter
	private int latestBuyPrice;

	@Getter
	@Setter
	private int latestSellPrice;

	@Getter
	@Setter
	private Instant latestBuyTime;

	@Getter
	@Setter
	private Instant latestSellTime;

	@Getter
	private Instant geLimitResetTime;

	public void addTradeHistory(final GrandExchangeTrade trade)
	{
		tradeHistory.add(trade);
	}

	public void updateGELimitReset()
	{
		if (tradeHistory != null)
		{
			GrandExchangeTrade oldestTrade = null;
			remainingGELimit = totalGELimit;

			//Check for the oldest trade within the last 4 hours.
			for (GrandExchangeTrade trade : tradeHistory)
			{
				if (trade.isBuy() && trade.getTime().getEpochSecond() >= Instant.now().minusSeconds(GE_RESET_TIME_SECONDS).getEpochSecond())
				{
					//Check if trade is older than oldest trade.
					if (oldestTrade == null || oldestTrade.getTime().getEpochSecond() > trade.getTime().getEpochSecond())
					{
						oldestTrade = trade;
					}
					remainingGELimit -= trade.getQuantity();
				}
			}

			//No buy trade found in the last 4 hours.
			if (oldestTrade == null)
			{
				geLimitResetTime = null;
			}
			else
			{
				geLimitResetTime = oldestTrade.getTime().plusSeconds(GE_RESET_TIME_SECONDS);
			}

		}
		else
		{
			//No previous trade history; assume no trades made.
			geLimitResetTime = null;
			remainingGELimit = totalGELimit;
		}
	}
}
