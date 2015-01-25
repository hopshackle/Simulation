package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

import java.io.File;
import java.util.ArrayList;

import org.junit.*;
public class TestClericSpells {

	Character goblin1, goblin2, cleric1, fighter1, fighter2;
	Character goblin3, goblin4, cleric2, fighter3, fighter4, goblin5;
	Party pcParty1, pcParty2, npcParty1, npcParty2;
	Fight fight1, fight2;
	static World w = new World();
	private static String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");

	@Before
	public void setUp() throws Exception {

		/* We want to set up a test Fight with the following participants:
		 - 1 cleric (set to always Buff)
		 - 2 fighters (set to always Defend)
		 - 2 goblins (set to always Defend)

		All second level. The cleric needs to have 3 magic points (Wis 14)

		We also need to force a getOpponent on fighters and goblins, elsewise with them
		all Defending they won't.

		Then another fight with the same, but all third level. The Cleric should have 8 magic points (Wis 14)

		 */

		goblin1 = new Character(Race.GOBLIN, CharacterClass.WARRIOR, w);
		goblin1.addItem(Weapon.LIGHT_MACE);
		goblin1.setStrength(new Attribute(14));

		goblin2 = new Character(Race.GOBLIN, CharacterClass.WARRIOR, w);;
		goblin2.addItem(Weapon.LIGHT_MACE);
		goblin2.setStrength(new Attribute(14));

		goblin5 = new Character(Race.GOBLIN, CharacterClass.WARRIOR, w);;
		goblin5.addItem(Weapon.LIGHT_MACE);
		goblin5.setStrength(new Attribute(14));

		fighter1 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		fighter1.addItem(Weapon.SHORT_SWORD);

		fighter2 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		fighter2.addItem(Weapon.SHORT_SWORD);
		fighter2.addItem(Shield.LARGE_SHIELD);

		Genome clrGenome = new Genome(new File(baseDir + "\\Genomes\\Standard\\CLR_TEST_SPELL.txt"));
		cleric1 = new Character(Race.HUMAN, CharacterClass.CLERIC, null, clrGenome, w);
		cleric1.setWisdom(new Attribute(14));
		cleric1.addItem(Weapon.HEAVY_MACE);
		cleric1.levelUp();
		cleric1.rest(1); // will set magic points to max mp
		assertEquals(cleric1.getMp(), 3, 0);


		goblin1.setCombatDecider(TestFight.alwaysDefend);
		goblin2.setCombatDecider(TestFight.alwaysDefend);
		goblin5.setCombatDecider(TestFight.alwaysDefend);
		fighter1.setCombatDecider(TestFight.alwaysDefend);
		fighter2.setCombatDecider(TestFight.alwaysDefend);
		cleric1.setCombatDecider(TestFight.alwaysDefend);

		pcParty1 = new Party(cleric1);
		pcParty1.addMember(fighter2);
		pcParty1.addMember(fighter1);

		npcParty1 = new Party(goblin1);
		npcParty1.addMember(goblin2);
		npcParty1.addMember(goblin5);
		fight1 = new Fight(pcParty1, npcParty1);

		do {
			fight1.moveToReserve(goblin1.getCombatAgent());
			fight1.getOpponent(goblin1.getCombatAgent(), true);
		} while (!fighter1.isTargetted());
		assertTrue(goblin1.getCurrentTarget().equals(fighter1.getCombatAgent()));
		assertTrue(!fighter2.isTargetted());
		assertTrue(!cleric1.isTargetted());

		do {
			fight1.moveToReserve(goblin2.getCombatAgent());
			fight1.getOpponent(goblin2.getCombatAgent(), true);
		} while (!fighter2.isTargetted());
		assertTrue(goblin2.getCurrentTarget().equals(fighter2.getCombatAgent()));

		fight1.setTarget(fighter1.getCombatAgent(), goblin1.getCombatAgent());
		assertTrue(fighter1.getCurrentTarget().equals(goblin1.getCombatAgent()));
		assertTrue(!goblin2.isTargetted());
		assertTrue(fighter1.isTargetted());

		fight1.setTarget(fighter2.getCombatAgent(), goblin2.getCombatAgent());
		assertTrue(fighter2.getCurrentTarget().equals(goblin2.getCombatAgent()));
		assertTrue(fighter2.isTargetted());
		assertTrue(goblin2.isTargetted());
		assertTrue(fighter1.isTargetted());
		assertTrue(goblin1.isTargetted());
		assertTrue(!cleric1.isTargetted());

		// So we now have fight1 set up, with goblin1 and fighter1 targeting each other
		// and goblin2 and fighter2 targeting each other
		// All are set to Always Defend - so they're not going to actually attack each other

		goblin3 = new Character(Race.GOBLIN, CharacterClass.WARRIOR, w);
		goblin3.addItem(Weapon.LIGHT_MACE);
		goblin3.addItem(Armour.BANDED_MAIL);
		goblin3.setStrength(new Attribute(14));

		goblin4 = new Character(Race.GOBLIN, CharacterClass.WARRIOR, w);
		goblin4.addItem(Weapon.LIGHT_MACE);
		goblin4.addItem(Shield.TOWER_SHIELD);
		goblin4.setStrength(new Attribute(14));

		fighter3 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		fighter3.addItem(Weapon.SHORT_SWORD);
		double temp = fighter3.getCombatAgent().getHitChance(null, null);
		fighter3.levelUp();
		fighter3.getCombatAgent().setCurrentCM(null);
		assertEquals(fighter3.getLevel(), 2, 0);
		double temp1 = fighter3.getCombatAgent().getHitChance(null, null);
		assertEquals(temp1-temp, 0.05, 0.00001);

		fighter4 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		fighter4.addItem(Weapon.SHORT_SWORD);
		fighter4.addItem(Shield.LARGE_SHIELD);

		cleric2 = new Character(Race.HUMAN, CharacterClass.CLERIC, null, clrGenome, w);
		cleric2.setWisdom(new Attribute(14));
		cleric2.addItem(Weapon.HEAVY_MACE);
		cleric2.levelUp();
		cleric2.levelUp();
		cleric2.rest(1); // will set magic points to max mp
		assertEquals(cleric2.getMp(), 9, 0);

		goblin3.setCombatDecider(TestFight.alwaysDefend);
		goblin4.setCombatDecider(TestFight.alwaysDefend);
		fighter3.setCombatDecider(TestFight.alwaysDefend);
		fighter4.setCombatDecider(TestFight.alwaysDefend);
		cleric2.setCombatDecider(TestFight.alwaysDefend);

		pcParty2 = new Party(cleric2);
		pcParty2.addMember(fighter4);
		pcParty2.addMember(fighter3);

		npcParty2 = new Party(goblin3);
		npcParty2.addMember(goblin4);
		fight2 = new Fight(pcParty2, npcParty2);

		fight2.setTarget(goblin3.getCombatAgent(), fighter3.getCombatAgent());
		fight2.setTarget(goblin4.getCombatAgent(), fighter4.getCombatAgent());
		fight2.setTarget(fighter3.getCombatAgent(), goblin3.getCombatAgent());
		fight2.setTarget(fighter4.getCombatAgent(), goblin4.getCombatAgent());
	}

