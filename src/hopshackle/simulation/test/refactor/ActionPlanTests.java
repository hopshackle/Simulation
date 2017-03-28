package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;

import java.util.*;

import hopshackle.simulation.*;
import hopshackle.simulation.test.refactor.TestActionFactory;

import org.junit.*;


public class ActionPlanTests {
	
	static List<ActionEnum<TestAgent>> actionList = new ArrayList<ActionEnum<TestAgent>> (EnumSet.allOf(TestActionEnum.class));
	World w;
	List<TestAgent> allAgents = new ArrayList<TestAgent>();
	List<Agent> emptyList = new ArrayList<Agent>();
	TestActionFactory taf;
	TestAgent one, two, three;
	
	TestActionPolicy actionPolicy = new TestActionPolicy("action");
	
	@Before
	public void setup() {
		w = new World(new SimpleWorldLogic<TestAgent>(actionList));
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
		TestAction a = taf.factory(1, 0, 500, 1000);
		assertEquals(a.getAllConfirmedParticipants().size(), 0);
		a.addToAllPlans();
		
		TestAction b = taf.factory(1, 0, 500, 1000);
		b.addToAllPlans();
		assertEquals(b.getAllConfirmedParticipants().size(), 0);
		assertTrue(b.getState() == Action.State.CANCELLED);
		
		TestAction c = taf.factory(1, 0, 0, 1000);
		actionPolicy.setValue(c, -2.0);
		c.addToAllPlans();
		assertTrue(c.getState() == Action.State.CANCELLED);
	}
	@Test
	public void overlappingHighPriorityActionIsAccepted() {
		TestAction a = taf.factory(1, 0, 500, 1000);
		assertEquals(a.getAllConfirmedParticipants().size(), 0);
		a.addToAllPlans();
		
		TestAction b = taf.factory(1, 0, 500, 1000);
		actionPolicy.setValue(b, 2.0);
		b.addToAllPlans();
		assertEquals(b.getAllConfirmedParticipants().size(), 1);
		assertTrue(b.getState() == Action.State.PLANNED);
		assertEquals(a.getAllConfirmedParticipants().size(), 0);
		assertTrue(a.getState() == Action.State.CANCELLED);
		
		TestAction c = taf.factory(1, 0, 0, 1000);
		actionPolicy.setValue(c, 3.0);
		c.addToAllPlans();
		assertTrue(c.getState() == Action.State.PLANNED);
		assertTrue(b.getState() == Action.State.CANCELLED);
		assertTrue(a.getState() == Action.State.CANCELLED);
	}
	@Test
	public void nonOverlappingActionIsAccepted() {
		TestAction a = taf.factory(1, 0, 0, 1000);
		assertEquals(a.getAllConfirmedParticipants().size(), 0);
		a.addToAllPlans();
		assertEquals(a.getAllConfirmedParticipants().size(), 1);
		assertTrue(a.getState() == Action.State.PLANNED);
		
		TestAction b = taf.factory(1, 0, 2000, 1000);
		b.addToAllPlans();
		assertEquals(b.getAllConfirmedParticipants().size(), 1);
		assertTrue(b.getState() == Action.State.PLANNED);
		
		TestAction c = taf.factory(1, 0, 3000, 1000);
		c.addToAllPlans();
		assertTrue(c.getState() == Action.State.PLANNED);
	}
	@Test
	public void overLappingHighPriorityActionIsRejectedIfOverlapIsEXECUTING() {
		TestAction a = taf.factory(1, 0, 500, 1000);
		assertEquals(a.getAllConfirmedParticipants().size(), 0);
		a.addToAllPlans();
		a.start();
		assertTrue(a.getState() == Action.State.EXECUTING);
		
		TestAction b = taf.factory(1, 0, 500, 1000);
		actionPolicy.setValue(b, 2.0);
		b.addToAllPlans();
		assertEquals(b.getAllConfirmedParticipants().size(), 0);
		assertTrue(b.getState() == Action.State.CANCELLED);
		assertEquals(a.getAllConfirmedParticipants().size(), 1);
		assertTrue(a.getState() == Action.State.EXECUTING);
	}
	@Test
	public void runActionCorrectlyUpdatesTheExecutedActionListInActionPlan() {
		one.setDecider(null);
		TestAction a = taf.factory(1, 0, 500, 1000);
		a.addToAllPlans();
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
		TestAction a = taf.factory(2, 1, 500, 1000);
		one.getActionPlan().addAction(a);
		three.getActionPlan().addAction(a);
		assertTrue(a.getState() == Action.State.PROPOSED);
		assertTrue(one.getNextAction() == a);
		assertTrue(two.getNextAction() == null);
		assertTrue(three.getNextAction() == a);
		a.reject(two);
		assertTrue(a.getState() == Action.State.CANCELLED);
		assertTrue(one.getNextAction() != a);
		assertTrue(two.getNextAction() != a);
		assertTrue(three.getNextAction() != a);
		assertEquals(one.getExecutedActions().size(), 0);
		assertEquals(two.getExecutedActions().size(), 0);
		assertEquals(three.getExecutedActions().size(), 0);
	}
	
