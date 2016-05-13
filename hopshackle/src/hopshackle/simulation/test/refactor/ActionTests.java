package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;
import java.util.*;
import hopshackle.simulation.*;
import org.junit.*;

class TestAction extends Action {
	public TestAction(List<Agent> mandatory, List<Agent> optional, long startOffset, long duration, boolean recordAction) {
		super(mandatory, optional, startOffset, duration, recordAction);
	}
}

class TestAgent extends Agent {
	
	List<Action> actionsAdded = new ArrayList<Action>();
	int decisionsTaken = 0;
	
	public TestAgent(World world) {
		super(world);
	}
	@Override
	public void addAction(Action a) {
		super.addAction(a);
		actionsAdded.add(a);
	}
	@Override
	public Action decide() {
		super.decide();
		decisionsTaken++;
		List<Agent> thisAgentAsList = new ArrayList<Agent>();
		thisAgentAsList.add(this);
		return new TestAction(thisAgentAsList, new ArrayList<Agent>(), 0, 1000, true);
	}
}

class TestActionFactory {
	
	List<TestAgent> allAgents;
	public TestActionFactory(List<TestAgent> allAgents) {
		this.allAgents = allAgents;
	}
	public TestAction factory(int mandatory, int optional, long offset, long duration) {
		List<Agent> mandatoryAgents = new ArrayList<Agent>();
		List<Agent> optionalAgents = new ArrayList<Agent>();
		for (int i = 0; i < mandatory; i++) {
			mandatoryAgents.add(allAgents.get(i));
		}
		for (int i = mandatory; i < mandatory + optional; i++) {
			optionalAgents.add(allAgents.get(i));
		}
		return new TestAction(mandatoryAgents, optionalAgents, offset, duration, true);
	}
}


public class ActionTests {
	
	World w;
	List<TestAgent> allAgents = new ArrayList<TestAgent>();
	List<Agent> emptyList = new ArrayList<Agent>();
	TestActionFactory taf;
	
	@Before
	public void setup() {
		w = new World();
		for (int i = 0; i < 10; i++) {
			allAgents.add(new TestAgent(w));
		}
		taf = new TestActionFactory(allAgents);
	}

	@Test
	public void newActionHasStateOfPROPOSED() {
		TestAction t = taf.factory(1, 0, 0, 0);
		assertTrue(t.getState() == Action.State.PROPOSED);
	}
	@Test
	public void onRejectionOfMandatoryAgentStateIsCANCELLEDFromPROPOSED() {
		TestAction t = taf.factory(2, 0, 0, 0);
		t.reject(allAgents.get(1));
		assertTrue(t.getState() == Action.State.CANCELLED);
	}
	@Test
	public void onRejectionOfMandatoryAgentStateIsCANCELLEDFromPLANNED() {
		TestAction t = taf.factory(2, 0, 0, 0);
		t.agree(allAgents.get(1));
		t.agree(allAgents.get(0));
		assertTrue(t.getState() == Action.State.PLANNED);
		assertEquals(t.getAllConfirmedParticipants().size(), 2);
		t.reject(allAgents.get(0));
		assertTrue(t.getState() == Action.State.CANCELLED);
	}
	@Test
	public void onRejectionOfOptionalAgentFromPLANNEDNoEffect() {
		TestAction t = taf.factory(1, 1, 0, 0);
		t.agree(allAgents.get(1));
		t.agree(allAgents.get(0));
		assertTrue(t.getState() == Action.State.PLANNED);
		assertEquals(t.getAllConfirmedParticipants().size(), 2);
		t.reject(allAgents.get(1));
		assertTrue(t.getState() == Action.State.PLANNED);
		assertEquals(t.getAllConfirmedParticipants().size(), 1);
	}
	@Test
	public void rejectionFromEXECUTINGNoEffect() {
		TestAction t = taf.factory(1, 1, 0, 0);
		t.agree(allAgents.get(1));
		t.agree(allAgents.get(0));
		assertTrue(t.getState() == Action.State.PLANNED);
		t.start();
		t.reject(allAgents.get(1));
		assertTrue(t.getState() == Action.State.EXECUTING);
		assertEquals(t.getAllConfirmedParticipants().size(), 2);
	}
	
