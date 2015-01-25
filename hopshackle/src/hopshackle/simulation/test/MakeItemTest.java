package hopshackle.simulation.test;
import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.actions.*;
import hopshackle.simulation.dnd.genetics.*;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.*;

public class MakeItemTest {

	Character expert, expert2;
	Market market;
	World w;
	private static String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");

	@Before
	public void setUp() throws Exception {
		SimProperties.clear();
		w = new World();
		CharacterClass.EXPERT.setDefaultTradeDecider(new TradeDeciderExp());

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
		expert.addGold(10);
		expert2.addGold(500);
		expert.setIntelligence(new Attribute(12));
		expert2.setIntelligence(new Attribute(14));
	}

	
	@Test
	public void testInventoryReduction() {
		assertEquals(expert.getGold(), 10, 0.001);
		MakeItem action = new MakeItem(expert, Armour.LEATHER_ARMOUR, 1);
		assertEquals(expert.getGold(), 7, 0.001);
		action.run();
		assertTrue(expert.getSummaryInventory().get(Armour.LEATHER_ARMOUR) == null);
		assertTrue(market.getItems().contains(Armour.LEATHER_ARMOUR));
		assertFalse(market.getItems().contains(Weapon.BASTARD_SWORD));

		assertEquals(expert.getGold(), 6.75, 0.25);
		for (int n=0; n<8; n++) 
			expert.addItem(Resource.METAL);
		assertEquals(expert.getSummaryInventory().get(Resource.METAL), 8, 0);

		action = new MakeItem(expert, Weapon.BASTARD_SWORD, 2);
		assertEquals(expert.getSummaryInventory().get(Resource.METAL), 2, 0);
		action.run();
		assertTrue(expert.getSummaryInventory().get(Armour.LEATHER_ARMOUR) == null);
		assertTrue(expert.getSummaryInventory().get(Weapon.BASTARD_SWORD) == null);
		assertEquals(expert.getSummaryInventory().get(Resource.METAL), 2, 0);
		assertTrue(market.getItems().contains(Armour.LEATHER_ARMOUR));
		assertTrue(market.getItems().contains(Weapon.BASTARD_SWORD));
		
	}
	@Test
	public void testMakeDC() {

		assertEquals(Shield.SMALL_SHIELD.getMakeDC(), 11, 0);

		Skill s =  expert.getSkill(Skill.skills.CRAFT);
		Craft craftSkill = (Craft) s;
		long timeToCreate = craftSkill.timeToCreate(Shield.SMALL_SHIELD);
		assertEquals(timeToCreate, 90);
		
		timeToCreate = Shield.SMALL_SHIELD.getTimeToMake(expert);
		assertEquals(timeToCreate, 90);

		timeToCreate = craftSkill.timeToCreate(Weapon.LIGHT_MACE);
		assertEquals(timeToCreate, 138);
		
		timeToCreate = Weapon.LIGHT_MACE.getTimeToMake(expert);
		assertEquals(timeToCreate, 138);
		
		timeToCreate = craftSkill.timeToCreate(Armour.LEATHER_ARMOUR);
		assertEquals(timeToCreate, 277);
		
		timeToCreate = Armour.LEATHER_ARMOUR.getTimeToMake(expert);
		assertEquals(timeToCreate, 277);

	}

