package hopshackle.simulation.test;


import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.genetics.*;

import java.util.*;

import org.junit.*;
public class TestFight {

	CharacterCombatAgent testcca, gcca1, fcca1, fcca2;
	static World w = new World();
	Character goblin1;
	Character fighter1, cleric1, fighter2;
	Party party;

	Fight mainFight;

	@Before
	public void setUp() throws Exception {
		// Let's set up the Fight we want to use

		// We will have two fighters vs one goblin

		goblin1 = new Character(Race.GOBLIN, CharacterClass.WARRIOR, w);
		goblin1.setStrength(new Attribute(12));
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

		fcca2 = (CharacterCombatAgent) fighter2.getCombatAgent();

		cleric1 = new Character(Race.HUMAN, CharacterClass.CLERIC, w);
		cleric1.setWisdom(new Attribute(13));
		cleric1.addMp(2);

		party = new Party(fighter1);
		party.addMember(fighter2);

		mainFight = new Fight(party, goblin1);
	}

	@Test
	public void testFight() {
		// mainFight has been set up already
		// here we test that basic conditions are met

		// isInternal
		assertTrue(mainFight.isInternal()==false);

		// currentRound
		assertTrue(mainFight.getCurrentRound()==0);

		// side sizes
		assertEquals(mainFight.getSideSize(fcca1), 2, 0);
		assertEquals(mainFight.getOtherSideSize(fcca1), 1, 0);
		assertEquals(mainFight.getSideSize(gcca1), 1, 0);
		assertEquals(mainFight.getOtherSideSize(gcca1), 2, 0);

		assertEquals(mainFight.getSide(gcca1).get(0), gcca1);

		// current Fight is correct on participants
		assertEquals(fighter1.getCurrentFight(), mainFight);
		assertEquals(fighter2.getCurrentFight(), mainFight);
		assertEquals(goblin1.getCurrentFight(), mainFight);
		assertTrue(cleric1.getCurrentFight()==null);

		assertTrue(fighter1.getCurrentTarget()==null);
		assertTrue(fighter2.getCurrentTarget()==null);
		assertTrue(goblin1.getCurrentTarget()==null);
	}

	@Test
	public void testResolve() {
		fighter1.setCombatDecider(alwaysAttack);
		fighter2.setCombatDecider(alwaysAttack);
		goblin1.setCombatDecider(alwaysAttack);
		mainFight.resolve();
		assertTrue(goblin1.isDead());
		// there really should be no way for the goblin to win given the odds

		// We should also have tidied up
		assertTrue(fighter1.getCurrentFight()==null);
		assertTrue(fighter2.getCurrentFight()==null);
		assertTrue(goblin1.getCurrentFight()==null);
		assertTrue(cleric1.getCurrentFight()==null);

		assertTrue(fighter1.getCurrentTarget()==null);
		assertTrue(fighter2.getCurrentTarget()==null);
		assertTrue(goblin1.getCurrentTarget()==null);
	}

