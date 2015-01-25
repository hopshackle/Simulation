package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;

import org.junit.*;
public class VillageCreationTest {

	private BasicAgent testAgent, testAgent2, testAgent3;
	private World world;
	private HexMap<BasicHex> map;
	private BasicHex plainsHex, otherHex;
	private FastCalendar calendar;

	@Before
	public void setUp() {
		world = new World();
		map = new HexMap<BasicHex>(2,2, BasicHex.getHexFactory());
		world.setLocationMap(map);
		calendar = new FastCalendar(0);
		world.setCalendar(calendar);

		testAgent = new BasicAgent(world);
		testAgent.setMale(true);
		testAgent2 = new BasicAgent(world);
		testAgent3 = new BasicAgent(world);
		plainsHex = (BasicHex) map.getHexAt(0, 0);
		otherHex = (BasicHex) map.getHexAt(1, 1);
		testAgent.setLocation(plainsHex);
		testAgent2.setLocation(plainsHex);
		testAgent3.setLocation(plainsHex);
	} 

	@Test
	public void villageIsCreatedIfHexIsFullAndOneDoesNotAlreadyExistAndHutsAreTransferredIntoIt() {
		addHutsToHex(plainsHex, 9, testAgent);
		Village village = villageIn(plainsHex);
		assertTrue(village == null);
		assertEquals(numberOfHutsIn(plainsHex), 9);
		addHutsToHex(plainsHex, 1, testAgent2);
		village = villageIn(plainsHex);
		assertTrue(village != null);
		assertEquals(numberOfHutsIn(village), 10);
		assertEquals(numberOfHutsIn(plainsHex), 0);
	}

	@Test
	public void villageIsNotCreatedIfOneAlreadyExists() {
		villageIsCreatedIfHexIsFullAndOneDoesNotAlreadyExistAndHutsAreTransferredIntoIt();
		Village village = villageIn(plainsHex);
		destroyHutsBelongingTo(testAgent2);
		assertTrue(villageIn(plainsHex) == village);
	}

	@Test
	public void onDeletionAllHutsArePlacedIntoParentHex() {
		villageIsCreatedIfHexIsFullAndOneDoesNotAlreadyExistAndHutsAreTransferredIntoIt();
		Village village = villageIn(plainsHex);
		village.destroy();
		assertEquals(numberOfHutsIn(village), 0);
		assertEquals(numberOfHutsIn(plainsHex), 10);
	}

	@Test
	public void newVillageLeaderIsPersonWithMostVillagesBreakingTiesOnAge() {
		villageIsCreatedIfHexIsFullAndOneDoesNotAlreadyExistAndHutsAreTransferredIntoIt();
		Village village = villageIn(plainsHex);
		assertTrue(village.getOwner() == testAgent);
		destroyHutsBelongingTo(testAgent);
		village.destroy();
		addHutsToHex(plainsHex, 3, testAgent);
		addHutsToHex(plainsHex, 6, testAgent2);
		village = villageIn(plainsHex);
		assertTrue(village.getOwner() == testAgent2);
		village.destroy();
		destroyHutsBelongingTo(testAgent);
		destroyHutsBelongingTo(testAgent2);
		addHutsToHex(plainsHex, 4, testAgent);
		addHutsToHex(plainsHex, 3, testAgent2);
		addHutsToHex(plainsHex, 3, testAgent3);
		village = villageIn(plainsHex);
		assertTrue(village.getOwner() == testAgent);
		village.destroy();
		destroyHutsBelongingTo(testAgent);
		testAgent2.setAge(2000);
		testAgent3.setAge(3000);
		testAgent.setAge(1000);
		addHutsToHex(plainsHex, 2, testAgent2);
		addHutsToHex(plainsHex, 2, testAgent3);
		village = villageIn(plainsHex);
		assertTrue(village.getOwner() == testAgent3);
	}
	
