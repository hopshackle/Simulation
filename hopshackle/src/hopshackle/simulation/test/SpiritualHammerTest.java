package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.World;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

import java.util.ArrayList;

import org.junit.*;
public class SpiritualHammerTest {

	Character cleric2, goblin3, goblin4, fighter3, fighter4;
	static World w = new World();
	Party pcParty, npcParty;
	Fight fight1;
	
	@Before
	public void setUp() throws Exception {
		
		goblin3 = new Character(Race.GOBLIN, CharacterClass.WARRIOR, w);
		goblin3.levelUp();
		goblin3.levelUp();
		goblin3.setMaxHp(25);
		goblin3.addItem(Weapon.LIGHT_MACE);
		goblin3.addItem(Armour.BANDED_MAIL);
		
		goblin4 = new Character(Race.GOBLIN, CharacterClass.WARRIOR, w);
		goblin4.levelUp();
		goblin4.levelUp();
		goblin4.setMaxHp(25);
		goblin4.addItem(Weapon.LIGHT_MACE);
		goblin4.addItem(Shield.TOWER_SHIELD);
		
		fighter3 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		fighter3.addItem(Weapon.SHORT_SWORD);
		fighter3.levelUp();

		fighter4 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		fighter4.addItem(Weapon.SHORT_SWORD);
		fighter4.addItem(Shield.LARGE_SHIELD);
		
		cleric2 = new Character(Race.HUMAN, CharacterClass.CLERIC, w);
		cleric2.setStrength(new Attribute(11));	// to give decent chance of hitting
		cleric2.setWisdom(new Attribute(14));
		cleric2.addItem(Weapon.HEAVY_MACE);
		cleric2.levelUp();
		cleric2.levelUp();
		cleric2.levelUp();
		cleric2.rest(1); // will set magic points to max mp
		assertEquals(cleric2.getMp(), 13, 0);
		
		goblin3.setCombatDecider(TestFight.alwaysDefend);
		goblin4.setCombatDecider(TestFight.alwaysDefend);
		fighter3.setCombatDecider(TestFight.alwaysDefend);
		fighter4.setCombatDecider(TestFight.alwaysDefend);
		cleric2.setCombatDecider(TestFight.alwaysDefend);
		
		pcParty = new Party(cleric2);
		pcParty.addMember(fighter4);
		pcParty.addMember(fighter3);
		
		npcParty = new Party(goblin3);
		npcParty.addMember(goblin4);
		fight1 = new Fight(pcParty, npcParty);

		fight1.setTarget(goblin3.getCombatAgent(), fighter3.getCombatAgent());
		fight1.setTarget(goblin4.getCombatAgent(), fighter4.getCombatAgent());
		fight1.setTarget(fighter3.getCombatAgent(), goblin3.getCombatAgent());
		fight1.setTarget(fighter4.getCombatAgent(), goblin4.getCombatAgent());
		
		fight1.oneRound();
	}

