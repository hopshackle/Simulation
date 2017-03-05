package hopshackle.simulation.test;

import static org.junit.Assert.*;

import java.util.*;

import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;

import org.junit.*;
public class BasicMoveTest {

	private World w;
	private HexMap<BasicHex> hexMap;
	private BasicAgent testAgent;
	private Location startLocation;
	private TestActionProcessor testAP;
	private List<ActionEnum<BasicAgent>> allActions = new ArrayList<ActionEnum<BasicAgent>>(EnumSet.allOf(BasicActions.class));

	@Before
	public void setUp() {
		SimProperties.setProperty("ReforestationRate", "0.00");
		SimProperties.setProperty("ForestationRate", "0.00");
		w = new World(null, "test", new SimpleWorldLogic<BasicAgent>(allActions));
		w.setCalendar(new FastCalendar(0l));
		testAP = new TestActionProcessor(w);
		hexMap = new HexMap<BasicHex>(10, 10, BasicHex.getHexFactory());
		setTerrainAtHex(hexMap, 2, 2, TerrainType.OCEAN);
		setTerrainAtHex(hexMap, 3, 2, TerrainType.OCEAN);
		setTerrainAtHex(hexMap, 4, 2, TerrainType.OCEAN);
		w.setLocationMap(hexMap);

		testAgent = new BasicAgent(w);
		testAgent.setLocation(hexMap.getHexAt(0, 0));

		testAgent.setDecider(new HardCodedDecider(BasicActions.FIND_WATER));
		startLocation = testAgent.getLocation();
	}


	@Test
	public void findWaterChooseableUnlessAdjacent() {
		assertFalse(BasicActions.FIND_WATER.isChooseable(testAgent)); // do not know of Water yet
		testAgent.getMapKnowledge().addLocationMap(hexMap);
		testAgent.getMapKnowledge().updateJourneyTrackers();
		assertTrue(BasicActions.FIND_WATER.isChooseable(testAgent)); // now know where it is
		testAgent.setLocation(hexMap.getHexAt(2, 1));
		assertFalse(BasicActions.FIND_WATER.isChooseable(testAgent));
	}

	@Test
	public void findTerrainChooseableUnlessAlreadyThere() {
		setTerrainAtHex(hexMap, 0, 0, TerrainType.FOREST);
		assertTrue(BasicActions.FIND_PLAINS.isChooseable(testAgent));
		assertFalse(BasicActions.FIND_FOREST.isChooseable(testAgent));

		setTerrainAtHex(hexMap, 0, 1, TerrainType.PLAINS);
		testAgent.setLocation(hexMap.getHexAt(0, 1));
		assertFalse(BasicActions.FIND_PLAINS.isChooseable(testAgent));
		assertTrue(BasicActions.FIND_FOREST.isChooseable(testAgent));
		assertTrue(BasicActions.FIND_CIVILISATION.isChooseable(testAgent));

		new Hut(testAgent);
		assertTrue(BasicActions.FIND_PLAINS.isChooseable(testAgent));
		assertTrue(BasicActions.FIND_CIVILISATION.isChooseable(testAgent));
		
		for (int loop=0; loop<8; loop++) {
			new Hut(testAgent);
		}
		assertTrue(BasicActions.FIND_CIVILISATION.isChooseable(testAgent));
		new Hut(testAgent);
		assertFalse(BasicActions.FIND_CIVILISATION.isChooseable(testAgent));
	}
	
	@Test
	public void findHutIsChooseableOnlyIfYouHaveOneButNotInCurrentHex() {
		assertFalse(BasicActions.FIND_HUT.isChooseable(testAgent));
		new Hut(testAgent);
		assertFalse(BasicActions.FIND_HUT.isChooseable(testAgent));
		testAgent.setLocation(hexMap.getHexAt(1, 1));
		assertTrue(BasicActions.FIND_HUT.isChooseable(testAgent));
		
		new Hut(testAgent);
		assertFalse(BasicActions.FIND_HUT.isChooseable(testAgent));
		new Village(hexMap.getHexAt(1, 1));
		assertFalse(BasicActions.FIND_HUT.isChooseable(testAgent));
		
		testAgent.setLocation(startLocation);
		assertFalse(BasicActions.FIND_HUT.isChooseable(testAgent));
	}
	