	@Test
	public void villageIsDestroyedIfItFallsBelowSixHuts() {
		addHutsToHex(plainsHex, 4, testAgent);
		addHutsToHex(plainsHex, 1, testAgent2);
		addHutsToHex(plainsHex, 5, testAgent3);
		destroyHutsBelongingTo(testAgent);
		assertTrue(villageIn(plainsHex) != null);
		assertEquals(testAgent3.getNumberInInventoryOf(BuildingType.VILLAGE), 1);
		destroyHutsBelongingTo(testAgent2);
		villageIn(plainsHex).maintenance();
		assertTrue(villageIn(plainsHex) == null);
		assertEquals(testAgent3.getNumberInInventoryOf(BuildingType.VILLAGE), 0);
	}

	@Test
	public void newVillageLeaderAndChildrenHaveSurnameAddedIfNotAlreadyInPossessionOfAVillage() {
		testAgent3.addParent(testAgent);
		testAgent2.addParent(testAgent);
		testAgent2.addItem(new Village(otherHex));
		String newSurname2 = testAgent2.getSurname();
		villageIsCreatedIfHexIsFullAndOneDoesNotAlreadyExistAndHutsAreTransferredIntoIt();
		Village village = villageIn(plainsHex);
		assertTrue(village.toString() != "");
		assertTrue(testAgent.getSurname().equals("of " + village.toString()));
		assertTrue(testAgent3.getSurname().equals("of " + village.toString()));
		assertTrue(testAgent2.getSurname().equals(newSurname2));
		
		testAgent.addItem(new Village(otherHex));
		assertTrue(testAgent.getSurname().equals("of " + village.toString()));
		assertTrue(testAgent3.getSurname().equals("of " + village.toString()));
		assertTrue(testAgent2.getSurname().equals(newSurname2));
	}

	@Test
	public void newVillageLeaderAndChildrenHaveDoNotHaveSurnameAddedIfTheyAlreadyHaveOne() {
		testAgent3.addParent(testAgent);
		testAgent3.setSurname("test");
		villageIsCreatedIfHexIsFullAndOneDoesNotAlreadyExistAndHutsAreTransferredIntoIt();
		Village village = villageIn(plainsHex);
		assertTrue(village.toString() != "");
		assertTrue(testAgent.getSurname().equals("of " + village.toString()));
		assertTrue(testAgent3.getSurname().equals("test"));
		assertTrue(testAgent2.getSurname().equals(""));
	}

	@Test
	public void villageLeadershipIsTransferredOnDeathOfPreviousLeader() {
		villageIsCreatedIfHexIsFullAndOneDoesNotAlreadyExistAndHutsAreTransferredIntoIt();
		Village village = villageIn(plainsHex);
		assertTrue(village.getOwner() == testAgent);
		testAgent.die("Ooops");
		world.setCurrentTime(2000l);
		assertTrue(village.getOwner() == testAgent2);
	}

	@Test
	public void villageIsDeletedIfNoLeaderIsAvailableOnDeathOfPreviousLeader() {
		villageIsCreatedIfHexIsFullAndOneDoesNotAlreadyExistAndHutsAreTransferredIntoIt();
		Village village = villageIn(plainsHex);
		testAgent2.die("Oops");
		testAgent.die("Ooops");
		world.setCurrentTime(2000l);
		assertTrue(village.getOwner() == null);
		assertTrue(village.getChildLocations().isEmpty());
		assertTrue(villageIn(plainsHex) == null);
	}

