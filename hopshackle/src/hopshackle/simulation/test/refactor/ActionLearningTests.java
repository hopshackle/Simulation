package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;
import hopshackle.simulation.*;

import java.util.*;

import org.junit.*;

public class ActionLearningTests {

	
	World w;
	List<TestAgent> allAgents = new ArrayList<TestAgent>();
	TestActionFactory taf;
	TestAgent testAgent;
	Decider<TestAgent> decider;
	TestAgentTeacher teacher = new TestAgentTeacher();
	
	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		w = new World();
		for (int i = 0; i < 10; i++) {
			allAgents.add(new TestAgent(w));
		}
		taf = new TestActionFactory(allAgents);
		testAgent = allAgents.get(0);
	//	decider.setTeacher(teacher);
		decider = (Decider<TestAgent>) testAgent.getDecider();
	}
	
	public ExperienceRecord<TestAgent> getNewER() {
		 return new ExperienceRecord<TestAgent>(testAgent, TestDecider.gvList,
				 decider.getCurrentState(testAgent, testAgent), TestActionEnum.TEST.getAction(testAgent), 
					decider.getChooseableOptions(testAgent, testAgent), decider);
	}
	public ExperienceRecord<TestAgent> getNewER(TestAgent agent) {
		 return new ExperienceRecord<TestAgent>(agent, TestDecider.gvList,
				 decider.getCurrentState(agent, agent), TestActionEnum.TEST.getAction(agent), 
					decider.getChooseableOptions(agent, agent), decider);
	}
	public ExperienceRecord<TestAgent> getNewER(TestAction action) {
		 return new ExperienceRecord<TestAgent>(testAgent, TestDecider.gvList,
				 decider.getCurrentState(testAgent, testAgent), action, 
					decider.getChooseableOptions(testAgent, testAgent), decider);
	}
	
	@Test
	public void registerExperienceRecordUpdatesAgentAndActionStates() {
		ExperienceRecord<TestAgent> er = getNewER();
		assertFalse(teacher.agentKnown(testAgent));
		assertTrue(teacher.agentActionState(testAgent, er.getActionTaken()) == ExperienceRecord.State.UNSEEN);
		teacher.registerDecision(testAgent, er);
		assertTrue(teacher.agentKnown(testAgent));
		assertTrue(teacher.agentActionState(testAgent, er.getActionTaken()) == ExperienceRecord.State.DECISION_TAKEN);
	}

	@Test
	public void unseenandDTERForOtherAction() {
		ExperienceRecord<TestAgent> er1 = getNewER();
		ExperienceRecord<TestAgent> er2 = getNewER();
		ExperienceRecord<TestAgent> er3 = getNewER();
		teacher.registerDecision(testAgent, er1);
		assertTrue(teacher.agentActionState(testAgent, er1.getActionTaken()) == ExperienceRecord.State.DECISION_TAKEN);
		assertTrue(teacher.agentActionState(testAgent, er2.getActionTaken()) == ExperienceRecord.State.UNSEEN);
		assertTrue(teacher.agentActionState(testAgent, er3.getActionTaken()) == ExperienceRecord.State.UNSEEN);
		teacher.registerDecision(testAgent, er2);
		assertTrue(teacher.agentActionState(testAgent, er1.getActionTaken()) == ExperienceRecord.State.DECISION_TAKEN);
		assertTrue(teacher.agentActionState(testAgent, er2.getActionTaken()) == ExperienceRecord.State.DECISION_TAKEN);
		assertTrue(teacher.agentActionState(testAgent, er3.getActionTaken()) == ExperienceRecord.State.UNSEEN);
	}	
	
	@Test
	public void unseenEventAgentKnown() {
		TestAction oldAction = taf.factory(1, 0, 0, 1000);
		testAgent.getActionPlan().addAction(oldAction);
		oldAction.start();
		oldAction.run();
		// We set an old action in executed actions first
		List<ExperienceRecord<TestAgent>> allER = teacher.getExperienceRecords(testAgent);
		assertEquals(allER.size(), 0);
		
		ExperienceRecord<TestAgent> er1 = getNewER();
		ExperienceRecord<TestAgent> er2 = getNewER();
		ExperienceRecord<TestAgent> er3 = getNewER();
		teacher.registerDecision(testAgent, er1);
		allER = teacher.getExperienceRecords(testAgent);
		assertEquals(allER.size(), 1);
		testAgent.getActionPlan().actionCompleted(oldAction);
		dispatchLearningEvent(testAgent);
		// Should create a new ExperienceRecord with a status of ACTION_COMPLETED
		allER = teacher.getExperienceRecords(testAgent);
		assertEquals(allER.size(), 2);
		boolean foundAddedOne = false;
		for (ExperienceRecord<TestAgent> er : allER) {
			Action<TestAgent> a = er.getActionTaken();
			if (a.equals(er1.getActionTaken())) {
				assertTrue(teacher.agentActionState(testAgent, a) == ExperienceRecord.State.DECISION_TAKEN);
			} else if (a.equals(er2.getActionTaken())) {
				assertTrue(teacher.agentActionState(testAgent, a) == ExperienceRecord.State.UNSEEN);
			} else if (a.equals(er3.getActionTaken())) {
				assertTrue(teacher.agentActionState(testAgent, a) == ExperienceRecord.State.UNSEEN);
			} else { 
				// the added one
				foundAddedOne = true;
				assertTrue(teacher.agentActionState(testAgent, a) == ExperienceRecord.State.ACTION_COMPLETED);
			}
		}
		assertTrue(foundAddedOne);
	}	
	
	@Test
	public void unseenEventOtherActionAgentUnknown() {
		ExperienceRecord<TestAgent> er2 = getNewER();
		ExperienceRecord<TestAgent> er3 = getNewER();
		assertTrue(teacher.agentActionState(testAgent, er2.getActionTaken()) == ExperienceRecord.State.UNSEEN);
		testAgent.getActionPlan().addAction(er3.getActionTaken()); // this ensures ER3 action is the one referenced on Learning Event
		List<ExperienceRecord<TestAgent>> allER = teacher.getExperienceRecords(testAgent);
		assertEquals(allER.size(), 0);
		dispatchLearningEvent(testAgent);
		allER = teacher.getExperienceRecords(testAgent);
		assertEquals(allER.size(), 0);
		assertTrue(teacher.agentActionState(testAgent, er2.getActionTaken()) == ExperienceRecord.State.UNSEEN);
		assertTrue(teacher.agentActionState(testAgent, er3.getActionTaken()) == ExperienceRecord.State.UNSEEN);
	}	
	
	@Test
	public void unseenAndDTDeath() {
		ExperienceRecord<TestAgent> er1 = getNewER();
		ExperienceRecord<TestAgent> er2 = getNewER();
		teacher.registerDecision(testAgent, er1);
		assertTrue(teacher.agentActionState(testAgent, er1.getActionTaken()) == ExperienceRecord.State.DECISION_TAKEN);
		assertFalse(er1.isInFinalState());
		testAgent.die("Ooops");
		assertTrue(er1.getState() == ExperienceRecord.State.NEXT_ACTION_TAKEN);
		assertTrue(er1.isInFinalState());
		assertTrue(er2.getState() == ExperienceRecord.State.DECISION_TAKEN);
		assertTrue(teacher.agentActionState(testAgent, er2.getActionTaken()) == ExperienceRecord.State.UNSEEN);
	}		
	
	@Test
	public void decisionTakenEventThisAndOtherAction() {
		ExperienceRecord<TestAgent> er1 = getNewER();
		ExperienceRecord<TestAgent> er2 = getNewER();
		teacher.registerDecision(testAgent, er1);
		teacher.registerDecision(testAgent, er2);
		testAgent.getActionPlan().addAction(er1.getActionTaken());
		er1.getActionTaken().start();
		assertTrue(teacher.agentActionState(testAgent, er1.getActionTaken()) == ExperienceRecord.State.DECISION_TAKEN);
		assertTrue(teacher.agentActionState(testAgent, er2.getActionTaken()) == ExperienceRecord.State.DECISION_TAKEN);
		er1.getActionTaken().run();
		
		assertTrue(teacher.agentActionState(testAgent, er1.getActionTaken()) == ExperienceRecord.State.ACTION_COMPLETED);
		assertTrue(teacher.agentActionState(testAgent, er2.getActionTaken()) == ExperienceRecord.State.DECISION_TAKEN);
		assertTrue(er1.getEndState().length > 0);
		assertTrue(er2.getEndState() == null);
	}	
	
	@Test
	public void actionCompletedOtherER() {
		ExperienceRecord<TestAgent> er1 = getNewER();
		ExperienceRecord<TestAgent> er2 = getNewER();
		teacher.registerDecision(testAgent, er1);
		testAgent.getActionPlan().addAction(er1.getActionTaken());
		er1.getActionTaken().start();
		er1.getActionTaken().run();

		assertTrue(teacher.agentActionState(testAgent, er1.getActionTaken()) == ExperienceRecord.State.ACTION_COMPLETED);
		assertTrue(teacher.agentActionState(testAgent, er2.getActionTaken()) == ExperienceRecord.State.UNSEEN);
		assertTrue(er1.getPossibleActionsFromEndState() == null);
		assertTrue(er2.getPossibleActionsFromEndState() == null);
		teacher.registerDecision(testAgent, er2);
		assertTrue(teacher.agentActionState(testAgent, er1.getActionTaken()) == ExperienceRecord.State.UNSEEN);	
		// now removed from AgentTeacher, so UNSEEN in that context
		assertTrue(er1.getState() == ExperienceRecord.State.NEXT_ACTION_TAKEN);
		assertTrue(teacher.agentActionState(testAgent, er2.getActionTaken()) == ExperienceRecord.State.DECISION_TAKEN);
		assertTrue(er1.getPossibleActionsFromEndState().size() > 0);
		assertTrue(er2.getPossibleActionsFromEndState() == null);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void actionCompletedOtherEvent() {
		ExperienceRecord<TestAgent> er1 = getNewER();
		teacher.registerDecision(testAgent, er1);
		testAgent.getActionPlan().addAction(er1.getActionTaken());
		er1.getActionTaken().start();
		er1.getActionTaken().run();
		assertTrue(teacher.agentActionState(testAgent, er1.getActionTaken()) == ExperienceRecord.State.ACTION_COMPLETED);
		assertTrue(er1.getPossibleActionsFromEndState() == null);
		
		Action<TestAgent> nextAction = (Action<TestAgent>) testAgent.getActionPlan().getNextAction();
		nextAction.start();
		nextAction.run();
		assertTrue(er1.getState() == ExperienceRecord.State.NEXT_ACTION_TAKEN);
		assertTrue(er1.getPossibleActionsFromEndState().size() > 0);
	}

	@Test
	public void actionCompletedDeath() {
		ExperienceRecord<TestAgent> er1 = getNewER();
		teacher.registerDecision(testAgent, er1);
		testAgent.getActionPlan().addAction(er1.getActionTaken());
		er1.getActionTaken().start();
		er1.getActionTaken().run();
		assertTrue(teacher.agentActionState(testAgent, er1.getActionTaken()) == ExperienceRecord.State.ACTION_COMPLETED);
		assertTrue(er1.getPossibleActionsFromEndState() == null);
		
		testAgent.die("Ooops");
		assertTrue(er1.getState() == ExperienceRecord.State.NEXT_ACTION_TAKEN);
		assertTrue(er1.getPossibleActionsFromEndState().size() == 0);
		assertTrue(er1.isInFinalState());
	}
	
	@Test
	public void actionWithSeveralParticipantsGeneratesLearningEventForEachOnceExecuted() {
		decider.setTeacher(teacher);
		TestAction twoParticipants = taf.factory(1, 1, 0, 1000);
		twoParticipants.addToAllPlans();
		ExperienceRecord<TestAgent> er1 = getNewER(twoParticipants);
		ExperienceRecord<TestAgent> er2 = getNewER(allAgents.get(1));
		teacher.registerDecision(testAgent, er1);
		teacher.registerDecision(allAgents.get(1), er2);	// just to register second agent with teacher
		assertTrue(teacher.agentActionState(allAgents.get(1), twoParticipants) == ExperienceRecord.State.UNSEEN);
		twoParticipants.start();
		twoParticipants.run();
		assertTrue(teacher.agentActionState(testAgent, twoParticipants) == ExperienceRecord.State.UNSEEN);
		assertTrue(er1.getState() == ExperienceRecord.State.NEXT_ACTION_TAKEN);
		assertTrue(teacher.agentActionState(allAgents.get(1), twoParticipants) == ExperienceRecord.State.ACTION_COMPLETED);
	}

	private void dispatchLearningEvent(TestAgent agent) {
		AgentEvent learningEvent = new AgentEvent(agent, AgentEvents.DECISION_STEP_COMPLETE);
		agent.eventDispatch(learningEvent);
	}
}
