package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.GiftPolicy.Gift;
import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;

import java.util.List;

import org.junit.*;

public class BasicGiftPolicyTest {

	World w = new World();
	BasicAgent parent1, parent2;
	BasicAgent[] children;
	BasicHex homeHex = new BasicHex(0, 0);
	BasicHex farAwayHex = new BasicHex(0, 1);
	BasicGiftPolicy testPolicy = new BasicGiftPolicy();
	Marriage marriage;

	@Before
	public void setup() {
		homeHex.setParentLocation(w);
		parent1 = new BasicAgent(w);
		parent1.setLocation(homeHex);
		parent1.setMale(true);
		parent2 = new BasicAgent(w);
		parent2.setLocation(homeHex);
		parent2.setMale(false);
		children = new BasicAgent[10];
		marriage = new Marriage(parent1, parent2);
	}

	@Test
	public void noGiftsIfNoChildrenAndSurplusHut() {
		new Hut(parent1);
		new Hut(parent1);
		List<Gift> giftList = testPolicy.getListOfItemsGiven(parent1);
		assertTrue(giftList.isEmpty());
	}

	@Test
	public void noGiftsIfChildrenAndNoHut() {
		children[0] = haveChild(parent1, parent2);
		List<Gift> giftList = testPolicy.getListOfItemsGiven(parent1);
		assertTrue(giftList.isEmpty());
	}
	
	@Test
	public void noGiftsIfChildrenAndOneHut() {
		new Hut(parent1);
		children[0] = haveChild(parent1, parent2);
		List<Gift> giftList = testPolicy.getListOfItemsGiven(parent1);
		assertTrue(giftList.isEmpty());
	}

	@Test
	public void giveSurplusHutToEldestChildWithoutHut() {
		new Hut(parent1);
		new Hut(parent1);
		children[0] = haveChild(parent1, parent2);
		children[1] = haveChild(parent1, parent2);
		List<Gift> giftList = testPolicy.getListOfItemsGiven(parent1);
		assertEquals(giftList.size(), 1);
		assertTrue(giftList.get(0).item instanceof Hut);
		assertTrue(giftList.get(0).recipient == children[0]);
	}
	
	@Test
	public void marriedFemalesDoNotKeepAHutForOwnUse() {
		new Hut(parent2);
		children[0] = haveChild(parent1, parent2);
		children[1] = haveChild(parent1, parent2);
		List<Gift> giftList = testPolicy.getListOfItemsGiven(parent2);
		assertEquals(giftList.size(), 1);
		assertTrue(giftList.get(0).item instanceof Hut);
		assertTrue(giftList.get(0).recipient == children[0]);
		testPolicy.giveItems(parent2);
		
		marriage.dissolve();
		new Hut(parent2);
		giftList = testPolicy.getListOfItemsGiven(parent2);
		assertTrue(giftList.isEmpty());
	}
	
	@Test
	public void hutOwnershipIsTransferredOnGiving() {
		Hut testHut1 = new Hut(parent1);
		Hut testHut2 = new Hut(parent1);
		assertEquals(parent1.getNumberInInventoryOf(BuildingType.HUT), 2);
		children[0] = haveChild(parent1, parent2);
		children[1] = haveChild(parent1, parent2);
		testPolicy.giveItems(parent1);
		assertEquals(parent1.getNumberInInventoryOf(BuildingType.HUT), 1);
		assertEquals(children[0].getNumberInInventoryOf(BuildingType.HUT), 1);
		assertEquals(children[1].getNumberInInventoryOf(BuildingType.HUT), 0);
		if (testHut1.getOwner() == parent1) {
			assertTrue(testHut2.getOwner() == children[0]);
		} else {
			assertTrue(testHut1.getOwner() == children[0]);
		}
	}

	@Test
	public void giveOneHutToEachChildWithoutHutAndKeepRemainder() {
		for (int n = 0; n < 5; n++)
			new Hut(parent1);
		for (int n = 0; n < 3; n++) 
			children[n] = haveChild(parent1, parent2);
		new Hut(children[0]);
		
		List<Gift> giftList = testPolicy.getListOfItemsGiven(parent1);
		assertEquals(giftList.size(), 2);
		assertTrue(giftList.get(0).item instanceof Hut);
		assertTrue(giftList.get(0).recipient == children[1]);
		assertTrue(giftList.get(1).item instanceof Hut);
		assertTrue(giftList.get(1).recipient == children[2]);
	}
	
	@Test
	public void doNotGiveHutsToDeadChildren() {
		new Hut(parent1);
		new Hut(parent1);
		children[0] = haveChild(parent1, parent2);
		children[1] = haveChild(parent1, parent2);
		children[0].die("Oops");
		List<Gift> giftList = testPolicy.getListOfItemsGiven(parent1);
		assertEquals(giftList.size(), 1);
		assertTrue(giftList.get(0).item instanceof Hut);
		assertTrue(giftList.get(0).recipient == children[1]);
	}
	
	@Test
	public void keepHutInCurrentLocationWherePossible() {
		parent1.setLocation(farAwayHex);
		new Hut(parent1);
		new Hut(parent1);
		parent1.setLocation(homeHex);
		new Hut(parent1);
		children[0] = haveChild(parent1, parent2);
		children[1] = haveChild(parent1, parent2);
		List<Gift> giftList = testPolicy.getListOfItemsGiven(parent1);
		assertEquals(giftList.size(), 2);
		assertTrue(giftList.get(0).recipient == children[0]);
		assertTrue(giftList.get(1).recipient == children[1]);
		assertTrue(((Hut)giftList.get(0).item).getLocation() == farAwayHex);
		assertTrue(((Hut)giftList.get(1).item).getLocation() == farAwayHex);
	}

	private BasicAgent haveChild(BasicAgent parent1, BasicAgent parent2) {
		BasicAgent baby =  new BasicAgent(parent1, parent2);
		parent1.addHealth(10.0);
		parent2.addHealth(10.0);
		return baby;
	}
}
