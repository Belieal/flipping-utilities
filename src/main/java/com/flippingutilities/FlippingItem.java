package com.flippingutilities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.http.api.ge.GrandExchangeTrade;

import java.time.Instant;
import java.util.ArrayList;

@AllArgsConstructor
public class FlippingItem
{

	/* This is to be used in a future trade history / statistics feature */
	@Getter
	private ArrayList<GrandExchangeTrade> tradeHistory;

	@Getter
	private final int itemId;

	@Getter
	private final String itemName;

	@Getter
	private final int totalGELimit;

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

	public void addTradeHistory(final GrandExchangeTrade trade)
	{
		tradeHistory.add(trade);
	}

}
