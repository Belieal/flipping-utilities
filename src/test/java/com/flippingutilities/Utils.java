package com.flippingutilities;

import com.flippingutilities.model.OfferEvent;
import net.runelite.api.GrandExchangeOfferState;

import java.time.Instant;

public class Utils
{
	//constructs an OfferEvent when you don't care about tick specific info.
	public static OfferEvent offer(boolean isBuy, int currentQuantityInTrade, int price, Instant time, int slot, GrandExchangeOfferState state, int totalQuantityInTrade)
	{
		return new OfferEvent(isBuy, 1, currentQuantityInTrade, price, time, slot, state, 0, 10, totalQuantityInTrade, true, "gooby", false, null);
	}

	public static OfferEvent offer(boolean isBuy, int currentQuantityInTrade, int price, Instant time, int slot, GrandExchangeOfferState state, int totalQuantityInTrade, int tickSinceFirstOffer)
	{
		return new OfferEvent(isBuy, 1, currentQuantityInTrade, price, time, slot, state, 0, tickSinceFirstOffer, totalQuantityInTrade, true, "gooby", false, null);
	}

	public static OfferEvent offer(boolean isBuy, int currentQuantityInTrade, int price, Instant time, int slot, GrandExchangeOfferState state, int tickArrivedAt, int tickSinceFirstOffer, int totalQuantityInTrade) {
		return new OfferEvent(isBuy, 1, currentQuantityInTrade, price, time, slot, state, tickArrivedAt, tickSinceFirstOffer, totalQuantityInTrade, true, "gooby", false, null);
	}
}