	public void marketSetUp() {
		/*
		 * 	Sets market up with:
		 * 	Small_Shield price of 10
		 * 	Wood price of 1
		 * 	Metal price of 5
		 * 
		 *	with no materials extant
		 */

		expert2.addItem(Shield.SMALL_SHIELD);
		expert2.addItem(Resource.WOOD);
		expert2.addItem(Resource.METAL);
		market.addItem(new Offer(expert2, Shield.SMALL_SHIELD, 10));
		market.addBid(new Bid(expert2, 20, Shield.SMALL_SHIELD));
		market.addItem(new Offer(expert2, Resource.WOOD, 1));
		market.addBid(new Bid(expert2, 1, Resource.WOOD));
		market.addItem(new Offer(expert2, Resource.METAL, 5));
		market.addBid(new Bid(expert2, 5, Resource.METAL));
		market.addBid(new Bid(expert2, 10, Weapon.BASTARD_SWORD));
		market.addBid(new Bid(expert2, 9, Armour.LEATHER_ARMOUR));
		market.addItem(new Offer(expert2, Weapon.MASTERWORK_BASTARD_SWORD, 8));
		market.resolveAllBids();

		assertEquals(market.getAveragePrice(Shield.SMALL_SHIELD, 1), 10, 0);
		assertEquals(market.getAveragePrice(Armour.LEATHER_ARMOUR, 1), 9, 0);
		assertEquals(market.getAveragePrice(Resource.WOOD, 1), 1, 0);
		assertEquals(market.getAveragePrice(Resource.METAL, 1), 5, 0);
	}
	public void marketSetUpII() {

		/* sets up market with:
		 * 	small_shield price of 6
		 * 	wood price of 1
		 * 	metal price of 5
		 * 	light mace price of 20
		 * 
		 * must be called after marketSetUp() [only changes are Light Mace and Small Shield]
		 * 
		 */
		expert.addItem(Weapon.LIGHT_MACE);
		market.addItem(new Offer(expert2, Weapon.LIGHT_MACE, 20));
		market.addBid(new Bid(expert2, 20, Weapon.LIGHT_MACE));
		market.addItem(new Offer(expert2, Resource.WOOD, 1));
		market.addBid(new Bid(expert2, 1, Resource.WOOD));
		market.addItem(new Offer(expert2, Shield.SMALL_SHIELD, 2));
		market.addBid(new Bid(expert2, 2, Shield.SMALL_SHIELD));
		market.addBid(new Bid(expert2, 10, Weapon.BASTARD_SWORD));
		market.addItem(new Offer(expert2, Weapon.BASTARD_SWORD, 8));
		market.addItem(new Offer(expert2, Resource.METAL, 5));
		market.addItem(new Offer(expert2, Resource.METAL, 5));
		market.resolveAllBids();

		assertEquals(market.getAveragePrice(Weapon.LIGHT_MACE, 5), 20, 0);
		assertEquals(market.getAveragePrice(Shield.SMALL_SHIELD, 5), 6, 0);
		assertEquals(market.getAveragePrice(Resource.WOOD, 5), 1, 0);
		assertEquals(market.getAveragePrice(Resource.METAL, 5), 5, 0);
	}

	public void marketSetUpIIb() {

		/* sets up market with:
		 * 	small_shield price of 6
		 * 	wood price of 1
		 * 	metal price of 5
		 * 	light mace price of 20
		 *  leather armour price of 2
		 * 
		 * must be called after marketSetUp() [only changes are Light Mace and Small Shield]
		 * 
		 */
		expert2.addItem(Weapon.LIGHT_MACE);
		expert2.addItem(Shield.SMALL_SHIELD);
		expert2.addItem(Armour.LEATHER_ARMOUR);
		market.addItem(new Offer(expert2, Weapon.LIGHT_MACE, 20));
		market.addBid(new Bid(expert2, 20, Weapon.LIGHT_MACE));
		market.addItem(new Offer(expert2, Shield.SMALL_SHIELD, 2));
		market.addBid(new Bid(expert2, 2, Shield.SMALL_SHIELD));
		market.addItem(new Offer(expert2, Armour.LEATHER_ARMOUR, 2));
		market.addBid(new Bid(expert2, 2, Armour.LEATHER_ARMOUR));
		market.resolveAllBids();

		assertEquals(market.getAveragePrice(Weapon.LIGHT_MACE, 5), 20, 0);
		assertEquals(market.getAveragePrice(Shield.SMALL_SHIELD, 5), 6, 0);
		assertEquals(market.getAveragePrice(Resource.WOOD, 5), 1, 0);
		assertEquals(market.getAveragePrice(Resource.METAL, 5), 5, 0);
		assertEquals(market.getAveragePrice(Armour.LEATHER_ARMOUR, 5), 2, 0);
	}

