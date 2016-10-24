package hopshackle.simulation.test;
import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.basic.BasicAgent;

import org.junit.*;

public class JourneyPlanTest {

	private World w;
	private JourneyPlan jPlan;
	private Agent agent;
	private Location[] locations;
	private GoalMatcher locMatcher;

	@Before
	public void initialiseLocations() {
		w = new World();
		agent = new BasicAgent(w);
		MapKnowledge map = agent.getMapKnowledge();
		locations = new Location[5];

		for (int loop = 0; loop < 5; loop++) {
			locations[loop] = new Location(w);
			map.addLocation(locations[loop]);
		}
		for (int loop = 0; loop <4; loop++) {
			locations[loop].addAccessibleLocation(locations[loop+1]);
		}

		locMatcher = new GoalMatcher<Location>() {

			@Override
			public boolean supercedes(GoalMatcher<Location> competitor) {
				return false;
			}

			@Override
			public boolean matches(Location loc) {
				return (loc == locations[4]);
			}
		};

		agent.setLocation(locations[0]);
		jPlan = new JourneyPlan(agent, locMatcher, null);
		agent.setJourneyPlan(jPlan);
	}


	@Test
	public void nextMoveProvidedThroughJourney() {
		for (int loop = 1; loop < 5; loop++) {
			assertTrue(jPlan.getNextMove() == locations[loop]);
			agent.setLocation(locations[loop]);
		}
	}

	@Test
	public void nullReturnedIfNextMoveNotAccessible() {
		agent.setLocation(locations[4]);
		assertTrue(jPlan.getNextMove() == null);
	}

	@Test
	public void nullReturnedIfCurrentLocationNotOnJourneyRoute() {
		agent.setLocation(new Location(w));
		assertTrue(jPlan.getNextMove() == null);
	}

	@Test
	public void getDestinationReturnsExpectedResult() {
		assertTrue(jPlan.getDestination().equals(locations[4]));
	}

	@Test
	public void jouneyPlannerSetToNullWhenDestinationReached() {
		assertTrue(agent.getJourneyPlan() == jPlan);
		nextMoveProvidedThroughJourney();
		assertTrue(agent.getJourneyPlan() == null);
	}

	@Test
	public void distanceReturnedCorrectly() {
		assertEquals(jPlan.distance(), 4, 0.001);

		agent.setLocation(locations[1]);
		jPlan.updatePlan();
		assertEquals(jPlan.distance(), 3, 0.001);
	}

	@Test
	public void journeyPlanWithNoRouteToLocationMatcherReturnsMinusOneAsDistance() {
		agent.setLocation(new Location(w));
		jPlan.updatePlan();
		assertEquals(jPlan.distance(), -1, 0.001);
	}

	@Test
	public void journeyPlanTakesAccountOfTerrainandReturnsMovementPoints() {
		HexMap<Hex> hexMap = new HexMap<Hex>(3, 3, Hex.getHexFactory());
		for (int n = 0; n < 3; n++) 
			for (int m = 0; m < 3; m++)
				hexMap.getHexAt(n, m).setTerrain(TerrainType.HILLS);
		hexMap.getHexAt(1, 2).setTerrain(TerrainType.PLAINS);
		locMatcher = new TerrainMatcher(TerrainType.PLAINS);
		agent.setLocation(hexMap.getHexAt(0, 0));
		agent.getMapKnowledge().addLocationMap(hexMap);
		jPlan = new JourneyPlan(agent, locMatcher, new TerrainMovementCalculator());
		assertEquals(jPlan.distance(), 3.0, 0.001); // 1,1 is HILLS, then 1,2 is PLAINS
	}

	@Test
	public void journeyPlanDoesNotBecomeDefaultJourneyPlanOfAgentUnlessSetExplicitly() {
		assertTrue(jPlan.equals(agent.getJourneyPlan()));
		jPlan = new JourneyPlan(agent, locMatcher, null);
		assertTrue(!jPlan.equals(agent.getJourneyPlan()));
	}
}