	@Test
	public void spiritualHammer() {
		// First set the cleric to default to offensive magic
		// everyone else says on Defend
		cleric2.setCombatDecider(TestFight.alwaysSpellAttack);
		
		// Execute next round
		assertEquals(fight1.getSideSize(cleric2.getCombatAgent()), 3, 0);
		fight1.oneRound();
		
		// Check that spiritual hammer has been cast, and that the number of 
		// combatants has increased by one (and that there is one combatant that is non-attackable
		// and that this combatant is an instance of Spiritual Hammer
		assertEquals(fight1.getSideSize(cleric2.getCombatAgent()), 4, 0);
		ArrayList<CombatAgent> combatants = fight1.getSide(cleric2.getCombatAgent());
		int notAttackable = 0;
		SpiritualHammer hammer = null;
		for (CombatAgent ca : combatants) {
			if (!ca.isAttackable(goblin3.getCombatAgent())) {
				notAttackable++;
				assertTrue(ca instanceof SpiritualHammer);
				hammer = (SpiritualHammer) ca;
			}
		}
		assertEquals(notAttackable, 1, 0);
		assertEquals(cleric2.getMp(), 10, 0);
		
		// Execute next round
		fight1.oneRound();
		
		ArrayList<Spell> spellsCast = cleric2.getActiveSpells();
		// BS should not be the last spell cast - as the only item with a target is invalid for it
		// execute next round, and confirm that Spiritual Hammer is not cast again
		for (Spell s : spellsCast){
			if (s instanceof SpiritualHammer)
				assertTrue(((SpiritualHammer)s) == hammer);
		}
		
		// check that the spiritual hammer now has a target, and find the hit chance
		assertTrue(hammer.getCurrentTarget()!=null);
		assertTrue(hammer.getCurrentTarget() == goblin3.getCombatAgent() || 
					hammer.getCurrentTarget() == goblin4.getCombatAgent());
		
		// compare to the hit chance of the cleric against the same target. The difference should
		// be the wisdom modifier minus the strength modifer of the cleric 
		double hammerChance = hammer.getHitChance(goblin3.getCombatAgent(), null);
		double clericChance = cleric2.getCombatAgent().getHitChance(goblin3.getCombatAgent(), null);
		int delta = cleric2.getWisdom().getMod() - cleric2.getStrength().getMod();
		assertEquals(hammerChance - clericChance, (delta*0.05), 0.0001);
		
		// Have the spiritual hammer attack 
		// roll a miss (2)
		CombatAgent target = hammer.getCurrentTarget();
		Character goblin = (Character) target.getAgent();
		int startHp = goblin.getHp();
		assertEquals(hammer.attack(target, false, 2, 20), 0, 0);
		// confirm that target has taken no damage
		assertEquals(goblin.getHp(), startHp, 0);
		
		// roll a hit (19)
		assertEquals(hammer.attack(target, false, 19, 5), 1, 0);
	
		// confirm that target has taken damage (then heal them)
		assertTrue(goblin.getHp() < startHp);
		goblin.addHp(20, false);
		
		// roll a critical (20)
		assertEquals(hammer.attack(target, false, 20, 19), 3, 0);
		assertTrue(!target.isDead());
		goblin.addHp(20, false);

		// try to set the hammer to be target - confirm that this fails
		CombatAgent targetsTarget = target.getCurrentTarget();
		assertTrue(targetsTarget!=null);
		assertTrue(targetsTarget != hammer);
		fight1.setTarget(target, hammer);
		assertTrue(target.getCurrentTarget() == targetsTarget);
		assertTrue(fight1.getOpponent(target, false) == targetsTarget);
		
		// cast Bless, and confirm this does not affect the hit chance of the hammer
		new Bless().cast(cleric2, pcParty);
		hammer.setCurrentCM(null);
		double newHammerChance = hammer.getHitChance(goblin3.getCombatAgent(), null);
		assertEquals(hammerChance, newHammerChance, 0.00001);
		
		// move both goblins to reserve. Force the hammer to find a target, and check that it does
		fight1.moveToReserve(goblin3.getCombatAgent());
		fight1.moveToReserve(goblin4.getCombatAgent());
		assertTrue(hammer.getCurrentTarget()==null);
		
		fight1.getOpponent(hammer, true);
		assertTrue(hammer.getCurrentTarget() == goblin3.getCombatAgent() || 
				hammer.getCurrentTarget() == goblin4.getCombatAgent());
		// then cast Bulls strength, and confirm that the hammer is not selected as a target
		// at this stage it should be the only combatant which has a target
		
		cleric2.setCombatDecider(TestFight.alwaysBuff);
		fight1.oneRound();
		assertTrue(fighter3.getCurrentTarget()==null);
		assertTrue(fighter4.getCurrentTarget()==null);
	
		spellsCast = cleric2.getActiveSpells();
		Spell bs = spellsCast.get(spellsCast.size()-1);
		// BS should not be the last spell cast - as the only item with a target is invalid for it
		// execute next round, and confirm that Spiritual Hammer is not cast again
		assertTrue(!(bs instanceof BullsStrength));
		for (Spell s : spellsCast){
			if (s instanceof SpiritualHammer)
				assertTrue(((SpiritualHammer)s) == hammer);
		}
		
		fighter3.setCombatDecider(TestFight.alwaysAttack);
		goblin3.setCombatDecider(TestFight.alwaysAttack);
		fight1.setTarget(fighter3.getCombatAgent(), goblin3.getCombatAgent());
		fight1.oneRound();
		fighter3.setCombatDecider(TestFight.alwaysDefend);
		goblin3.setCombatDecider(TestFight.alwaysDefend);
		cleric2.setCombatDecider(TestFight.alwaysDefend);
		
		spellsCast = cleric2.getActiveSpells();
		bs = spellsCast.get(spellsCast.size()-1);
		// BS should be the last spell cast - as we now have a valid target
		// execute next round, and confirm that Spiritual Hammer is not cast again
		assertTrue(bs instanceof BullsStrength);
		assertTrue(((BullsStrength)bs).getTarget().equals(fighter3));
		for (Spell s : spellsCast){
			if (s instanceof SpiritualHammer)
				assertTrue(((SpiritualHammer)s) == hammer);
		}
		// when spell expires, check that hammer is removed from the combatant list
		// at this point we have made three attacks; so should have one more
		assertTrue(hammer.isActive());
		fight1.oneRound();  // and the fourth and final
		fight1.oneRound();	// and then the Spiritual Hammer is removed
		assertTrue(!hammer.isActive());
		
		assertEquals(fight1.getSideSize(cleric2.getCombatAgent()), 3, 0);
		combatants = fight1.getSide(cleric2.getCombatAgent());
		notAttackable = 0;
		hammer = null;
		for (CombatAgent ca : combatants) {
			if (!ca.isAttackable(goblin3.getCombatAgent())) {
				notAttackable++;
				assertTrue(false);
			}
		}
		assertEquals(notAttackable, 0, 0);
	}
}
