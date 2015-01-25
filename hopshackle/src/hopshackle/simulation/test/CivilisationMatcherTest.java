package hopshackle.simulation.test;
import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;

import org.junit.*;
public class CivilisationMatcherTest {
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
	public void civMatcherReturnsTrueOnlyForGreaterCivLevels() {
		for (int loop = 0; loop < 11; loop++) {
			testAgent.setLocation(locations[loop]);
			CivilisationMatcher cm = new CivilisationMatcher(testAgent);
			for (int innerLoop = 0; innerLoop < 11; innerLoop++) {
				boolean expectedResult = false;
				if (innerLoop > loop)
					expectedResult = true;
				assertEquals(cm.matches(locations[innerLoop]), expectedResult);
			}
		}
	}

	@Test
	public void civMatcherIsNotOverriddenWhenPassingThroughLowerCivLevelOnWayToDestination() {
		builder.setLocation(locations[2]);
		new Hut(builder);
		new Hut(builder);
		testAgent.setLocation(locations[2]);
		testAgent.setDecider(new HardCodedDecider(BasicActions.FIND_CIVILISATION));
		makeValidateAndRunFirstDecision(BasicMove.class);
		getValidateAndRunNextAction(Move.class);
		JourneyPlan jPlan = testAgent.getJourneyPlan();
		assertTrue(jPlan.getLocationMatcher() instanceof CivilisationMatcher);
		assertTrue(testAgent.getLocation() == locations[3]);
		
		getValidateAndRunNextAction(BasicMove.class);
		getValidateAndRunNextAction(Move.class);
		assertTrue(jPlan == testAgent.getJourneyPlan());
		assertTrue(testAgent.getLocation() == locations[4]);
		
		getValidateAndRunNextAction(BasicMove.class);
		assertTrue(jPlan == testAgent.getJourneyPlan());
		getValidateAndRunNextAction(Move.class);
		assertTrue(testAgent.getLocation() == locations[5]);
		assertTrue(testAgent.getJourneyPlan() == null);
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
		nextAction.run();
	}
}
