package hopshackle.simulation.test;
import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;

import org.junit.*;
public class WildernessMatcherTest {
	private World w;
	private Hex[] locations;
	private Agent testAgent, builder;
	private ActionProcessor actionProcessor;
	
	@Before
	public void setUp() {
		actionProcessor = new ActionProcessor("test", false);
		w = new World();
		w.setActionProcessor(actionProcessor);
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
		makeValidateAndRunFirstDecision(BasicMove.class);
		getValidateAndRunNextAction(Move.class);
		JourneyPlan jPlan = testAgent.getJourneyPlan();
		assertTrue(jPlan.getLocationMatcher() instanceof WildernessMatcher);
		assertTrue(testAgent.getLocation() == locations[2]);
		
		getValidateAndRunNextAction(BasicMove.class);
		getValidateAndRunNextAction(Move.class);
		assertTrue(jPlan == testAgent.getJourneyPlan());
		assertTrue(testAgent.getLocation() == locations[1]);
		
		getValidateAndRunNextAction(BasicMove.class);
		assertTrue(jPlan == testAgent.getJourneyPlan());
		getValidateAndRunNextAction(Move.class);
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
	

	private void getValidateAndRunNextAction(Class<? extends Action> classType) {
		Action nextAction = actionProcessor.getNextUndeletedAction();
		validateAndRunAction(nextAction, classType);
	}

	private void makeValidateAndRunFirstDecision(Class<? extends Action> classType) {
		Action nextAction = testAgent.decide();
		validateAndRunAction(nextAction, classType);
	}

	private void validateAndRunAction(Action nextAction, Class<? extends Action> classType) {
		assertTrue(classType.isInstance(nextAction));
		nextAction.agree(nextAction.getActor());
		nextAction.start();
		nextAction.run();
	}
}