	@Test
	public void testOneRound() {

		// first we have to make sure we know the actions undertaken
		fighter1.setCombatDecider(alwaysAttack);
		fighter2.setCombatDecider(alwaysAttack);
		goblin1.setCombatDecider(alwaysAttack);
		goblin1.setMaxHp(20); // to ensure it's not killed by two attacks

		mainFight.oneRound();
		assertEquals(fighter1.getCurrentTarget(), gcca1);
		assertEquals(fighter2.getCurrentTarget(), gcca1);
		assertEquals(mainFight.getCurrentRound(), 1, 0);
		CombatAgent goblinTarget = goblin1.getCurrentTarget();
		boolean f1 = (goblinTarget.equals(fcca1));
		boolean f2 = (goblinTarget.equals(fcca2));
		assertTrue(f1 || f2);
		assertTrue(!f1 || !f2);
		assertTrue(goblin1.isTargetted());
		assertTrue(fighter1.isInCombat());
		if (f1) assertTrue(fighter1.isTargetted() && !fighter2.isTargetted());
		if (f2) assertTrue(fighter2.isTargetted() && !fighter1.isTargetted());

		fighter1.addHp(10, false);
		fighter2.addHp(10, false);
		fighter1.setCombatDecider(alwaysDefend);
		fighter2.setCombatDecider(alwaysDefend);
		mainFight.oneRound();

		// Defend still leaves engagement the same for a of o
		assertTrue(fighter1.getCurrentTarget()==gcca1);
		assertEquals(mainFight.getOpponent(fcca1, false), gcca1);
		assertTrue(fighter2.getCurrentTarget()==gcca1);
		assertEquals(mainFight.getOpponent(fcca2, false), gcca1);
		assertEquals(mainFight.getCurrentRound(), 2, 0);
		CombatAgent newGoblinTarget = goblin1.getCurrentTarget();
		assertEquals(goblinTarget, newGoblinTarget);
		assertTrue(goblin1.isTargetted());
		assertTrue(fighter1.isInCombat());
		if (f1) {
			assertTrue(fighter1.isTargetted() && !fighter2.isTargetted());
			assertEquals(mainFight.getOpponent(gcca1, false), fcca1);
		}
		if (f2) {
			assertTrue(fighter2.isTargetted() && !fighter1.isTargetted());
			assertEquals(mainFight.getOpponent(gcca1, false), fcca2);
		}

		fighter1.addHp(10, false);
		fighter2.addHp(10, false);
		fighter1.setCombatDecider(alwaysRetreat);
		fighter2.setCombatDecider(alwaysRetreat);
		mainFight.oneRound();

		// Retreat should move to reserve
		// However goblin may - or may not - still be targetting one depending on initiative order
		assertTrue(fighter1.getCurrentTarget()==null);
		assertTrue(fighter2.getCurrentTarget()==null);
		assertEquals(mainFight.getCurrentRound(), 3, 0);
		assertTrue(!goblin1.isTargetted());
		assertTrue(fighter1.isInCombat());

	}

	@Test
	public void testGetAttackersOf() {
		// first we have to make sure we know the actions undertaken
		fighter1.setCombatDecider(alwaysAttack);
		fighter2.setCombatDecider(alwaysAttack);
		goblin1.setCombatDecider(alwaysAttack);
		goblin1.setMaxHp(20); // to ensure it's not killed by two attacks

		mainFight.oneRound();

		ArrayList<CombatAgent> attackersOfGoblin =  mainFight.getAttackersOf(gcca1);
		assertTrue(attackersOfGoblin.contains(fcca1));
		assertTrue(attackersOfGoblin.contains(fcca2));
		assertEquals(attackersOfGoblin.size(), 2, 0);
	}

	@Test
	public void testGetOpponent() {
		// Before starting combat, there should be no opponent
		assertTrue(mainFight.getOpponent(fcca1, false)==null);
		// But we should be able to force a choice
		assertTrue(mainFight.getOpponent(fcca1, true)==gcca1);
	}

	@Test
	public void testMoveToReserve() {
		// first we have to make sure we know the actions undertaken
		fighter1.setCombatDecider(alwaysAttack);
		fighter2.setCombatDecider(alwaysAttack);
		goblin1.setCombatDecider(alwaysAttack);
		goblin1.setMaxHp(20); // to ensure it's not killed by two attacks

		mainFight.oneRound();

		// Move fighter1 to Reserve. Now goblin should only be targetted by fighter 2
		mainFight.moveToReserve(fcca1);

		assertTrue(fighter1.getCurrentTarget()==null);
		assertTrue(fighter2.getCurrentTarget()==gcca1);

		assertTrue(mainFight.getOpponent(fcca1, false)==null);
		assertTrue(mainFight.getAttackersOf(gcca1).get(0)==fcca2);
	}