	@Test
	public void onAcceptanceOfAllMandatoryAgentsStateIsPLANNED() {
		TestAction t = taf.factory(2, 0, 0, 0);
		t.agree(allAgents.get(1));
		assertTrue(t.getState() == Action.State.PROPOSED);
		t.agree(allAgents.get(0));
		assertTrue(t.getState() == Action.State.PLANNED);
	}
	@Test
	public void onAcceptanceOfAllMandatoryAgentsStateIsPLANNEDreversedOrder() {
		TestAction t = taf.factory(2, 0, 0, 0);
		t.agree(allAgents.get(0));
		assertTrue(t.getState() == Action.State.PROPOSED);
		t.agree(allAgents.get(1));
		assertTrue(t.getState() == Action.State.PLANNED);
	}
	@Test
	public void onAcceptanceOfOptionalAgentsWhenPROPOSEDNothingHappens() {
		TestAction t = taf.factory(2, 2, 0, 0);
		assertTrue(t.isOptionalParticipant(allAgents.get(3)));
		assertFalse(t.isOptionalParticipant(allAgents.get(0)));
		t.agree(allAgents.get(2));
		assertTrue(t.getState() == Action.State.PROPOSED);
		t.agree(allAgents.get(3));
		assertTrue(t.getState() == Action.State.PROPOSED);
	}
	@Test
	public void onAcceptanceOfOptionalAgentOnceStateIsPLANNEDNothingHappens() {
		TestAction t = taf.factory(1, 1, 0, 0);
		t.agree(allAgents.get(0));
		assertTrue(t.getState() == Action.State.PLANNED);
		t.agree(allAgents.get(1));
		assertTrue(t.getState() == Action.State.PLANNED);
	}
	@Test (expected = Action.InvalidStateTransition.class)
	public void actionCannotBeStartedFromPROPOSED() {
		TestAction t = taf.factory(1, 1, 0, 0);
		t.start();
	}
	@Test
	public void actionCanBeStartedFromPLANNEDAndMovesToEXECUTING() {
		TestAction t = taf.factory(1, 0, 0, 0);
		t.agree(allAgents.get(0));
		assertTrue(t.getState() == Action.State.PLANNED);
		t.start();
		assertTrue(t.getState() == Action.State.EXECUTING);
	}

	@Test (expected = Action.InvalidStateTransition.class)
	public void actionCannotBeFinishedFromPROPOSED() {
		TestAction t = taf.factory(1, 0, 0, 0);
		t.run();
	}
	@Test (expected = Action.InvalidStateTransition.class)
	public void actionCannotBeFinishedFromPLANNED() {
		TestAction t = taf.factory(1, 0, 0, 0);
		t.agree(allAgents.get(0));
		t.run();
	}

