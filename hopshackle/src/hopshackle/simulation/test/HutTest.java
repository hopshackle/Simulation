package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;

import org.junit.*;
public class HutTest {

	private BasicAgent testAgent, testAgent2;
	private World world;
	private HexMap<BasicHex> map;
	private BasicHex plainsHex, forestHex, hillHex;
	private FastCalendar calendar;
	
	@Before
	public void setUp() {
		world = new World();
		map = new HexMap<BasicHex>(2,2, BasicHex.getHexFactory());
		world.setLocationMap(map);
		calendar = new FastCalendar(0);
		world.setCalendar(calendar);
		
		testAgent = new BasicAgent(world);
		testAgent2 = new BasicAgent(world);
		plainsHex = (BasicHex) map.getHexAt(0, 0);
		forestHex = (BasicHex) map.getHexAt(0, 1);
		forestHex.setTerrain(TerrainType.FOREST);
		hillHex = (BasicHex) map.getHexAt(1, 0);
		hillHex.setTerrain(TerrainType.FOREST);
		testAgent.setLocation(plainsHex);
		testAgent2.setLocation(plainsHex);
	}
	
	@Test
	public void hutCanOnlyBeBuiltInPlainsWithAvailableCarryingCapacityAndEnoughWood() {
		assertFalse(BasicActions.BUILD.isChooseable(testAgent));
		testAgent.addItem(Resource.WOOD);
		testAgent.addItem(Resource.WOOD);
		assertFalse(BasicActions.BUILD.isChooseable(testAgent));
		testAgent.addItem(Resource.WOOD);
		assertTrue(BasicActions.BUILD.isChooseable(testAgent));
		
		testAgent.setLocation(forestHex);
		assertFalse(BasicActions.BUILD.isChooseable(testAgent));
		
		testAgent.setLocation(plainsHex);
		plainsHex.changeMaxCarryingCapacity(-20);
		assertFalse(BasicActions.BUILD.isChooseable(testAgent));
	}
	
	@Test
	public void hutReducesCarryingCapacityByOnePermanently() {
		testAgent.addItem(Resource.WOOD);
		testAgent.addItem(Resource.WOOD);
		testAgent.addItem(Resource.WOOD);
		assertEquals(testAgent.getNumberInInventoryOf(BuildingType.HUT), 0);
		assertEquals(plainsHex.getCarryingCapacity(), 10);
		assertEquals(plainsHex.getMaxCarryingCapacity(), 10);
		assertEquals(testAgent.getNumberInInventoryOf(Resource.WOOD), 3);
		Action buildAction = new BuildHut(testAgent);
		assertEquals(plainsHex.getMaxCarryingCapacity(), 9);
		assertEquals(plainsHex.getCarryingCapacity(), 9);
		buildAction.run();
		assertEquals(testAgent.getNumberInInventoryOf(Resource.WOOD), 0);
		assertEquals(testAgent.getNumberInInventoryOf(BuildingType.HUT), 1);
		assertEquals(plainsHex.getMaxCarryingCapacity(), 9);
		assertEquals(plainsHex.getCarryingCapacity(), 9);
	}
	
	@Test
	public void destructionOfAHutReleasesMaxCarryingCapacity() {
		assertEquals(plainsHex.getMaxCarryingCapacity(), 10);
		new BuildHut(testAgent).doStuff();
		assertEquals(plainsHex.getMaxCarryingCapacity(), 9);
		Hut hut = plainsHex.getHuts().get(0);
		hut.destroy();
		assertEquals(plainsHex.getMaxCarryingCapacity(), 10);
	}
	
	@Test
	public void hexStoresNumberOfHutsPresent() {
		assertEquals(plainsHex.getHuts().size(), 0);
		Hut h1 = new Hut(testAgent);
		assertEquals(plainsHex.getHuts().size(), 1);
		new Hut(testAgent);
		assertEquals(plainsHex.getHuts().size(), 2);
		h1.destroy();
		assertEquals(plainsHex.getHuts().size(), 1);
	}
	