	@Test
	public void testStunned() {
		// first we have to make sure we know the actions undertaken

		fighter1.setCombatDecider(new NeuralDecider(new ArrayList<ActionEnum>(EnumSet.allOf(CombatActionsI.class)),
				new ArrayList<GeneticVariable>(EnumSet.allOf(CombatNeuronInputsReduced.class))));
		fighter2.setCombatDecider(new NeuralDecider(new ArrayList<ActionEnum>(EnumSet.allOf(CombatActionsI.class)),
				new ArrayList<GeneticVariable>(EnumSet.allOf(CombatNeuronInputsReduced.class))));
		goblin1.setCombatDecider(alwaysDefend);
		goblin1.setMaxHp(20); // to ensure it's not killed by two attacks

		fighter1.addHp(-fighter1.getHp(), false);
		assertTrue(fighter1.isStunned());

		assertTrue(fighter1.getExecutedActions().isEmpty());

		mainFight.oneRound();
		assertTrue(fighter1.getExecutedActions().isEmpty());
		assertTrue(fighter1.isStunned());
		assertTrue(!fighter2.isDead() && !fighter2.isStunned());
		if (fighter2.getExecutedActions().size() > 1) {
			System.out.println(fighter2.getExecutedActions());
			assertEquals(fighter2.getExecutedActions().size(), 2);
			assertTrue(fighter2.getExecutedActions().get(0).toString().equals("DISENGAGE"));
			assertTrue(fighter2.getExecutedActions().get(1).toString().equals("ATTACK"));
		} else {
			assertEquals(fighter2.getExecutedActions().size(), 1);
		}

		// Stunned should mean no action taken
	}

	@Test
	public void testStunnedII() {
		// first we have to make sure we know the actions undertaken

		fighter1.setCombatDecider(alwaysDefend);
		fighter2.setCombatDecider(alwaysDefend);
		goblin1.setCombatDecider(alwaysDefend);
		goblin1.setMaxHp(20); // to ensure it's not killed by two attacks

		fighter1.addHp(-fighter1.getHp(), true);
		assertTrue(fighter1.isStunned());

		assertTrue(fighter1.getExecutedActions().isEmpty());

		mainFight.oneRound();
		assertTrue(fighter1.getExecutedActions().isEmpty());
		assertTrue(fighter1.isStunned());

		fighter1.addHp(1, true);
		assertFalse(fighter1.isStunned());

		mainFight.oneRound();
		assertEquals(fighter1.getTempHp(), fighter1.getHp()-1);

	}

	@Test
	public void testRemoveCombatant() {
		// first we have to make sure we know the actions undertaken
		fighter1.setCombatDecider(alwaysAttack);
		fighter2.setCombatDecider(alwaysAttack);
		goblin1.setCombatDecider(alwaysAttack);
		goblin1.setMaxHp(20); // to ensure it's not killed by two attacks

		mainFight.oneRound();

		// Remove Goblin as combatant. Now neither fighter should have a target or any attackers

		mainFight.removeCombatant(gcca1);

		assertTrue(fighter1.getCurrentTarget()==null);
		assertTrue(fighter2.getCurrentTarget()==null);

		assertTrue(mainFight.getOpponent(fcca1, false)==null);
		assertTrue(mainFight.getAttackersOf(fcca1).size()==0);
		assertTrue(mainFight.getAttackersOf(fcca2).size()==0);
	}

	@Test
	public void testIsInternal() {
		// set up a fight between cleric1 and fighter1
		cleric1.setCombatDecider(new HardCodedDecider(CombatActionsI.ATTACK));
		fighter1.setCombatDecider(new HardCodedDecider(CombatActionsI.ATTACK));
		fighter2.setCombatDecider(new HardCodedDecider(CombatActionsI.ATTACK));
		goblin1.setCombatDecider(new HardCodedDecider(CombatActionsI.DEFEND));
		assertTrue(!mainFight.isInternal());
		mainFight.resolve();
		party.addMember(cleric1);
		Fight f = new Fight(fighter1, cleric1);
		assertTrue(f.isInternal());
	}