	@Test
	public void testMakeItem() {
		marketSetUp();
		Action a = expert.decide();
		assertTrue(a instanceof Trade);
		Trade t = (Trade)a;
		assertTrue(t.getRecipe().getProduct() == Armour.LEATHER_ARMOUR);
		assertEquals(t.getNumber(), 3);

		expert.addItem(Resource.WOOD);
		a = expert.decide();
		assertTrue(a instanceof Trade);
		t = (Trade)a;
		assertTrue(t.getRecipe().getProduct() == Shield.SMALL_SHIELD);
		assertEquals(t.getNumber(), 2);

		marketSetUpII();
		// Changes relative prices, so now Light Maces should be more profitable

		a = expert.decide();
		assertTrue(a instanceof Trade);
		t = (Trade)a;
		assertTrue(t.getRecipe().getProduct() == Weapon.LIGHT_MACE);
		assertEquals(t.getNumber(), 1);
	}

	@Test
	public void testMakeItemII() {
		marketSetUp();
		marketSetUpIIb();
		assertEquals(TradeGeneticEnum.SALES_VOLUME.getValue(expert, Resource.WOOD), 1.0, 0.01);
		expert.addGold(-5);
		Action a = expert.decide();
		assertTrue(a instanceof Trade);
		Trade t = (Trade)a;
		
		// Leather Armour now unprofitable, so should go to other item
		// has 4 gold, minus a bit for 'maintenance', and Wood costs 1.0 gold each - so can buy maximum of 4 wood for 4 Light Maces
		// Limit on trading period is 5, so therefore should make 4 light maces
		// and we can expect to buy 4 in one period
		assertTrue(t.getRecipe().getProduct() == Weapon.LIGHT_MACE);
		assertEquals(t.getNumber(), 4, 1);
//		assertTrue(t.getNumber()>=3);
//		assertTrue(t.getNumber()<=4);
		// Max amount possible given gold
		
		expert.addGold(30);   //to check that limit is now one market period
		a = expert.decide();
		assertTrue(a instanceof Trade);
		t = (Trade)a;
		assertTrue(t.getRecipe().getProduct() == Weapon.LIGHT_MACE);
		assertEquals(t.getNumber(), 5);
		// with limit on one trading period
		
		
		addOfferToMarket(Resource.WOOD, 10, 1);
		addBidToMarket(Resource.WOOD, 10, 1);
		market.resolveAllBids();	
		// increases sales volume of Wood, so now we should make more
		assertEquals(TradeGeneticEnum.SALES_VOLUME.getValue(expert, Resource.WOOD), 3.66667, 0.01);
		
		a = expert.decide();
		assertTrue(a instanceof Trade);
		t = (Trade)a;
		assertTrue(t.getRecipe().getProduct() == Weapon.LIGHT_MACE);
		assertEquals(t.getNumber(), 25);
		// with limit on one trading period

	}
	
