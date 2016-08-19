package hopshackle.simulation.test;
import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;

import org.junit.*;

public class MapKnowledgeTest {

	private Agent agent;
	private MapKnowledge testMapKnowledge;

	@Before
	public void setUp() {
		agent = new BasicAgent(new World());
		testMapKnowledge = agent.getMapKnowledge();
	}

	@Test
	public void addALocationAndCheckItIsKnown() {
		Location l = new Location();
		assertFalse(testMapKnowledge.isKnown(l));
		testMapKnowledge.addLocation(l);
		assertTrue(testMapKnowledge.isKnown(l));
	}

	@Test
	public void checkThatAMovingAgentRemembersWhereItHasBeen() {
		World w = new World();
		Location loc1 = new Location();
		Location loc2 = new Location();
		loc1.setParentLocation(w);
		loc2.setParentLocation(w);
		Agent c = new BasicAgent(w);
		MapKnowledge map = c.getMapKnowledge();
		assertFalse(map.isKnown(loc1));
		c.setLocation(loc1);
		c.setLocation(loc2);
		assertTrue(map.isKnown(loc2));
		assertTrue(map.isKnown(loc1));
	}

	@Test
	public void addALocationMatcherToTrackVariableAndRetrieveIt() {
		HexMap<Hex> hexMap = threeByThreeHexMapWithOneForestHex(2, 1);
		testMapKnowledge.addLocationMap(hexMap);
		agent.setLocation(hexMap.getHexAt(0, 0));
		assertTrue(testMapKnowledge.addJourneyTracker("FOREST", new TerrainMatcher(TerrainType.FOREST), new TerrainMovementCalculator()));
		JourneyPlan jPlan = testMapKnowledge.getJourneyTracker("FOREST");
		assertTrue(!jPlan.isEmpty());
	}

	private HexMap<Hex> threeByThreeHexMapWithOneForestHex(int row, int column) {
		HexMap<Hex> hexMap = new HexMap<Hex>(3, 3, Hex.getHexFactory());
		hexMap.getHexAt(row, column).setTerrain(TerrainType.FOREST);
		return hexMap;
	}

	@Test
	public void addADuplicateLocationMatcherFailsToDoSo() {
		addALocationMatcherToTrackVariableAndRetrieveIt();
		JourneyPlan jPlan1 = testMapKnowledge.getJourneyTracker("FOREST");
		assertFalse(testMapKnowledge.addJourneyTracker("FOREST", new TerrainMatcher(TerrainType.FOREST), new TerrainMovementCalculator()));
		JourneyPlan jPlan2 = testMapKnowledge.getJourneyTracker("FOREST");
		assertTrue(jPlan1 == jPlan2);
	}

	@Test
	public void gettingALocationMatcherThatDoesNotExistReturnsAnEmptyJourneyPlan() {
		JourneyPlan jPlan = testMapKnowledge.getJourneyTracker("PLAINS");
		assertTrue(jPlan.isEmpty());
	}

	@Test
	public void ifAgentMovesThenJourneyPlanIsUpdated() {
		HexMap<Hex> hexMap = threeByThreeHexMapWithOneForestHex(2, 1);
		agent.setLocation(hexMap.getHexAt(0, 0));
		assertTrue(testMapKnowledge.addJourneyTracker("FOREST", new TerrainMatcher(TerrainType.FOREST), new TerrainMovementCalculator()));
		JourneyPlan jPlan = testMapKnowledge.getJourneyTracker("FOREST");
		assertEquals(jPlan.distance(), -1, 0.001);
		agent.setLocation(hexMap.getHexAt(1, 1)); // now next to Forest
		jPlan = testMapKnowledge.getJourneyTracker("FOREST");
		assertEquals(jPlan.distance(), 2, 0.001);
	}

	@Test
	public void updatingJourneyTrackersWithNewDataUpdatesThem() {
		HexMap<Hex> hexMap = threeByThreeHexMapWithOneForestHex(2, 1);
		agent.setLocation(hexMap.getHexAt(0, 0));
		assertTrue(testMapKnowledge.addJourneyTracker("FOREST", new TerrainMatcher(TerrainType.FOREST), new TerrainMovementCalculator()));
		JourneyPlan jPlanForest = testMapKnowledge.getJourneyTracker("FOREST");
		assertEquals(jPlanForest.distance(), -1, 0.001);
		assertTrue(testMapKnowledge.addJourneyTracker("WATER", new TerrainMatcher(TerrainType.OCEAN), new TerrainMovementCalculator()));
		JourneyPlan jPlanWater = testMapKnowledge.getJourneyTracker("WATER");
		assertEquals(jPlanWater.distance(), -1, 0.001);
		testMapKnowledge.addLocationMap(hexMap);
		testMapKnowledge.updateJourneyTrackers();
		JourneyPlan newForestPlan = testMapKnowledge.getJourneyTracker("FOREST");
		JourneyPlan newWaterPlan = testMapKnowledge.getJourneyTracker("WATER");
		assertFalse(newForestPlan == jPlanForest);
		assertEquals(newForestPlan.distance(), 3, 0.001);
		assertEquals(newWaterPlan.distance(), -1, 0.001);
	}
	
	@Test
	public void journeyTrackerNotUpdatedIfNoNewMatchingLocationsHaveBeenFound() {
		HexMap<Hex> hexMap = threeByThreeHexMapWithOneForestHex(2, 1);
		agent.setLocation(hexMap.getHexAt(0, 0));
		assertTrue(testMapKnowledge.addJourneyTracker("FOREST", new TerrainMatcher(TerrainType.FOREST), new TerrainMovementCalculator()));
		JourneyPlan jPlanForest = testMapKnowledge.getJourneyTracker("FOREST");
		assertEquals(jPlanForest.distance(), -1, 0.001);
		agent.setLocation(hexMap.getHexAt(0, 2));
		JourneyPlan jPlanForest2 = testMapKnowledge.getJourneyTracker("FOREST");
		assertTrue(jPlanForest == jPlanForest2);
		agent.setLocation(hexMap.getHexAt(2, 0));
		JourneyPlan jPlanForest3 = testMapKnowledge.getJourneyTracker("FOREST");
		assertFalse(jPlanForest3 == jPlanForest2);
	}
}
