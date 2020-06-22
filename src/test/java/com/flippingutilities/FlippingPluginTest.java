package com.flippingutilities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.runelite.api.GrandExchangeOfferState;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;


public class FlippingPluginTest
{
	FlippingPlugin plugin;

	@Before
	public void preparePluginForTests()
	{
		plugin = new FlippingPlugin();
		plugin.setCurrentlyLoggedInAccount("testacc");
		plugin.setAccountCache(Collections.singletonMap("testacc", new AccountData()));
		plugin.setSlotTimers(plugin.setupSlotTimers());
	}

	/**
	 * This test will replicate the offer events that come in during a typical interaction with the GE by a user
	 */
	@Test
	public void screenOfferTest()
	{

		Instant baseTime = Instant.now();
		List<OfferEvent> offerEvents = new ArrayList<>();
		//some empty slot events on login
		offerEvents.add(Utils.offer(false,0,0,baseTime,5,GrandExchangeOfferState.EMPTY,0,0,0));
		offerEvents.add(Utils.offer(false,0,0,baseTime,6,GrandExchangeOfferState.EMPTY,0,0,0));
		offerEvents.add(Utils.offer(false,0,0,baseTime,7,GrandExchangeOfferState.EMPTY,0,0,0));

		//user sets an offer and receives start of trade event and then also a duplicate start of trade event on the next tick
		offerEvents.add(Utils.offer(true,0, 0, baseTime, 2, GrandExchangeOfferState.BUYING, 10, 0,3,0));
		offerEvents.add(Utils.offer(true,0, 0, baseTime, 2, GrandExchangeOfferState.BUYING, 11, 0,3,0));

		//offer completes and user gets a redundant BUYING offer event and then a BOUGHT offer event saying that the offer is complete a tick later
		offerEvents.add(Utils.offer(true, 3, 100, baseTime, 2, GrandExchangeOfferState.BUYING, 20, 0,3, 0));
		offerEvents.add(Utils.offer(true, 3, 100, baseTime, 2, GrandExchangeOfferState.BOUGHT, 21, 0,3, 0));

		//user places another trade and receives two start of trade events again
		offerEvents.add(Utils.offer(true,0, 0, baseTime, 2, GrandExchangeOfferState.BUYING, 40, 0,1,0));
		offerEvents.add(Utils.offer(true,0, 0, baseTime, 2, GrandExchangeOfferState.BUYING, 41, 0,1,0));

		//as the item buys, user received multiple duplicate BUYING offer events
		offerEvents.add(Utils.offer(true, 2, 100, baseTime, 2, GrandExchangeOfferState.BUYING, 45, 0, 10, 0));
		offerEvents.add(Utils.offer(true, 2, 100, baseTime, 2, GrandExchangeOfferState.BUYING, 46, 0, 10, 0));

		offerEvents.add(Utils.offer(true, 5, 100, baseTime, 2, GrandExchangeOfferState.BUYING, 50, 0, 10, 0));
		offerEvents.add(Utils.offer(true, 5, 100, baseTime, 2, GrandExchangeOfferState.BUYING, 51, 0, 10, 0));

		//offer completes and user gets a redundant BUYING offer and then a BOUGHT offer event a tick later
		offerEvents.add(Utils.offer(true, 10, 100, baseTime, 2, GrandExchangeOfferState.BUYING, 55, 0,10, 0));
		offerEvents.add(Utils.offer(true, 10, 100, baseTime, 2, GrandExchangeOfferState.BOUGHT, 56, 0,10, 0));

		//These are copied and pasted from the offers above that should have passed the screening with one difference which is that the tickSinceFirstOffer is changed to what it should be.
		List<OfferEvent> expectedOfferEvents = new ArrayList<>();
		expectedOfferEvents.add(Utils.offer(true, 3, 100, baseTime, 2, GrandExchangeOfferState.BOUGHT, 21, 11,3, 0));
		expectedOfferEvents.add(Utils.offer(true, 2, 100, baseTime, 2, GrandExchangeOfferState.BUYING, 45, 5, 10, 0));
		expectedOfferEvents.add(Utils.offer(true, 5, 100, baseTime, 2, GrandExchangeOfferState.BUYING, 50, 10, 10, 0));
		expectedOfferEvents.add(Utils.offer(true, 10, 100, baseTime, 2, GrandExchangeOfferState.BOUGHT, 56, 16,10, 0));

		List<OfferEvent> actualScreenedOffers = offerEvents.stream().map(plugin::screenOfferEvent).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());

		assertEquals(expectedOfferEvents, actualScreenedOffers);
	}


}