	@Test
	public void testBless() {
		fight1.oneRound();
		// In the first round, all Defend, and no spells are cast

		double f1Hit = fighter1.getCombatAgent().getHitChance(null, null);
		double f2Hit = fighter2.getCombatAgent().getHitChance(null, null);
		double g1Hit = goblin1.getCombatAgent().getHitChance(null, null);
		double g2Hit = goblin2.getCombatAgent().getHitChance(null, null);
		double c1Hit = cleric1.getCombatAgent().getHitChance(null, null);

		// In the second round, Bless should be cast - so we set the Cleric to start buffing
		cleric1.setCombatDecider(TestFight.alwaysBuff);

		fight1.oneRound();
		assertEquals(cleric1.getMp(), 2, 0);
		ArrayList<Spell> spellsCast = cleric1.getActiveSpells();
		assertEquals(spellsCast.size(), 1, 0);
		assertTrue(spellsCast.get(0) instanceof Bless);
		Bless bless = (Bless) spellsCast.get(0);
		assertTrue(bless.isActive());

		// We then test that the fighters (and cleric) are getting the benefit of the +1
		// and that the goblins are not
		double newf1Hit = fighter1.getCombatAgent().getHitChance(null, null);
		double newf2Hit = fighter2.getCombatAgent().getHitChance(null, null);
		double newg1Hit = goblin1.getCombatAgent().getHitChance(null, null);
		double newg2Hit = goblin2.getCombatAgent().getHitChance(null, null);
		double newc1Hit = cleric1.getCombatAgent().getHitChance(null, null);
		assertEquals(newf1Hit-f1Hit, 0.05, 0.0001);
		assertEquals(newf2Hit-f2Hit, 0.05, 0.0001);
		assertEquals(newc1Hit-c1Hit, 0.05, 0.0001);
		assertEquals(newg1Hit-g1Hit, 0.00, 0.0001);
		assertEquals(newg2Hit-g2Hit, 0.00, 0.0001);

		// We then set Cleric to always Defend
		cleric1.setCombatDecider(TestFight.alwaysDefend);

		// and iterate through 20 rounds  to confirm that the Bless is removed at the right point, and the 
		// fighter's benefit ceases

		for (int n = 1; n < 19 ; n++) 
			fight1.oneRound();

		newf1Hit = fighter1.getCombatAgent().getHitChance(null, null);
		newf2Hit = fighter2.getCombatAgent().getHitChance(null, null);
		newg1Hit = goblin1.getCombatAgent().getHitChance(null, null);
		newg2Hit = goblin2.getCombatAgent().getHitChance(null, null);
		newc1Hit = cleric1.getCombatAgent().getHitChance(null, null);
		assertEquals(newf1Hit-f1Hit, 0.05, 0.0001);
		assertEquals(newf2Hit-f2Hit, 0.05, 0.0001);
		assertEquals(newc1Hit-c1Hit, 0.05, 0.0001);
		assertEquals(newg1Hit-g1Hit, 0.00, 0.0001);
		assertEquals(newg2Hit-g2Hit, 0.00, 0.0001);
		assertTrue(bless.isActive());

		fight1.oneRound();
		fight1.oneRound();

		// we have to move on two rounds, as we do not know exactly where the
		// cleric is in the initiative order. Hence in the two border rounds
		// each fighter may - or may not - have the Bless benefit

		newf1Hit = fighter1.getCombatAgent().getHitChance(null, null);
		newf2Hit = fighter2.getCombatAgent().getHitChance(null, null);
		newg1Hit = goblin1.getCombatAgent().getHitChance(null, null);
		newg2Hit = goblin2.getCombatAgent().getHitChance(null, null);
		newc1Hit = cleric1.getCombatAgent().getHitChance(null, null);
		assertEquals(newf1Hit-f1Hit, 0.00, 0.0001);
		assertEquals(newf2Hit-f2Hit, 0.00, 0.0001);
		assertEquals(newc1Hit-c1Hit, 0.00, 0.0001);
		assertEquals(newg1Hit-g1Hit, 0.00, 0.0001);
		assertEquals(newg2Hit-g2Hit, 0.00, 0.0001);
		assertTrue(!bless.isActive());
	}

