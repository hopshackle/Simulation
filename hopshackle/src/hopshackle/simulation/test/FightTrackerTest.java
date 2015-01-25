package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.genetics.*;

import java.util.*;

import org.junit.*;

public class FightTrackerTest {

	CharacterCombatAgent testcca, gcca1, fcca1, fcca2;
	Character c;
	static World w = new World();
	Character goblin1;
	Character fighter1, cleric1, fighter2, fighter3;
	Party party;
	Fight mainFight;
	FightTracker mainTracker;

	@Before
	public void setUp() throws Exception {
		// Let's set up the Fight we want to use

		// We will have two fighters vs one goblin

		SimProperties.setProperty("StartWithPotion", "false");
		SimProperties.setProperty("RandomDeciderMaxChance", "0.00");
		SimProperties.setProperty("NeuralDeciderHiddenLayers", "1");
		SimProperties.setProperty("NeuralDeciderInitialOptimismTraining", "true");

		c = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		testcca = (CharacterCombatAgent) c.getCombatAgent();
		goblin1 = new Character(Race.GOBLIN, CharacterClass.WARRIOR, w);
		goblin1.setStrength(new Attribute(9));
		goblin1.setDexterity(new Attribute(14));
		goblin1.setConstitution(new Attribute(10));
		goblin1.setMaxHp(8);
		goblin1.addItem(Weapon.LIGHT_MACE);

		gcca1 = (CharacterCombatAgent) goblin1.getCombatAgent();

		fighter1 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		fighter1.setStrength(new Attribute(13));
		fighter1.setDexterity(new Attribute(10));
		fighter1.setConstitution(new Attribute(12));
		fighter1.setMaxHp(11);
		fighter1.addItem(Weapon.SHORT_SWORD);

		fcca1 = (CharacterCombatAgent) fighter1.getCombatAgent();

		fighter2 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		fighter2.setStrength(new Attribute(13));
		fighter2.setDexterity(new Attribute(12));
		fighter2.setConstitution(new Attribute(12));
		fighter2.setMaxHp(11);
		fighter2.addItem(Weapon.SHORT_SWORD);
		fighter2.addItem(Shield.LARGE_SHIELD);

		fighter3 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		fighter3.setStrength(new Attribute(13));
		fighter3.setDexterity(new Attribute(12));
		fighter3.setConstitution(new Attribute(12));
		fighter3.setMaxHp(11);
		fighter3.addItem(Weapon.SHORT_SWORD);
		fighter3.addItem(Shield.LARGE_SHIELD);

		fcca2 = (CharacterCombatAgent) fighter2.getCombatAgent();

		cleric1 = new Character(Race.HUMAN, CharacterClass.CLERIC, w);
		cleric1.setWisdom(new Attribute(13));
		cleric1.setConstitution(new Attribute(12));
		cleric1.setMaxHp(9);
		cleric1.addMp(2);

		party = new Party(fighter1);
		party.addMember(fighter2);
		SimProperties.setProperty("Temperature", "0.00");


	}

	@Test
	public void testStartingHealth1() {
		mainFight = new Fight(party, goblin1);
		mainTracker = mainFight.getFightTracker();

		double[] sides = mainTracker.getStartingHealth();
		assertEquals(Math.min(sides[0], sides[1]), 8.0, 0.001);
		assertEquals(Math.max(sides[0], sides[1]), 22.0, 0.001);

		assertEquals(mainTracker.getStartingHealth(fighter1), 11.0, 0.001);
		assertEquals(mainTracker.getHealth(fighter1), 11.0, 0.001);
		fighter1.addHp(-5, false);
		assertEquals(mainTracker.getStartingHealth(fighter1), 11.0, 0.001);
		assertEquals(mainTracker.getHealth(fighter1), 6.0, 0.001);

		assertEquals(mainTracker.rewardFunction(fighter1), 0.5 + 6.0/11.0/2.0, 0.001);
	}

	@Test
	public void testHealthWithPotion() {
		mainFight = new Fight(party, goblin1);
		mainTracker = mainFight.getFightTracker();

		assertEquals(mainTracker.getHealth(fighter1), 11.0, 0.001);

		fighter1.addItem(Potion.CURE_LIGHT_WOUNDS);
		assertEquals(mainTracker.getHealth(fighter1), 14.0, 0.001);

		fighter1.addItem(Potion.CURE_LIGHT_WOUNDS);
		assertEquals(mainTracker.getHealth(fighter1), 17.0, 0.001);

		fighter1.addItem(Potion.CURE_LIGHT_WOUNDS);
		assertEquals(mainTracker.getHealth(fighter1), 17.0, 0.001);
	}