	@Test
	public void claimableHutIsAutomaticallyGivenToVillageLeaderAndTimerReset() {
		addHutsToHex(plainsHex, 4, testAgent);
		addHutsToHex(plainsHex, 3, testAgent2);
		addHutsToHex(plainsHex, 3, testAgent3);
		assertEquals(testAgent.getNumberInInventoryOf(BuildingType.HUT), 4);
		Village village = villageIn(plainsHex);
		Hut transferredHut = getHutBelongingTo(village, testAgent2);
		testAgent2.die("Ooops");
		village.maintenance();
		assertFalse(transferredHut.isClaimable());
		assertTrue(transferredHut.isOccupied());
		assertTrue(transferredHut.getOwner().equals(testAgent));
		assertEquals(testAgent.getNumberInInventoryOf(BuildingType.HUT), 7);
		
		transferredHut = getHutBelongingTo(village, testAgent3);
		testAgent3.setLocation(otherHex);
		assertFalse(transferredHut.isOccupied());
		assertFalse(transferredHut.isClaimable());
		world.setCurrentTime(27000l);
		assertTrue(transferredHut.getOwner().equals(testAgent));
		assertEquals(testAgent.getNumberInInventoryOf(BuildingType.HUT), 10);
		assertTrue(transferredHut.isOccupied());
		assertFalse(transferredHut.isClaimable());
	}

	@Test
	public void claimableHutAlreadyOwnedByVillageLeaderDoesBecomeClaimableAsNormal() {
		villageIsCreatedIfHexIsFullAndOneDoesNotAlreadyExistAndHutsAreTransferredIntoIt();
		Village village = villageIn(plainsHex);
		Hut transferredHut = getHutBelongingTo(village, testAgent);
		testAgent.setLocation(otherHex);
		world.setCurrentTime(10000l);
		assertFalse(transferredHut.isClaimable());
		assertFalse(transferredHut.isOccupied());
		world.setCurrentTime(25000l);
		assertTrue(transferredHut.isClaimable());
		assertFalse(transferredHut.isOccupied());
		assertTrue(transferredHut.getOwner() == testAgent);
	}
	
	@Test
	public void villageInheritedLikeHuts() {
		villageIsCreatedIfHexIsFullAndOneDoesNotAlreadyExistAndHutsAreTransferredIntoIt();
		testAgent3.addParent(testAgent);
		testAgent.die("Ooops");
		Village village = villageIn(plainsHex);
		assertTrue(village.getOwner() == testAgent3);
		assertEquals(testAgent3.getNumberInInventoryOf(BuildingType.VILLAGE), 1);
		assertEquals(testAgent3.getNumberInInventoryOf(BuildingType.HUT), 9);
	}

	@Test
	public void newHutBuiltInAHexWithAVillageIsAutomaticallyPartOfTheVillage() {
		villageIsCreatedIfHexIsFullAndOneDoesNotAlreadyExistAndHutsAreTransferredIntoIt();
		Village village = villageIn(plainsHex);
		assertEquals(numberOfHutsIn(village), 10);
		destroyHutsBelongingTo(testAgent2);
		assertEquals(numberOfHutsIn(village), 9);
		new Hut(testAgent3);
		assertEquals(numberOfHutsIn(village), 10);
	}
	
	private void addHutsToHex(BasicHex hutLocation, int i, BasicAgent builder) {
		Location initialLocation = builder.getLocation();
		builder.setLocation(hutLocation);
		for (int n = 0; n < i; n++) {
			new Hut(builder);
		}
		builder.setLocation(initialLocation);
	}

	private Village villageIn(BasicHex location) {
		for (Location potentialVillage : location.getChildLocations()) {
			if (potentialVillage instanceof Village) {
				return (Village)potentialVillage;
			}
		}
		return null;
	}

	private int numberOfHutsIn(Location location) {
		int huts = 0;
		for (Location potentialHut : location.getChildLocations()) {
			if (potentialHut instanceof Hut)
				huts++;
		}
		return huts;
	}

	private Hut getHutBelongingTo(Location location, BasicAgent who) {
		for (Location potentialHut : location.getChildLocations()) {
			if (potentialHut instanceof Hut) {
				Hut hut = (Hut)potentialHut;
				if (hut.getOwner().equals(who))
					return hut;
			}
		}
		return null;
	}

	private void destroyHutsBelongingTo(BasicAgent builder) {
		for (Artefact item : builder.getInventory()) {
			if (item instanceof Hut)
				((Hut)item).destroy();
		}
	}
}
