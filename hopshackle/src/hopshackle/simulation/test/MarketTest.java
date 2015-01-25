package hopshackle.simulation.test;
import static org.junit.Assert.assertEquals;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.genetics.*;

import java.io.File;
import java.util.*;

import org.junit.*;

public class MarketTest {
	World w;
	Character expert, expert2;
	Market market;
	Character c, c2;	
	private static String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");

	@Before
	public void setUp() throws Exception {
		w = new World();
		
		SimProperties.setProperty("StartWithPotion", "true");
		CharacterClass.EXPERT.setDefaultTradeDecider(new TradeDeciderExp());
		CharacterClass.FIGHTER.setDefaultTradeDecider(new TradeDecider());
		
		expert = new Character(Race.HUMAN, CharacterClass.EXPERT, w);
		expert2 = new Character(Race.HUMAN, CharacterClass.EXPERT, w);
		expert.setDecider(new HardCodedDecider(ExpertActionsI.MAKE_ITEM));
		expert.setGenome(new Genome(new File(baseDir + "\\Genomes\\Standard\\Starter_EXP.txt")));
		Location tradeVille = new Location(w);
		expert.setLocation(tradeVille);
		new Market(new File(baseDir + "\\logs\\Market_test.txt"), w);
		// normally the market thread will be running - here it won't be
		market = w.getMarket();

		expert.addXp(500);
		expert2.addXp(500);
		expert.addGold(500);
		expert2.addGold(500);
		expert.setIntelligence(new Attribute(12));
		expert2.setIntelligence(new Attribute(14));

		c = new Character(w);
		c2 = new Character(w);
		c.addGold(500);
		c2.addGold(400);
		c.setLocation(tradeVille);
		c2.setLocation(tradeVille);
		c.setGenome(new Genome(new File(baseDir + "\\Genomes\\Standard\\Starter_FTR.txt")));
		c2.setGenome(new Genome(new File(baseDir + "\\Genomes\\Standard\\Starter_FTR.txt")));
	}

	@Test
	public void testXPChange() {
		long xpStart = expert.getXp() + expert2.getXp();
		long xpEnd = 0;

		expert.addItem(Resource.METAL);
		market.addItem(new Offer(expert,Resource.METAL, 1235));
		market.addBid(new Bid(expert2, 1246, Resource.METAL));

		market.resolveAllBids();

		xpEnd = expert.getXp() + expert2.getXp();

		assertEquals(xpEnd - xpStart, -62);
		// as 1% tax rate will be 12.4 gp, which *5 is 62xp
		xpStart = xpEnd;

		expert.addItem(Shield.TOWER_SHIELD);
		market.addItem(new Offer(expert,Shield.TOWER_SHIELD, 12.89));
		market.addBid(new Bid(expert2, 12.99, Shield.TOWER_SHIELD));

		xpEnd = expert.getXp() + expert2.getXp();

		assertEquals(xpEnd - xpStart, 0);

		expert.addItem(Resource.METAL);
		market.addItem(new Offer(expert,Resource.METAL, 0.00));
		market.addBid(new Bid(expert2, 0.00, Resource.METAL));

		market.resolveAllBids();

		xpEnd = expert.getXp() + expert2.getXp();
		assertEquals(xpEnd - xpStart, 0);
	}

	@Test
	public void testMarketTaxRate() {
		
		double startGold1 = expert.getGold();
		double startGold2 = expert2.getGold();
		expert.addItem(Resource.METAL);
		market.addItem(new Offer(expert,Resource.METAL, 1.2));
		market.addBid(new Bid(expert2, 1.2, Resource.METAL));
		market.resolveAllBids();

		assertEquals(startGold2 - expert2.getGold(), 1.2, 0.0001);
		assertEquals(startGold1 - expert.getGold(), -0.99*1.2, 0.0001);
		startGold1 = expert.getGold();
		startGold2 = expert2.getGold();
		
		expert.addItem(Resource.METAL);
		market.addItem(new Offer(expert,Resource.METAL, 0.00));
		market.addBid(new Bid(expert2, 0.00, Resource.METAL));
		market.resolveAllBids();
		
		assertEquals(startGold2 - expert2.getGold(), 0.00, 0.0001);
		assertEquals(startGold1 - expert.getGold(), 0.01, 0.0001);
		//minimum tax rate of 1 cp
		
	}