	@Test
	public void testLiquidity() {
		assertEquals(market.getSalesVolume(Resource.WOOD, 5), 0.0, 0.001);
		
		marketSetUp();
		
		assertEquals(market.getSalesVolume(Resource.WOOD, 1), 1.0, 0.001);
		assertEquals(market.getSalesVolume(Resource.WOOD, 5), 1.0, 0.001);
		assertEquals(market.getSalesVolume(Weapon.BASTARD_SWORD, 1), 0.0, 0.001);
		assertEquals(market.getBidVolume(Weapon.BASTARD_SWORD, 1), 1.0, 0.001);
		
		assertEquals(TradeGeneticEnum.BID_LIQUIDITY.getValue(expert, Weapon.BASTARD_SWORD), 0.0, 0.001);
		assertEquals(TradeGeneticEnum.BID_LIQUIDITY.getValue(expert, Weapon.MASTERWORK_BASTARD_SWORD), 1.0, 0.001);
		assertEquals(TradeGeneticEnum.OFFER_LIQUIDITY.getValue(expert, Weapon.MASTERWORK_BASTARD_SWORD), 0.0, 0.001);
		assertEquals(TradeGeneticEnum.OFFER_LIQUIDITY.getValue(expert, Weapon.BASTARD_SWORD), 1.0, 0.001);
		assertEquals(TradeGeneticEnum.BID_LIQUIDITY.getValue(expert, Resource.WOOD), 1.0, 0.001);
		assertEquals(TradeGeneticEnum.BID_LIQUIDITY.getValue(expert, Weapon.HEAVY_MACE), 0.0, 0.001);
		
		marketSetUpII();
		assertEquals(market.getSalesVolume(Resource.WOOD, 1), 1.0, 0.001);
		
		assertEquals(market.getSalesVolume(Resource.WOOD, 5), 2.0, 0.001);
		
		assertEquals(market.getSalesVolume(Resource.METAL, 1), 0.0, 0.001); //Offers only
		assertEquals(market.getSalesVolume(Resource.METAL, 5), 1.0, 0.001);
		
		assertEquals(market.getSalesVolume(Weapon.BASTARD_SWORD, 1), 1.0, 0.001);
		assertEquals(market.getSalesVolume(Weapon.BASTARD_SWORD, 5), 1.0, 0.001);
		
		assertEquals(TradeGeneticEnum.OFFER_LIQUIDITY.getValue(expert, Resource.WOOD), 1.0, 0.001);
		assertEquals(TradeGeneticEnum.OFFER_LIQUIDITY.getValue(expert, Resource.METAL), 0.33333, 0.001);
		assertEquals(TradeGeneticEnum.OFFER_LIQUIDITY.getValue(expert, Weapon.BASTARD_SWORD), 1.0, 0.001);
		
		assertEquals(TradeGeneticEnum.BID_LIQUIDITY.getValue(expert, Resource.WOOD), 1.0, 0.001);
		assertEquals(TradeGeneticEnum.BID_LIQUIDITY.getValue(expert, Resource.METAL), 1.0, 0.001);
		assertEquals(TradeGeneticEnum.BID_LIQUIDITY.getValue(expert, Weapon.BASTARD_SWORD), 0.5, 0.001);
		
		assertEquals(market.getTradingPeriods(Resource.WOOD, 1), 1);
		assertEquals(market.getTradingPeriods(Resource.WOOD, 5), 2);
		
		assertEquals(TradeGeneticEnum.SALES_VOLUME.getValue(expert, Resource.WOOD), 1.0, 0.001);
		assertEquals(TradeGeneticEnum.SALES_VOLUME.getValue(expert, Resource.METAL), 0.5, 0.001);
		assertEquals(TradeGeneticEnum.SALES_VOLUME.getValue(expert, Weapon.BASTARD_SWORD), 0.5, 0.001);
		assertEquals(TradeGeneticEnum.SALES_VOLUME.getValue(expert, Weapon.HEAVY_MACE), 0.4, 0.001);
		/*
		 * Sales volume is number sold divided by number of periods.
		 * Do the following count:
		 * No Bid OR Offers					No currently
		 * Bids but no Offers?				Yes currently
		 * Offers but no Bids?				Yes currently
		 * Bids and Offers, but no sales?	Yes currently
		 *
		 *
		 * Offer_Liquidity is the %age of Offers made over the last five periods that were converted into Sales
		 */
	}

	@Test
	public void testNoRecipe() {
		expert.addItem(Weapon.BASTARD_SWORD);
		expert.setDecider(new HardCodedDecider(ExpertActionsI.TRADE));
		Action a = expert.decide();
		a.run();
		assertFalse(expert.getSummaryInventory().containsKey(Weapon.BASTARD_SWORD));
	}
	@Test
	public void testSuccessfulSequence() {
		marketSetUp();
		marketSetUpIIb();
		Action a = expert.decide();
		assertTrue(a instanceof Trade);
		Trade t = (Trade)a;

		ActionProcessor ap = new ActionProcessor("test", false);
		w.setActionProcessor(ap);

		t.run();

		List<Action> actions = expert.getExecutedActions();
		assertTrue(actions.get(0) instanceof Trade);

		Action next = ap.getNextUndeletedAction(100, TimeUnit.MILLISECONDS);
		assertTrue(next instanceof WaitForMarket);

		next.run();
		actions = expert.getExecutedActions();
		assertTrue(actions.get(1) instanceof WaitForMarket);

		assertTrue(ap.getNextUndeletedAction(100, TimeUnit.MILLISECONDS) == null);
		// test no action added by WaitForMarket

		addOfferToMarket(Resource.WOOD, 5, 1);
		market.resolveAllBids();

		next = ap.getNextUndeletedAction();
		assertTrue(next instanceof CheckMaterials);

		next.run();

		next = ap.getNextUndeletedAction();
		assertTrue(next instanceof MakeItem);

		market.resolveAllBids();
		assertTrue(ap.getNextUndeletedAction(100, TimeUnit.MILLISECONDS) == null);
		// check that the listener has been removed, and doesn't repeat its job
	}