	@Test
	public void movesToDestinationWithFullKnowledge() {
		giveTestAgentFullKnowledge();
		Hex destination = hexMap.getHexAt(2, 2);

		testAP.makeValidateAndRunFirstDecision(testAgent, BasicMove.class);
		assertFalse(testAgent.getLocation().equals(startLocation)); // now should actually have moved one hex

		testAP.validateAndRunNextAction(BasicMove.class);
		testAP.validateAndRunNextAction(BasicMove.class);

		assertTrue(testAgent.getLocation().equals(destination));
	}
	
	@Test
	public void movingReducesHealthInLineWithMovementPointsSpent() {
		giveTestAgentFullKnowledge();
		double startingHealth = testAgent.getHealth();
		BasicAction a = new BasicMove(BasicActions.FIND_UNKNOWN ,testAgent, new LocationMatcher(hexMap.getHexAt(1, 1)));
		run(a);
		assertTrue(testAgent.getLocation() == hexMap.getHexAt(1, 1));
		assertEquals(startingHealth - testAgent.getHealth(), 1.0, 0.01);
		setTerrainAtHex(hexMap, 1, 2, TerrainType.SWAMP);
		a = new BasicMove(BasicActions.FIND_UNKNOWN ,testAgent, new LocationMatcher(hexMap.getHexAt(1, 2)));
		run(a);
		assertEquals(startingHealth - testAgent.getHealth(), 4.0, 0.01);	// an additional 3.0 to enter SWAMP
	}

	@Test
	public void moveToUnexploredLocationWithoutFullKnowledge() {	
		testAP.makeValidateAndRunFirstDecision(testAgent, BasicMove.class);
		assertFalse(testAgent.getLocation().equals(startLocation));
	}
	
	@Test
	public void knowsOfUnexploredLocationsDefaultsToTrue() {
		assertTrue(testAgent.hasUnexploredLocations());
	}
	
	@Test
	public void unexploredLocationsSetToFalseOnceMoveUnknownFails() {
		giveTestAgentFullKnowledge();
		assertTrue(testAgent.hasUnexploredLocations());
		testAgent.setDecider(new HardCodedDecider(BasicActions.FIND_UNKNOWN));
		testAP.makeValidateAndRunFirstDecision(testAgent, BasicMove.class);
		assertFalse(testAgent.hasUnexploredLocations());
	}
	
	@Test
	public void lackOfUnexploredLocationsMeansThatMoveUnknownIsNotChooseable() {
		testAgent.setHasUnexploredLocations(false);
		assertFalse(BasicActions.FIND_UNKNOWN.isChooseable(testAgent));
	}
	
	@Test
	public void unexploredLocationsSetToTrueIfANewOneIsFound() {
		testAgent.setHasUnexploredLocations(false);
		MapKnowledge map = testAgent.getMapKnowledge();
		map.addLocation(new Location());
		assertTrue(testAgent.hasUnexploredLocations());
	}

	@Test
	public void moveToOwnHut() {
		testAgent.setDecider(new HardCodedDecider(BasicActions.FIND_HUT));
		Location targetLocation = testAgent.getLocation();
		new Hut(testAgent);
		startLocation = hexMap.getHexAt(0, 2);
		testAgent.setLocation(startLocation);
		testAP.makeValidateAndRunFirstDecision(testAgent, BasicMove.class);
		assertFalse(testAgent.getLocation().equals(startLocation));
		testAP.validateAndRunNextAction(BasicMove.class);
		assertTrue(testAgent.getLocation().equals(targetLocation));
	}

	@Test
	public void useJourneyPlanIfItMatches() {
		giveTestAgentFullKnowledge();
		Hex destination = hexMap.getHexAt(2, 2);

		assertTrue(testAgent.getJourneyPlan() == null);
		testAP.makeValidateAndRunFirstDecision(testAgent, BasicMove.class);

		JourneyPlan journeyPlan = testAgent.getJourneyPlan();
		assertTrue(journeyPlan != null);

		assertTrue(journeyPlan.getLocationMatcher().equals(new TerrainMatcher(TerrainType.OCEAN)));
		assertTrue(journeyPlan.getDestination().equals(destination));

		testAP.validateAndRunNextAction(BasicMove.class);

		JourneyPlan journeyPlan2 = testAgent.getJourneyPlan();
		assertTrue(journeyPlan == journeyPlan2);
		assertTrue(journeyPlan2.getDestination().equals(destination));
	}

