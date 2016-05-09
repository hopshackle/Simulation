package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;
import java.util.*;
import hopshackle.simulation.*;
import org.junit.*;

public class ActionTests {
	
	class TestAction extends Action {
		public TestAction(List<Agent> mandatory, List<Agent> optional, long startOffset, long duration, boolean recordAction) {
			super(mandatory, optional, startOffset, duration, recordAction);
		}
	}
	
	class TestAgent extends Agent {
		public TestAgent(World world) {
			super(world);
		}
		@Override
		public double getScore() {return 0;}
		@Override
		public double getMaxScore() {return 0;}
	}
	
	World w;
	List<TestAgent> allAgents = new ArrayList<TestAgent>();
	List<Agent> emptyList = new ArrayList<Agent>();
	
	@Before
	public void setup() {
		w = new World();
		for (int i = 0; i < 10; i++) {
			allAgents.add(new TestAgent(w));
		}
	}

	@Test
	public void newActionHasStateOfPROPOSED() {
		TestAction t = factory(1, 0, 0, 0);
		assertTrue(t.getState() == Action.State.PROPOSED);
	}
	@Test
	public void onRejectionOfMandatoryAgentStateIsCANCELLED() {
		TestAction t = factory(2, 0, 0, 0);
		t.reject(allAgents.get(1));
		assertTrue(t.getState() == Action.State.CANCELLED);
	}
	@Test
	public void onAcceptanceOfAllMandatoryAgentsStateIsPLANNED() {
		TestAction t = factory(2, 0, 0, 0);
		t.agree(allAgents.get(1));
		assertTrue(t.getState() == Action.State.PROPOSED);
		t.agree(allAgents.get(0));
		assertTrue(t.getState() == Action.State.PLANNED);
	}
	@Test
	public void onAcceptanceOfAllMandatoryAgentsStateIsPLANNEDreversedOrder() {
		TestAction t = factory(2, 0, 0, 0);
		t.agree(allAgents.get(0));
		assertTrue(t.getState() == Action.State.PROPOSED);
		t.agree(allAgents.get(1));
		assertTrue(t.getState() == Action.State.PLANNED);
	}
	@Test
	public void onAcceptanceOfOptionalAgentsWhenPROPOSEDNothingHappens() {
		TestAction t = factory(2, 2, 0, 0);
		assertTrue(t.isOptionalParticipant(allAgents.get(3)));
		assertFalse(t.isOptionalParticipant(allAgents.get(0)));
		t.agree(allAgents.get(2));
		assertTrue(t.getState() == Action.State.PROPOSED);
		t.agree(allAgents.get(3));
		assertTrue(t.getState() == Action.State.PROPOSED);
	}
	@Test
	public void onAcceptanceOfOptionalAgentOnceStateIsPLANNEDNothingHappens() {
		TestAction t = factory(1, 1, 0, 0);
		t.agree(allAgents.get(0));
		assertTrue(t.getState() == Action.State.PLANNED);
		t.agree(allAgents.get(1));
		assertTrue(t.getState() == Action.State.PLANNED);
	}
	@Test (expected = Action.InvalidStateTransition.class)
	public void actionCannotBeStartedFromPROPOSED() {
		TestAction t = factory(1, 1, 0, 0);
		t.start();
	}
	@Test
	public void actionCanBeStartedFromPLANNEDAndMovesToEXECUTING() {
		TestAction t = factory(1, 0, 0, 0);
		t.agree(allAgents.get(0));
		assertTrue(t.getState() == Action.State.PLANNED);
		t.start();
		assertTrue(t.getState() == Action.State.EXECUTING);
	}

	@Test (expected = Action.InvalidStateTransition.class)
	public void actionCannotBeFinishedFromPROPOSED() {
		TestAction t = factory(1, 0, 0, 0);
		t.run();
	}
	@Test (expected = Action.InvalidStateTransition.class)
	public void actionCannotBeFinishedFromPLANNED() {
		TestAction t = factory(1, 0, 0, 0);
		t.agree(allAgents.get(0));
		t.run();
	}

	@Test
	public void actionCanBeFinishedFromEXECUTING() {
		TestAction t = factory(1, 2, 0, 0);
		t.agree(allAgents.get(0));
		t.start();
		assertTrue(t.getState() == Action.State.EXECUTING);
		t.run();
		assertTrue(t.getState() == Action.State.FINISHED);
	}
	@Test (expected = Action.InvalidStateTransition.class)
	public void actionCannotBeFinishedFromFINISHED() {
		TestAction t = factory(1, 0, 0, 0);
		t.agree(allAgents.get(0));
		t.run();
		t.run();
	}
	@Test (expected = Action.InvalidStateTransition.class)
	public void actionCannotBeFinishedFromCANCELLED() {
		TestAction t = factory(1, 0, 0, 0);
		t.reject(allAgents.get(0));
		t.run();
	}
	@Test
	public void actionCanBeCANCELLEDFromPROPOSEDorPLANNED() {
		TestAction t = factory(1, 0, 0, 0);
		t.agree(allAgents.get(0));
		t.cancel();
		assertTrue(t.getState() == Action.State.CANCELLED);
		t = factory(1, 0, 0, 0);
		t.agree(allAgents.get(0));
		t.start();
		t.cancel();
		assertTrue(t.getState() == Action.State.CANCELLED);
	}
	@Test
	public void cancelFromFINISHEDHasNoEffect() {
		TestAction t = factory(1, 0, 0, 0);
		t.agree(allAgents.get(0));
		t.start();
		t.run();
		assertTrue(t.getState() == Action.State.FINISHED);
		t.cancel();
		assertTrue(t.getState() == Action.State.FINISHED);
	}
	@Test
	public void plannedStartAndEndTimesUseOffset() {
		TestAction t = factory(1, 0, 100, 1000);
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
	public void cancellingAnActionRemovesItFromActionQueuesOfAllAgents() {
		fail("Not yet implemented");
	}

	
	private TestAction factory(int mandatory, int optional, long offset, long duration) {
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
