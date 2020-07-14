package com.flippingutilities;

import java.time.Instant;
import net.runelite.api.GrandExchangeOfferState;

public class Utils
{
	//constructs an OfferEvent when you don't care about tick specific info.
	public static OfferEvent offer(boolean isBuy, int currentQuantityInTrade, int price, Instant time, int slot, GrandExchangeOfferState state, int totalQuantityInTrade)
	{
		return new OfferEvent(isBuy, 1, currentQuantityInTrade, price, time, slot, state, 0, 10, totalQuantityInTrade, true, true, "gooby");
	}

	public static OfferEvent offer(boolean isBuy, int currentQuantityInTrade, int price, Instant time, int slot, GrandExchangeOfferState state, int totalQuantityInTrade, int tickSinceFirstOffer)
	{
		return new OfferEvent(isBuy, 1, currentQuantityInTrade, price, time, slot, state, 0, tickSinceFirstOffer, totalQuantityInTrade, true, true, "gooby");
	}

	public static OfferEvent offer(boolean isBuy, int currentQuantityInTrade, int price, Instant time, int slot, GrandExchangeOfferState state, int tickArrivedAt, int tickSinceFirstOffer, int totalQuantityInTrade) {
		return new OfferEvent(isBuy, 1, currentQuantityInTrade, price, time, slot, state, tickArrivedAt, tickSinceFirstOffer, totalQuantityInTrade, true, true, "gooby");
	}
}
