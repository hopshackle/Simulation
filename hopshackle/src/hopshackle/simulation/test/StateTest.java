package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.basic.BasicActions;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.genetics.*;

import java.util.*;

import org.junit.*;
public class StateTest {

	private HopshackleState testState;
	private Character testLoneClr;
	private World w;

	@Before
	public void setUp() {
		SimProperties.setProperty("Alpha", "0.02");
		SimProperties.setProperty("Gamma", "0.98");
		SimProperties.setProperty("VisitMemoryLimit", "1000");
		SimProperties.setProperty("Temperature", "1.0");
		SimProperties.setProperty("StateBaseValue", "1000");
		SimProperties.setProperty("StateMaxNoise", "0.5");
		SimProperties.setProperty("RandomDeciderMaxChance", "0.0");

		HopshackleState.clear();
		w = new World();
		testLoneClr = new Character(Race.HUMAN, CharacterClass.CLERIC, w);
		testLoneClr.setWisdom(new Attribute(16));
		testLoneClr.rest(1);
		testLoneClr.setLocation(new Square(0, 1));
	}

	@Test
	public void testNoAction() {
		testLoneClr.setDecider(new StateDecider(new ArrayList<ActionEnum>(), new ArrayList<GeneticVariable>()));
		assertTrue(testLoneClr.getDecider().decide(testLoneClr) == null);	
	}

	@Test
	public void testAction() {
		testLoneClr.setDecider(new StateDecider(new ArrayList<ActionEnum>(EnumSet.allOf(PartyJoinActions.class)),
				new ArrayList<GeneticVariable>(EnumSet.allOf(GeneticEnum.class))));
		assertTrue(testLoneClr.getDecider().decide(testLoneClr) == PartyJoinActions.APPLY_TO_PARTY);
	}

	@Test
	public void testDeath() {
		ArrayList<ActionEnum> actionList = new ArrayList<ActionEnum>();
		actionList.add(BasicActionsI.REST);
		StateDecider sd = new StateDecider(actionList,
				new ArrayList<GeneticVariable>(EnumSet.allOf(GeneticEnum.class)));
		sd.setStateType("STATE_TEST1");
		sd.setTeacher(new AgentTeacher());
		Square s = new Square(0,0);
		s.setParentLocation(w);
		testLoneClr.setLocation(s);
		testLoneClr.setDecider(sd);

		HopshackleState clrState = sd.getState(testLoneClr, null);
		HopshackleState deathState = HopshackleState.getState("STATE_TEST1:DEAD");
		assertEquals(deathState.getValue(), 0.0, 0.001);

		testLoneClr.decide().run();
		assertEquals(clrState.getVisited(), 1);
		testLoneClr.getNextAction().run();
		assertEquals(clrState.getVisited(), 2);

		assertEquals(clrState.getCountOfNextState(BasicActionsI.REST, clrState), 2);
		assertEquals(clrState.getCountOfNextState(BasicActionsI.REST, deathState), 0);

		testLoneClr.die("test");

		assertEquals(clrState.getVisited(), 3);
		assertTrue(testLoneClr.isDead());

		assertEquals(clrState.getCountOfNextState(BasicActionsI.REST, clrState), 2);
		assertEquals(clrState.getCountOfNextState(BasicActionsI.REST, deathState), 1);
	}

	@Test
	public void newActionAddedToList() {
		testSetUpForBasicActionDecider();
		StateDecider sd = (StateDecider) testLoneClr.getDecider();
		testState = sd.getState(testLoneClr, null);
		sd.decide(testLoneClr);
		assertFalse(testState.getActions().contains(PartyJoinActions.APPLY_TO_PARTY));
		assertTrue(testState.getActions().contains(BasicActionsI.ADVENTURE));
		sd.setActions(new ArrayList<ActionEnum>(EnumSet.allOf(PartyJoinActions.class)));

		assertTrue(sd.decide(testLoneClr) == PartyJoinActions.APPLY_TO_PARTY);
		assertTrue(testState.getActions().contains(PartyJoinActions.APPLY_TO_PARTY));
		assertTrue(testState.getActions().contains(BasicActionsI.ADVENTURE));
	}

	private void testSetUpForBasicActionDecider() {
		StateDecider sd = new StateDecider(new ArrayList<ActionEnum>(EnumSet.allOf(BasicActionsI.class)),
				new ArrayList<GeneticVariable>(EnumSet.allOf(GeneticEnum.class)));
		sd.setStateType("STATE_TEST2");
		testLoneClr.setDecider(sd);
	}

