package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;

import java.util.*;

import hopshackle.simulation.*;
import hopshackle.simulation.test.refactor.TestActionFactory;

import org.junit.*;


public class ActionPlanTests {
	
	World w;
	List<TestAgent> allAgents = new ArrayList<TestAgent>();
	List<Agent> emptyList = new ArrayList<Agent>();
	TestActionFactory taf;
	Agent one, two, three;
	
	class TestActionPolicy extends Policy<Action> {
		Map<Action, Double> actionValues = new HashMap<Action, Double>();
		public TestActionPolicy(String name) {
			super(name);
		}
		@Override
		public double getValue(Action a, Agent p) {
			return actionValues.getOrDefault(a, 0.0);
		}
		public void setValue(Action a, double value) {
			actionValues.put(a, value);
		}
		
	}
	
	TestActionPolicy actionPolicy = new TestActionPolicy("action");
	
	@Before
	public void setup() {
		w = new World();
		for (int i = 0; i < 10; i++) {
			allAgents.add(new TestAgent(w));
			allAgents.get(i).setPolicy(actionPolicy);
		}
		taf = new TestActionFactory(allAgents);
		one = allAgents.get(0);
		two = allAgents.get(1);
		three = allAgents.get(2);
	}

	@Test
	public void overlappingLowPriorityActionIsRejected() {
		Action a = taf.factory(1, 0, 500, 1000);
		assertEquals(a.getAllConfirmedParticipants().size(), 0);
		one.addAction(a);
		
		Action b = taf.factory(1, 0, 500, 1000);
		one.addAction(b);
		assertEquals(b.getAllConfirmedParticipants().size(), 0);
		assertTrue(b.getState() == Action.State.CANCELLED);
		
		Action c = taf.factory(1, 0, 0, 1000);
		actionPolicy.setValue(c, -2.0);
		one.addAction(c);
		assertTrue(c.getState() == Action.State.CANCELLED);
	}
	@Test
	public void overlappingHighPriorityActionIsAccepted() {
		Action a = taf.factory(1, 0, 500, 1000);
		assertEquals(a.getAllConfirmedParticipants().size(), 0);
		one.addAction(a);
		
		Action b = taf.factory(1, 0, 500, 1000);
		actionPolicy.setValue(b, 2.0);
		one.addAction(b);
		assertEquals(b.getAllConfirmedParticipants().size(), 1);
		assertTrue(b.getState() == Action.State.PLANNED);
		assertEquals(a.getAllConfirmedParticipants().size(), 0);
		assertTrue(a.getState() == Action.State.CANCELLED);
		
		Action c = taf.factory(1, 0, 0, 1000);
		actionPolicy.setValue(c, 3.0);
		one.addAction(c);
		assertTrue(c.getState() == Action.State.PLANNED);
		assertTrue(b.getState() == Action.State.CANCELLED);
		assertTrue(a.getState() == Action.State.CANCELLED);
	}
	@Test
	public void nonOverlappingActionIsAccepted() {
		Action a = taf.factory(1, 0, 0, 1000);
		assertEquals(a.getAllConfirmedParticipants().size(), 0);
		one.addAction(a);
		assertEquals(a.getAllConfirmedParticipants().size(), 1);
		assertTrue(a.getState() == Action.State.PLANNED);
		
		Action b = taf.factory(1, 0, 2000, 1000);
		one.addAction(b);
		assertEquals(b.getAllConfirmedParticipants().size(), 1);
		assertTrue(b.getState() == Action.State.PLANNED);
		
		Action c = taf.factory(1, 0, 3000, 1000);
		one.addAction(c);
		assertTrue(c.getState() == Action.State.PLANNED);
	}
	@Test
	public void overLappingHighPriorityActionIsRejectedIfOverlapIsEXECUTING() {
		Action a = taf.factory(1, 0, 500, 1000);
		assertEquals(a.getAllConfirmedParticipants().size(), 0);
		one.addAction(a);
		a.start();
		assertTrue(a.getState() == Action.State.EXECUTING);
		
		Action b = taf.factory(1, 0, 500, 1000);
		actionPolicy.setValue(b, 2.0);
		one.addAction(b);
		assertEquals(b.getAllConfirmedParticipants().size(), 0);
		assertTrue(b.getState() == Action.State.CANCELLED);
		assertEquals(a.getAllConfirmedParticipants().size(), 1);
		assertTrue(a.getState() == Action.State.EXECUTING);
	}
	@Test
	public void runActionCorrectlyUpdatesTheExecutedActionListInActionPlan() {
		Action a = taf.factory(1, 0, 500, 1000);
		one.addAction(a);
		a.start();
		assertEquals(one.getExecutedActions().size(), 0);
		a.run();
		assertEquals(one.getExecutedActions().size(), 1);
		assertTrue(one.getExecutedActions().contains(a));
		assertTrue(a.getState() == Action.State.FINISHED);
		assertTrue(one.getNextAction() == null);
	}
	@Test
	public void cancellingAnActionRemovesItFromActionQueuesOfAllAgents() {
		Action a = taf.factory(2, 1, 500, 1000);
		one.addAction(a);
		three.addAction(a);
		assertTrue(a.getState() == Action.State.PROPOSED);
		assertTrue(one.getNextAction() == a);
		assertTrue(two.getNextAction() == null);
		assertTrue(three.getNextAction() == a);
		a.reject(two);
		assertTrue(a.getState() == Action.State.CANCELLED);
		assertTrue(one.getNextAction() == null);
		assertTrue(two.getNextAction() == null);
		assertTrue(three.getNextAction() == null);
		assertEquals(one.getExecutedActions().size(), 0);
		assertEquals(two.getExecutedActions().size(), 0);
		assertEquals(three.getExecutedActions().size(), 0);
	}
	@Test
	public void purgeActionsRemovesAllPROPOSEDAndPLANNEDActionsFromQueue() {
		Action a = taf.factory(2, 1, 500, 1000);
		Action b = taf.factory(1, 0, 1500, 1000);
		Action c = taf.factory(1, 1, 2500, 1000);
		one.addAction(a);
		one.addAction(b);
		one.addAction(c);
		b.start();
		assertEquals(one.getExecutedActions().size(), 0);
		assertTrue(one.getNextAction() == a);
		one.purgeActions();
		assertEquals(one.getExecutedActions().size(), 0);
		assertTrue(one.getNextAction() == b);
	}
	@Test
	public void purgeActionsCancelsMandatoryActionsOnly() {
		Action a = taf.factory(2, 1, 500, 1000);
		Action b = taf.factory(1, 0, 1500, 1000);
		Action c = taf.factory(1, 1, 2500, 1000);
		Action d = taf.factory(4, 0, 3500, 1000);
		one.addAction(a);
		one.addAction(b);
		one.addAction(c);
		one.addAction(d);
		two.addAction(a);
		two.addAction(c);
		two.addAction(d);
		assertTrue(a.getState() == Action.State.PLANNED);
		assertTrue(b.getState() == Action.State.PLANNED);
		assertTrue(c.getState() == Action.State.PLANNED);
		assertTrue(d.getState() == Action.State.PROPOSED);
		two.purgeActions();
		assertTrue(a.getState() == Action.State.CANCELLED);
		assertTrue(b.getState() == Action.State.PLANNED);
		assertTrue(c.getState() == Action.State.PLANNED);
		assertTrue(d.getState() == Action.State.CANCELLED);
		assertEquals(two.getExecutedActions().size(), 0);
		assertTrue(two.getNextAction() == null);
	}
	
	@Test
	public void decisionRequiredIfThereIsAGapUpToSpecifiedTime() {
		assertTrue(one.getActionPlan().requiresDecision());
		Action a = taf.factory(1, 0, 0, 1000);
		one.addAction(a);
		assertFalse(one.getActionPlan().requiresDecision());
		a.cancel();
		assertTrue(one.getActionPlan().requiresDecision());
		Action b = taf.factory(1, 0, 500, 1000);
		one.addAction(b);
		assertTrue(one.getActionPlan().requiresDecision(250));
		assertFalse(one.getActionPlan().requiresDecision(1000));
	}
}
