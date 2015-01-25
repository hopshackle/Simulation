package hopshackle.simulation;

import static org.junit.Assert.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.genetics.*;
import hopshackle.simulation.test.StateTest;

import java.util.*;

import org.junit.*;
public class StateOffPolicyDeciderTest {

	private Character testChar;
	private World w;
	private StateOffPolicyDecider sopd;
	private AgentTeacher agentTeacher;
	private HopshackleState startState, endState;
	
	@Before
	public void setUp() {
		SimProperties.setProperty("Temperature", "1.0");
		SimProperties.setProperty("StateBaseValue", "2000.0");
		SimProperties.setProperty("RandomDeciderMaxChance", "0.00");
		SimProperties.setProperty("RandomDeciderMinChance", "0.00");
		SimProperties.setProperty("StateMaxNoise", "0.50");
		SimProperties.setProperty("StateMinNoise", "0.00");
		HopshackleState.clear();
		w = new World();
		testChar = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		testChar.rest(1);
		testChar.maintenance();
		testChar.addGold(100);
		testChar.setLocation(new Square(0, 1));
		ArrayList<GeneticVariable> geneticVariablesToUse = new ArrayList<GeneticVariable>(EnumSet.allOf(GeneticEnum.class));
		geneticVariablesToUse.remove(GeneticEnum.AGE);
		
		sopd = new StateOffPolicyDecider(new ArrayList<ActionEnum>(EnumSet.allOf(BasicActionsI.class)), 
										 geneticVariablesToUse);
		agentTeacher = new AgentTeacher();
		sopd.setTeacher(agentTeacher);
		sopd.setStateType("SOPD_TEST1");
		testChar.setDecider(sopd);
		startState = sopd.getState(testChar, null);
		endState = HopshackleState.getState("TEST_END");
	}
	
	@After
	public void tearDown() {
		HopshackleState.clear();
	}
	
	@Test
	public void teachingDecisionCreatedWhenBestActionChosen() {
		StateTest.addExperienceForActionFromState(startState, BasicActionsI.ADVENTURE, endState, 300, 100);
		StateTest.addExperienceForActionFromState(startState, BasicActionsI.REST, endState, 250, 100);
		for (ActionEnum ae : new ArrayList<ActionEnum>(EnumSet.allOf(BasicActionsI.class))) {
			if (ae != BasicActionsI.ADVENTURE && ae != BasicActionsI.REST) {
				StateTest.addExperienceForActionFromState(startState, ae, endState, 0, 25);
			}
		}
		assertTrue(startState == sopd.getState(testChar, null));
		Action actionChosen  = testChar.decide();
		assertTrue(actionChosen.toString().equals("ADVENTURE"));
		List<ExperienceRecord> decisions = agentTeacher.getExperienceRecords(testChar);
		assertEquals(decisions.size(), 1);
	}
	
	@Test
	public void teachingDecisionNotCreatedWhenBestActionNotChosen() {
		StateTest.addExperienceForActionFromState(startState, BasicActionsI.ADVENTURE, endState, 300, 100);
		StateTest.addExperienceForActionFromState(startState, BasicActionsI.REST, endState, 250, 5);
		for (ActionEnum ae : new ArrayList<ActionEnum>(EnumSet.allOf(BasicActionsI.class))) {
			if (ae != BasicActionsI.ADVENTURE && ae != BasicActionsI.REST) {
				StateTest.addExperienceForActionFromState(startState, ae, endState, 0, 25);
			}
		}
		assertTrue(startState == sopd.getState(testChar, null));
		Action actionChosen  = testChar.decide();
		assertTrue(actionChosen.toString().equals("REST"));
		List<ExperienceRecord> decisions = agentTeacher.getExperienceRecords(testChar);
		assertEquals(decisions.size(), 1);
	}
	
}