	@Test
	public void cancelledActionDoesNotAffectOverlapCalculations() {
		one.setDecider(new HardCodedDecider<TestAgent>(TestActionEnum.LEFT));
		TestAction a = taf.factory(2, 1, 0, 1000);
		a.addToAllPlans();
		assertTrue(one.getNextAction().getType() == TestActionEnum.TEST);
		a.reject(two);
		assertTrue(one.getNextAction().getType() == TestActionEnum.LEFT);
	}
	
	@Test
	public void purgeActionsRemovesAllPROPOSEDAndPLANNEDActionsFromQueue() {
		TestAction a = taf.factory(2, 1, 500, 1000);
		TestAction b = taf.factory(1, 0, 1500, 1000);
		TestAction c = taf.factory(1, 1, 2500, 1000);
		one.getActionPlan().addAction(a);
		one.getActionPlan().addAction(b);
		one.getActionPlan().addAction(c);
		b.start();
		assertEquals(one.getExecutedActions().size(), 0);
		assertTrue(one.getNextAction() == a);
		one.purgeActions(false);
		assertEquals(one.getExecutedActions().size(), 0);
		assertTrue(one.getNextAction() == b);
	}
	@Test
	public void purgeActionsCanOverrideEXECUTINGActions() {
		TestAction a = taf.factory(2, 1, 500, 1000);
		TestAction b = taf.factory(1, 0, 1500, 1000);
		one.getActionPlan().addAction(a);
		one.getActionPlan().addAction(b);
		b.start();
		assertTrue(a.getState() == Action.State.PROPOSED);
		assertTrue(b.getState() == Action.State.EXECUTING);
		one.purgeActions(false);
		assertTrue(a.getState() == Action.State.CANCELLED);
		assertTrue(b.getState() == Action.State.EXECUTING);
		one.purgeActions(true);
		assertTrue(a.getState() == Action.State.CANCELLED);
		assertTrue(b.getState() == Action.State.CANCELLED);
		assertTrue(one.getNextAction() != b);
	}
	@Test
	public void purgeActionsCancelsMandatoryActionsOnly() {
		TestAction a = taf.factory(2, 1, 500, 1000);
		TestAction b = taf.factory(1, 0, 1500, 1000);
		TestAction c = taf.factory(1, 1, 2500, 1000);
		TestAction d = taf.factory(4, 0, 3500, 1000);
		one.getActionPlan().addAction(a);
		one.getActionPlan().addAction(b);
		one.getActionPlan().addAction(c);
		one.getActionPlan().addAction(d);
		two.getActionPlan().addAction(a);
		two.getActionPlan().addAction(c);
		two.getActionPlan().addAction(d);
		assertTrue(a.getState() == Action.State.PLANNED);
		assertTrue(b.getState() == Action.State.PLANNED);
		assertTrue(c.getState() == Action.State.PLANNED);
		assertTrue(d.getState() == Action.State.PROPOSED);
		two.purgeActions(false);
		assertTrue(a.getState() == Action.State.CANCELLED);
		assertTrue(b.getState() == Action.State.PLANNED);
		assertTrue(c.getState() == Action.State.PLANNED);
		assertTrue(d.getState() == Action.State.CANCELLED);
		assertEquals(two.getExecutedActions().size(), 0);
	}
	
