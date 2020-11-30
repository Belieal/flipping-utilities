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
		plugin.getAccountCache().get("testacc").setSlotTimers(plugin.setupSlotTimers());
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
		offerEvents.add(Utils.offer(true,0, 0, baseTime, 2, GrandExchangeOfferState.BUYING, 10, 0,3));
		offerEvents.add(Utils.offer(true,0, 0, baseTime, 2, GrandExchangeOfferState.BUYING, 11, 0,3));

		//offer completes and user gets a redundant BUYING offer event and then a BOUGHT offer event saying that the offer is complete a tick later
		offerEvents.add(Utils.offer(true, 3, 100, baseTime, 2, GrandExchangeOfferState.BUYING, 20, 0,3));
		offerEvents.add(Utils.offer(true, 3, 100, baseTime, 2, GrandExchangeOfferState.BOUGHT, 21, 0,3));

		//user places another trade and receives two start of trade events again
		offerEvents.add(Utils.offer(true,0, 0, baseTime, 2, GrandExchangeOfferState.BUYING, 40, 0,1));
		offerEvents.add(Utils.offer(true,0, 0, baseTime, 2, GrandExchangeOfferState.BUYING, 41, 0,1));

		//as the item buys, user receives multiple duplicate BUYING offer events, the only difference being that one arrived a tick later
		offerEvents.add(Utils.offer(true, 2, 100, baseTime, 2, GrandExchangeOfferState.BUYING, 45, 0, 10));
		offerEvents.add(Utils.offer(true, 2, 100, baseTime, 2, GrandExchangeOfferState.BUYING, 46, 0, 10));

		offerEvents.add(Utils.offer(true, 5, 100, baseTime, 2, GrandExchangeOfferState.BUYING, 50, 0, 10));
		offerEvents.add(Utils.offer(true, 5, 100, baseTime, 2, GrandExchangeOfferState.BUYING, 51, 0, 10));

		//offer completes and user gets a redundant BUYING offer and then a BOUGHT offer event a tick later
		offerEvents.add(Utils.offer(true, 10, 100, baseTime, 2, GrandExchangeOfferState.BUYING, 55, 0,10));
		offerEvents.add(Utils.offer(true, 10, 100, baseTime, 2, GrandExchangeOfferState.BOUGHT, 56, 0,10));

		//These are copied and pasted from the offers above that should have passed the screening with one difference which is that the tickSinceFirstOffer is changed to what it should be.
		List<OfferEvent> expectedOfferEvents = new ArrayList<>();
		expectedOfferEvents.add(Utils.offer(true, 3, 100, baseTime, 2, GrandExchangeOfferState.BOUGHT, 21, 11,3));
		expectedOfferEvents.add(Utils.offer(true, 2, 100, baseTime, 2, GrandExchangeOfferState.BUYING, 45, 5, 10));
		expectedOfferEvents.add(Utils.offer(true, 5, 100, baseTime, 2, GrandExchangeOfferState.BUYING, 50, 10, 10));
		expectedOfferEvents.add(Utils.offer(true, 10, 100, baseTime, 2, GrandExchangeOfferState.BOUGHT, 56, 16,10));

		List<OfferEvent> actualScreenedOffers = offerEvents.stream().map(plugin::screenOfferEvent).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());

		assertEquals(expectedOfferEvents, actualScreenedOffers);
	}
}