	@Test
	public void testStartingHealthSide1() {
		testStartingHealth1();
		assertEquals(mainTracker.getStartingHealthOfSide(fighter1), 22.0, 0.001);
		assertEquals(mainTracker.getStartingHealthOfSide(goblin1), 8.0, 0.001);
		assertEquals(mainTracker.getCurrentHealthOfSide(fighter1), 17.0, 0.001);

		fighter2.addHp(-100, false);
		assertEquals(mainTracker.getCurrentHealthOfSide(fighter1), 6.0, 0.001);
	}

	@Test
	public void testStartingHealth2() {
		mainFight = new Fight(cleric1, goblin1);
		mainTracker = mainFight.getFightTracker();

		double[] sides = mainTracker.getStartingHealth();
		assertEquals(Math.min(sides[0], sides[1]), 8.0, 0.001);
		assertEquals(Math.max(sides[0], sides[1]), 17.0, 0.001);

		assertEquals(mainTracker.getStartingHealth(cleric1), 17.0, 0.001);
		assertEquals(mainTracker.getStartingHealth(goblin1), 8.0, 0.001);
	}

	@Test
	public void testStartingHealthOfSide2() {
		testStartingHealth2();
		assertEquals(mainTracker.getStartingHealthOfSide(cleric1), 17.0, 0.001);
		cleric1.addMp(-1);
		assertEquals(mainTracker.getCurrentHealthOfSide(cleric1), 13.0, 0.001);

		assertEquals(mainTracker.getCurrentHealthOfSide(fighter2), 0.00, 0.001);
		// not in fight
	}

	@Test
	public void testWrongCharacter() {
		mainFight = new Fight(party, goblin1);
		mainTracker = mainFight.getFightTracker();

		assertEquals(mainTracker.getStartingHealth(cleric1), -1.0, 0.001);
		assertEquals(mainTracker.rewardFunction(cleric1), 0.0, 0.00001);
	}

	@Test
	public void testTraining1() {

		goblin1.addHp(-7, false);
		goblin1.setStrength(new Attribute(6));

		goblin1.setCombatDecider(TestFight.alwaysDefend);

		ArrayList<ActionEnum> attackOnly = new ArrayList<ActionEnum>();
		attackOnly.add(CombatActionsI.ATTACK);

		NeuralDecider testDecider = new NeuralDecider(attackOnly, 
				new ArrayList<GeneticVariable>(EnumSet.allOf(CombatNeuronInputsReduced.class)));

		fighter1.setCombatDecider(testDecider);
		fighter2.setCombatDecider(testDecider);

		mainFight = new Fight(party, goblin1);
		mainTracker = mainFight.getFightTracker();

		double preValue1 = testDecider.valueOption(CombatActionsI.ATTACK, fighter3, fighter3);
		double preValue2 = testDecider.valueOption(CombatActionsI.ATTACK, cleric1, cleric1);

		mainFight.resolve();
		assertEquals(mainTracker.getHealth(goblin1), 0.0, 0.0001);
		assertEquals(mainTracker.rewardFunction(fighter1), 1.0, 0.0001);

		double postValue1 = testDecider.valueOption(CombatActionsI.ATTACK, fighter3, fighter3);
		double postValue2 = testDecider.valueOption(CombatActionsI.ATTACK, cleric1, cleric1);
		assertEquals(preValue1, postValue1, 0.05);
		assertTrue(preValue1 != postValue1);
		assertTrue(preValue1 < postValue1);

		assertEquals(preValue2, postValue2, 0.05);
		assertTrue(preValue2 != postValue2);
		assertTrue(preValue2 < postValue2);
	}

