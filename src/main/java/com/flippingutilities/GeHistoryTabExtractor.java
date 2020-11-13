package com.flippingutilities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.widgets.Widget;

/**
 * Extracts data from the widgets in the trade history tab so that the TradeHistoryTabPanel can display them. I wanted
 * the TradeHistoryTabPanel, and all UI components, to pretty much only draw stuff and not do much else, hence the separation.
 */
@Slf4j
public class GeHistoryTabExtractor
{
	private static Pattern PRICE_PATTERN = Pattern.compile(">= (.*) each");
	private static FlippingPlugin plugin;

	public static List<OfferEvent> convertWidgetsToOfferEvents(Widget[] widgets)
	{
		//a group of 6 widgets makes up an offer in the trade history tab
		List<List<Widget>> groupsOfWidgets = ModelUtilities.splitListIntoChunks(Arrays.asList(widgets), 6);
		return groupsOfWidgets.stream().map(w -> createOfferEventFromWidgetGroup(w)).collect(Collectors.toList());
	}

	public static OfferEvent createOfferEventFromWidgetGroup(List<Widget> widgets)
	{
		//set slot to -1 so we can handle it appropriately in the history manager.
		int slot = -1;
		GrandExchangeOfferState offerState = getState(widgets.get(2));
		int quantity = widgets.get(4).getItemQuantity();
		int itemId = widgets.get(4).getItemId();
		int price = getPrice(widgets.get(5));
		boolean isBuy = offerState == GrandExchangeOfferState.BOUGHT;
		Instant time = Instant.now();
		int totalQuantity = quantity;
		int tickArrivedAt = -1;
		//just making ticksSinceFirst offer something > 2 so it doesn't count as a margin check
		int ticksSinceFirstOffer = 10;

		OfferEvent offerEvent = new OfferEvent(isBuy, itemId, quantity, price, time, slot, offerState, tickArrivedAt, ticksSinceFirstOffer, totalQuantity, true, null, false, null);
		return offerEvent;
	}

	private static int getPrice(Widget w)
	{
		String text = w.getText();
		String numString;
		if (text.contains("each"))
		{
			Matcher m = PRICE_PATTERN.matcher(text);
			m.find();
			numString = m.group(1);
		}
		else
		{
			numString = text.split(" coins")[0];
		}
		StringBuilder s = new StringBuilder();
		for (char c : numString.toCharArray())
		{
			if (c != ',')
			{
				s.append(c);
			}
		}
		return Integer.parseInt(s.toString());
	}

	private static GrandExchangeOfferState getState(Widget w)
	{
		String text = w.getText();
		if (text.startsWith("Bought"))
		{
			return GrandExchangeOfferState.BOUGHT;
		}
		else
		{
			return GrandExchangeOfferState.SOLD;
		}
	}

	//think about moving this to a more appropriate class. Perhaps its even time to make history a class now, instead of just
	//a list of flipping items..
	public static List<OfferEvent> findOfferMatches(OfferEvent offer, List<FlippingItem> history, int limit)
	{
		FlippingItem flippingItem = null;
		List<OfferEvent> matches = new ArrayList<>();
		for (FlippingItem item : history)
		{
			if (item.getItemId() == offer.getItemId())
			{
				flippingItem = item;
				break;
			}
		}

		int count = 0;
		if (flippingItem != null)
		{
			List<OfferEvent> itemHistory = flippingItem.getHistory().getCompressedOfferEvents();
			for (int i = itemHistory.size()-1;i > -1;i--)
			{
				OfferEvent pastOffer = itemHistory.get(i);
				if (offer.getPrice() == pastOffer.getPrice() && offer.getCurrentQuantityInTrade() == pastOffer.getCurrentQuantityInTrade()
					&& offer.getState() == pastOffer.getState())
				{
					matches.add(pastOffer);
					count ++;
					if (count == limit) {
						break;
					}
				}
			}
		}
		return matches;
	}
}