	@Test
	public void testConcentrationRoll() {

		Bless b = new Bless();
		for(int n=0; n<20; n++)
			assertFalse(b.isDistracted(cleric1));

		fight1.setTarget(goblin1.getCombatAgent(), cleric1.getCombatAgent());
		for(int n=0; n<20; n++)
			assertFalse(b.isDistracted(cleric1));

		fight1.setTarget(goblin2.getCombatAgent(), cleric1.getCombatAgent());
		for(int n=0; n<20; n++)
			assertFalse(b.isDistracted(cleric1));

		fight1.setTarget(goblin5.getCombatAgent(), cleric1.getCombatAgent());
		boolean distracted = false;
		for(int n=0; n<20; n++)
			if (b.isDistracted(cleric1)) {
				distracted = true;
				break;
			}
		assertTrue(distracted);
	}

	@Test
	public void testShieldOfFaith() {
		fight1.oneRound();
		// In the first round, all Defend, and no spells are cast
		
		cleric1.addSpell(new ShieldOfFaith());
		// as the std choice at second level is MAgicWeapon

		// In the second round, Bless should be cast - so we set the Cleric to start buffing
		cleric1.setCombatDecider(TestFight.alwaysBuff);

		fight1.oneRound();
		assertEquals(cleric1.getMp(), 2, 0);
		ArrayList<Spell> spellsCast = cleric1.getActiveSpells();
		assertEquals(spellsCast.size(), 1, 0);
		assertTrue(spellsCast.get(0) instanceof Bless);
		// then in the third round, the Cleric should cast Shield of Faith on someone who is targeted by the 
		// goblins

		double f1Hit = fighter1.getCombatAgent().getHitChance(null, null);
		double f2Hit = fighter2.getCombatAgent().getHitChance(null, null);
		double g1Hit = goblin1.getCombatAgent().getHitChance(null, null);
		double g2Hit = goblin2.getCombatAgent().getHitChance(null, null);
		double c1Hit = cleric1.getCombatAgent().getHitChance(null, null);

		fight1.oneRound();
		assertEquals(cleric1.getMp(), 1, 0);
		double newf1Hit = fighter1.getCombatAgent().getHitChance(null, null);
		double newf2Hit = fighter2.getCombatAgent().getHitChance(null, null);
		double newg1Hit = goblin1.getCombatAgent().getHitChance(null, null);
		double newg2Hit = goblin2.getCombatAgent().getHitChance(null, null);
		double newc1Hit = cleric1.getCombatAgent().getHitChance(null, null);


		// and check that the spell is active
		spellsCast = cleric1.getActiveSpells();
		assertEquals(spellsCast.size(), 2, 0);
		assertTrue(spellsCast.get(1) instanceof ShieldOfFaith);
		ShieldOfFaith shieldOfFaith = (ShieldOfFaith) spellsCast.get(1);
		assertTrue(shieldOfFaith.isActive());

		// test that one fighter (and only one) is getting the +2 benefit to armour class
		assertEquals(newg1Hit + newg2Hit - g1Hit - g2Hit, -0.10, 0.0001);
		assertEquals(newf1Hit + newf2Hit - f1Hit - f2Hit, 0, 0.0001);
		assertEquals(newc1Hit - c1Hit, 0, 0.0001);

		// then restore the Cleric's magic points back to 3
		cleric1.rest(1);
		assertEquals(cleric1.getMp(), 3, 0);

		// then in the fourth round, the Cleric should cast Shield of Faith on the person targeted by the other goblin
		fight1.oneRound();
		assertEquals(cleric1.getMp(), 2, 0);
		newf1Hit = fighter1.getCombatAgent().getHitChance(null, null);
		newf2Hit = fighter2.getCombatAgent().getHitChance(null, null);
		newg1Hit = goblin1.getCombatAgent().getHitChance(null, null);
		newg2Hit = goblin2.getCombatAgent().getHitChance(null, null);
		newc1Hit = cleric1.getCombatAgent().getHitChance(null, null);

		assertEquals(newg1Hit - g1Hit, -0.10, 0.0001);
		if (newg2Hit > g2Hit - 0.05) {
			int n = 1;
		}
		assertEquals(newg2Hit - g2Hit, -0.10, 0.0001);
		assertEquals(newf1Hit + newf2Hit - f1Hit - f2Hit, 0, 0.0001);
		assertEquals(newc1Hit - c1Hit, 0, 0.0001);

		// then in the fourth round, the Cleric should not cast Shield of Faith, as there are no targeted 
		// party members left

		fight1.oneRound();
		assertEquals(cleric1.getMp(), 2, 0);
		newf1Hit = fighter1.getCombatAgent().getHitChance(null, null);
		newf2Hit = fighter2.getCombatAgent().getHitChance(null, null);
		newg1Hit = goblin1.getCombatAgent().getHitChance(null, null);
		newg2Hit = goblin2.getCombatAgent().getHitChance(null, null);
		newc1Hit = cleric1.getCombatAgent().getHitChance(null, null);

		assertEquals(newg1Hit - g1Hit, -0.10, 0.0001);
		assertEquals(newg2Hit - g2Hit, -0.10, 0.0001);
		assertEquals(newf1Hit + newf2Hit - f1Hit - f2Hit, 0, 0.0001);
		assertEquals(newc1Hit - c1Hit, 0, 0.0001);
		assertTrue(shieldOfFaith.isActive());
		spellsCast = cleric1.getActiveSpells();
		assertEquals(spellsCast.size(), 3, 0); 	// 1 Bless, 2 Shields of Faith
	}