	@Test
	public void testAddCombatant() {
		mainFight.getOpponent(fcca1, true);
		mainFight.getOpponent(fcca2, true);
		mainFight.getOpponent(gcca1, true);
		assertTrue(fcca1.getCurrentTarget()==gcca1);
		assertTrue(fcca2.getCurrentTarget()==gcca1);
		// We start with the main fight
		// and add a new person (cleric1)
		Dice.setNextRoll(20); // for the Cleric's initiative - to ensure it leapfrogs and is not at end
		mainFight.addCombatant(cleric1.getCombatAgent(), fcca1);
		assertTrue(cleric1.getCurrentFight() == mainFight);
		assertTrue(cleric1.isInCombat());
		assertTrue(fcca1.getCurrentTarget()==gcca1); // to check 
		assertTrue(fcca2.getCurrentTarget()==gcca1); // opponent targets have not been upset
		assertTrue(mainFight.getOpponent(fcca1, false)==gcca1);
		assertTrue(mainFight.getOpponent(fcca2, false)==gcca1);

		// check that the total number of combatants has increased
		assertEquals(mainFight.getSideSize(fcca1), 3, 0);
		assertEquals(mainFight.getSideSize(gcca1), 1, 0);

		// then test that getOpponent (forced) works, and this that is one the goblin
		// and that this goblin now has one attacker (ad ten two when the fighter also targets it)
		assertTrue(mainFight.getOpponent(cleric1.getCombatAgent(), true) == gcca1);
		assertTrue(cleric1.getCurrentTarget() == gcca1);
		assertEquals(mainFight.getAttackersOf(gcca1).size(), 3, 0);

		// then test that a goblin can target the new combatant, and that this combatant now has one attacker
		mainFight.setTarget(gcca1, cleric1.getCombatAgent());
		assertEquals(mainFight.getAttackersOf(fcca1).size(), 0, 0);
		assertEquals(mainFight.getAttackersOf(cleric1.getCombatAgent()).size(), 1, 0);

		fcca1.setCombatDecider(alwaysDefend);
		fcca2.setCombatDecider(alwaysDefend);
		gcca1.setCombatDecider(alwaysDefend);
		cleric1.setCombatDecider(alwaysDefend);
		mainFight.oneRound();
		assertTrue(fcca1.getCurrentTarget()==gcca1); // to check 
		assertTrue(fcca2.getCurrentTarget()==gcca1); // opponent targets have not been upset
		assertTrue(mainFight.getOpponent(fcca1, false)==gcca1);
		assertTrue(gcca1.getCurrentTarget()==cleric1.getCombatAgent());
	}

	@Test
	public void testFiveOpponents() {
		Character fighter3, fighter4, fighter5, fighter6;
		fighter3 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		fighter4 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		fighter5 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		fighter6 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		Party p2 = new Party(cleric1);
		p2.addMember(fighter5);
		p2.addMember(fighter3);
		p2.addMember(fighter4);
		p2.addMember(fighter6);
		fighter3.setCombatDecider(alwaysAttack);
		fighter4.setCombatDecider(alwaysAttack);
		fighter5.setCombatDecider(alwaysAttack);
		fighter6.setCombatDecider(alwaysAttack);
		cleric1.setCombatDecider(alwaysAttack);

		Character goblin = new Character(Race.GOBLIN, CharacterClass.WARRIOR, w);
		goblin.setCombatDecider(alwaysAttack);
		goblin.levelUp();
		goblin.levelUp();
		goblin.levelUp();
		goblin.addItem(Armour.CHAIN_SHIRT);
		goblin.addItem(Shield.LARGE_SHIELD);

		Fight f = new Fight(p2, goblin);
		f.oneRound();

		// At this stage, four of the party should be attacking the goblin - and one not
		assertEquals(f.getAttackersOf(goblin.getCombatAgent()).size(), 4, 0);
		int attacking = 0;
		attacking += attacking(cleric1, goblin);
		attacking += attacking(fighter3, goblin);
		attacking += attacking(fighter4, goblin);
		attacking += attacking(fighter5, goblin);
		attacking += attacking(fighter6, goblin);

		assertEquals(attacking, 4, 0);

	}

