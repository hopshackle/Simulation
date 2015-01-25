package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.actions.*;
import hopshackle.simulation.dnd.genetics.*;

import java.io.File;
import java.util.List;

import org.junit.*;
public class ForageAndExpertTradeTest {

	Character expert, expert2;
	Market market;
	private static String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");
	
	@Before
	public void setUp() throws Exception {
		World w = new World();
		TradeDecider td = new TradeDeciderExp();
		CharacterClass.EXPERT.setDefaultTradeDecider(td);

		expert = new Character(Race.HUMAN, CharacterClass.EXPERT, w);
		expert2 = new Character(Race.HUMAN, CharacterClass.EXPERT, w);
		expert.setDecider(new HardCodedDecider(ExpertActionsI.FORAGE));
		expert.setGenome(new Genome(new File(baseDir + "\\Genomes\\Standard\\Starter_EXP.txt")));
		Location tradeVille = new Location(w);
		expert.setLocation(tradeVille);
		new Market(new File(baseDir + "\\logs\\Market_test.txt"), w);
		// normally the market thread will be runnning - here it won't be
		market = w.getMarket();

		expert.addXp(500);
		expert2.addXp(500);
	}

	@Test
	public void testWood(){
		List<Artefact> preInv = expert.getInventory();
		assertFalse(preInv.contains(Resource.WOOD));

		expert2.addItem(Resource.WOOD);
		market.addItem(new Offer(expert2, Resource.WOOD, 1));
		market.addBid(new Bid(expert2, 2, Resource.WOOD));
		market.addBid(new Bid(expert2, 3, Resource.WOOD));
		market.addItem(new Offer(expert2, Resource.METAL, 1));

		assertEquals(market.getAveragePrice(Resource.WOOD, 1), 0, 0);

		expert2.addGold(20);
		market.resolveAllBids();

		assertEquals(market.getAveragePrice(Resource.WOOD, 1), 2, 0);

		expert.decide().run();

		List<Artefact> postInv = expert.getInventory();
		assertTrue(postInv.contains(Resource.WOOD));
		assertFalse(postInv.contains(Resource.METAL));
	}

	@Test
	public void testMetalDefault(){
		List<Artefact> preInv = expert.getInventory();
		assertFalse(preInv.contains(Resource.WOOD));
		assertFalse(preInv.contains(Resource.METAL));

		expert.decide().run();

		List<Artefact> postInv = expert.getInventory();
		assertFalse(postInv.contains(Resource.WOOD));
		assertTrue(postInv.contains(Resource.METAL));
	}

	@Test
	public void testMetal() {
		List<Artefact> preInv = expert.getInventory();
		assertFalse(preInv.contains(Resource.METAL));

		expert2.addItem(Resource.METAL);
		market.addItem(new Offer(expert2, Resource.METAL, 1));
		market.addBid(new Bid(expert2, 3, Resource.METAL));
		market.addBid(new Bid(expert2, 3, Resource.METAL));
		market.addBid(new Bid(expert2, 4, Resource.METAL));
		market.addItem(new Offer(expert2, Resource.WOOD, 1));
		market.addBid(new Bid(expert2, 1, Resource.WOOD));

		assertEquals(market.getAveragePrice(Resource.METAL, 1), 0, 0);

		expert2.addGold(20);
		market.resolveAllBids();

		assertEquals(market.getAveragePrice(Resource.METAL, 1), 3, 0);
	}

	@Test
	public void testPostMetal() {
		testMetal();
		expert.decide().run();
		List<Artefact> postInv = expert.getInventory();
		assertTrue(postInv.contains(Resource.METAL));
		assertFalse(postInv.contains(Resource.WOOD));
	}

	@Test
	public void testDefaultAction() {

		expert.addGold(-expert.getGold());
		Action a = ExpertActionsI.MAKE_ITEM.getAction(expert);
		assertTrue(a instanceof Forage);

		expert.addItem(Weapon.SHORT_SWORD);
		a = ExpertActionsI.MAKE_ITEM.getAction(expert);
		assertTrue(a instanceof Trade);

		expert.addGold(10);
		a = ExpertActionsI.MAKE_ITEM.getAction(expert);
		assertTrue(a instanceof Trade);
	}

	@Test
	public void testCostToMake() {
		testWood();
		testMetal();

		assertEquals(market.getAveragePrice(Resource.WOOD, 2), 1.5, 0.001);
		assertEquals(market.getAveragePrice(Resource.METAL, 2), 3.0, 0.001);

		assertEquals(Shield.SMALL_SHIELD.costToMake(expert), 0.75, 0.15*0.75);
		assertEquals(Armour.CHAIN_MAIL.costToMake(expert), 24.0, 0.15*24.0);
		assertEquals(Weapon.HEAVY_MACE.costToMake(expert), 3.0, 0.15*3.0);	
	}

	@Test
	public void testRecipeTrade() {
		setUpWoodPrice(0.5);

		expert2.addItem(Resource.WOOD);
		expert2.addItem(Resource.WOOD);
		expert2.addItem(Resource.METAL);
		expert2.addItem(Resource.METAL);
		expert2.addItem(Armour.BANDED_MAIL);
		market.addItem(new Offer(expert2, Resource.WOOD, 0.5*0.85));
		market.addItem(new Offer(expert2, Resource.WOOD, 0.5*0.85));
		market.addItem(new Offer(expert2, Resource.METAL, 1));
		market.addItem(new Offer(expert2, Armour.BANDED_MAIL, 22));
		market.addItem(new Offer(expert2, Resource.METAL, 1));

		expert.addGold(-expert.getGold());
		expert.addGold(25);

		Trade t = new Trade(expert, 0);
		t.addRecipe(Shield.SMALL_SHIELD.getRecipe(), 3);
		// make 3 small shield, requiring 1.5 wood
		// rounded up to 2

		t.run();

		market.resolveAllBids();

		assertFalse(expert.getInventory().contains(Armour.BANDED_MAIL));
		int wood = 0, metal = 0;
		for (Artefact i : expert.getInventory()) {
			if (i == Resource.WOOD) wood++;
			if (i == Resource.METAL) metal++;
		}
		assertEquals(wood, 2, 0);
		assertEquals(metal, 0, 0);

	}

	@Test
	public void checkBidPriceIsAccurate() {
		setUpWoodPrice(0.5);
		
		expert2.addItem(Resource.WOOD);
		expert2.addItem(Resource.WOOD);
		market.addItem(new Offer(expert2, Resource.WOOD, 0.2));

		expert.addGold(-expert.getGold());
		expert.addGold(25);

		Trade t = new Trade(expert, 0);
		t.addRecipe(Shield.SMALL_SHIELD.getRecipe(), 3);
		// make 3 small shield, requiring 1.5 wood
		// rounded up to 2

		t.run();

		market.resolveAllBids();
		
		assertEquals(market.getAveragePrice(Resource.WOOD, 1), 0.50, 0.50*0.15);
	}
	
	private void setUpWoodPrice(double price) {
		setUpMarket(Resource.WOOD, price);
		market.resolveAllBids();
	}
	
	private void setUpMarket(Artefact item, double price) {
		market.addBid(new Bid(expert2, price, item));
		market.addItem(new Offer(null, item, price));
	}
}