	@Test
	public void testMoreBidsThanOffersWithAllOffersFilled() {
		market.addItem(new Offer(null,Armour.CHAIN_SHIRT,25));
		c.rest(1); // bids 200 
		c2.rest(1); // bids 240
		market.resolveAllBids();

		assertEquals(market.getAveragePrice(Armour.CHAIN_SHIRT, 1), 200, 0.5);
	}
	


	@Test
	public void testOneBidWithOffersWithAllBidsFilled() {
		// Test #2: 2 items, one bid
		market.addItem(new Offer(null,Armour.CHAIN_SHIRT,25));
		market.addItem(new Offer(null,Armour.CHAIN_SHIRT,30));
		c.rest(1);
		market.resolveAllBids();

		assertEquals(market.getAveragePrice(Armour.CHAIN_SHIRT, 1), 25, 0.5);
	}

	@Test 
	public void testLessBidsThanOffersWithAllBidsFilled() {
		// Test #3: 2 bids, 3 items - market clears at 2 sold
		market.addItem(new Offer(null,Armour.CHAIN_SHIRT,25));
		market.addItem(new Offer(null,Armour.CHAIN_SHIRT,30));
		market.addItem(new Offer(null,Armour.CHAIN_SHIRT,100));
		c.rest(1);
		c2.rest(1);
		market.resolveAllBids();

		assertEquals(market.getAveragePrice(Armour.CHAIN_SHIRT, 1), 30, 0.5);
	}
	
	@Test
	public void testMinAndMaxPrices() {
		testOneBidWithOffersWithAllBidsFilled();
			// 1 Chain_shirt bought for 25
		c.removeItem(Armour.CHAIN_SHIRT);
		testLessBidsThanOffersWithAllBidsFilled();
			// 2 chain_shirts bought for 30
		
		assertEquals(market.getAveragePrice(Armour.CHAIN_SHIRT, 5), 85.0/3.0, 0.01);
		assertEquals(market.getAveragePrice(Armour.CHAIN_SHIRT, 1), 30, 0.01);
		
		assertEquals(market.getMaxPrice(Armour.CHAIN_SHIRT, 1), 30, 0.01);
		assertEquals(market.getMaxPrice(Armour.CHAIN_SHIRT, 2), 30, 0.01);
		assertEquals(market.getMaxPrice(Armour.CHAIN_SHIRT, 5), 30, 0.01);
		
		assertEquals(market.getMinPrice(Armour.CHAIN_SHIRT, 1), 30, 0.01);
		assertEquals(market.getMinPrice(Armour.CHAIN_SHIRT, 2), 25, 0.01);
		assertEquals(market.getMinPrice(Armour.CHAIN_SHIRT, 5), 25, 0.01);
		
		assertEquals(market.getVariance(Armour.CHAIN_SHIRT, 1), 0.0, 0.01);
		assertEquals(market.getVariance(Armour.CHAIN_SHIRT, 2), 5.55556, 0.01);
		assertEquals(market.getVariance(Armour.CHAIN_SHIRT, 5), 5.55556, 0.01);

	}

	@Test
	public void testEqualBidsAndOffersAndOnlyOneFilled() {
		// Test #4: 2 bids, 2 items - market clears at 1 sold
		market.addItem(new Offer(null,Armour.CHAIN_SHIRT,25));
		market.addItem(new Offer(null,Armour.CHAIN_SHIRT,300));
		c.rest(1); // bids 200
		c2.rest(1); // bids 240
		market.resolveAllBids();

		assertEquals(market.getAveragePrice(Armour.CHAIN_SHIRT, 1), 200, 0.5);
	}

	@Test
	public void testMultipleBidsAndOffersButNoSales() {
		//Test #5: No bids reach reserve price
		market.addItem(new Offer(null,Armour.CHAIN_SHIRT, 750));
		market.addItem(new Offer(null,Armour.CHAIN_SHIRT, 800));
		market.addItem(new Offer(null,Armour.CHAIN_SHIRT, 1000));
		market.addBid(new Bid(c2, 200, Armour.CHAIN_SHIRT));
		market.addBid(new Bid(c, 240, Armour.CHAIN_SHIRT));
		
		market.resolveAllBids();

		assertEquals(market.getAveragePrice(Armour.CHAIN_SHIRT, 1), 240, 0.5);
	}

	@Test
	public void testAllOffersAndNoBids() {
		// All Offers, no bids
		market.addItem(new Offer(null,Armour.CHAIN_SHIRT, 75));
		market.addItem(new Offer(null,Armour.CHAIN_SHIRT, 100));
		market.resolveAllBids();

		assertEquals(market.getAveragePrice(Armour.CHAIN_SHIRT, 1), 0, 0.5);
	}

