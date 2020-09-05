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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.runelite.api.GrandExchangeOfferState;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class HistoryManagerTest
{
	private static HistoryManager historyManager;
	private static Instant baseTime = Instant.now();


	@Before
	public void setUp()
	{
		List<OfferEvent> offerEvents = new ArrayList<>();

		//overall bought 24+3+20=47
		//overall sold 7 + 3 + 30 = 40
		//5gp profit each

		offerEvents.add(Utils.offer(true, 7, 100, baseTime.minus(40, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BUYING, 24));
		offerEvents.add(Utils.offer(true, 13, 100, baseTime.minus(30, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BUYING, 24));
		offerEvents.add(Utils.offer(true, 24, 100, baseTime.minus(20, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT, 24));

		offerEvents.add(Utils.offer(false, 7, 105, baseTime.minus(15, ChronoUnit.MINUTES), 3, GrandExchangeOfferState.SOLD, 7));
		offerEvents.add(Utils.offer(false, 3, 105, baseTime.minus(12, ChronoUnit.MINUTES), 4, GrandExchangeOfferState.SELLING, 5));
		offerEvents.add(Utils.offer(false, 3, 105, baseTime.minus(12, ChronoUnit.MINUTES), 4, GrandExchangeOfferState.CANCELLED_SELL, 5));

		offerEvents.add(Utils.offer(true, 3, 100, baseTime.minus(10, ChronoUnit.MINUTES), 2, GrandExchangeOfferState.BOUGHT, 3));
		offerEvents.add(Utils.offer(true, 10, 100, baseTime.minus(9, ChronoUnit.MINUTES), 2, GrandExchangeOfferState.BUYING, 20));
		offerEvents.add(Utils.offer(true, 20, 100, baseTime.minus(7, ChronoUnit.MINUTES), 2, GrandExchangeOfferState.BOUGHT, 20));

		offerEvents.add(Utils.offer(false, 10, 105, baseTime.minus(6, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SELLING, 30));
		offerEvents.add(Utils.offer(false, 20, 105, baseTime.minus(5, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SELLING, 30));
		offerEvents.add(Utils.offer(false, 30, 105, baseTime.minus(4, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 30));

		historyManager = new HistoryManager();
		for (OfferEvent offerEvent : offerEvents)
		{
			historyManager.updateHistory(offerEvent);
		}
	}

	/**
	 * Tests that updating the history manager standardizes the offers correctly, truncates them appropriately, and
	 * manages state such as ge properties correctly.
	 */
	@Test
	public void historyManagerCorrectlyUpdatedTest()
	{
		List<OfferEvent> recordedOffers = new ArrayList<>();

		recordedOffers.add(Utils.offer(true, 24, 100, baseTime.minus(20, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT, 24));
		recordedOffers.add(Utils.offer(false, 7, 105, baseTime.minus(15, ChronoUnit.MINUTES), 3, GrandExchangeOfferState.SOLD, 7));
		recordedOffers.add(Utils.offer(false, 3, 105, baseTime.minus(12, ChronoUnit.MINUTES), 4, GrandExchangeOfferState.CANCELLED_SELL, 5));
		recordedOffers.add(Utils.offer(true, 3, 100, baseTime.minus(10, ChronoUnit.MINUTES), 2, GrandExchangeOfferState.BOUGHT, 3));
		recordedOffers.add(Utils.offer(true, 20, 100, baseTime.minus(7, ChronoUnit.MINUTES), 2, GrandExchangeOfferState.BOUGHT, 20));
		recordedOffers.add(Utils.offer(false, 30, 105, baseTime.minus(4, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 30));

		assertEquals(recordedOffers, historyManager.getCompressedOfferEvents());

		assertEquals(47, historyManager.getItemsBoughtThisLimitWindow());

		//first buy was 40 mins before baseTime, so ge refresh should be at after 3 hours and 20 minutes which is 200 minutes
		assertEquals(baseTime.plus(200, ChronoUnit.MINUTES), historyManager.getNextGeLimitRefresh());
	}

	@Test
	public void getProfitCorrectnessTest()
	{
		List<OfferEvent> tradesList;
		tradesList = historyManager.getIntervalsHistory(baseTime.minus(1, ChronoUnit.HOURS));
		assertEquals(200, historyManager.currentProfit(tradesList));

		//sell 5 more of the item
		historyManager.updateHistory(Utils.offer(false, 5, 105, baseTime.minus(4, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 5, 0));

		tradesList = historyManager.getIntervalsHistory(baseTime.minus(1, ChronoUnit.HOURS));
		assertEquals(225, historyManager.currentProfit(tradesList)); //47 buys and 45 sells, so looks for 45 items and profit is 5 gp ea.

		//when no trades are present given the interval
		tradesList = historyManager.getIntervalsHistory(baseTime);
		assertEquals(0, historyManager.currentProfit(tradesList));
	}

	@Test
	public void gePropertiesCorrectnessTest()
	{
		HistoryManager historyManager = new HistoryManager();

		OfferEvent offer1 = Utils.offer(true, 7, 100, baseTime.minus(4, ChronoUnit.HOURS), 1, GrandExchangeOfferState.BUYING, 10, 0);

		//buy 7 of an item 4 hours ago
		historyManager.updateHistory(offer1);
		assertEquals(7, historyManager.getItemsBoughtThisLimitWindow());
		assertEquals(offer1.getTime().plus(4, ChronoUnit.HOURS), historyManager.getNextGeLimitRefresh());

		//buy another 3 of that item 3 hours ago, so the amount you bought before the ge limit has refreshed is now 10
		OfferEvent offer2 = Utils.offer(true, 10, 100, baseTime.minus(3, ChronoUnit.HOURS), 1, GrandExchangeOfferState.BOUGHT, 10, 0);
		historyManager.updateHistory(offer2);
		assertEquals(10, historyManager.getItemsBoughtThisLimitWindow());
		assertEquals(offer1.getTime().plus(4, ChronoUnit.HOURS), historyManager.getNextGeLimitRefresh());

		//buy another 1 of that item, but 1 minute in the future, so more than 4 hours from the first purchase of the item. By this time, the ge limit has reset
		//so the amount you bought after the last ge refresh is 1.
		OfferEvent offer3 = Utils.offer(true, 1, 100, baseTime.plus(1, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BUYING, 2, 0);
		historyManager.updateHistory(offer3);
		assertEquals(1, historyManager.getItemsBoughtThisLimitWindow());
		assertEquals(offer3.getTime().plus(4, ChronoUnit.HOURS), historyManager.getNextGeLimitRefresh());
	}

	@Test
	public void testCreateFlips()
	{
		//in setup we defined a bunch of offers. The offers signify the following: The user bought 24 of that item, then
		//sold 7 of that item, then sold 3 of that item, then bought 3 of that item, then bought 20 of that item, then
		//sold 30 of that item. So, get flips should return flips that represent all of those complete transaction.
		List<Flip> generatedFlips;

		ArrayList<Flip> flips = new ArrayList<>();
		flips.add(new Flip(100, 105, 7, baseTime.minus(15, ChronoUnit.MINUTES), false, false));
		flips.add(new Flip(100, 105, 3, baseTime.minus(12, ChronoUnit.MINUTES), false, false));
		flips.add(new Flip(100, 105, 30, baseTime.minus(4, ChronoUnit.MINUTES), false,false));
		generatedFlips = historyManager.createFlips(historyManager.getCompressedOfferEvents());

		assertEquals(flips, generatedFlips);

		//now lets add some margin checks in there!!!!!!!
		OfferEvent marginBuy = Utils.offer(true, 1, 105, baseTime.minus(3, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT, 1, 0, 1);
		OfferEvent marginSell = Utils.offer(false, 1, 100, baseTime.minus(3, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 1, 0, 1);

		historyManager.updateHistory(marginBuy);
		historyManager.updateHistory(marginSell);
		generatedFlips = historyManager.createFlips(historyManager.getCompressedOfferEvents());

		generatedFlips.sort(Comparator.comparing(Flip::getTime));

		//add the flip generated by the margin check
		flips.add(new Flip(105, 100, 1, baseTime.minus(3, ChronoUnit.MINUTES), true, false));
		assertEquals(flips, generatedFlips);
	}


	//tests that flips are correctly generated even when there are an uneven amount of margins checks. The
	//unpaired margin check should be paired with the a regular non margin check offer at a time close to it.
	@Test
	public void createFlipsUnEvenMarginChecks()
	{
		HistoryManager historyManager = new HistoryManager();
		List<OfferEvent> standardizedOffers = new ArrayList<>();
		List<Flip> flips = new ArrayList<>();

		//add a buy margin check and a sell margin check
		standardizedOffers.add(Utils.offer(true, 1, 2, baseTime.minus(10, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT, 1, 1, 1));
		standardizedOffers.add(Utils.offer(false, 1, 1, baseTime.minus(10, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 1, 1, 1));

		standardizedOffers.add(Utils.offer(false, 1, 2, baseTime.minus(9, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 1, 1, 100));
		standardizedOffers.add(Utils.offer(true, 10, 1, baseTime.minus(8, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT, 10, 10, 100));

		standardizedOffers.add(Utils.offer(false, 1, 2, baseTime.minus(7, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 1, 1, 100));

		standardizedOffers.add(Utils.offer(false, 1, 2, baseTime.minus(7, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.CANCELLED_SELL, 6, 1, 100));

		//some random buy margin check, for example this can be the case when a user just wants to instabuy something and see if its
		//sell price has changed
		standardizedOffers.add(Utils.offer(true, 1, 2, baseTime.minus(6, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT, 1, 1, 1));

		standardizedOffers.add(Utils.offer(false, 8, 2, baseTime.minus(5, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 8, 8));

		flips.add(new Flip(2, 1, 1, baseTime.minus(10, ChronoUnit.MINUTES), true, false));
		flips.add(new Flip(1, 2, 1, baseTime.minus(9, ChronoUnit.MINUTES), false, false));
		flips.add(new Flip(1, 2, 1, baseTime.minus(7, ChronoUnit.MINUTES), false, false));
		flips.add(new Flip(1, 2, 1, baseTime.minus(7, ChronoUnit.MINUTES), false, false));
		flips.add(new Flip(1, 2, 8, baseTime.minus(5, ChronoUnit.MINUTES), false, false));


		List<Flip> calculatedFlips = historyManager.createFlips(standardizedOffers);

		assertEquals(flips, calculatedFlips);
	}

	//Tests pairing margin checks when you have intermediate "half margin checks" that shouldn't
	//be matched with another offer. For example, a random insta buy that was never followed by a insta sell
	//at a reasonable time.
	@Test
	public void pairUnevenMarginChecksTest()
	{
		List<Flip> flips = new ArrayList<>();
		List<OfferEvent> expectedRemainder = new ArrayList<>();

		List<OfferEvent> buyMarginChecks = new ArrayList<>();
		List<OfferEvent> sellMarginChecks = new ArrayList<>();
		List<OfferEvent> remainder = new ArrayList<>();
		//initial buy margin check
		buyMarginChecks.add(Utils.offer(true, 1, 2, baseTime.minus(10, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT, 1, 1, 1));
		//sell margin check
		sellMarginChecks.add(Utils.offer(false, 1, 1, baseTime.minus(10, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 1, 1, 1));

		//random half margin check if user is just checking out optimal sell price
		buyMarginChecks.add(Utils.offer(true, 1, 3, baseTime.minus(8, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT, 1, 1, 1));

		//another buy margin check
		buyMarginChecks.add(Utils.offer(true, 1, 2, baseTime.minus(6, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT, 1, 1, 1));
		//accompanied by sell margin check
		sellMarginChecks.add(Utils.offer(false, 1, 1, baseTime.minus(6, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 1, 1, 1));

		//some random half margin check to check optimal buy price
		sellMarginChecks.add(Utils.offer(false, 1, 1, baseTime.minus(5, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 1, 1, 1));


		flips.add(new Flip(2, 1, 1, baseTime.minus(10, ChronoUnit.MINUTES), true, false));
		flips.add(new Flip(2, 1, 1, baseTime.minus(6, ChronoUnit.MINUTES), true, false));


		assertEquals(flips, historyManager.pairMarginChecks(buyMarginChecks, sellMarginChecks, remainder));

		//add both the half margin checks that should be unpaired
		expectedRemainder.add(Utils.offer(true, 1, 3, baseTime.minus(8, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT, 1, 1, 1));
		expectedRemainder.add(Utils.offer(false, 1, 1, baseTime.minus(5, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 1, 1, 1));

		assertEquals(expectedRemainder, remainder);
	}

	//this test checks that flips are created correctly when there are not only uneven margin checks
	//but there are half margin checks mixed in. As such, this will functionally also test that unpaired
	//margin checks get matched to the most appropriate offer.
	@Test
	public void createFlipsUnevenAndIntermediateMarginChecks()
	{
		List<OfferEvent> offers = new ArrayList<>();

		List<Flip> expectedFlips = new ArrayList<>();

		//a full margin check (a buy margin check followed by a sell margin check)
		offers.add(Utils.offer(true, 1, 2, baseTime.minus(20, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT, 1, 1, 1));
		offers.add(Utils.offer(false, 1, 1, baseTime.minus(20, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 1, 1, 1));

		//some random offers
		offers.add(Utils.offer(false, 1, 2, baseTime.minus(19, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 1, 10));
		offers.add(Utils.offer(true, 5, 1, baseTime.minus(17, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT, 5, 5));

		//half margin check to see optimal sell price
		offers.add(Utils.offer(true, 1, 3, baseTime.minus(17, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT, 1, 1, 1));

		offers.add(Utils.offer(false, 5, 3, baseTime.minus(15, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 5, 5));

		//you start flipping it again so u do a full margin check
		//a full margin check (a buy margin check followed by a sell margin check)
		offers.add(Utils.offer(true, 1, 7, baseTime.minus(14, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT, 1, 1, 1));
		offers.add(Utils.offer(false, 1, 4, baseTime.minus(14, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 1, 1, 1));

		offers.add(Utils.offer(true, 5, 4, baseTime.minus(12, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT, 5, 5));

		//half margin check to see optimal sell price
		offers.add(Utils.offer(true, 1, 8, baseTime.minus(12, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT, 1, 1, 1));

		offers.add(Utils.offer(false, 3, 8, baseTime.minus(11, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 3, 3));

		//half margin check to see optimal sell price
		offers.add(Utils.offer(true, 1, 8, baseTime.minus(12, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT, 1, 1, 1));

		offers.add(Utils.offer(false, 3, 8, baseTime.minus(10, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 3, 3));

		//you think about buying it again for more, so you insta sell your last one to see optimal buy price
		offers.add(Utils.offer(false, 1, 3, baseTime.minus(10, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 1, 1));

		expectedFlips.add(new Flip(2, 1, 1, baseTime.minus(20, ChronoUnit.MINUTES), true, false));
		expectedFlips.add(new Flip(1, 2, 1, baseTime.minus(19, ChronoUnit.MINUTES), false, false));
		expectedFlips.add(new Flip(1, 3, 5, baseTime.minus(15, ChronoUnit.MINUTES), false, false));
		expectedFlips.add(new Flip(7, 4, 1, baseTime.minus(14, ChronoUnit.MINUTES), true, false));
		expectedFlips.add(new Flip(4, 8, 3, baseTime.minus(11, ChronoUnit.MINUTES), false, false));
		expectedFlips.add(new Flip(5, 8, 3, baseTime.minus(10, ChronoUnit.MINUTES), false, false));
		expectedFlips.add(new Flip(8, 3, 1, baseTime.minus(10, ChronoUnit.MINUTES), false, false));

		List<Flip> generatedFlips = historyManager.createFlips(offers);
		generatedFlips.sort(Comparator.comparing(Flip::getTime));

		assertEquals(expectedFlips, generatedFlips);
	}

	//tests that un unpaired margin check is paired with an appropriate offer, this was also covered in the
	//last test, but this is testing only that.
	@Test
	public void unpairedMarginCheckPairedWithAppropriateOffer()
	{
		List<Flip> expectedFlips = new ArrayList<>();

		List<OfferEvent> offers = new ArrayList<>();

		//a full margin check (a buy margin check followed by a sell margin check)
		offers.add(Utils.offer(true, 1, 2, baseTime.minus(20, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT, 1, 1, 1));
		offers.add(Utils.offer(false, 1, 1, baseTime.minus(20, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 1, 1, 1));

		//some random offers
		offers.add(Utils.offer(false, 1, 2, baseTime.minus(19, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 1, 1));
		offers.add(Utils.offer(true, 5, 1, baseTime.minus(17, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT, 5, 5));
		offers.add(Utils.offer(false, 4, 2, baseTime.minus(16, ChronoUnit.MINUTES),1, GrandExchangeOfferState.SOLD,4,4));


		//half margin check that should be paired with the next sell offer
		offers.add(Utils.offer(true, 1, 20, baseTime.minus(10, ChronoUnit.MINUTES),1, GrandExchangeOfferState.BOUGHT, 1,1));

		offers.add(Utils.offer(true, 5, 1, baseTime.minus(9, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT, 5, 5));

		offers.add(Utils.offer(false, 1, 2, baseTime.minus(9, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 1, 1));

		expectedFlips.add(new Flip(2,1,1,baseTime.minus(20, ChronoUnit.MINUTES),true, false));
		expectedFlips.add(new Flip(1,2,1,baseTime.minus(19, ChronoUnit.MINUTES),false, false));
		expectedFlips.add(new Flip(1,2,4,baseTime.minus(16,ChronoUnit.MINUTES),false, false));
		expectedFlips.add(new Flip(20,2,1, baseTime.minus(9, ChronoUnit.MINUTES), false, false));

		List<Flip> generatedFlips = historyManager.createFlips(offers);
		generatedFlips.sort(Comparator.comparing(Flip::getTime));

		assertEquals(expectedFlips, generatedFlips);
	}

	@Test
	public void offersCorrectlyTruncatedTest()
	{
		HistoryManager historyManager = new HistoryManager();

		ArrayList<OfferEvent> expectedCompressedEvents = new ArrayList<>();

		//test truncation on incomplete offers
		historyManager.updateHistory(Utils.offer(true, 10, 100, baseTime, 1, GrandExchangeOfferState.BUYING, 50));
		historyManager.updateHistory(Utils.offer(true, 5, 100, baseTime, 2, GrandExchangeOfferState.BUYING, 1));
		historyManager.updateHistory(Utils.offer(true, 30, 100, baseTime, 1, GrandExchangeOfferState.BUYING, 50));

		expectedCompressedEvents.add(Utils.offer(true, 5, 100, baseTime, 2, GrandExchangeOfferState.BUYING, 1));
		expectedCompressedEvents.add(Utils.offer(true, 30, 100, baseTime, 1, GrandExchangeOfferState.BUYING, 50));

		assertEquals(historyManager.getCompressedOfferEvents(), expectedCompressedEvents);

		//now lets add a completed offer for slot 1.
		historyManager.updateHistory(Utils.offer(true, 50, 100, baseTime, 1, GrandExchangeOfferState.BOUGHT, 50));

		//rebuild the compressed offers list
		expectedCompressedEvents.clear();
		expectedCompressedEvents.add(Utils.offer(true, 5, 100, baseTime, 2, GrandExchangeOfferState.BUYING, 1));
		expectedCompressedEvents.add(Utils.offer(true, 50, 100, baseTime, 1, GrandExchangeOfferState.BOUGHT, 50));

		historyManager.deletePreviousOffersForTrade(Utils.offer(true, 50, 100, baseTime, 1, GrandExchangeOfferState.BOUGHT, 50));
		assertEquals(historyManager.getCompressedOfferEvents(), expectedCompressedEvents);

		//lets add a complete offer for slot 3, this offer has no previous offers in that same trade
		historyManager.updateHistory(Utils.offer(false, 20, 100, baseTime, 3, GrandExchangeOfferState.SOLD, 20 ));

		expectedCompressedEvents.add(Utils.offer(false, 20, 100, baseTime, 3, GrandExchangeOfferState.SOLD, 20));

		assertEquals(historyManager.getCompressedOfferEvents(), expectedCompressedEvents);
	}
}
