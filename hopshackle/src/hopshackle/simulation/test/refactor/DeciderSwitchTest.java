package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;
import java.util.*;
import hopshackle.simulation.*;
import org.junit.*;

public class DeciderSwitchTest {

	TestDecider decider1, decider2, decider3;
	AgentTeacher<TestAgent> teacher1, teacher2;
	TestAgent agent;
	World w;
	
	@Before
	public void setUp() throws Exception {
		w = new World();
		agent = new TestAgent(w);
		decider1 = new TestDecider();
		decider2 = new TestDecider();
		decider3 = new TestDecider();
		teacher1 = new AgentTeacher<TestAgent>();
		teacher2 = new AgentTeacher<TestAgent>();
		decider1.setTeacher(teacher1);
		decider2.setTeacher(teacher1);
		decider3.setTeacher(teacher2);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void experienceRecordUpdatedOnNextDecision() {
		agent.setDecider(decider1);
		agent.decide();
		assertEquals(decider1.learningEpisodes, 0);
		assertEquals(teacher1.getExperienceRecords(agent).size(), 1);
		ExperienceRecord<TestAgent> er1 = teacher1.getExperienceRecords(agent).get(0);
		assertTrue(er1.getPossibleActionsFromStartState() != null);
		assertTrue(er1.getPossibleActionsFromEndState() == null);
		agent.getNextAction().start();
		agent.getNextAction().run();
		assertEquals(teacher1.getExperienceRecords(agent).size(), 1);
		assertEquals(agent.getActionPlan().sizeOfQueue(), 1); 
		ExperienceRecord<TestAgent> er2 = teacher1.getExperienceRecords(agent).get(0);
		assertEquals(decider1.learningEpisodes, 1);
		assertTrue(er1 != er2);
		assertTrue(er1.getPossibleActionsFromStartState() != null);
		assertTrue(er1.getPossibleActionsFromEndState() != null);
		assertTrue(er2.getPossibleActionsFromStartState() != null);
		assertTrue(er2.getPossibleActionsFromEndState() == null);
	}
	@Test
	public void experienceRecordUpdatedOnNextDecisionAfterDeciderSwitch() {
		agent.setDecider(decider1);
		agent.decide();
		assertEquals(teacher1.getExperienceRecords(agent).size(), 1);
		ExperienceRecord<TestAgent> er1 = teacher1.getExperienceRecords(agent).get(0);
		assertTrue(er1.getPossibleActionsFromStartState() != null);
		assertTrue(er1.getPossibleActionsFromEndState() == null);
		agent.setDecider(decider2);
		agent.getNextAction().start();
		agent.getNextAction().run();
		assertEquals(decider1.learningEpisodes, 1);
		assertEquals(decider2.learningEpisodes, 0);
		assertEquals(teacher1.getExperienceRecords(agent).size(), 1);
		ExperienceRecord<TestAgent> er2 = teacher1.getExperienceRecords(agent).get(0);
		assertTrue(er1 != er2);
		assertTrue(er1.getPossibleActionsFromStartState() != null);
		assertTrue(er1.getPossibleActionsFromEndState() != null);
		assertTrue(er2.getPossibleActionsFromStartState() != null);
		assertTrue(er2.getPossibleActionsFromEndState() == null);
	}
	@Test
	public void experienceRecordUpdatedOnNextDecisionAfterDeciderAndTeacherSwitch() {
		agent.setDecider(decider1);
		agent.decide();
		assertEquals(teacher1.getExperienceRecords(agent).size(), 1);
		ExperienceRecord<TestAgent> er1 = teacher1.getExperienceRecords(agent).get(0);
		assertTrue(er1.getPossibleActionsFromStartState() != null);
		assertTrue(er1.getPossibleActionsFromEndState() == null);
		agent.setDecider(decider3);
		agent.getNextAction().start();
		agent.getNextAction().run();
		assertEquals(agent.decisionsTaken, 2);
		assertEquals(decider1.learningEpisodes, 0);
		assertEquals(decider3.learningEpisodes, 0);
		// We need another event to trigger learning on the old decider (as it does not listen to registerDecision)
		agent.getNextAction().start();
		agent.getNextAction().run();
		assertEquals(agent.decisionsTaken, 3);
		assertEquals(decider1.learningEpisodes, 1);		// triggered by second learning event
		assertEquals(decider3.learningEpisodes, 1);		// triggered by registerDecision after learning event
		assertEquals(teacher1.getExperienceRecords(agent).size(), 1);	// and now both teachers keep track of progress
		assertEquals(teacher2.getExperienceRecords(agent).size(), 1);	// both listen ... both create ER for each new cycle
		ExperienceRecord<TestAgent> er2 = teacher2.getExperienceRecords(agent).get(0);
		assertTrue(er1 != er2);
		assertTrue(er1.getPossibleActionsFromStartState() != null);
		assertTrue(er1.getPossibleActionsFromEndState() != null);
		assertTrue(er2.getPossibleActionsFromStartState() != null);
		assertTrue(er2.getPossibleActionsFromEndState() == null);
	}

	@Test
	public void doNothingUpdatesPreviousExperienceRecord() {
		agent.setDecider(decider1);
		agent.decide();
		assertEquals(teacher1.getExperienceRecords(agent).size(), 1);
		ExperienceRecord<TestAgent> er1 = teacher1.getExperienceRecords(agent).get(0);
		assertTrue(er1.getPossibleActionsFromStartState() != null);
		assertTrue(er1.getPossibleActionsFromEndState() == null);
		TestActionEnum.dummyMode = true;
		agent.getNextAction().start();
		agent.getNextAction().run();
		assertEquals(agent.decisionsTaken, 2); // new decision made
		assertEquals(agent.getActionPlan().sizeOfQueue(), 0); // but nothing added to action queue
		assertEquals(decider1.learningEpisodes, 1);	// old decision has been learnt from
		assertEquals(teacher1.getExperienceRecords(agent).size(), 1);	// but er created
		ExperienceRecord<TestAgent> er2 = teacher1.getExperienceRecords(agent).get(0);
		assertTrue(er1 != er2);
		assertTrue(er1.getPossibleActionsFromStartState() != null);
		assertTrue(er1.getPossibleActionsFromEndState() != null);
		assertTrue(er2.getPossibleActionsFromStartState() != null);
		assertTrue(er2.getPossibleActionsFromEndState() == null);
		assertEquals(er2.getPossibleActionsFromStartState().size(), 1);
	}
}
