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
	TestERCollector erc = new TestERCollector();
	TestTeacher teacher = new TestTeacher();
	
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
		erc.registerAgent(testAgent);
		teacher.registerToERStream(erc);
		decider = (Decider<TestAgent>) testAgent.getDecider();
	}
	
	@Test
	public void registerExperienceRecordUpdatesAgentAndActionStates() {
		TestAgent otherAgent = allAgents.get(1);
		assertFalse(erc.agentKnown(otherAgent));
		erc.registerAgent(otherAgent);
		assertTrue(erc.agentKnown(otherAgent));
		otherAgent.decide();
		TestAction actionTaken = (TestAction) otherAgent.getNextAction();
		assertTrue(erc.agentActionState(otherAgent, actionTaken) == ExperienceRecord.State.DECISION_TAKEN);
		assertEquals(teacher.eventsReceived.size(), 0);
	}

	@Test
	public void unseenandDTERForOtherAction() {
		testAgent.decide();
		TestAction actionTaken = (TestAction) testAgent.getNextAction();
		assertTrue(erc.agentActionState(testAgent, actionTaken) == ExperienceRecord.State.DECISION_TAKEN);
		assertEquals(teacher.eventsReceived.size(), 0);
		TestAction forwardAction = taf.factory(1, 0, 2000, 1000);
		testAgent.getActionPlan().addAction(forwardAction);
		assertTrue(erc.agentActionState(testAgent, actionTaken) == ExperienceRecord.State.DECISION_TAKEN);
		assertTrue(erc.agentActionState(testAgent, forwardAction) == ExperienceRecord.State.DECISION_TAKEN);
		assertEquals(teacher.eventsReceived.size(), 0);
	}	
	
	@Test
	public void unseenAgreementAgentKnown() {
		List<ExperienceRecord<TestAgent>> allER = erc.getExperienceRecords(testAgent);
		assertEquals(allER.size(), 0);
		erc.registerAgent(allAgents.get(1));

		TestAction unknownAction = taf.factory(2, 0, 0, 1000);
		unknownAction.addToAllPlans();
		// will trigger Agree on all participants
		allER = erc.getExperienceRecords(testAgent);
		assertEquals(allER.size(), 1);
		allER.addAll(erc.getExperienceRecords(allAgents.get(1)));
		assertEquals(allER.size(), 2);

		for (ExperienceRecord<TestAgent> er : allER) {
			Action<TestAgent> a = er.getActionTaken();
			if (a.equals(unknownAction)) {
				assertTrue(erc.agentActionState(testAgent, a) == ExperienceRecord.State.DECISION_TAKEN);
			} 
		}
		assertEquals(teacher.eventsReceived.size(), 0);
	}	
	
	@Test
	public void unseenEventAgentKnown() {
		List<ExperienceRecord<TestAgent>> allER = erc.getExperienceRecords(testAgent);
		assertEquals(allER.size(), 0);
		erc.registerAgent(allAgents.get(1));

		TestAction unknownAction = taf.factory(2, 0, 0, 1000);
		dispatchLearningEvent(testAgent, unknownAction);
		dispatchLearningEvent(allAgents.get(1), unknownAction);

		allER = erc.getExperienceRecords(testAgent);
		assertEquals(allER.size(), 1);
		allER.addAll(erc.getExperienceRecords(allAgents.get(1)));
		assertEquals(allER.size(), 2);
		assertEquals(teacher.eventsReceived.size(), 0);
	}
	
	@Test
	public void unseenAgreementAgentUnknown() {
		List<ExperienceRecord<TestAgent>> allER = erc.getExperienceRecords(testAgent);
		assertEquals(allER.size(), 0);

		TestAction unknownAction = taf.factory(2, 0, 0, 1000);
		unknownAction.addToAllPlans();
		// will trigger Agree on all participants, but only one is known
		allER = erc.getExperienceRecords(testAgent);
		assertEquals(allER.size(), 1);
		allER.addAll(erc.getExperienceRecords(allAgents.get(1)));
		assertEquals(allER.size(), 1);
		
		for (ExperienceRecord<TestAgent> er : allER) {
			Action<TestAgent> a = er.getActionTaken();
			if (a.equals(unknownAction)) {
				assertTrue(erc.agentActionState(testAgent, a) == ExperienceRecord.State.DECISION_TAKEN);
			} 
		}
		assertEquals(teacher.eventsReceived.size(), 0);
	}	
	
	@Test
	public void DTDeath() {
		testAgent.decide();
		TestAction actionTaken = (TestAction) testAgent.getActionPlan().getNextAction();
		assertTrue(erc.agentActionState(testAgent, actionTaken) == ExperienceRecord.State.DECISION_TAKEN);
		ExperienceRecord<TestAgent> er1 = erc.getExperienceRecords(testAgent).get(0);
		assertFalse(er1.isInFinalState());
		assertEquals(teacher.eventsReceived.size(), 0);
		TestAction forwardAction = taf.factory(1, 0, 2000, 1000);
		forwardAction.addToAllPlans();
		ExperienceRecord<TestAgent> er2 = erc.getExperienceRecords(testAgent).get(1);
		assertTrue(er2.getActionTaken().equals(forwardAction));
		assertTrue(er1.getState() == ExperienceRecord.State.DECISION_TAKEN);
		testAgent.die("Ooops");
		assertTrue(er1.isInFinalState());
		assertTrue(er1.getState() == ExperienceRecord.State.NEXT_ACTION_TAKEN);
		assertTrue(er2.isInFinalState());
		assertTrue(er2.getState() == ExperienceRecord.State.NEXT_ACTION_TAKEN);
		assertEquals(teacher.eventsReceived.size(), 1);	// one Death event
		assertEquals(erc.getCompleteExperienceRecords(testAgent).size(), 2); // completes two separate ER
	}		
	
	@Test
	public void decisionTakenEventThisAndOtherAction() {
		testAgent.decide();
		TestAction actionTaken = (TestAction) testAgent.getActionPlan().getNextAction();
		TestAction forwardAction = taf.factory(1, 0, 2000, 1000);
		forwardAction.addToAllPlans();
		actionTaken.start();
		assertTrue(erc.agentActionState(testAgent, actionTaken) == ExperienceRecord.State.DECISION_TAKEN);
		assertTrue(erc.agentActionState(testAgent, forwardAction) == ExperienceRecord.State.DECISION_TAKEN);
		ExperienceRecord<TestAgent> er1 = erc.getExperienceRecords(testAgent).get(0);
		ExperienceRecord<TestAgent> er2 = erc.getExperienceRecords(testAgent).get(1);
		assertEquals(teacher.eventsReceived.size(), 0);
		actionTaken.run();
		assertEquals(teacher.eventsReceived.size(), 1); // DECISION_STEP_COMPLETE
		assertTrue(er1.getState() == ExperienceRecord.State.NEXT_ACTION_TAKEN);
		assertTrue(erc.agentActionState(testAgent, forwardAction) == ExperienceRecord.State.DECISION_TAKEN);
		assertTrue(er1.getEndState().length > 0);
		assertTrue(er2.getEndState() == null);
	}	
	
	@Test
	public void actionCompletedOtherER() {
		testAgent.decide();
		TestAction forwardAction = taf.factory(1, 0, 1000, 1000);
		forwardAction.addToAllPlans();
		ExperienceRecord<TestAgent> er1 = erc.getExperienceRecords(testAgent).get(0);
		ExperienceRecord<TestAgent> er2 = erc.getExperienceRecords(testAgent).get(1);
		assertTrue(er2.getActionTaken().equals(forwardAction));
		er1.getActionTaken().start();
		assertEquals(teacher.eventsReceived.size(), 0);
		testAgent.setDecider(null);
		er1.getActionTaken().run();
		assertEquals(teacher.eventsReceived.size(), 0);
		assertTrue(erc.agentActionState(testAgent, er1.getActionTaken()) == ExperienceRecord.State.ACTION_COMPLETED);
		assertTrue(erc.agentActionState(testAgent, er2.getActionTaken()) == ExperienceRecord.State.DECISION_TAKEN);
		assertTrue(er1.getPossibleActionsFromEndState() == null);
		assertTrue(er2.getPossibleActionsFromEndState() == null);
		
		testAgent.setDecider(decider);
		testAgent.decide();		// this will now make a new decision...which should close off the completed action
		assertEquals(teacher.eventsReceived.size(), 1);
		assertTrue(er1.getState() == ExperienceRecord.State.NEXT_ACTION_TAKEN);
		assertTrue(erc.agentActionState(testAgent, er2.getActionTaken()) == ExperienceRecord.State.DECISION_TAKEN);
		assertTrue(er1.getPossibleActionsFromEndState().size() > 0);
		assertTrue(er2.getPossibleActionsFromEndState() == null);
	}
	
	@Test
	public void actionCompletedOtherEvent() {
		testAgent.decide();
		TestAction actionTaken = (TestAction) testAgent.getActionPlan().getNextAction();
		ExperienceRecord<TestAgent> er1 = erc.getExperienceRecords(testAgent).get(0);
		actionTaken.start();
		testAgent.setDecider(null);
		assertEquals(teacher.eventsReceived.size(), 0);
		actionTaken.run();
		assertEquals(teacher.eventsReceived.size(), 0);
		assertTrue(erc.agentActionState(testAgent, er1.getActionTaken()) == ExperienceRecord.State.ACTION_COMPLETED);
		assertTrue(er1.getPossibleActionsFromEndState() == null);
		testAgent.setDecider(decider);
		System.out.println("About to dispatch event");
		dispatchLearningEvent(testAgent, taf.factory(1, 0, 0, 1000));
		assertEquals(teacher.eventsReceived.size(), 1);
		assertTrue(er1.getState() == ExperienceRecord.State.NEXT_ACTION_TAKEN);
		assertEquals(er1.getPossibleActionsFromEndState().size(), 1);
	}

	@Test
	public void actionCompletedDeath() {
		testAgent.decide();
		TestAction actionTaken = (TestAction) testAgent.getActionPlan().getNextAction();
		ExperienceRecord<TestAgent> er1 = erc.getExperienceRecords(testAgent).get(0);
		actionTaken.start();
		testAgent.setDecider(null); // otherwise we move on to next decision
		actionTaken.run();
		assertEquals(teacher.eventsReceived.size(), 0);
		assertTrue(er1.getState() == ExperienceRecord.State.ACTION_COMPLETED);
		assertTrue(er1.getPossibleActionsFromEndState() == null);
		
		testAgent.die("Ooops");
		assertTrue(er1.getState() == ExperienceRecord.State.NEXT_ACTION_TAKEN);
		assertTrue(er1.getPossibleActionsFromEndState().size() == 0);
		assertTrue(er1.isInFinalState());
		assertEquals(teacher.eventsReceived.size(), 1);
	}
	
	@Test
	public void actionWithSeveralParticipantsGeneratesLearningEventForEachOnceExecuted() {
		erc.registerAgent(allAgents.get(1));;
		TestAction twoParticipants = taf.factory(1, 1, 0, 1000);
		twoParticipants.addToAllPlans();
		ExperienceRecord<TestAgent> er1 = erc.getExperienceRecords(testAgent).get(0);
		assertTrue(erc.agentActionState(allAgents.get(1), twoParticipants) == ExperienceRecord.State.DECISION_TAKEN);
		twoParticipants.start();
		allAgents.get(1).setDecider(null);
		assertEquals(teacher.eventsReceived.size(), 0);
		twoParticipants.run();
		assertTrue(erc.agentActionState(testAgent, twoParticipants) == ExperienceRecord.State.NEXT_ACTION_TAKEN);
		assertTrue(er1.getState() == ExperienceRecord.State.NEXT_ACTION_TAKEN);
		assertTrue(erc.agentActionState(allAgents.get(1), twoParticipants) == ExperienceRecord.State.ACTION_COMPLETED);
		assertEquals(teacher.eventsReceived.size(), 1); // only one (for the deciding agent) actually completes the ER
		assertTrue(allAgents.get(1).getActionPlan().getNextAction() == null);
		allAgents.get(1).setDecider(decider);
		allAgents.get(1).decide();
		assertFalse(allAgents.get(1).getActionPlan().getNextAction() == null);
		assertEquals(teacher.eventsReceived.size(), 2); 
		assertTrue(erc.agentActionState(allAgents.get(1), twoParticipants) == ExperienceRecord.State.NEXT_ACTION_TAKEN);
	}
	
	@Test
	public void rejectingAnActionCancelsExperienceRecord() {
		erc.registerAgent(allAgents.get(1));
		TestAction twoParticipants = taf.factory(1, 1, 0, 1000);
		twoParticipants.addToAllPlans();
		twoParticipants.agree(testAgent);
		assertEquals(teacher.eventsReceived.size(), 0);
		ExperienceRecord<TestAgent> er1 = erc.getExperienceRecords(testAgent).get(0);
		ExperienceRecord<TestAgent> er2 = erc.getExperienceRecords(allAgents.get(1)).get(0);
		assertTrue(er1.getState() == ExperienceRecord.State.DECISION_TAKEN);
		assertTrue(er2.getState() == ExperienceRecord.State.DECISION_TAKEN);
		twoParticipants.reject(allAgents.get(1));
		assertTrue(er1.getState() == ExperienceRecord.State.DECISION_TAKEN);
		assertTrue(er2.getState() == ExperienceRecord.State.ACTION_COMPLETED);
		assertEquals(teacher.eventsReceived.size(), 0);
	}
	
	@Test
	public void cancellingAnActionMovesERToFinalState() {
		erc.registerAgent(allAgents.get(1));
		TestAction twoParticipants = taf.factory(2, 0, 0, 1000);
		twoParticipants.addToAllPlans();
		twoParticipants.agree(testAgent);
		assertEquals(teacher.eventsReceived.size(), 0);
		ExperienceRecord<TestAgent> er1 = erc.getExperienceRecords(testAgent).get(0);
		ExperienceRecord<TestAgent> er2 = erc.getExperienceRecords(allAgents.get(1)).get(0);
		assertTrue(er1.getState() == ExperienceRecord.State.DECISION_TAKEN);
		assertTrue(er2.getState() == ExperienceRecord.State.DECISION_TAKEN);
		twoParticipants.reject(allAgents.get(1));
		assertTrue(er1.getState() == ExperienceRecord.State.NEXT_ACTION_TAKEN);
		assertTrue(er2.getState() == ExperienceRecord.State.NEXT_ACTION_TAKEN);	
		assertEquals(teacher.eventsReceived.size(), 2);
	}
	
	@Test
	public void ercPolicyIsCalledOnBirth() {
		class TestERCPolicy implements ExperienceRecordCollector.ERCAllocationPolicy<TestAgent> {
			private Map<TestAgent, Integer> map = new HashMap<TestAgent, Integer>();
			@Override
			public void apply(TestAgent agent) {
				int currentCount = map.getOrDefault(agent, 0);
				map.put(agent, currentCount + 1);
				erc.registerAgent(agent);
			}
			public int getCalls(TestAgent agent) {
				return map.getOrDefault(agent, 0);
			}
		};

		TestERCPolicy ercPolicy = new TestERCPolicy();

		erc.setAllocationPolicy(ercPolicy);
		TestAgent parent = new TestAgent(w);
		erc.registerAgent(parent);
		TestAgent child = new TestAgent(w);
		assertEquals(ercPolicy.getCalls(parent), 0);
		assertEquals(ercPolicy.getCalls(child), 0);
		assertFalse(erc.agentKnown(child));
		assertTrue(erc.agentKnown(parent));
		child.addParent(parent);
		assertEquals(ercPolicy.getCalls(parent), 0);
		assertEquals(ercPolicy.getCalls(child), 1);
		assertTrue(erc.agentKnown(child));
	}

	private void dispatchLearningEvent(TestAgent agent, Action actionToUse) {
		AgentEvent learningEvent = new AgentEvent(agent, AgentEvent.Type.DECISION_STEP_COMPLETE, actionToUse);
		agent.eventDispatch(learningEvent);
	}
}