	@Test
	public void testTraining2() {

		ArrayList<ActionEnum> defendOnly = new ArrayList<ActionEnum>();
		defendOnly.add(CombatActionsI.DEFEND);

		NeuralDecider testDecider = new NeuralDecider(defendOnly, 
				new ArrayList<GeneticVariable>(EnumSet.allOf(CombatNeuronInputsReduced.class)));

		fighter1.setCombatDecider(testDecider);
		fighter2.setCombatDecider(testDecider);

		mainFight = new Fight(party, goblin1);
		mainTracker = mainFight.getFightTracker();

		fighter1.addHp(-10, false);
		fighter2.addHp(-10, false);
		goblin1.setStrength(new Attribute(16));
		goblin1.addItem(Armour.CHAIN_SHIRT);
		goblin1.setCombatDecider(TestFight.alwaysAttack);
		fighter1.setStrength(new Attribute(6));
		fighter2.setStrength(new Attribute(6));

		double preValue = testDecider.valueOption(CombatActionsI.DEFEND, fighter3, fighter3);
		mainFight.resolve();

		double postValue = testDecider.valueOption(CombatActionsI.DEFEND, fighter3, fighter3);
		assertEquals(preValue, postValue, 0.05);
		assertTrue(preValue != postValue);
		assertTrue(preValue > postValue);

		assertEquals(mainTracker.rewardFunction(fighter1), 0.00, 0.00001);
		assertEquals(mainTracker.rewardFunction(fighter2), 0.00, 0.00001);
	}

	@Test
	public void internalFight() {
		party.addMember(cleric1);

		fighter1.setCombatDecider(TestFight.alwaysAttack);

		mainFight = new Fight(cleric1, fighter1);
		mainTracker = mainFight.getFightTracker();

		double startingclericHealth = mainTracker.getStartingHealthOfSide(cleric1);

		cleric1.addMp(-1);
		cleric1.setCombatDecider(TestFight.alwaysDefend);
		assertEquals(mainTracker.getHealth(cleric1), mainTracker.getStartingHealth(cleric1) - 4, 0.001);
		assertEquals(mainTracker.rewardFunction(cleric1), 0.5 + (cleric1.getHp() + 4.0) / (cleric1.getHp() + 8.0) / 2.0, 0.001);

		assertEquals(mainTracker.getCurrentHealthOfSide(cleric1), startingclericHealth - 4.0, 0.0001);

		mainFight.resolve();
		assertTrue(mainTracker.getHealth(cleric1) <= mainTracker.getStartingHealth(cleric1));
		assertTrue(mainTracker.getHealth(fighter1) <= mainTracker.getStartingHealth(fighter1));

		if (!cleric1.isDead()) {
			assertTrue(cleric1.getTempHp() >= cleric1.getHp());

			assertEquals(mainTracker.getHealth(cleric1), 0.0, 0.001);
			assertEquals(mainTracker.getCurrentHealthOfSide(cleric1), 0.0, 0.0001);

			assertEquals(mainTracker.rewardFunction(cleric1), 0.00, 0.00001);
			cleric1.addHp(cleric1.getTempHp()-cleric1.getHp()/2, false);
			assertFalse(cleric1.getTempHp() >= cleric1.getHp());
			assertTrue(mainTracker.rewardFunction(cleric1)> 0.4);
			assertTrue(mainTracker.rewardFunction(cleric1)< 0.9);
		}
	}
	
	@Test
	public void internalUnendingFight() {
		party.addMember(cleric1);

		fighter1.setCombatDecider(TestFight.alwaysDefend);

		mainFight = new Fight(cleric1, fighter1);
		mainTracker = mainFight.getFightTracker();

		double startingclericHealth = mainTracker.getStartingHealthOfSide(cleric1);

		cleric1.addMp(-1);
		cleric1.setCombatDecider(TestFight.alwaysDefend);
		assertEquals(mainTracker.getHealth(cleric1), mainTracker.getStartingHealth(cleric1) - 4, 0.001);
		assertEquals(mainTracker.rewardFunction(cleric1), 0.5 + (cleric1.getHp() + 4.0) / (cleric1.getHp() + 8.0) / 2.0, 0.001);

		assertEquals(mainTracker.getCurrentHealthOfSide(cleric1), startingclericHealth - 4.0, 0.0001);

		mainFight.resolve();
		assertEquals((int)mainTracker.getHealth(cleric1), 0);
		assertEquals((int)mainTracker.getHealth(fighter1), 0);
		assertFalse(cleric1.isDead());
		assertFalse(fighter1.isDead());
		assertEquals(mainTracker.rewardFunction(cleric1), 0.00, 0.00001);
		assertEquals(mainTracker.rewardFunction(fighter1), 0.00, 0.00001);
	}

}

class TestND extends NeuralDecider {

	public TestND(ArrayList<ActionEnum> actions, ArrayList<GeneticVariable> variables) {
		super(actions, variables);
	}
}