	@Test
	public void whenTheOwnerOfAHutDiesTheHutIsNotDestroyed() {
		Hut h1 = new Hut(testAgent);
		assertTrue(h1.getParentLocation().equals(plainsHex));
		assertTrue(h1.getLocation().equals(plainsHex));
		assertTrue(h1.getOwner().equals(testAgent));
		testAgent.die("oops");
		assertTrue(h1.getParentLocation().equals(plainsHex));
		assertTrue(h1.getLocation().equals(plainsHex));
		assertFalse(h1.getOwner() == null);
		assertFalse(h1.isClaimable());
		plainsHex.maintenance();
		assertTrue(h1.getOwner() == null);
		assertTrue(h1.isClaimable());
	}
	
	@Test
	public void hutMaintenanceRecordsTimeSinceLastOccupied() {
		Hut h1 = new Hut(testAgent);
		assertTrue(h1.isOccupied());
		assertEquals(h1.getTimeLeftVacant(), 0.00, 0.001);
		
		calendar.setTime(2000);
		plainsHex.maintenance();
		assertTrue(h1.isOccupied());
		assertEquals(h1.getTimeLeftVacant(), 0.00, 0.001);
	
		testAgent.setLocation(hillHex);
		assertFalse(h1.isOccupied());
		assertEquals(h1.getTimeLeftVacant(), 0.00, 0.001);
		
		calendar.setTime(4000);
		plainsHex.maintenance();
		assertFalse(h1.isOccupied());
		assertEquals(h1.getTimeLeftVacant(), 2000.00, 0.001);
	}
	
	@Test
	public void ifAHutIsUnoccupiedForTooLongThenItCanBeClaimed() {
		Hut h1 = new Hut(testAgent);
		testAgent.setLocation(hillHex);
		calendar.setTime(19000);
		assertTrue(h1.getOwner() != null);
		assertTrue(!h1.isClaimable());
		
		calendar.setTime(21000);
		assertTrue(h1.getOwner() != null);
		assertTrue(h1.isClaimable());
	}
	
	@Test
	public void ifHutsAreClaimableThenRestingWillClaimOne() {
		Hut h1 = new Hut(testAgent);
		Hut h2 = new Hut(testAgent);
		testAgent.setLocation(hillHex);
		calendar.setTime(30000);
		
		assertTrue(h1.isClaimable() && h2.isClaimable());
		(new Rest(testAgent2)).run();
		assertTrue(h1.getOwner().equals(testAgent2) || h2.getOwner().equals(testAgent2));
		assertEquals(testAgent2.getNumberInInventoryOf(BuildingType.HUT), 1);
		assertEquals(testAgent.getNumberInInventoryOf(BuildingType.HUT), 1);
		assertTrue(h1.isClaimable() || h2.isClaimable());
	}
	
	@Test
	public void ifAHutIsUnoccupiedForTooLongThenItIsDestroyed() {
		Hut h1 = new Hut(testAgent);
		testAgent.setLocation(hillHex);
		calendar.setTime(49000);
		assertTrue(h1.getOwner() != null);
		
		calendar.setTime(50500);
		assertTrue(h1.getOwner() == null);
	}
	
	@Test
	public void journeyTrackerforHutUpdatedWhenBuilt() {
		MapKnowledge testMapKnowledge = testAgent.getMapKnowledge();
		assertTrue(testMapKnowledge.addJourneyTracker("HUT", new HutsOwnedByMatcher(testAgent), new TerrainMovementCalculator()));
		JourneyPlan jPlanHut = testMapKnowledge.getJourneyTracker("HUT");
		assertEquals(jPlanHut.distance(), -1, 0.001);
		Action buildAction = new BuildHut(testAgent);
		buildAction.run();
		testAgent.setLocation(forestHex);
		JourneyPlan jPlanHut2 = testMapKnowledge.getJourneyTracker("HUT");
		assertFalse(jPlanHut == jPlanHut2);
		assertEquals(jPlanHut2.distance(), 1.0, 0.001);
	}
	
}