	@Test
	public void decisionRequiredIfThereIsAGapUpToSpecifiedTime() {
		one.setDecider(null);
		assertEquals(one.getActionPlan().timeToNextActionStarts(), Long.MAX_VALUE);
		TestAction a = taf.factory(1, 0, 0, 1000);
		one.getActionPlan().addAction(a);
		assertEquals(one.getActionPlan().timeToNextActionStarts(), 0);
		a.cancel();
		assertEquals(one.getActionPlan().timeToNextActionStarts(), Long.MAX_VALUE);
		TestAction b = taf.factory(1, 0, 500, 1000);
		one.getActionPlan().addAction(b);
		assertEquals(one.getActionPlan().timeToNextActionStarts(), 500);
	}
	
	@Test
	public void agentMakesDecisionIfGapIsLargeEnough() {
		TestAction a = taf.factory(1, 1, 1000, 1000);
		one.getActionPlan().addAction(a);
		assertEquals(one.getActionPlan().sizeOfQueue(), 1);
		one.decide();
		assertEquals(one.getActionPlan().sizeOfQueue(), 2);
		one.decide();
		assertEquals(one.getActionPlan().sizeOfQueue(), 2);
	}
	@Test
	public void agentMakesNoDecisionIfGapIsNotLargeEnough() {
		TestAction a = taf.factory(1, 1, 1000, 1000);
		TestAction b = taf.factory(1, 1, 200, 500);
		one.getActionPlan().addAction(a);
		one.getActionPlan().addAction(b);
		assertEquals(one.getActionPlan().sizeOfQueue(), 2);
		one.decide();
		assertEquals(one.getActionPlan().sizeOfQueue(), 2);
	}
	
	@Test
	public void addingActionOnceForAgentAddsForAllParticipants() {
		TestAction a = taf.factory(1, 1, 1000, 1000);
		assertEquals(one.getActionPlan().timeToEndOfQueue(), 0);
		assertEquals(two.getActionPlan().timeToEndOfQueue(), 0);
		a.addToAllPlans();
		assertEquals(one.getActionPlan().timeToEndOfQueue(), 2000);
		assertEquals(two.getActionPlan().timeToEndOfQueue(), 2000);
	}
	@Test
	public void addingActionAgainDoesNotCauseItToBeCancelled() {
		TestAction a = taf.factory(1, 1, 1000, 1000);
		a.addToAllPlans();
		assertTrue(a.getState() == Action.State.PLANNED);
		assertEquals(one.getActionPlan().timeToEndOfQueue(), 2000);
		assertEquals(two.getActionPlan().timeToEndOfQueue(), 2000);
		a.addToAllPlans();
		one.getActionPlan().addAction(a);
		assertTrue(a.getState() == Action.State.PLANNED);
		assertEquals(one.getActionPlan().timeToEndOfQueue(), 2000);
		assertEquals(two.getActionPlan().timeToEndOfQueue(), 2000);
	}
	@Test
	public void timeUntilAllAvailableReturnsMaxEndQueueTime() {
		assertEquals(ActionPlan.timeUntilAllAvailable(allAgents), 0);
		TestAction a = taf.factory(1, 1, 0, 1000);
		a.addToAllPlans();
		assertEquals(ActionPlan.timeUntilAllAvailable(allAgents), 1000);
		TestAction b = taf.factory(1, 0, 3000, 3000);
		b.addToAllPlans();
		assertEquals(ActionPlan.timeUntilAllAvailable(allAgents), 6000);
	}
}
