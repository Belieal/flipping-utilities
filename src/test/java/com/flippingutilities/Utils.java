package com.flippingutilities;

import java.time.Instant;
import net.runelite.api.GrandExchangeOfferState;

public class Utils
{
	public static OfferEvent offer(boolean isBuy, int currentQuantityInTrade, int price, Instant time, int slot, GrandExchangeOfferState state, int totalQuantityInTrade, int quantitySinceLastOffer)
	{
		return new OfferEvent(isBuy, 1, currentQuantityInTrade, price, time, slot, state, 0, 10, totalQuantityInTrade, quantitySinceLastOffer, true, true, "gooby");
	}

	public static OfferEvent offer(boolean isBuy, int currentQuantityInTrade, int price, Instant time, int slot, GrandExchangeOfferState state, int totalQuantityInTrade, int quantitySinceLastOffer, int tickSinceFirstOffer)
	{
		return new OfferEvent(isBuy, 1, currentQuantityInTrade, price, time, slot, state, 0, tickSinceFirstOffer, totalQuantityInTrade, quantitySinceLastOffer, true, true, "gooby");
	}

	public static OfferEvent offer(boolean isBuy, int currentQuantityInTrade, int price, Instant time, int slot, GrandExchangeOfferState state, int tickArrivedAt, int tickSinceFirstOffer, int totalQuantityInTrade, int quantitySinceLastOffer) {
		return new OfferEvent(isBuy, 1, currentQuantityInTrade, price, time, slot, state, tickArrivedAt, tickSinceFirstOffer, totalQuantityInTrade, quantitySinceLastOffer, true, true, "gooby");
	}
}