//
//caused by empty slot: OfferEvent(buy=false, itemId=0, currentQuantityInTrade=0, price=0, time=2020-06-21T23:17:02Z, slot=5, state=EMPTY, tickArrivedAt=0, ticksSinceFirstOffer=0, totalQuantityInTrade=0, quantitySinceLastOffer=0, validStatOffer=true, validFlippingOffer=true, madeBy=firerune116)
//caused by empty slot: OfferEvent(buy=false, itemId=0, currentQuantityInTrade=0, price=0, time=2020-06-21T23:17:02Z, slot=6, state=EMPTY, tickArrivedAt=0, ticksSinceFirstOffer=0, totalQuantityInTrade=0, quantitySinceLastOffer=0, validStatOffer=true, validFlippingOffer=true, madeBy=firerune116)
//caused by empty slot: OfferEvent(buy=false, itemId=0, currentQuantityInTrade=0, price=0, time=2020-06-21T23:17:02Z, slot=7, state=EMPTY, tickArrivedAt=0, ticksSinceFirstOffer=0, totalQuantityInTrade=0, quantitySinceLastOffer=0, validStatOffer=true, validFlippingOffer=true, madeBy=firerune116)
//not duplicate start of trade event: OfferEvent(buy=true, itemId=379, currentQuantityInTrade=0, price=0, time=2020-06-21T23:17:38Z, slot=2, state=BUYING, tickArrivedAt=59, ticksSinceFirstOffer=0, totalQuantityInTrade=1, quantitySinceLastOffer=0, validStatOffer=true, validFlippingOffer=true, madeBy=firerune116)
//redundant offer event: OfferEvent(buy=true, itemId=379, currentQuantityInTrade=1, price=212, time=2020-06-21T23:17:38Z, slot=2, state=BUYING, tickArrivedAt=60, ticksSinceFirstOffer=0, totalQuantityInTrade=1, quantitySinceLastOffer=0, validStatOffer=true, validFlippingOffer=true, madeBy=firerune116)
//good event, passing through: OfferEvent(buy=true, itemId=379, currentQuantityInTrade=1, price=212, time=2020-06-21T23:17:39Z, slot=2, state=BOUGHT, tickArrivedAt=61, ticksSinceFirstOffer=0, totalQuantityInTrade=1, quantitySinceLastOffer=0, validStatOffer=true, validFlippingOffer=true, madeBy=firerune116)
//caused by empty slot: OfferEvent(buy=false, itemId=0, currentQuantityInTrade=0, price=0, time=2020-06-21T23:17:43Z, slot=2, state=EMPTY, tickArrivedAt=68, ticksSinceFirstOffer=0, totalQuantityInTrade=0, quantitySinceLastOffer=0, validStatOffer=true, validFlippingOffer=true, madeBy=firerune116)
//not duplicate start of trade event: OfferEvent(buy=true, itemId=379, currentQuantityInTrade=0, price=0, time=2020-06-21T23:18:18Z, slot=2, state=BUYING, tickArrivedAt=125, ticksSinceFirstOffer=0, totalQuantityInTrade=1, quantitySinceLastOffer=0, validStatOffer=true, validFlippingOffer=true, madeBy=firerune116)
//duplicate start of trade event: OfferEvent(buy=true, itemId=379, currentQuantityInTrade=0, price=0, time=2020-06-21T23:18:18Z, slot=2, state=BUYING, tickArrivedAt=126, ticksSinceFirstOffer=0, totalQuantityInTrade=1, quantitySinceLastOffer=0, validStatOffer=true, validFlippingOffer=true, madeBy=firerune116)
//good event, passing through: OfferEvent(buy=true, itemId=379, currentQuantityInTrade=0, price=0, time=2020-06-21T23:18:22Z, slot=2, state=CANCELLED_BUY, tickArrivedAt=133, ticksSinceFirstOffer=0, totalQuantityInTrade=1, quantitySinceLastOffer=0, validStatOffer=true, validFlippingOffer=true, madeBy=firerune116)
//Duplicate event: OfferEvent(buy=true, itemId=379, currentQuantityInTrade=0, price=0, time=2020-06-21T23:18:23Z, slot=2, state=CANCELLED_BUY, tickArrivedAt=134, ticksSinceFirstOffer=0, totalQuantityInTrade=1, quantitySinceLastOffer=0, validStatOffer=true, validFlippingOffer=true, madeBy=firerune116)
//caused by empty slot: OfferEvent(buy=false, itemId=0, currentQuantityInTrade=0, price=0, time=2020-06-21T23:18:25Z, slot=2, state=EMPTY, tickArrivedAt=138, ticksSinceFirstOffer=0, totalQuantityInTrade=0, quantitySinceLastOffer=0, validStatOffer=true, validFlippingOffer=true, madeBy=firerune116)
