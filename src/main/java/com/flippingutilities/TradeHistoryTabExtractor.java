package com.flippingutilities;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.runelite.api.widgets.Widget;

/**
 * Extracts data from the widgets in the trade history tab so that the TradeHistoryTabPanel can display them. I wanted
 * the TradeHistoryTabPanel, and all UI components, to pretty much only draw stuff and not do much else, hence the separation.
 */
public class TradeHistoryTabExtractor
{
//	public static List<OfferEvent> convertWidgetsToOfferEvents(Widget[] widgets) {
//		//a group of 6 widgets makes up an offer in the trade history tab
//		List<List<Widget>> groupsOfWidgets = ModelUtilities.splitListIntoChunks(Arrays.asList(widgets),6);
//		return groupsOfWidgets.stream().map(w -> createOfferEventFromWidgetGroup(w)).collect(Collectors.toList());
//	}

//	public static OfferEvent createOfferEventFromWidgetGroup(List<Widget> widgets) {
//		//set slot to -1 or something so i can ignore it in history manager
//		OfferEvent offerEvent = new OfferEvent()
//		//state
//		//how many
//		//price
//		//
//	}
}
