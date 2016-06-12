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
		 return new ExperienceRecord<TestAgent>(testAgent, new ArrayList<GeneticVariable>(), 
				 decider.getCurrentState(testAgent, testAgent), TestActionEnum.TEST.getAction(testAgent), 
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
	public void unseenERForOtherAction() {
		ExperienceRecord<TestAgent> er1 = getNewER();
		ExperienceRecord<TestAgent> er2 = getNewER();
		teacher.registerDecision(testAgent, er1);
		assertTrue(teacher.agentActionState(testAgent, er2.getActionTaken()) == ExperienceRecord.State.UNSEEN);
	}	
	
	@Test
	public void unseenEventThisActionAgentKnown() {
		TestAction oldAction = taf.factory(1, 0, 0, 1000);
		testAgent.getActionPlan().addAction(oldAction);
		oldAction.start();
		oldAction.run();
		// We set an old action in executed actions first
		
		ExperienceRecord<TestAgent> er1 = getNewER();
		ExperienceRecord<TestAgent> er2 = getNewER();
		ExperienceRecord<TestAgent> er3 = getNewER();
		teacher.registerDecision(testAgent, er1);
		assertTrue(teacher.agentActionState(testAgent, er2.getActionTaken()) == ExperienceRecord.State.UNSEEN);
		testAgent.getActionPlan().addAction(er3.getActionTaken()); // this ensures ER3 action is the one referenced on Learning Event
		List<ExperienceRecord<TestAgent>> allER = teacher.getExperienceRecords(testAgent);
		assertEquals(allER.size(), 1);
		testAgent.getActionPlan().actionCompleted(oldAction);
		dispatchLearningEvent(testAgent);
		// Should create a new ExperienceRecord with a status of ACTION_COMPLETED
		allER = teacher.getExperienceRecords(testAgent);
		assertEquals(allER.size(), 2);
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
				assertTrue(teacher.agentActionState(testAgent, a) == ExperienceRecord.State.ACTION_COMPLETED);
			}
		}
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
	}	
	
	@Test
	public void unseenEventOtherAction() {
		fail("Not yet implemented");
	}	
	
	@Test
	public void unseenDeath() {
		fail("Not yet implemented");
	}	
	

	@Test
	public void decisionTakenERThisAction() {
		fail("Not yet implemented");
	}

	@Test
	public void decisionTakenEROtherAction() {
		fail("Not yet implemented");
	}	
	
	@Test
	public void decisionTakenEventThisAction() {
		fail("Not yet implemented");
	}	
	
	@Test
	public void decisionTakenEventOtherAction() {
		fail("Not yet implemented");
	}	
	
	@Test
	public void decisionTakenDeath() {
		fail("Not yet implemented");
	}	
	
	private void dispatchLearningEvent(TestAgent agent) {
		AgentEvent learningEvent = new AgentEvent(agent, AgentEvents.DECISION_STEP_COMPLETE);
		agent.eventDispatch(learningEvent);
	}
}