	@Test
	public void testBullsStrength() {
		fight2.oneRound();
		// In the first round, all Defend, and no spells are cast

		// In the second round, Bless should be cast - so we set the Cleric to start buffing
		cleric2.setCombatDecider(TestFight.alwaysBuff);
		cleric2.addSpell(new ShieldOfFaith());

		fight2.oneRound();
		assertEquals(cleric2.getMp(), 8, 0);
		// then in the third round, the Cleric should cast  Bulls Strength on someone who is attacking
		// a goblin

		double f3Hit = fighter3.getCombatAgent().getHitChance(null, null);
		double f4Hit = fighter4.getCombatAgent().getHitChance(null, null);
		double g3Hit = goblin3.getCombatAgent().getHitChance(null, null);
		double g4Hit = goblin4.getCombatAgent().getHitChance(null, null);
		double c2Hit = cleric2.getCombatAgent().getHitChance(null, null);
		double f3Dmg = fighter3.getCombatAgent().getCurrentCM().getTotalDamageBonus();
		double f4Dmg = fighter4.getCombatAgent().getCurrentCM().getTotalDamageBonus();
		double g3Dmg = goblin3.getCombatAgent().getCurrentCM().getTotalDamageBonus();
		double g4Dmg = goblin4.getCombatAgent().getCurrentCM().getTotalDamageBonus();
		double c2Dmg = cleric2.getCombatAgent().getCurrentCM().getTotalDamageBonus();

		// check that get the +4 Str modifier, and that this also applies to Damage as well as to Hit
		fight2.oneRound();
		assertEquals(cleric2.getMp(), 5, 0);
		double newf3Hit = fighter3.getCombatAgent().getHitChance(null, null);
		double newf4Hit = fighter4.getCombatAgent().getHitChance(null, null);
		double newg3Hit = goblin3.getCombatAgent().getHitChance(null, null);
		double newg4Hit = goblin4.getCombatAgent().getHitChance(null, null);
		double newc2Hit = cleric2.getCombatAgent().getHitChance(null, null);
		double newf3Dmg = fighter3.getCombatAgent().getCurrentCM().getTotalDamageBonus();
		double newf4Dmg = fighter4.getCombatAgent().getCurrentCM().getTotalDamageBonus();
		double newg3Dmg = goblin3.getCombatAgent().getCurrentCM().getTotalDamageBonus();
		double newg4Dmg = goblin4.getCombatAgent().getCurrentCM().getTotalDamageBonus();
		double newc2Dmg = cleric2.getCombatAgent().getCurrentCM().getTotalDamageBonus();

		assertEquals(newf3Hit + newf4Hit - f3Hit - f4Hit, 0.10, 0.00001);
		assertEquals(newg3Hit + newg4Hit - g3Hit - g4Hit, 0.00, 0.00001);
		assertEquals(newc2Hit - c2Hit, 0, 0.0001);
		assertEquals(newf3Dmg + newf4Dmg - f3Dmg - f4Dmg, 2, 0.0001);
		assertEquals(newg3Dmg + newg4Dmg, g3Dmg + g4Dmg, 0.00001);
		assertEquals(newc2Dmg, c2Dmg, 0.00001);

		// then in the fourth round, the Cleric should cast Bulls Strength on the other fighter
		fight2.oneRound();
		assertEquals(cleric2.getMp(), 2, 0);
		newf3Hit = fighter3.getCombatAgent().getHitChance(null, null);
		newf4Hit = fighter4.getCombatAgent().getHitChance(null, null);
		newg3Hit = goblin3.getCombatAgent().getHitChance(null, null);
		newg4Hit = goblin4.getCombatAgent().getHitChance(null, null);
		newc2Hit = cleric2.getCombatAgent().getHitChance(null, null);
		newf3Dmg = fighter3.getCombatAgent().getCurrentCM().getTotalDamageBonus();
		newf4Dmg = fighter4.getCombatAgent().getCurrentCM().getTotalDamageBonus();
		newg3Dmg = goblin3.getCombatAgent().getCurrentCM().getTotalDamageBonus();
		newg4Dmg = goblin4.getCombatAgent().getCurrentCM().getTotalDamageBonus();
		newc2Dmg = cleric2.getCombatAgent().getCurrentCM().getTotalDamageBonus();

		assertEquals(newf3Hit - f3Hit, 0.10, 0.00001);
		assertEquals(newf4Hit - f4Hit, 0.10, 0.00001);
		assertEquals(newg3Hit + newg4Hit - g3Hit - g4Hit, 0.00, 0.00001);
		assertEquals(newc2Hit - c2Hit, 0, 0.0001);
		assertEquals(newf3Dmg - f3Dmg, 2, 0.0001);		
		assertEquals(newf4Dmg - f4Dmg, 2, 0.0001);
		assertEquals(newg3Dmg + newg4Dmg, g3Dmg + g4Dmg, 0.00001);
		assertEquals(newc2Dmg, c2Dmg, 0.00001);

		// and finally, in the fifth round, with only two Magic points, they should cast Shield of Faith
		// on someone who is targeted by a goblin
		fight2.oneRound();
		assertEquals(cleric2.getMp(), 1, 0);
		newf3Hit = fighter3.getCombatAgent().getHitChance(null, null);
		newf4Hit = fighter4.getCombatAgent().getHitChance(null, null);
		newg3Hit = goblin3.getCombatAgent().getHitChance(null, null);
		newg4Hit = goblin4.getCombatAgent().getHitChance(null, null);
		newc2Hit = cleric2.getCombatAgent().getHitChance(null, null);
		newf3Dmg = fighter3.getCombatAgent().getCurrentCM().getTotalDamageBonus();
		newf4Dmg = fighter4.getCombatAgent().getCurrentCM().getTotalDamageBonus();
		newg3Dmg = goblin3.getCombatAgent().getCurrentCM().getTotalDamageBonus();
		newg4Dmg = goblin4.getCombatAgent().getCurrentCM().getTotalDamageBonus();
		newc2Dmg = cleric2.getCombatAgent().getCurrentCM().getTotalDamageBonus();

		assertEquals(newf3Hit - f3Hit, 0.10, 0.00001);
		assertEquals(newf4Hit - f4Hit, 0.10, 0.00001);
		assertEquals(newg3Hit + newg4Hit - g3Hit - g4Hit, -0.10, 0.00001);
		assertEquals(newc2Hit - c2Hit, 0, 0.0001);
		assertEquals(newf3Dmg - f3Dmg, 2, 0.0001);		
		assertEquals(newf4Dmg - f4Dmg, 2, 0.0001);
		assertEquals(newg3Dmg + newg4Dmg, g3Dmg + g4Dmg, 0.00001);
		assertEquals(newc2Dmg, c2Dmg, 0.00001);
	}

