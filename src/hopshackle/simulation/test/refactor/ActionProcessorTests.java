package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;

import java.util.*;

import hopshackle.simulation.*;
import hopshackle.simulation.basic.BasicAgent;
import hopshackle.simulation.test.TestActionProcessor;

import org.junit.*;

public class ActionProcessorTests {

	TestActionProcessor ap;
	World w;
	List<TestAgent> allAgents = new ArrayList<TestAgent>();
	TestActionFactory taf;
	TestAgent one, two, three;
	List<ActionEnum<TestAgent>> allActions = new ArrayList<ActionEnum<TestAgent>>(EnumSet.allOf(TestActionEnum.class));

	@Before
	public void setup() {
		TestAction.incrementTime = false;
		w = new World(null, "test", new SimpleWorldLogic<TestAgent>(allActions));
		w.setCalendar(new FastCalendar(0l));
		ap = new TestActionProcessor(w);
		for (int i = 0; i < 10; i++) {
			allAgents.add(new TestAgent(w));
		}
		taf = new TestActionFactory(allAgents);
		one = allAgents.get(0);
		two = allAgents.get(1);
		three = allAgents.get(2);
	}

	@Test
	public void actionIsQueuedInTwoStagesForStartAndRun() throws InterruptedException {
		Action<?> a = taf.factory(1, 0, 200, 1000);
		assertEquals(w.getCurrentTime().intValue(), 0);
		a.addToAllPlans();
		assertTrue(a.getState() == Action.State.PLANNED);
		ap.processOneStep();
		assertTrue(a.getState() == Action.State.EXECUTING);
		assertEquals(w.getCurrentTime().intValue(), 200);
		ap.processOneStep();
		assertTrue(a.getState() == Action.State.FINISHED);
		assertEquals(w.getCurrentTime().intValue(), 1200);
	}
	@Test
	public void afterActionHasBeenExecutedANewOneIsQueuedForStartTimeIfAgentPlanIsEmpty() throws InterruptedException {
		Action<?> a = taf.factory(1, 0, 200, 1000);
		assertEquals(w.getCurrentTime().intValue(), 0);
		a.addToAllPlans();
		assertTrue(a.getState() == Action.State.PLANNED);
		ap.processOneStep();
		assertTrue(a.getState() == Action.State.EXECUTING);
		assertEquals(w.getCurrentTime().intValue(), 200);
		assertTrue(one.getNextAction() == a);
		assertEquals(a.getStartTime(), 200);
		assertEquals(a.getEndTime(), 1200);
		ap.processOneStep();
		assertTrue(a.getState() == Action.State.FINISHED);
		assertEquals(w.getCurrentTime().intValue(), 1200);
		assertTrue(one.getNextAction() != null);
		assertTrue(one.getNextAction() != a);
		assertEquals(one.getNextAction().getStartTime(), 1200);
		assertEquals(one.getNextAction().getEndTime(), 2200);
	}
	@Test
	public void ifAgentQueueIsFullThenNoNewDecisionIsTaken() throws InterruptedException {
		Action<?> a = taf.factory(1, 0, 200, 1000);
		Action<?> b = taf.factory(1, 0, 1400, 1000);
		Action<?> c = taf.factory(1, 0, 2600, 1000);
		assertEquals(w.getCurrentTime().intValue(), 0);
		a.addToAllPlans();
		b.addToAllPlans();
		c.addToAllPlans();
		ap.processOneStep();
		assertTrue(a.getState() == Action.State.EXECUTING);
		assertTrue(b.getState() == Action.State.PLANNED);
		assertTrue(c.getState() == Action.State.PLANNED);
		assertEquals(one.getActionPlan().sizeOfQueue(), 3);
		assertEquals(one.getActionPlan().timeToNextActionStarts(), 0);
		assertEquals(one.getActionPlan().timeToEndOfQueue(), 3400);
		ap.processOneStep();
		assertTrue(a.getState() == Action.State.FINISHED);
		assertTrue(b.getState() == Action.State.PLANNED);
		assertTrue(c.getState() == Action.State.PLANNED);
		assertEquals(one.getActionPlan().sizeOfQueue(), 2);
		assertEquals(one.getActionPlan().timeToNextActionStarts(), 200);
		assertEquals(one.getActionPlan().timeToEndOfQueue(), 2400);
		assertEquals(w.getCurrentTime().intValue(), 1200);
		assertTrue(one.getNextAction() == b);
		ap.processOneStep();
		assertTrue(one.getNextAction() == b);
		assertEquals(w.getCurrentTime().intValue(), 1400);
		assertTrue(b.getState() == Action.State.EXECUTING);
		assertTrue(c.getState() == Action.State.PLANNED);
		assertEquals(one.getActionPlan().sizeOfQueue(), 2);
		assertEquals(one.getActionPlan().timeToEndOfQueue(), 2200);
	}

	@Test
	public void PROPOSEDActionIsCANCELLEDByActionProcessor() throws InterruptedException {
		Action<?> a = taf.factory(2, 0, 200, 1000);
		assertEquals(w.getCurrentTime().intValue(), 0);
		one.getActionPlan().addAction(a);
		w.addAction(a);
		assertTrue(a.getState() == Action.State.PROPOSED);
		ap.processOneStep();
		assertTrue(a.getState() == Action.State.CANCELLED);
		assertEquals(w.getCurrentTime().intValue(), 200);
		assertTrue(one.getNextAction() != a);
	}
	@Test
	public void FINISHEDActionIsIgnoredByActionProcessor() throws InterruptedException {
		Action<?> a = taf.factory(1, 0, 0, 1000);
		Action<?> b = taf.factory(1, 0, 1000, 1000);
		assertEquals(w.getCurrentTime().intValue(), 0);
		a.addToAllPlans();
		b.addToAllPlans();
		w.setCurrentTime(500l);
		ap.processOneStep();
		assertTrue(a.getState() == Action.State.EXECUTING);
		a.run();		// this FINISHES it outside ap framework
		assertTrue(a.getState() == Action.State.FINISHED);
		assertTrue(b.getState() == Action.State.PLANNED);
		assertEquals(w.getCurrentTime().intValue(), 500);
		assertTrue(one.getNextAction() == b);
		ap.processOneStep();
		assertTrue(a.getState() == Action.State.FINISHED);
		System.out.println(b.getState());
		assertTrue(b.getState() == Action.State.EXECUTING);
		assertEquals(w.getCurrentTime().intValue(), 1000);
		assertTrue(one.getNextAction() == b);

	}

	@After
	public void cleanup() {
		ap.stop();
		TestAction.incrementTime = true;
	}
}