	@Test
	public void testInsufficientMaterials() {
		marketSetUp();
		marketSetUpIIb();
		Action a = expert.decide();
		assertTrue(a instanceof Trade);
		Trade t = (Trade)a;

		ActionProcessor ap = new ActionProcessor("test", false);
		w.setActionProcessor(ap);

		t.run();

		List<Action> actions = expert.getExecutedActions();
		assertTrue(actions.get(0) instanceof Trade);

		Action next = ap.getNextUndeletedAction();
		assertTrue(next instanceof WaitForMarket);

		next.run();
		actions = expert.getExecutedActions();
		assertTrue(actions.get(1) instanceof WaitForMarket);

		market.resolveAllBids();

		next = ap.getNextUndeletedAction();
		assertTrue(next instanceof CheckMaterials);

		next.run();

		next = ap.getNextUndeletedAction();
		assertFalse(next instanceof MakeItem);	
	}

	@Test
	public void testInsufficientGold() {
		Trade t = new Trade(expert, 0);
		t.addRecipe(Armour.LEATHER_ARMOUR.getRecipe(), 4);
		// 10 gold should be insufficient for this

		ActionProcessor ap = new ActionProcessor("test", false);
		w.setActionProcessor(ap);

		t.run();

		List<Action> actions = expert.getExecutedActions();
		assertTrue(actions.get(0) instanceof Trade);

		Action next = ap.getNextUndeletedAction();
		assertTrue(next instanceof WaitForMarket);

		next.run();
		market.addBid(new Bid(expert2,1, Resource.WOOD));
		market.resolveAllBids();

		expert.addGold(-expert.getGold());
		next = ap.getNextUndeletedAction();
		assertTrue(next instanceof CheckMaterials);

		next.run();

		next = ap.getNextUndeletedAction();
		assertFalse(next instanceof MakeItem);

	}

	@Test
	public void testHasStartingInventoryI() {
		// all present
		marketSetUp();
		marketSetUpII();

		Trade t = new Trade(expert, 0);
		t.addRecipe(Armour.CHAIN_SHIRT.getRecipe(), 1);
		// 5 metal needed

		for (int n=0; n<5; n++)
			expert.addItem(Resource.METAL);
		
		expert.addGold(200);

		ActionProcessor ap = new ActionProcessor("test", false);
		w.setActionProcessor(ap);

		t.run();

		Action next = ap.getNextUndeletedAction();
		assertTrue(next instanceof MakeItem);
	}

	@Test
	public void testHasStartingInventoryII() {
		// most materials present; only enough to make n% of original plan
		// all present
		marketSetUp();

		Trade t = new Trade(expert, 0);
		t.addRecipe(Weapon.HEAVY_MACE.getRecipe(), 10);
		// 10 wood, 5 metal needed

		for (int n=0; n<5; n++) 
			expert.addItem(Resource.METAL);
		for (int n=0; n<7; n++) 
			expert.addItem(Resource.WOOD);
		
		expert.addGold(200);

		ActionProcessor ap = new ActionProcessor("test", false);
		w.setActionProcessor(ap);

		t.run();

		Action next = ap.getNextUndeletedAction();
		assertTrue(next instanceof WaitForMarket);
		next.run();
		market.resolveAllBids();
		
		next = ap.getNextUndeletedAction();
		assertTrue(next instanceof CheckMaterials);
		next.run();
		next = ap.getNextUndeletedAction();
		assertTrue(next instanceof MakeItem);
		
		assertEquals(((MakeItem) next).getVolume(), 7);
		
		next.run();
		// should use up 7 Wood, and 4 Metal
		assertFalse(expert.getInventory().contains(Resource.WOOD));
		
		assertEquals(expert.getSummaryInventory().get(Resource.METAL), 1, 0.001);
		
	}

	private void addOfferToMarket(Artefact item, int number, int price) {
		for (int n=0; n<number; n++) {
			expert2.addItem(item);
			market.addItem(new Offer(expert2,item, price));
		}
	}
	private void addBidToMarket(Artefact item, int number, int price) {
		for (int n=0; n<number; n++) {
			market.addBid(new Bid(expert2, price, item));
		}
	}
}