	@Test
	public void testSpellExpiry() {
		// Here we just want to test that spells expire once the fight is over
		// so we set all to attack

		// resolve the fight

		// and confirm that all spells are now inActive

		testShieldOfFaith();
		assertTrue(fight1.isActive());
		goblin1.setCombatDecider(TestFight.alwaysAttack);
		goblin2.setCombatDecider(TestFight.alwaysAttack);
		fighter1.setCombatDecider(TestFight.alwaysAttack);
		fighter2.setCombatDecider(TestFight.alwaysAttack);
		cleric1.setCombatDecider(TestFight.alwaysAttack);
		ArrayList<Spell> spellsCast = cleric1.getActiveSpells();
		assertEquals(spellsCast.size(), 3, 0);
		fight1.resolve();
		assertTrue(!fight1.isActive());

		for (Spell s : spellsCast) {
			assertTrue(!s.isActive());
		}
	}

	@Test
	public void testSpellRemoval() {
		testBless();
		// We just have one spell at this stage
		ArrayList<Spell> spellsCast;
		spellsCast = cleric1.getActiveSpells();
		Spell originalBless = spellsCast.get(0);
		assertEquals(spellsCast.size(), 1, 0);
		// We try removing a different spell - and confirm this has no effect

		Spell b = new Bless();
		boolean test = cleric1.spellExpires(b);
		assertTrue(!test);

		spellsCast = cleric1.getActiveSpells();
		assertEquals(spellsCast.size(), 1, 0);
		assertTrue(spellsCast.get(0) == originalBless);

		// We then then remove it
		test = cleric1.spellExpires(originalBless);
		assertTrue(test);
		spellsCast = cleric1.getActiveSpells();
		assertEquals(spellsCast.size(), 0, 0);

		// and then try to remove it again
		test = cleric1.spellExpires(originalBless);
		assertTrue(!test);
		spellsCast = cleric1.getActiveSpells();
		assertEquals(spellsCast.size(), 0, 0);
	}	

}
