package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;

import java.util.*;

import hopshackle.simulation.*;

import org.junit.*;

public class ActionProcessorTests {
	
	ActionProcessor ap = new ActionProcessor("test", false);
	World w;
	List<TestAgent> allAgents = new ArrayList<TestAgent>();
	TestActionFactory taf;
	TestAgent one, two, three;
	
	@Before
	public void setup() {
		w = new World(ap, "test");
		w.setCalendar(new FastCalendar(0l));
		ap.start();
		for (int i = 0; i < 10; i++) {
			allAgents.add(new TestAgent(w));
		}
		taf = new TestActionFactory(allAgents);
		one = allAgents.get(0);
		two = allAgents.get(1);
		three = allAgents.get(2);
	}
	
	@Test
	public void addingAnActionQueuesItForPlannedStartTime() throws InterruptedException {
		Action a = taf.factory(1, 0, 200, 1000);
		assertEquals(w.getCurrentTime().intValue(), 0);
		synchronized (ap) {
			one.addAction(a);
			assertTrue(a.getState() == Action.State.PLANNED);
			ap.wait(1000);
			assertTrue(a.getState() == Action.State.EXECUTING);
			assertEquals(w.getCurrentTime().intValue(), 200);
			ap.wait(1000);
			assertTrue(a.getState() == Action.State.FINISHED);
			assertEquals(w.getCurrentTime().intValue(), 1200);
		}
	}
	@Test
	public void afterActionHasBeenStartedItIsQueuedForEndTime() {
		fail("Not yet implemented.");
	}
	@Test
	public void afterActionHasBeenExecutedANewOneIsQueuedForStartTimeIfAgentPlanIsEmpty() {
		fail("Not yet implemented.");
	}
	@Test
	public void ifAgentQueueIsFullThenNoNewDecisionIsTaken() {
		fail("Not yet implemented.");
	}
}
