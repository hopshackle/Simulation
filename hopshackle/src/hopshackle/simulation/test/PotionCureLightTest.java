package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.genetics.TradeDecider;

import org.junit.*;

public class PotionCureLightTest {

	CharacterCombatAgent testcca, gcca1, gcca2, fcca1, fcca2, gcca3;
	World w;
	Character goblin1, goblin2, goblin3;
	Character fighter1, cleric1, fighter2;
	Party party;

	Fight mainFight;

	@Before
	public void setUp() throws Exception {
		// Let's set up the Fight we want to use

		// We will have two fighters vs one goblin
		SimProperties.setProperty("StartWithPotion", "false");
		
		w = new World();
		CharacterClass.FIGHTER.setDefaultTradeDecider(new TradeDecider());

		goblin1 = new Character(Race.GOBLIN, CharacterClass.WARRIOR, w);
		goblin1.setStrength(new Attribute(12));
		goblin1.setDexterity(new Attribute(14));
		goblin1.setConstitution(new Attribute(10));
		goblin1.setMaxHp(8);
		goblin1.addItem(Weapon.LIGHT_MACE);

		gcca1 = (CharacterCombatAgent) goblin1.getCombatAgent();
		
		goblin2 = new Character(Race.GOBLIN, CharacterClass.WARRIOR, w);
		goblin2.setStrength(new Attribute(12));
		goblin2.setDexterity(new Attribute(14));
		goblin2.setConstitution(new Attribute(10));
		goblin2.setMaxHp(8);
		goblin2.addItem(Weapon.LIGHT_MACE);

		gcca2 = (CharacterCombatAgent) goblin2.getCombatAgent();
		
		goblin3 = new Character(Race.GOBLIN, CharacterClass.WARRIOR, w);
		goblin3.setStrength(new Attribute(12));
		goblin3.setDexterity(new Attribute(14));
		goblin3.setConstitution(new Attribute(10));
		goblin3.setMaxHp(8);
		goblin3.addItem(Weapon.LIGHT_MACE);

		gcca3 = (CharacterCombatAgent) goblin3.getCombatAgent();

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

		fighter1.setCombatDecider(TestFight.alwaysDefend);
		fighter2.setCombatDecider(TestFight.alwaysDefend);
		cleric1.setCombatDecider(TestFight.alwaysDefend);
		goblin1.setCombatDecider(TestFight.alwaysDefend);
		goblin2.setCombatDecider(TestFight.alwaysDefend);
		goblin3.setCombatDecider(TestFight.alwaysDefend);
		fighter1.setCombatDecider(TestFight.alwaysDefend);
		
		Party goblins = new Party(goblin1);
		goblins.addMember(goblin2);
		goblins.addMember(goblin3);
		mainFight = new Fight(party, goblins);
	}

	@Test
	public void testPotionStats() {
		Potion p = Potion.CURE_LIGHT_WOUNDS;

		assertEquals(p.getBaseCost(), 25, 0.01);
		assertEquals(p.getMakeDC(), 16);
		assertEquals(p.getRecipe().getIngredients().get(Component.ORC_HEART), 5, 0.01);
		assertEquals(p.getOneOffHeal(), 5.5, 0.01);
	}

	@Test
	public void testDrinkingPotionInCombat() {
		fighter1.setCombatDecider(TestFight.alwaysHealPotion);
		assertFalse(fighter1.getInventory().contains(Potion.CURE_LIGHT_WOUNDS));
		fighter1.addItem(Potion.CURE_LIGHT_WOUNDS);
		fighter1.addHp(-5, false);
		
		assertTrue(fighter1.getInventory().contains(Potion.CURE_LIGHT_WOUNDS));
		int startHp = fighter1.getHp();
		mainFight.oneRound();
		
		assertFalse(fighter1.getInventory().contains(Potion.CURE_LIGHT_WOUNDS));
		assertTrue(fighter1.getHp() > startHp);
	}
	
	@Test 
	public void testProvokeAO1() {
		fighter1.setCombatDecider(TestFight.alwaysHealPotion);
		fighter1.addItem(Potion.CURE_LIGHT_WOUNDS);
		fighter1.addHp(-9, false);
		mainFight.setTarget(gcca1, fcca1);
		mainFight.setTarget(gcca2, fcca1);
		mainFight.setTarget(fcca1, gcca1);
		
		int sampleTotal =0;
		for (int n = 1; n < 50; n++) {
			sampleTotal += provokeAOOnce();
			if (fighter1.isDead())
				return;	// successful
			fighter1.addItem(Potion.CURE_LIGHT_WOUNDS);
			fighter1.addHp(100, false);
			fighter1.addHp(-9, false);
		}
		
		assertTrue(true);
	}
	
	@Test 
	public void testProvokeAO2() {
		// As AO1, but this will actually provoke AOs
		fighter1.setCombatDecider(TestFight.alwaysHealPotion);
		fighter1.addItem(Potion.CURE_LIGHT_WOUNDS);
		fighter1.addHp(-9, false);
		mainFight.setTarget(gcca1, fcca1);
		mainFight.setTarget(gcca2, fcca1);
		mainFight.setTarget(fcca1, gcca3);

		int sampleTotal =0;
		for (int n = 1; n < 50; n++) {
			sampleTotal += provokeAOOnce();
			if (fighter1.isDead())
				return;	// successful
			fighter1.addItem(Potion.CURE_LIGHT_WOUNDS);
			fighter1.addHp(100, false);
			fighter1.addHp(-9, false);
		}
		
		assertTrue(false);
	}
	
	@Test 
	public void testProvokeAO3() {
		fighter1.setCombatDecider(TestFight.alwaysHealPotion);
		fighter1.addItem(Potion.CURE_LIGHT_WOUNDS);
		fighter1.addHp(-2, false);
		mainFight.setTarget(gcca1, fcca1);
		mainFight.setTarget(gcca2, fcca1);
		mainFight.setTarget(fcca1, gcca1);

		int sampleTotal =0;
		for (int n = 1; n < 50; n++) {
			sampleTotal += provokeAOOnce();
			if (fighter1.isDead())
				assertTrue(false);
			fighter1.addItem(Potion.CURE_LIGHT_WOUNDS);
			fighter1.addHp(100, false);
			fighter1.addHp(-2, false);
		}
		
		assertTrue(true);
	}

	private int provokeAOOnce() {
		int startHp = fighter1.getHp();
		mainFight.oneRound();
		return fighter1.getHp() - startHp;
	}
	
	@Test
	public void testRest() {
		fighter1.addHp(-6, false);
		fighter1.addItem(Potion.CURE_LIGHT_WOUNDS);
		
		fighter1.rest(1);
		assertTrue(fighter1.getInventory().contains(Potion.CURE_LIGHT_WOUNDS));
		assertEquals(fighter1.getHp(), 6);
		
		fighter1.addHp(-2, false);
		fighter1.rest(1);
		assertTrue(fighter1.getInventory().contains(Potion.CURE_LIGHT_WOUNDS));
		assertEquals(fighter1.getHp(), 5);
		
		Square sq = new Square(0,1);
		fighter1.setLocation(sq);
		
		fighter1.rest(1);
		assertTrue(fighter1.getInventory().contains(Potion.CURE_LIGHT_WOUNDS));
		assertEquals(fighter1.getHp(), 6);
		
		fighter1.addHp(-2, false);
		fighter1.rest(1);
		assertFalse(fighter1.getInventory().contains(Potion.CURE_LIGHT_WOUNDS));
		assertTrue(fighter1.getHp() > 6);
		
		
	}
}
