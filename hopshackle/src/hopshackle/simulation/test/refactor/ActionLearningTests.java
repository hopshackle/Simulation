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
		teacher.registerAgent(testAgent);
		decider = (Decider<TestAgent>) testAgent.getDecider();
	}
	
	@Test
	public void registerExperienceRecordUpdatesAgentAndActionStates() {
		TestAgent otherAgent = allAgents.get(1);
		assertFalse(teacher.agentKnown(otherAgent));
		teacher.registerAgent(otherAgent);
		assertTrue(teacher.agentKnown(otherAgent));
		otherAgent.decide();
		TestAction actionTaken = (TestAction) otherAgent.getNextAction();
		assertTrue(teacher.agentActionState(otherAgent, actionTaken) == ExperienceRecord.State.DECISION_TAKEN);
	}

	@Test
	public void unseenandDTERForOtherAction() {
		testAgent.decide();
		TestAction actionTaken = (TestAction) testAgent.getNextAction();
		assertTrue(teacher.agentActionState(testAgent, actionTaken) == ExperienceRecord.State.DECISION_TAKEN);
		TestAction forwardAction = taf.factory(1, 0, 2000, 1000);
		testAgent.getActionPlan().addAction(forwardAction);
		assertTrue(teacher.agentActionState(testAgent, actionTaken) == ExperienceRecord.State.DECISION_TAKEN);
		assertTrue(teacher.agentActionState(testAgent, forwardAction) == ExperienceRecord.State.DECISION_TAKEN);
	}	
	
	@Test
	public void unseenAgreementAgentKnown() {
		List<ExperienceRecord<TestAgent>> allER = teacher.getExperienceRecords(testAgent);
		assertEquals(allER.size(), 0);
		teacher.registerAgent(allAgents.get(1));

		TestAction unknownAction = taf.factory(2, 0, 0, 1000);
		unknownAction.addToAllPlans();
		// will trigger Agree on all participants
		allER = teacher.getExperienceRecords(testAgent);
		assertEquals(allER.size(), 1);
		allER.addAll(teacher.getExperienceRecords(allAgents.get(1)));
		assertEquals(allER.size(), 2);

		for (ExperienceRecord<TestAgent> er : allER) {
			Action<TestAgent> a = er.getActionTaken();
			if (a.equals(unknownAction)) {
				assertTrue(teacher.agentActionState(testAgent, a) == ExperienceRecord.State.DECISION_TAKEN);
			} 
		}
	}	
	
	@Test
	public void unseenEventAgentKnown() {
		List<ExperienceRecord<TestAgent>> allER = teacher.getExperienceRecords(testAgent);
		assertEquals(allER.size(), 0);
		teacher.registerAgent(allAgents.get(1));

		TestAction unknownAction = taf.factory(2, 0, 0, 1000);
		dispatchLearningEvent(testAgent, unknownAction);
		dispatchLearningEvent(allAgents.get(1), unknownAction);

		allER = teacher.getExperienceRecords(testAgent);
		assertEquals(allER.size(), 0);
		allER.addAll(teacher.getExperienceRecords(allAgents.get(1)));
		assertEquals(allER.size(), 0);
	}
	
	@Test
	public void unseenAgreementAgentUnknown() {
		List<ExperienceRecord<TestAgent>> allER = teacher.getExperienceRecords(testAgent);
		assertEquals(allER.size(), 0);

		TestAction unknownAction = taf.factory(2, 0, 0, 1000);
		unknownAction.addToAllPlans();
		// will trigger Agree on all participants, but only one is known
		allER = teacher.getExperienceRecords(testAgent);
		assertEquals(allER.size(), 1);
		allER.addAll(teacher.getExperienceRecords(allAgents.get(1)));
		assertEquals(allER.size(), 1);
		
		for (ExperienceRecord<TestAgent> er : allER) {
			Action<TestAgent> a = er.getActionTaken();
			if (a.equals(unknownAction)) {
				assertTrue(teacher.agentActionState(testAgent, a) == ExperienceRecord.State.DECISION_TAKEN);
			} 
		}
	}	
	
	@Test
	public void DTDeath() {
		testAgent.decide();
		TestAction actionTaken = (TestAction) testAgent.getActionPlan().getNextAction();
		assertTrue(teacher.agentActionState(testAgent, actionTaken) == ExperienceRecord.State.DECISION_TAKEN);
		ExperienceRecord<TestAgent> er1 = teacher.getExperienceRecords(testAgent).get(0);
		assertFalse(er1.isInFinalState());
		TestAction forwardAction = taf.factory(1, 0, 2000, 1000);
		forwardAction.addToAllPlans();
		ExperienceRecord<TestAgent> er2 = teacher.getExperienceRecords(testAgent).get(1);
		assertTrue(er2.getActionTaken().equals(forwardAction));
		assertTrue(er1.getState() == ExperienceRecord.State.DECISION_TAKEN);
		testAgent.die("Ooops");
		assertTrue(er1.isInFinalState());
		assertTrue(er1.getState() == ExperienceRecord.State.NEXT_ACTION_TAKEN);
		assertTrue(er2.isInFinalState());
		assertTrue(er2.getState() == ExperienceRecord.State.NEXT_ACTION_TAKEN);
	}		
	
	@Test
	public void decisionTakenEventThisAndOtherAction() {
		testAgent.decide();
		TestAction actionTaken = (TestAction) testAgent.getActionPlan().getNextAction();
		TestAction forwardAction = taf.factory(1, 0, 2000, 1000);
		forwardAction.addToAllPlans();
		actionTaken.start();
		assertTrue(teacher.agentActionState(testAgent, actionTaken) == ExperienceRecord.State.DECISION_TAKEN);
		assertTrue(teacher.agentActionState(testAgent, forwardAction) == ExperienceRecord.State.DECISION_TAKEN);
		ExperienceRecord<TestAgent> er1 = teacher.getExperienceRecords(testAgent).get(0);
		ExperienceRecord<TestAgent> er2 = teacher.getExperienceRecords(testAgent).get(1);
		actionTaken.run();
		
		assertTrue(er1.getState() == ExperienceRecord.State.NEXT_ACTION_TAKEN);
		assertTrue(teacher.agentActionState(testAgent, forwardAction) == ExperienceRecord.State.DECISION_TAKEN);
		assertTrue(er1.getEndState().length > 0);
		assertTrue(er2.getEndState() == null);
	}	
	
	@Test
	public void actionCompletedOtherER() {
		testAgent.decide();
		TestAction forwardAction = taf.factory(1, 0, 1000, 1000);
		forwardAction.addToAllPlans();
		ExperienceRecord<TestAgent> er1 = teacher.getExperienceRecords(testAgent).get(0);
		ExperienceRecord<TestAgent> er2 = teacher.getExperienceRecords(testAgent).get(1);
		assertTrue(er2.getActionTaken().equals(forwardAction));
		er1.getActionTaken().start();
		testAgent.setDecider(null);
		er1.getActionTaken().run();

		assertTrue(teacher.agentActionState(testAgent, er1.getActionTaken()) == ExperienceRecord.State.ACTION_COMPLETED);
		assertTrue(teacher.agentActionState(testAgent, er2.getActionTaken()) == ExperienceRecord.State.DECISION_TAKEN);
		assertTrue(er1.getPossibleActionsFromEndState() == null);
		assertTrue(er2.getPossibleActionsFromEndState() == null);
		
		testAgent.setDecider(decider);
		testAgent.decide();		// this will now make a new decision...which should close off the completed action
		
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
		testAgent.decide();
		TestAction actionTaken = (TestAction) testAgent.getActionPlan().getNextAction();
		ExperienceRecord<TestAgent> er1 = teacher.getExperienceRecords(testAgent).get(0);
		actionTaken.start();
		testAgent.setDecider(null);
		actionTaken.run();
		assertTrue(teacher.agentActionState(testAgent, er1.getActionTaken()) == ExperienceRecord.State.ACTION_COMPLETED);
		assertTrue(er1.getPossibleActionsFromEndState() == null);
		testAgent.setDecider(decider);
		dispatchLearningEvent(testAgent, taf.factory(1, 0, 0, 1000));
		assertTrue(er1.getState() == ExperienceRecord.State.NEXT_ACTION_TAKEN);
		assertEquals(er1.getPossibleActionsFromEndState().size(), 1);
	}

	@Test
	public void actionCompletedDeath() {
		testAgent.decide();
		TestAction actionTaken = (TestAction) testAgent.getActionPlan().getNextAction();
		ExperienceRecord<TestAgent> er1 = teacher.getExperienceRecords(testAgent).get(0);
		actionTaken.start();
		testAgent.setDecider(null); // otherwise we move on to next decision
		actionTaken.run();
		assertTrue(er1.getState() == ExperienceRecord.State.ACTION_COMPLETED);
		assertTrue(er1.getPossibleActionsFromEndState() == null);
		
		testAgent.die("Ooops");
		assertTrue(er1.getState() == ExperienceRecord.State.NEXT_ACTION_TAKEN);
		assertTrue(er1.getPossibleActionsFromEndState().size() == 0);
		assertTrue(er1.isInFinalState());
	}
	
	@Test
	public void actionWithSeveralParticipantsGeneratesLearningEventForEachOnceExecuted() {
		teacher.registerAgent(allAgents.get(1));;
		TestAction twoParticipants = taf.factory(1, 1, 0, 1000);
		twoParticipants.addToAllPlans();
		ExperienceRecord<TestAgent> er1 = teacher.getExperienceRecords(testAgent).get(0);
		assertTrue(teacher.agentActionState(allAgents.get(1), twoParticipants) == ExperienceRecord.State.DECISION_TAKEN);
		twoParticipants.start();
		allAgents.get(1).setDecider(null);
		twoParticipants.run();
		assertTrue(teacher.agentActionState(testAgent, twoParticipants) == ExperienceRecord.State.UNSEEN);
		assertTrue(er1.getState() == ExperienceRecord.State.NEXT_ACTION_TAKEN);
		assertTrue(teacher.agentActionState(allAgents.get(1), twoParticipants) == ExperienceRecord.State.ACTION_COMPLETED);
	}
	
	@Test
	public void rejectingAnActionCancelsExperienceRecord() {
		teacher.registerAgent(allAgents.get(1));
		TestAction twoParticipants = taf.factory(1, 1, 0, 1000);
		twoParticipants.addToAllPlans();
		twoParticipants.agree(testAgent);
		ExperienceRecord<TestAgent> er1 = teacher.getExperienceRecords(testAgent).get(0);
		ExperienceRecord<TestAgent> er2 = teacher.getExperienceRecords(allAgents.get(1)).get(0);
		assertTrue(er1.getState() == ExperienceRecord.State.DECISION_TAKEN);
		assertTrue(er2.getState() == ExperienceRecord.State.DECISION_TAKEN);
		twoParticipants.reject(allAgents.get(1));
		assertTrue(er1.getState() == ExperienceRecord.State.DECISION_TAKEN);
		assertTrue(er2.getState() == ExperienceRecord.State.ACTION_COMPLETED);
	}
	
	@Test
	public void cancellingAnActionMovesERToFinalState() {
		teacher.registerAgent(allAgents.get(1));
		TestAction twoParticipants = taf.factory(2, 0, 0, 1000);
		twoParticipants.addToAllPlans();
		twoParticipants.agree(testAgent);
		ExperienceRecord<TestAgent> er1 = teacher.getExperienceRecords(testAgent).get(0);
		ExperienceRecord<TestAgent> er2 = teacher.getExperienceRecords(allAgents.get(1)).get(0);
		assertTrue(er1.getState() == ExperienceRecord.State.DECISION_TAKEN);
		assertTrue(er2.getState() == ExperienceRecord.State.DECISION_TAKEN);
		twoParticipants.reject(allAgents.get(1));
		assertTrue(er1.getState() == ExperienceRecord.State.NEXT_ACTION_TAKEN);
		assertTrue(er2.getState() == ExperienceRecord.State.ACTION_COMPLETED);	// as this one has not taken a new decision
	}

	private void dispatchLearningEvent(TestAgent agent, Action actionToUse) {
		AgentEvent learningEvent = new AgentEvent(agent, AgentEvent.Type.DECISION_STEP_COMPLETE, actionToUse);
		agent.eventDispatch(learningEvent);
	}
}
