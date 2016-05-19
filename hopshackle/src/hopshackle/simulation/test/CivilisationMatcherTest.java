package hopshackle.simulation.test;
import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;
import org.junit.*;
public class CivilisationMatcherTest {
	private World w;
	private Hex[] locations;
	private Agent testAgent, builder;
	private ActionProcessor ap;
	
	@Before
	public void setUp() {
		ap = new ActionProcessor("test", false);
		w = new World(ap, "test");
		w.setCalendar(new FastCalendar(0l));
		ap.start();
		ap.setTestingMode(true);
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
	public void civMatcherIsNotOverriddenWhenPassingThroughLowerCivLevelOnWayToDestination() throws InterruptedException {
		builder.setLocation(locations[2]);
		new Hut(builder);
		new Hut(builder);
		testAgent.setLocation(locations[2]);
		testAgent.setDecider(new HardCodedDecider(BasicActions.FIND_CIVILISATION));
		testAgent.updatePlan();
		synchronized (ap) {
			validateAndRunNextAction(BasicMove.class);
			JourneyPlan jPlan = testAgent.getJourneyPlan();
			assertTrue(jPlan.getLocationMatcher() instanceof CivilisationMatcher);
			assertTrue(testAgent.getLocation() == locations[3]);
			
			validateAndRunNextAction(BasicMove.class);
			assertTrue(jPlan == testAgent.getJourneyPlan());
			assertTrue(testAgent.getLocation() == locations[4]);
			
			assertTrue(jPlan == testAgent.getJourneyPlan());
			validateAndRunNextAction(BasicMove.class);
			assertTrue(testAgent.getLocation() == locations[5]);
			assertTrue(testAgent.getJourneyPlan() == null);
		}
	}
	
	private void validateAndRunNextAction(Class<? extends Action> classType) throws InterruptedException {
		Action next = ap.processNextAction();	//start
		assertTrue(classType.isInstance(next));
		ap.wait();
		next = ap.processNextAction();	// run
		ap.wait();
	}
}