	@Test 
	public void actionValueWithNoError() {
		testSetUpForBasicActionDecider();
		StateDecider sd = (StateDecider) testLoneClr.getDecider();
		testState = sd.getState(testLoneClr, null);
		setUpTestDataInState(testState);
		double basicValueWithoutExploration = 1000;

		assertEquals(testState.valueOption(BasicActionsI.ADVENTURE, 1.0), 1300, 50);
		assertEquals(testState.valueOption(BasicActionsI.REST, 1.0), 1700, 50);
		assertEquals(testState.valueOptionWithoutExploration(BasicActionsI.ADVENTURE), 300.0 * 100.0 / 102.0 + basicValueWithoutExploration, 1);
		assertEquals(testState.valueOptionWithoutExploration(BasicActionsI.REST), 250.0 * 5.0 / 7.0 + basicValueWithoutExploration, 1);

		assertTrue(sd.decide(testLoneClr) == BasicActionsI.REST);
	}
	

	@Test
	public void memoryPurge() {
		testState = HopshackleState.getState("MemoryTest1");
		addExperienceForActionFromState(testState, BasicActions.FIND_UNKNOWN, HopshackleState.getState("Destination"), 0, 1100);
		assertEquals(testState.getVisited(), 1100);
		addExperienceForActionFromState(testState, BasicActions.FIND_UNKNOWN, HopshackleState.getState("Destination"), 0, 1);
		assertEquals(testState.getVisited(), 991, 1);
	}

	@Test
	public void memoryPurgeWithActionEnumNotInMainList() {
		testState = HopshackleState.getState("MemoryTest2");
		addExperienceForActionFromState(testState, BasicActions.FORAGE, HopshackleState.getState("Destination"), 0, 1100);
		assertEquals(testState.getVisited(), 1100);
		addExperienceForActionFromState(testState, BasicActions.FORAGE, HopshackleState.getState("Destination"), 0, 1);
		assertEquals(testState.getVisited(), 991, 1);
	}

	@Test
	public void actionsAddedWhenSeen() {
		testState = HopshackleState.getState("NewActionTest");
		HopshackleState destinationState = HopshackleState.getState("NewActionTest2");
		assertEquals(testState.getActions().size(), 0);
		testState.addExperience(BasicActions.BREED, destinationState, 0);
		assertEquals(testState.getActions().size(), 1);
		assertEquals(destinationState.getActions().size(), 0);
		assertTrue(testState.getActions().get(0) == BasicActions.BREED);
	}
	
	@Test
	public void valueIncrementCorrectWithMultipleEndStates()  {
		testState = HopshackleState.getState("Start");
		HopshackleState destination1 = HopshackleState.getState("B");
		HopshackleState destination2 = HopshackleState.getState("C");
		assertEquals(testState.getValue(), 1000.0, 1.0);
		assertEquals(destination1.getValue(), 1000.0, 1.0);
		testState.updateStateValue(BasicActions.BUILD, destination1, 100);
		assertEquals(testState.getValue(), 1001.60, 0.01);
		testState.updateStateValue(BasicActions.BUILD, destination1, 100);
		assertEquals(testState.getValue(), 1003.17, 0.01);
		destination1.updateStateValue(BasicActions.BUILD, destination2, -500);
		assertEquals(destination1.getValue(), 989.60, 0.01);
		testState.updateStateValue(BasicActions.BUILD, destination2, 0);
		assertEquals(testState.getValue(), 1002.71, 0.01);
	}

	public void setUpTestDataInState(HopshackleState stateToUpdate) {
		// We want to set up data for each Action:
		// ADVENTURE = 100 experiences that go to TEST_ADV with a reward of 300 each time
		// REST = 5 experiences that go to TEST_REST with a reward of 250 each time

		// Then, on this basis, ADVENTURE should be the best option with no error
		// while REST is best option with exploration error
		addExperienceForActionFromState(stateToUpdate, BasicActionsI.ADVENTURE, HopshackleState.getState("TEST_ADV"), 300, 100);
		addExperienceForActionFromState(stateToUpdate, BasicActionsI.REST, HopshackleState.getState("TEST_REST"), 250, 5);
		for (ActionEnum ae : new ArrayList<ActionEnum>(EnumSet.allOf(BasicActionsI.class))) {
			if (ae != BasicActionsI.ADVENTURE && ae != BasicActionsI.REST) {
				addExperienceForActionFromState(stateToUpdate, ae, HopshackleState.getState("TEST_OTHER"), 0, 5);
			}
		}
	}

	public static void addExperienceForActionFromState(HopshackleState stateToUpdate, ActionEnum action, HopshackleState endState, double reward, int numberOfLessons) {
		for (int loop = 0; loop < numberOfLessons; loop++) {
			stateToUpdate.addExperience(action, endState, reward);
		}
	}

}