	@Test
	public void actionCanBeFinishedFromEXECUTING() {
		TestAction t = taf.factory(1, 2, 0, 0);
		t.agree(allAgents.get(0));
		t.start();
		assertTrue(t.getState() == Action.State.EXECUTING);
		t.run();
		assertTrue(t.getState() == Action.State.FINISHED);
	}
	@Test (expected = Action.InvalidStateTransition.class)
	public void actionCannotBeFinishedFromFINISHED() {
		TestAction t = taf.factory(1, 0, 0, 0);
		t.agree(allAgents.get(0));
		t.run();
		t.run();
	}
	@Test (expected = Action.InvalidStateTransition.class)
	public void actionCannotBeFinishedFromCANCELLED() {
		TestAction t = taf.factory(1, 0, 0, 0);
		t.reject(allAgents.get(0));
		t.run();
	}
	@Test
	public void actionCanBeCANCELLEDFromPROPOSEDorPLANNED() {
		TestAction t = taf.factory(1, 0, 0, 0);
		t.agree(allAgents.get(0));
		t.cancel();
		assertTrue(t.getState() == Action.State.CANCELLED);
		t = taf.factory(1, 0, 0, 0);
		t.agree(allAgents.get(0));
		t.start();
		t.cancel();
		assertTrue(t.getState() == Action.State.CANCELLED);
	}
	@Test
	public void cancelFromFINISHEDHasNoEffect() {
		TestAction t = taf.factory(1, 0, 0, 0);
		t.agree(allAgents.get(0));
		t.start();
		t.run();
		assertTrue(t.getState() == Action.State.FINISHED);
		t.cancel();
		assertTrue(t.getState() == Action.State.FINISHED);
	}
	@Test
	public void plannedStartAndEndTimesUseOffset() {
		TestAction t = taf.factory(1, 0, 100, 1000);
		assertEquals(t.getStartTime(), 100);
		assertEquals(t.getEndTime(), 1100);
		t.agree(allAgents.get(0));
		t.start();
		assertEquals(t.getStartTime(), 0);
		assertEquals(t.getEndTime(), 1100);
		t.run();
		assertEquals(t.getStartTime(), 0);
		assertEquals(t.getEndTime(), 0);
	}
	@Test
	public void confirmedParticipantsChangeOverLifeCycle() {
		TestAction t = taf.factory(1, 2, 0, 0);
		assertEquals(t.getAllConfirmedParticipants().size(), 0);
		t.agree(allAgents.get(0));
		assertEquals(t.getAllConfirmedParticipants().size(), 1);
		assertTrue(t.getAllConfirmedParticipants().contains(allAgents.get(0)));
		t.agree(allAgents.get(2));
		t.agree(allAgents.get(1));
		assertEquals(t.getAllConfirmedParticipants().size(), 3);
		t.reject(allAgents.get(1));
		t.start();
		assertTrue(t.getState() == Action.State.EXECUTING);
		assertEquals(t.getAllConfirmedParticipants().size(), 2);
		assertTrue(t.getAllConfirmedParticipants().contains(allAgents.get(0)));
		assertFalse(t.getAllConfirmedParticipants().contains(allAgents.get(1)));
		assertTrue(t.getAllConfirmedParticipants().contains(allAgents.get(2)));
		t.run();
		assertTrue(t.getState() == Action.State.FINISHED);
		assertEquals(t.getAllConfirmedParticipants().size(), 2);
	}
	@Test
	public void whenAnActionFinishesANewDecisionIsTriggeredForAllParticipants() {
		TestAction a = taf.factory(1, 1, 0, 1000);
		a.agree(allAgents.get(0));
		a.agree(allAgents.get(1));
		a.start();
		assertEquals(allAgents.get(0).decisionsTaken, 0);
		assertEquals(allAgents.get(0).actionsAdded.size(), 0);
		assertEquals(allAgents.get(1).decisionsTaken, 0);
		assertEquals(allAgents.get(1).actionsAdded.size(), 0);
		a.run();
		assertEquals(allAgents.get(0).decisionsTaken, 1);
		assertEquals(allAgents.get(0).actionsAdded.size(), 1);
		assertEquals(allAgents.get(1).decisionsTaken, 1);
		assertEquals(allAgents.get(1).actionsAdded.size(), 1);
	}
	@Test
	public void cancellingAnEXECUTINGActionTriggersADecisionToo() {
		TestAction a = taf.factory(1, 1, 0, 1000);
		a.agree(allAgents.get(0));
		a.agree(allAgents.get(1));
		a.start();
		assertEquals(allAgents.get(0).decisionsTaken, 0);
		assertEquals(allAgents.get(0).actionsAdded.size(), 0);
		a.cancel();
		assertEquals(allAgents.get(0).decisionsTaken, 1);
		assertEquals(allAgents.get(0).actionsAdded.size(), 1);
		assertEquals(allAgents.get(1).decisionsTaken, 1);
		assertEquals(allAgents.get(1).actionsAdded.size(), 1);
	}
}
