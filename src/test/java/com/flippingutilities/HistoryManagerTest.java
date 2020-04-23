package com.flippingutilities;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.GrandExchangeOfferState;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;


public class HistoryManagerTest
{
	private static HistoryManager historyManager;
	private static Instant baseTime = Instant.now();

	@BeforeClass
	public static void setUp()
	{
		List<OfferInfo> offers = new ArrayList<>();
		//overall bought 24+3+20=47
		//overall sold 7 + 3 + 30 = 40
		//5gp profit each
		offers.add(new OfferInfo(true, 0, 7, 100, baseTime.minus(40, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BUYING, 0, 0, 0, 0));
		offers.add(new OfferInfo(true, 0, 13, 100, baseTime.minus(30, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BUYING, 0, 0, 0, 0));
		offers.add(new OfferInfo(true, 0, 24, 100, baseTime.minus(20, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT, 0, 0, 0, 0));

		offers.add(new OfferInfo(false, 0, 7, 105, baseTime.minus(15, ChronoUnit.MINUTES), 3, GrandExchangeOfferState.SOLD, 0, 0, 0, 0));
		offers.add(new OfferInfo(false, 0, 3, 105, baseTime.minus(12, ChronoUnit.MINUTES), 4, GrandExchangeOfferState.SELLING, 0, 0, 0, 0));
		offers.add(new OfferInfo(false, 0, 3, 105, baseTime.minus(12, ChronoUnit.MINUTES), 4, GrandExchangeOfferState.CANCELLED_SELL, 0, 0, 0, 0));

		offers.add(new OfferInfo(true, 0, 3, 100, baseTime.minus(10, ChronoUnit.MINUTES), 2, GrandExchangeOfferState.BOUGHT, 0, 0, 0, 0));
		offers.add(new OfferInfo(true, 0, 10, 100, baseTime.minus(9, ChronoUnit.MINUTES), 2, GrandExchangeOfferState.BUYING, 0, 0, 0, 0));
		offers.add(new OfferInfo(true, 0, 20, 100, baseTime.minus(7, ChronoUnit.MINUTES), 2, GrandExchangeOfferState.BOUGHT, 0, 0, 0, 0));


		offers.add(new OfferInfo(false, 0, 10, 105, baseTime.minus(6, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SELLING, 0, 0, 0, 0));
		offers.add(new OfferInfo(false, 0, 20, 105, baseTime.minus(5, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SELLING, 0, 0, 0, 0));
		offers.add(new OfferInfo(false, 0, 30, 105, baseTime.minus(4, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 0, 0, 0, 0));

		historyManager = new HistoryManager();
		for (OfferInfo offer : offers)
		{
			historyManager.updateHistory(offer);
		}
	}

	@Test
	public void offersAreCorrectlyStandardizedTest()
	{
		List<OfferInfo> standardizedOffers = new ArrayList<>();
		standardizedOffers.add(new OfferInfo(true, 0, 7, 100, baseTime.minus(40, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BUYING, 0, 0, 0, 7));
		standardizedOffers.add(new OfferInfo(true, 0, 13, 100, baseTime.minus(30, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BUYING, 0, 0, 0, 6));
		standardizedOffers.add(new OfferInfo(true, 0, 24, 100, baseTime.minus(20, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT, 0, 0, 0, 11));

		standardizedOffers.add(new OfferInfo(false, 0, 7, 105, baseTime.minus(15, ChronoUnit.MINUTES), 3, GrandExchangeOfferState.SOLD, 0, 0, 0, 7));
		standardizedOffers.add(new OfferInfo(false, 0, 3, 105, baseTime.minus(12, ChronoUnit.MINUTES), 4, GrandExchangeOfferState.SELLING, 0, 0, 0, 3));
		standardizedOffers.add(new OfferInfo(false, 0, 3, 105, baseTime.minus(12, ChronoUnit.MINUTES), 4, GrandExchangeOfferState.CANCELLED_SELL, 0, 0, 0, 0));

		standardizedOffers.add(new OfferInfo(true, 0, 3, 100, baseTime.minus(10, ChronoUnit.MINUTES), 2, GrandExchangeOfferState.BOUGHT, 0, 0, 0, 3));
		standardizedOffers.add(new OfferInfo(true, 0, 10, 100, baseTime.minus(9, ChronoUnit.MINUTES), 2, GrandExchangeOfferState.BUYING, 0, 0, 0, 10));
		standardizedOffers.add(new OfferInfo(true, 0, 20, 100, baseTime.minus(7, ChronoUnit.MINUTES), 2, GrandExchangeOfferState.BOUGHT, 0, 0, 0, 10));


		standardizedOffers.add(new OfferInfo(false, 0, 10, 105, baseTime.minus(6, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SELLING, 0, 0, 0, 10));
		standardizedOffers.add(new OfferInfo(false, 0, 20, 105, baseTime.minus(5, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SELLING, 0, 0, 0, 10));
		standardizedOffers.add(new OfferInfo(false, 0, 30, 105, baseTime.minus(4, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 0, 0, 0, 10));


		assertEquals(standardizedOffers, historyManager.getStandardizedOffers());
	}

	@Test
	public void getProfitCorrectnessTest()
	{
		List<OfferInfo> tradesList;
		tradesList = historyManager.getIntervalsHistory(baseTime.minus(1, ChronoUnit.HOURS));
		assertEquals(200, historyManager.currentProfit(tradesList));

		historyManager.updateHistory(new OfferInfo(false, 0, 5, 105, baseTime.minus(4, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 0, 0, 0, 0));

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

		OfferInfo offer1 = new OfferInfo(true, 0, 7, 100, baseTime.minus(4, ChronoUnit.HOURS), 1, GrandExchangeOfferState.BUYING, 0, 0, 0, 0);

		//buy 7 of an item 4 hours ago
		historyManager.updateHistory(offer1);
		assertEquals(7, historyManager.getItemsBoughtThisLimitWindow());
		assertEquals(offer1.getTime().plus(4, ChronoUnit.HOURS), historyManager.getNextGeLimitRefresh());

		//buy another 3 of that item 3 hours ago, so the amount you bought before the ge limit has refreshed is now 10
		OfferInfo offer2 = new OfferInfo(true, 0, 10, 100, baseTime.minus(3, ChronoUnit.HOURS), 1, GrandExchangeOfferState.BOUGHT, 0, 0, 0, 0);
		historyManager.updateHistory(offer2);
		assertEquals(10, historyManager.getItemsBoughtThisLimitWindow());
		assertEquals(offer1.getTime().plus(4, ChronoUnit.HOURS), historyManager.getNextGeLimitRefresh());

		//buy another 1 of that item, but 1 minute in the future, so more than 4 hours from the first purchase of the item. By this time, the ge limit has reset
		//so the amount you bought after the last ge refresh is 1.
		OfferInfo offer3 = new OfferInfo(true, 0, 1, 100, baseTime.plus(1, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BUYING, 0, 0, 0, 0);
		historyManager.updateHistory(offer3);
		assertEquals(1, historyManager.getItemsBoughtThisLimitWindow());
		assertEquals(offer3.getTime().plus(4, ChronoUnit.HOURS), historyManager.getNextGeLimitRefresh());
	}

	@Test
	public void testGetFlips()
	{
		//in setup we defined a bunch of offers. The offers signify the following: The user bought 24 of that item, then
		//sold 7 of that item, then sold 3 of that item, then bought 3 of that item, then bought 20 of that item, then
		//sold 30 of that item. So, get flips should return flips that represent all of those complete transaction.
		ArrayList<Flip> generatedFlips;

		ArrayList<Flip> flips = new ArrayList<>();
		flips.add(new Flip(100, 105, 30, baseTime.minus(4, ChronoUnit.MINUTES), false));
		flips.add(new Flip(100, 105, 3, baseTime.minus(12, ChronoUnit.MINUTES), false));
		flips.add(new Flip(100, 105, 7, baseTime.minus(15, ChronoUnit.MINUTES), false));
		generatedFlips = historyManager.getFlips(baseTime.minus(1, ChronoUnit.HOURS));

		assertEquals(generatedFlips, flips);

		//now lets add some margin checks in there!!!!!!!

		OfferInfo marginBuy = new OfferInfo(true, 0, 1, 105, baseTime.minus(3, ChronoUnit.MINUTES),
			1, GrandExchangeOfferState.BOUGHT, 0, 0, 1, 0);
		OfferInfo marginSell = new OfferInfo(false, 0, 1, 100, baseTime.minus(3, ChronoUnit.MINUTES),
			1, GrandExchangeOfferState.SOLD, 0, 0, 1, 0);

		historyManager.updateHistory(marginBuy);
		historyManager.updateHistory(marginSell);

		generatedFlips = historyManager.getFlips(baseTime.minus(1, ChronoUnit.HOURS));

		//add the flip generated by the margin check
		flips.add(0, new Flip(105, 100, 1, baseTime.minus(3, ChronoUnit.MINUTES), marginSell.isMarginCheck()));
		assertEquals(generatedFlips, flips);
	}

}