	@Test
	public void doNotUseJourneyPlanIfItDoesNotMatch() {
		setTerrainAtHex(hexMap, 0, 0, TerrainType.FOREST);
		giveTestAgentFullKnowledge();
		Hex destination = hexMap.getHexAt(2, 2);

		testAP.makeValidateAndRunFirstDecision(testAgent, BasicMove.class);
		JourneyPlan journeyPlan = testAgent.getJourneyPlan();
		assertTrue(journeyPlan != null);

		assertTrue(journeyPlan.getLocationMatcher().equals(new TerrainMatcher(TerrainType.OCEAN)));
		assertTrue(journeyPlan.getDestination().equals(destination));

		testAgent.setDecider(new HardCodedDecider(BasicActions.FIND_FOREST));
		testAP.validateAndRunNextAction(BasicMove.class);
		assertFalse(((Hex)testAgent.getLocation()).getTerrainType() == TerrainType.OCEAN);
		testAP.validateAndRunNextAction(BasicMove.class);
		// We need to run two actions...the first executes the FIND_OCEAN plan action form before the decider
		// was changed. Then the second executes FIND_FOREST and overrides the JourneyPlan
		JourneyPlan journeyPlan2 = testAgent.getJourneyPlan();
		assertFalse(journeyPlan == journeyPlan2);
		assertFalse(((Hex)testAgent.getLocation()).getTerrainType() == TerrainType.OCEAN);
		assertTrue(journeyPlan2.getLocationMatcher().equals(new TerrainMatcher(TerrainType.FOREST)));
		assertTrue(journeyPlan2.getDestination().equals(startLocation));
	}

	@Test
	public void journeyPlanUseImprovesPerformance() {
		hexMap.getHexAt(9,9).setTerrain(TerrainType.FOREST);
		Location destination = hexMap.getHexAt(9, 9);
		
		Long startTime = System.currentTimeMillis();
		for (int loop = 0; loop < 20; loop++) {
			resetAgentMapKnowledgeLocationAndActionQueue();
			agentMovesToDestinationWithJourneyPlan(destination);
		}
		Long interimTime = System.currentTimeMillis();

		for (int loop = 0; loop < 20; loop++) {
			resetAgentMapKnowledgeLocationAndActionQueue();
			agentMovesToDestinationWithoutJourneyPlan(destination);
		}

		Long endTime = System.currentTimeMillis();
		System.out.println(interimTime-startTime);
		System.out.println(endTime-interimTime);
		assertTrue(endTime-interimTime > (interimTime-startTime));
	}
	
	private void resetAgentMapKnowledgeLocationAndActionQueue() {
		testAgent.setLocation(startLocation);
		testAgent.setAge(1000);
		testAgent.setDecider(null);
		giveTestAgentFullKnowledge();
		testAP.clearQueue();
		testAgent.purgeActions(true);
		testAgent.setDecider(new HardCodedDecider(BasicActions.FIND_FOREST));
	}


	private void agentMovesToDestinationWithoutJourneyPlan(Location destination) {
		testAP.makeValidateAndRunFirstDecision(testAgent, BasicMove.class);
		while(testAgent.getLocation() != destination) {
			testAgent.setJourneyPlan(null);
			testAP.validateAndRunNextAction(BasicMove.class);
			testAgent.addHealth(5.0);
		}
	}

	private void agentMovesToDestinationWithJourneyPlan(Location destination) {
		testAP.makeValidateAndRunFirstDecision(testAgent, BasicMove.class);
		while(testAgent.getLocation() != destination) {
			testAP.validateAndRunNextAction(BasicMove.class);
			testAgent.addHealth(5.0);
		}
	}


	public static void setTerrainAtHex(HexMap<?> map, int row, int column, TerrainType terrain) {
		Hex hexToUpdate = map.getHexAt(row, column);
		hexToUpdate.setTerrain(terrain);
	}

	private void giveTestAgentFullKnowledge() {
		MapKnowledge knownLocations = testAgent.getMapKnowledge();
		knownLocations.addLocationMap(w.getLocationMap());
	}

	private void run(BasicAction a) {
		a.agree((BasicAgent) a.getActor());
		a.start();
		a.run();
	}
	@After
	public void cleanup() {
		testAP.stop();
	}
}
