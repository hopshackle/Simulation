package hopshackle.simulation.test;
import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.genetics.*;

import java.util.*;

import org.junit.*;

public class AgentTeacherTest {

	private Character ch;
	private World w;
	private StateDecider stateDecider;
	private AgentTeacher agentTeacher;
	
	@Before
	public void setUp() {
		SimProperties.setProperty("Alpha", "0.02");
		SimProperties.setProperty("Gamma", "0.98");
		SimProperties.setProperty("StateBaseValue", "2000.0");
		w = new World();
		ch = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		ch.setLocation(new Square(0,0));
		
		ArrayList<ActionEnum> actions = new ArrayList<ActionEnum>(EnumSet.allOf(PartyJoinActionsII.class));
		ArrayList<GeneticVariable> geneticVariables = new ArrayList<GeneticVariable>(EnumSet.allOf(GeneticEnum.class));
		geneticVariables.remove(GeneticEnum.AC);
		geneticVariables.remove(GeneticEnum.DMG);
		stateDecider = new StateDecider(actions, geneticVariables);
		agentTeacher = new AgentTeacher();
		stateDecider.setTeacher(agentTeacher);
		ch.setDecider(stateDecider);
	}
	
	@After
	public void tearDown() {
		HopshackleState.clear();
	}
	
	@Test
	public void addAnAgentAndRegisterDecision() {
		ch.decide();
		List<ExperienceRecord> tdList = agentTeacher.getExperienceRecords(ch);
		assertEquals(tdList.size(), 1);
	}
	
	@Test 
	public void secondDecisionRemovesFirst() {
		ch.decide();
		ch.decide();
		List<ExperienceRecord> tdList = agentTeacher.getExperienceRecords(ch);
		assertEquals(tdList.size(), 1);
	}
	
	@Test
	public void registerAndLearnFromDecision() {
		HopshackleState startState = stateDecider.getState(ch, null);
		double startStateValue = startState.getValue();
		assertEquals(startStateValue, 2000.0, 0.01);
		
		ch.decide();
		ch.addXp(600);
		ch.decide();
		HopshackleState midState = stateDecider.getState(ch, null);
		double midStateValue = midState.getValue();
		assertTrue(midStateValue > startStateValue);
	//	value = value + alpha * (reward + gamma * statistics.getValueOfNextState() - value);
		assertEquals(midStateValue - startStateValue, (600.0 - 0.02 * 2000.0)*0.02, 0.01);
		
		ch.decide();
		HopshackleState newState = stateDecider.getState(ch, null);
		double newStateValue = newState.getValue();
		
		assertEquals(newStateValue - midStateValue, 0.02 * -0.02 * midStateValue, 0.01);
	}
	

	@Test
	public void agentDeath() {
		HopshackleState startState = stateDecider.getState(ch, null);
		double startStateValue = startState.getValue();
		ch.addXp(500);
		ch.decide();
		ch.addXp(600);
	
		assertEquals(startStateValue, 2000.0, 0.01);
		List<ExperienceRecord> tdList = agentTeacher.getExperienceRecords(ch);
		assertEquals(tdList.size(), 1);
	
		ch.die("Ooops");
		startStateValue = startState.getValue();
		assertEquals(startStateValue, 2000.0 + (600.0 - 2000.0)*0.02, 0.01);
		tdList = agentTeacher.getExperienceRecords(ch);
		assertEquals(tdList.size(), 0);
	}
	
	
	@Test
	public void moreThanOneAgent() {
		Character ch2 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		ch2.setLocation(new Square(0,0));
		Character ch3 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		ch3.setLocation(new Square(0,0));
		ch2.setDecider(stateDecider);
		ch3.setDecider(stateDecider);
		
		HopshackleState startState = stateDecider.getState(ch, null);
		double startStateValue = startState.getValue();
		assertEquals(startStateValue, 2000.0, 0.01);
		assertTrue(stateDecider.getState(ch2, null).equals(startState));
		
		ch.decide();
		ch2.decide();
		List<ExperienceRecord> tdList = agentTeacher.getExperienceRecords(ch);
		assertEquals(tdList.size(), 1);
		tdList = agentTeacher.getExperienceRecords(ch2);
		assertEquals(tdList.size(), 1);
		tdList = agentTeacher.getExperienceRecords(ch3);
		assertEquals(tdList.size(), 0);
		
		ch2.addXp(600);
		ch.decide();
		ch2.decide();
		ch3.decide();
		tdList = agentTeacher.getExperienceRecords(ch);
		assertEquals(tdList.size(), 1);
		tdList = agentTeacher.getExperienceRecords(ch2);
		assertEquals(tdList.size(), 1);
		tdList = agentTeacher.getExperienceRecords(ch3);
		assertEquals(tdList.size(), 1);
		
		startStateValue = startState.getValue();
		assertEquals(startStateValue, 2000.0 + (600.0 - 0.04 * 2000.0)*0.02, 0.01);
		// 0.04 as two decisions have come through
	}
}