	@Test
	public void testAllBidsAndNoOffers() {
		// All bids, no Offers
		market.addBid(new Bid(c, 40, Armour.CHAIN_SHIRT));
		market.addBid(new Bid(c2, 50, Armour.CHAIN_SHIRT));
		market.resolveAllBids();

		assertEquals(market.getAveragePrice(Armour.CHAIN_SHIRT, 1), 50, 0.5);
		
		assertEquals(market.getMaxPrice(Armour.BANDED_MAIL, 1), 0.0, 0.0001);
		assertEquals(market.getMinPrice(Armour.BANDED_MAIL, 1), 0.0, 0.0001);
	}

	@Test
	public void testPriceMetricsAfterSalesFollowedByNoActivity() {
		// One round of sales, followed by two of none
		// Then check metrics on price work
		testLessBidsThanOffersWithAllBidsFilled();
		market.addItem(new Offer(null, Armour.CHAIN_SHIRT, 200));
		market.resolveAllBids();
		market.addItem(new Offer(null, Armour.CHAIN_SHIRT, 200));
		market.resolveAllBids();

		assertEquals(market.getAveragePrice(Armour.CHAIN_SHIRT, 5), 30, 0.5);
		assertEquals(market.getAveragePrice(Armour.CHAIN_SHIRT, 1), 0, 0.5);

		assertEquals(market.getBidVolume(Armour.CHAIN_SHIRT, 5), 2, 0.5);
		assertEquals(market.getBidVolume(Armour.CHAIN_SHIRT, 1), 0, 0.5);

		assertEquals(market.getSalesVolume(Armour.CHAIN_SHIRT, 5), 2, 0.5);
		assertEquals(market.getSalesVolume(Armour.CHAIN_SHIRT, 1), 0, 0.5);
	}

	@Test 
	public void testPriceMetricsAfterNoActivityAndThenSales() {
		// One round of no sales, then two of sales
		// then check metrics on price work
		market.addBid(new Bid(c2, 200, Armour.CHAIN_SHIRT));
		market.resolveAllBids();
		market.addBid(new Bid(c, 200, Armour.CHAIN_SHIRT));
		market.resolveAllBids();

		market.addBid(new Bid(c2, 200, Armour.CHAIN_SHIRT));
		market.addItem(new Offer(null, Armour.CHAIN_SHIRT, 200));
		market.resolveAllBids();

		assertEquals(market.getAveragePrice(Armour.CHAIN_SHIRT, 5), 200, 0.5);
		assertEquals(market.getAveragePrice(Armour.CHAIN_SHIRT, 1), 200, 0.5);
		
		assertEquals(market.getAveragePrice(Armour.CHAIN_SHIRT, 5), 200, 0.5);
		assertEquals(market.getAveragePrice(Armour.CHAIN_SHIRT, 1), 200, 0.5);

		assertEquals(market.getOfferVolume(Armour.CHAIN_SHIRT, 5), 1, 0.5);
		assertEquals(market.getOfferVolume(Armour.CHAIN_SHIRT, 1), 1, 0.5);

		assertEquals(market.getBidVolume(Armour.CHAIN_SHIRT, 5), 3, 0.5);
		assertEquals(market.getBidVolume(Armour.CHAIN_SHIRT, 1), 1, 0.5);

		assertEquals(market.getSalesVolume(Armour.CHAIN_SHIRT, 5), 1, 0.5);
		assertEquals(market.getSalesVolume(Armour.CHAIN_SHIRT, 1), 1, 0.5);
	}

	@Test
	public void testTen() {
		// with no experts, check that a fighter puts random items on
		c.addGold(2000);
		c.rest(1);
		List<Artefact> onMarket = market.getItems();
		assertEquals(onMarket.size(), 0);
		
		onMarket = market.getItemsBidFor();
		int shields = 0, weapons = 0, armour = 0, potion = 0;
		for (Artefact a : onMarket) {
			if (a instanceof Shield) shields++;
			if (a instanceof Armour) armour++;
			if (a instanceof Weapon) weapons++;
			if (a instanceof Potion) potion++;
		}
		assertEquals(shields, 1);
		assertEquals(armour, 1);
		assertEquals(weapons, 1);
		assertEquals(potion, 1);
		
		assertEquals(onMarket.size(), 4);
	}
}
