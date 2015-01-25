package hopshackle.simulation.test;


import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.genetics.*;

import java.awt.AWTEvent;
import java.awt.event.AWTEventListener;
import java.util.*;

import org.junit.*;
public class CareerDecisionTest {
	
	Character c1, c2, c3;
	World w;
	private static StateDecider testCareerDecider;

	@Before
	public void setUp() throws Exception {
		
		SimProperties.setProperty("StateBaseValue", "10000");
		w = new World();
		c1 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		c2 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		c3 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);

		setAttributes(c1);
		setAttributes(c2);
		setAttributes(c3);
		
		testCareerDecider = new StateDecider(new ArrayList<ActionEnum>(EnumSet.allOf(CareerActionsI.class)),
				new ArrayList<GeneticVariable>(EnumSet.allOf(CareerGeneticEnum.class)));
		testCareerDecider.setStateType("TEST");
		testCareerDecider.setPigeonHoles(4);
		
		c1.setCareerDecider(testCareerDecider);
		c2.setCareerDecider(testCareerDecider);
		c3.setCareerDecider(testCareerDecider);
	}
	
	private void setAttributes(Character c) {
		Attribute base10 = new Attribute(10);
		Attribute base16 = new Attribute(16);
		Attribute base12 = new Attribute(12);
		Attribute base9 = new Attribute(9);
		
		c.setStrength(base16);
		c.setIntelligence(base9);
		c.setWisdom(base12);
		c.setConstitution(base12);
		c.setCharisma(base10);
		c.setDexterity(base9);
	}
	
	@After
	public void tearDown() throws Exception {
		HopshackleState.clear();
	}

	@Test
	public void testDeathEvent() {
		 // 		Create Character. Add a listener.
		 // 		Kill agent. Check that Death event is received.
		DummyEventListener del = new DummyEventListener(testCareerDecider);
		
		c1.addListener(del);
		
		assertTrue(del.getLastEvent() == null);
		c1.die("Test");
		
		assertTrue(del.getLastEvent() instanceof AgentEvent);
		AgentEvent ae = (AgentEvent)del.getLastEvent();
		assertTrue(ae.getEvent().equals(AgentEvents.DEATH));	
	}
	
	@Test
	public void testStateDescription() {
		String description = testCareerDecider.getState(c1, c1).toString();
		assertTrue(description.equals("TEST:4:2:3:2:3:3"));
		
		testCareerDecider.setPigeonHoles(5);
		description = testCareerDecider.getState(c1, c1).toString();
		assertTrue(description.equals("TEST:5:3:4:3:4:3"));
	}
	
	@Test
	public void testThreeIdenticalI() {
		 // 		For three identical characters, get it to make a choice.
		 // 		Confirm that all give the same result.
		
		ActionEnum decision1 = testCareerDecider.decide(c1);
		ActionEnum decision2 = testCareerDecider.decide(c2);
		ActionEnum decision3 = testCareerDecider.decide(c3);
		
		assertTrue (decision1.equals(decision2));
		assertTrue (decision2.equals(decision3));
		
		String stateString = testCareerDecider.getState(c1, c1).toString();
		HopshackleState s = HopshackleState.getState(stateString);
		
		assertEquals(s.getVisited(), 0, 0);
		assertEquals(s.getValue(), 10000.0, 0.0001);
	}
	
	@Test
	public void testThreeIdenticalWithDeath() {
		/* Tests:
		 * 		For three identical characters, get it to make a choice.
		 * 		Confirm that all give the same result.
		 * 
		 * 		Repeat previous test, but after each one, kill the Character. This time we should have
		 *		one of each of CLERIC, FIGHTER, EXPERT.
		 *		Also, on character death, check that the State value has been updated. (getValue and getVisited)
		 */
		DummyEventListener del = new DummyEventListener(testCareerDecider);
		testCareerDecider.setTeacher(del);
		String stateString = testCareerDecider.getState(c1, c1).toString();
		
		ActionEnum decision1 = testCareerDecider.decide(c1);
		c1.die("Test");
		ActionEnum decision2 = testCareerDecider.decide(c2);
		c2.die("Test");
		ActionEnum decision3 = testCareerDecider.decide(c3);
		c3.die("Test");
		
		HopshackleState s = HopshackleState.getState(stateString);
		HopshackleState dead = HopshackleState.getState("TEST:DEAD");
		
		assertEquals(s.getVisited(), 3, 0);
		assertEquals(dead.getValue(), 0.0, 0.00001);
		assertTrue(s.getValue()< 10000);
		
		assertFalse (decision1.equals(decision2));
		assertFalse (decision2.equals(decision3));
		assertFalse (decision1.equals(decision3));
		
	}

}

class DummyEventListener implements AWTEventListener, Teacher<Agent> {

	private AWTEvent lastEvent;
	private HashMap<Character, ExperienceRecord> characterMap = new HashMap<Character, ExperienceRecord>();
	private Decider decider;
	
	public DummyEventListener(StateDecider decider) {
		this.decider = decider;
	}
	public void eventDispatched(AWTEvent arg0) {
		lastEvent = arg0;
		
		AgentEvent ae = (AgentEvent)arg0;
		switch (ae.getEvent()) {
		case DEATH:
			// good - the one we want
			Character c = (Character)ae.getAgent();
			if (characterMap.containsKey(c)) {
				ExperienceRecord td = characterMap.get(c);
				td.updateWithResults(Math.sqrt(c.getXp()), decider.getCurrentState(c, c), new ArrayList<ActionEnum>(), true);
				decider.learnFrom(td, Math.sqrt(10000));
				characterMap.remove(c);
			}
		default:
			break;
		}
	}
	public AWTEvent getLastEvent() {return lastEvent;}
	public boolean registerDecision(Agent decider, ExperienceRecord decision) {
		Character c = (Character) decider;
		characterMap.put(c, decision);
		c.addListener(this);
		return true;
	}	
	public List<ExperienceRecord> getExperienceRecords(Agent a) {
		return null;
	}
}
