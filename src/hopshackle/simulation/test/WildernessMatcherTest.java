package hopshackle.simulation.test;
import static org.junit.Assert.*;

import java.util.*;

import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;

import org.junit.*;
public class WildernessMatcherTest {
	private World w;
	private Hex[] locations;
	private Agent testAgent, builder;
	private TestActionProcessor ap;
	private List<ActionEnum<BasicAgent>> allActions = new ArrayList<ActionEnum<BasicAgent>>(EnumSet.allOf(BasicActions.class));

	
	@Before
	public void setUp() {
		w = new World(null, "test", new SimpleWorldLogic<BasicAgent>(allActions));
		w.setCalendar(new FastCalendar(0l));
		ap = new TestActionProcessor(w);
		testAgent = new BasicAgent(w);
		builder = new BasicAgent(w);
		locations = new Hex[11];
		for (int loop = 0; loop<11; loop++) {
			locations[loop] = BasicHex.getHexFactory().getHex(0, loop);
			locations[loop].setParentLocation(w);
			testAgent.setLocation(locations[loop]);
			if (loop > 0) {
				locations[loop].addAccessibleLocation(locations[loop-1]);
				locations[loop-1].addAccessibleLocation(locations[loop]);
			}
			builder.setLocation(locations[loop]);
			for (int hutLoop = 0; hutLoop < loop; hutLoop++) {
				new Hut(builder);
			}
		}
	}

	@Test
	public void wildernessMatcherReturnsTrueOnlyForLowerCivLevels() {
		for (int loop = 0; loop < 11; loop++) {
			testAgent.setLocation(locations[loop]);
			WildernessMatcher wm = new WildernessMatcher(testAgent);
			for (int innerLoop = 10; innerLoop >= 0; innerLoop--) {
				boolean expectedResult = false;
				if (innerLoop < loop)
					expectedResult = true;
				assertEquals(wm.matches(locations[innerLoop]), expectedResult);
			}
		}
	}

	@Test
	public void wildernessMatcherIsNotOverriddenWhenPassingThroughHigherCivLevelOnWayToDestination() {
		builder.setLocation(locations[3]);
		BasicHex bh = (BasicHex) locations[3];
		bh.getHuts().get(0).destroy();
		bh.getHuts().get(0).destroy();
		testAgent.setLocation(locations[3]);
		testAgent.setDecider(new HardCodedDecider(BasicActions.FIND_PLAINS));
		ap.makeValidateAndRunFirstDecision(testAgent, BasicMove.class);
		JourneyPlan jPlan = testAgent.getJourneyPlan();
		assertTrue(jPlan.getLocationMatcher() instanceof WildernessMatcher);
		assertTrue(testAgent.getLocation() == locations[2]);
		
		ap.validateAndRunNextAction(BasicMove.class);
		assertTrue(jPlan == testAgent.getJourneyPlan());
		assertTrue(testAgent.getLocation() == locations[1]);
		
		ap.validateAndRunNextAction(BasicMove.class);
		assertTrue(testAgent.getLocation() == locations[0]);
		assertTrue(testAgent.getJourneyPlan() == null);
	}
	
	@Test
	public void wildernessMatcherOnlyMatchesOnPlains() {
		testAgent.setLocation(locations[1]);
		locations[0].setTerrain(TerrainType.DESERT);
		WildernessMatcher wm = new WildernessMatcher(testAgent);
		assertFalse(wm.matches(locations[0]));
		
		locations[0].setTerrain(TerrainType.FOREST);
		wm = new WildernessMatcher(testAgent);
		assertFalse(wm.matches(locations[0]));
		
		locations[0].setTerrain(TerrainType.PLAINS);
		wm = new WildernessMatcher(testAgent);
		assertTrue(wm.matches(locations[0]));
	}
	
	@Test
	public void wildernessMatcherOriginatingInForestMatchesWithPlainsRegardlessOfCivLevel() {
		testAgent.setLocation(locations[0]);
		locations[0].setTerrain(TerrainType.FOREST);
		WildernessMatcher wm = new WildernessMatcher(testAgent);
		assertFalse(wm.matches(locations[0]));
		assertTrue(wm.matches(locations[3]));
	}
	@After
	public void cleanup() {
		ap.stop();
	}
}