	@Test
	public void testDisengage() {
		for (int n=0; n<10; n++) {
			cleric1 = new Character(Race.HUMAN, CharacterClass.CLERIC, w);
			testDisengageHelper();
		}
	}

	@Test
	public void testDisengageOpponent(){
		// Opponent gets AofO even if not attacking directly
		fighter2.removeItem(Shield.LARGE_SHIELD);
		goblin1.setDexterity(new Attribute(16));
		goblin1.setStrength(new Attribute(16));
		goblin1.levelUp();
		goblin1.levelUp();
		goblin1.addItem(Armour.CHAIN_SHIRT);
		goblin1.addItem(Shield.LARGE_SHIELD);
		goblin1.setCombatDecider(alwaysDefend);

		fighter1.addHp(-fighter1.getHp()+1, false);
		fighter2.addHp(-fighter2.getHp()+1, false);

		for (int n = 0; n< 10; n++) {

			fighter1.setCombatDecider(alwaysAttack);
			fighter2.setCombatDecider(alwaysAttack);

			mainFight.oneRound();
			// at this stage the two fighter should both be attacking the goblin
			assertTrue(!goblin1.isDead());


			fighter1.setCombatDecider(alwaysDisengage);
			fighter2.setCombatDecider(alwaysDisengage);

			mainFight.oneRound();

			assertTrue(fighter1.isDead() || fighter2.isDead());
		}

	}

	public void testDisengageHelper() {
		cleric1.setCombatDecider(alwaysDisengage);
		goblin1.setCombatDecider(alwaysAttack);
		goblin1.levelUp();
		goblin1.addItem(Armour.CHAIN_SHIRT);
		ArrayList<CombatAgent> temp = null;
		Fight f = new Fight(cleric1, goblin1);
		for (int n=0; n<200; n++) {
			f.oneRound();
			goblin1.addHp(10, false);
			assertFalse(goblin1.isDead());
			if (cleric1.isDead()) {
				temp = f.getSide(cleric1.getCombatAgent());
				assertFalse(temp.contains(cleric1.getCombatAgent()));
			}
		}
		assertTrue(cleric1.isDead());
		assertFalse(goblin1.isDead());
		temp = f.getSide(cleric1.getCombatAgent());
		assertFalse(temp.contains(cleric1.getCombatAgent()));
	}

	private int attacking(Character a, Character d) {
		//This tests that the attacker is right, and that the one that is not attacking
		// cannot be forced to attack, as there are no slots left
		if (a.getCurrentTarget()==d.getCombatAgent()) {
			assertTrue(a.getCurrentFight().getOpponent(a.getCombatAgent(), false)==d.getCombatAgent());
			return 1;
		}
		assertTrue(a.getCurrentFight().getOpponent(a.getCombatAgent(), false)==null);
		assertTrue(a.getCurrentFight().getOpponent(a.getCombatAgent(), true)==null);
		return 0;
	}

	public static BaseDecider alwaysAttack = new HardCodedDecider(CombatActionsI.ATTACK);
	public static BaseDecider alwaysDefend = new HardCodedDecider(CombatActionsI.DEFEND);
	public static BaseDecider alwaysDisengage = new HardCodedDecider(CombatActionsI.DISENGAGE);
	public static BaseDecider alwaysRetreat = new HardCodedDecider(CombatActionsI.RETREAT);
	public static BaseDecider alwaysBuff = new HardCodedDecider(CombatActionsI.BUFF);
	public static BaseDecider alwaysSpellAttack = new HardCodedDecider(CombatActionsI.SPELL_ATTACK); 
	public static BaseDecider alwaysHealPotion = new HardCodedDecider(CombatActionsI.HEAL_ITEM);
}
