package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.actions.Defend;

import java.util.*;

import org.junit.*;
public class CharacterCombatAgentTest {

	CharacterCombatAgent testcca, gcca1, fcca1;
	Character c;
	static World w = new World();
	Character goblin1;
	Character fighter1, cleric1;
	
	@Before
	public void setUp() {

		SimProperties.setProperty("StartWithPotion", "false");
		c = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		testcca = (CharacterCombatAgent) c.getCombatAgent();
		goblin1 = new Character(Race.GOBLIN, CharacterClass.WARRIOR, w);
		goblin1.setStrength(new Attribute(9));
		goblin1.setDexterity(new Attribute(14));
		goblin1.setConstitution(new Attribute(10));
		goblin1.setMaxHp(20);
		fighter1 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		fighter1.setStrength(new Attribute(13));
		fighter1.setDexterity(new Attribute(10));
		fighter1.setConstitution(new Attribute(12));
		fighter1.setMaxHp(11);
		fighter1.addItem(Weapon.SHORT_SWORD);
		goblin1.addItem(Weapon.LIGHT_MACE);
		fcca1 = (CharacterCombatAgent) fighter1.getCombatAgent();
		gcca1 = (CharacterCombatAgent) goblin1.getCombatAgent();
		
		cleric1 = new Character(Race.HUMAN, CharacterClass.CLERIC, w);
		cleric1.setWisdom(new Attribute(13));
		cleric1.addMp(2);
	}

	@Test
	public void testAddCondition() {
		int oldDamage = c.getCurrentCM().getDamage();
		int oldCount = testcca.getCombatConditions().size();
		new WeaponSpecialization(c, Skill.skills.WEAPON_SPECIALIZATION);
		ArrayList<CombatCondition> temp = testcca.getCombatConditions();
		
		int newDamage = c.getCurrentCM().getDamage();
		int newCount = testcca.getCombatConditions().size();
		
		assertEquals(oldDamage-newDamage, -2, 0.0001);
		assertEquals(newCount-oldCount, 1, 0.0001);
		
		new WeaponSpecialization(c, Skill.skills.WEAPON_SPECIALIZATION);
		temp = testcca.getCombatConditions();
		
		newDamage = c.getCurrentCM().getDamage();
		newCount = testcca.getCombatConditions().size();
		
		assertEquals(oldDamage-newDamage, -2, 0.0001);
		assertEquals(newCount-oldCount, 1, 0.0001);
		
	}

	@Test
	public void testRemoveCondition() {
		ArrayList<CombatCondition> testList = testcca.getCombatConditions();
		CombatCondition tCC = testList.get(0);
		int startNumber = testList.size();
		testcca.removeCondition(tCC);
		testList = testcca.getCombatConditions();
		assertTrue(!testList.contains(tCC));
		assertEquals(startNumber - testList.size(), 1, 0);
	}

	@Test
	public void testPermanentRemoval() {
		assertTrue(c.getCurrentFight() == null);
		(new Defend(testcca, false)).run();
		assertTrue(hasFullDefense(testcca));
		
		testcca.setCurrentFight(new Fight(goblin1, fighter1));

		assertTrue(!hasFullDefense(testcca));
	}
	private boolean hasFullDefense(CombatAgent ca) {
		ArrayList<CombatCondition> testList = ca.getCombatConditions();
		boolean hasFullDefense = false;
		for (CombatCondition cc : testList) {
			if (cc.toString().equals("FullDefense"))
					hasFullDefense = true;
		}
		return hasFullDefense;
	}
	@Test
	public void testAttack() {
		// Characters to use
		gcca1.setCurrentTarget(fcca1);
		fcca1.setCurrentTarget(gcca1);
		
		// +1 Str, +1 Base, +1 W Focus, -2 Dex, -1 Size
		int hp = goblin1.getHp();
		
		int retValue = fcca1.attack(gcca1, false, 9, 10);
		assertEquals(retValue, 0, 0); // miss
		assertEquals(goblin1.getHp()-hp, 0, 0);
		
		retValue = fcca1.attack(gcca1, false, 10, 20);
		assertEquals(retValue, 1, 0); // hit
		assertTrue(goblin1.getHp() < hp);
		goblin1.addHp(20, false);
		
		retValue = fcca1.attack(gcca1, false, 1, 7);
		assertTrue(retValue == 4 || retValue == 12); // fumble
		// note - a retValue of 12 is also possible if weapn breaks
		assertEquals(goblin1.getHp()-hp, 0, 0);
		
		retValue = fcca1.attack(gcca1, false, 19, 9);
		assertEquals(retValue, 1, 0); // hit
		assertTrue(goblin1.getHp() < hp);
		goblin1.addHp(20, false);
		
		retValue = fcca1.attack(gcca1, false, 19, 10);
		assertEquals(retValue, 3, 0); // crit
		assertTrue(goblin1.getHp() < hp);
		goblin1.addHp(20, false);
		
		fcca1.addCondition(new CombatCondition() {
			public int roundsLeft() {return 0;}
			public boolean isPermanent() {return false;}
			public void apply(CombatModifier cm) {
				if (cm.getAttacker().equals(fcca1))
					cm.setStrengthBonus(10);
			}
		}) ;
		
		retValue = fcca1.attack(gcca1, false, 1, 8);
		assertEquals(retValue, 0, 0); // miss
		assertEquals(goblin1.getHp()-hp, 0, 0);
		assertTrue(fighter1.getWeapon() != null);
		
		retValue = fcca1.attack(gcca1, false, 1, 1);
		assertEquals(retValue, 12, 0); // weapon break
		assertEquals(goblin1.getHp()-hp, 0, 0);
		assertTrue(fighter1.getWeapon() == Weapon.FIST);
		
	}

	@Test
	public void testApplyDamage() {
		goblin1.addItem(Weapon.LIGHT_MACE);
		fcca1.applyDamage(gcca1, goblin1.getWeapon(), 1, false);		
		assertTrue(fighter1.getHp() == 10);
		assertTrue(!fighter1.isStunned());
		
		fcca1.applyDamage(gcca1, goblin1.getWeapon(), 2, false);		
		assertTrue(fighter1.getHp() == 8);
		
		fcca1.applyDamage(gcca1, goblin1.getWeapon(), 1, true);		
		assertTrue(fighter1.getHp() == 8);
		assertTrue(!fighter1.isStunned());
		
		fcca1.applyDamage(gcca1, goblin1.getWeapon(), 10, true);		
		assertTrue(fighter1.getHp() == 8);
		assertTrue(fighter1.isStunned());
	}
	
	@Test
	public void testCheckInventory() {
		// Weapon adding / removal is well tested in WeaponTestRun.
		// so here we test armour and shields
		// Take a fighter. check all are null.
		// Add a shield, then a suit of armour, and check they are Equipped
		
		fighter1.addItem(Shield.SMALL_SHIELD);
		fighter1.addItem(Armour.LEATHER_ARMOUR);
		fighter1.removeItem(Weapon.SHORT_SWORD); // this is added in set up - so let's remove it
		assertTrue(fighter1.getWeapon()== Weapon.FIST);
		assertTrue(fighter1.getArmour()==Armour.LEATHER_ARMOUR);
		assertTrue(fighter1.getShield()==Shield.SMALL_SHIELD);
		
		// Add a better shield and armour. Check they are equipped instead
		fighter1.addItem(Shield.LARGE_SHIELD);
		fighter1.addItem(Armour.CHAIN_MAIL);
		
		// check that there are a total of four items in inventory
		List<Artefact>	test = fighter1.getInventory();
		assertEquals(test.size(), 4);
		
		// Remove the better shield, and the worse armour. Check equipment is correct
		fighter1.removeItem(Shield.LARGE_SHIELD);
		fighter1.removeItem(Armour.LEATHER_ARMOUR);
		assertTrue(fighter1.getWeapon()==Weapon.FIST);
		assertTrue(fighter1.getShield()==Shield.SMALL_SHIELD);
		assertTrue(fighter1.getArmour()==Armour.CHAIN_MAIL);

		// Add a duplicate copy of the worse shield. then remove one shield. check the duplicate
		// is still equipped
		fighter1.addItem(Shield.SMALL_SHIELD);
		fighter1.removeItem(Shield.SMALL_SHIELD);
		assertTrue(fighter1.getArmour()==Armour.CHAIN_MAIL);
		assertTrue(fighter1.getShield()==Shield.SMALL_SHIELD);
		test = fighter1.getInventory();
		assertEquals(test.size(), 2);
		
		// Remove the worse shield again
		fighter1.removeItem(Shield.SMALL_SHIELD);
		
		// check complete inventory. Should just have one suit of armour.
		test = fighter1.getInventory();
		assertEquals(test.size(), 1);
		assertTrue(test.get(0)==Armour.CHAIN_MAIL);
		assertTrue(fighter1.getShield()==null);
		assertTrue(fighter1.getWeapon()==Weapon.FIST);
		assertTrue(fighter1.getArmour()==Armour.CHAIN_MAIL);
	}

	@Test
	public void testGetInitiative() {
		// Just roll initiative, and confirm it is equal to the last roll, plus
		// dex modifier
		int i = fighter1.getCombatAgent().getInitiative(null);
		assertTrue(i==Dice.lastRoll()+fighter1.getDexterity().getMod());
	}

	@Test
	public void testInitialiseFightStatus() {
		Fight f = new Fight(fighter1, goblin1);
		
		// this will initialise fight for all participants
		
		// Now call function
		// check that targetted is false
		// and that current Fight is now set
		assertTrue(fcca1.isTargetted()==false);
		assertTrue(fcca1.getCurrentFight()==f);
		assertTrue(fcca1.hasAofO()==true);
		
		// We will also test setting a target, and leaving the fight here
		fcca1.setCurrentTarget(gcca1);
		assertTrue(fcca1.getCurrentTarget()==gcca1);
		fcca1.setTargetted(true);
		assertTrue(fcca1.isTargetted()==true);
		
		fcca1.leavesFight();
		assertTrue(fcca1.getCurrentTarget()==null);
		assertTrue(fcca1.isTargetted()==false);
		
	}

	@Test
	public void testGetAgent() {
		assertTrue(fcca1.getAgent()==fighter1);
	}

	@Test
	public void testGetController() {
		assertTrue(fcca1.getController()==fighter1);
	}

	@Test
	public void testIsAttackable() {
		assertTrue(fcca1.isAttackable(gcca1)==true);
		assertTrue(gcca1.isAttackable(fcca1)==true);
	}

	@Test
	public void testIsDead() {
		assertTrue(fcca1.isDead()==false);
		assertTrue(gcca1.isDead()==false);
		fcca1.setCombatDecider(TestFight.alwaysAttack);
		gcca1.setCombatDecider(TestFight.alwaysAttack);
		Fight f = new Fight(fighter1, goblin1);
		f.resolve();
		if (fcca1.isDead()) {
			assertTrue(gcca1.isDead()==false);
		} else
			assertTrue(gcca1.isDead()==true);
	}
	
	@Test
	public void testRemovalOfCombatConditions() {
		// Set up Fight
		Fight f = new Fight(fighter1, goblin1);
		
		// Add a combat condition to last one round - Defend
		// plus another that lasts longer - Bless
		new Defend(fcca1,false).run();
		
		Bless b = new Bless();
		b.cast(cleric1, fighter1);
		
		ArrayList<CombatCondition> ccList = fighter1.getCombatConditions();
		boolean hasFullDefense = false;
		boolean hasBless = false;
		for (CombatCondition cc : ccList) {
			if (cc.toString().equals("FullDefense")) hasFullDefense = true;
			if (cc instanceof Bless) hasBless = true;
		}
		assertTrue(hasFullDefense);
		assertTrue(hasBless);
		// call newRound(), and check that it is removed
		fcca1.newRound();
		hasFullDefense = false;
		hasBless = false;
		ccList = fighter1.getCombatConditions();
		for (CombatCondition cc : ccList) {
			if (cc.toString().equals("FullDefense")) hasFullDefense = true;
			if (cc instanceof Bless) hasBless = true;
		}
		assertTrue(!hasFullDefense);
		assertTrue(hasBless);
		
	}
	
	@Test
	public void testSpellRemovalAfterFight() {
		// cast some healing spells
		cleric1.levelUp();
		cleric1.levelUp();
		cleric1.setMaxHp(50);
		cleric1.addHp(50, false);
		fighter1.levelUp();
		fighter1.levelUp();
		
		CureLightWounds clw1 = new CureLightWounds();
		clw1.cast(cleric1, fighter1);
		
		// check spell count
		assertEquals(cleric1.getActiveSpells().size(), 1, 0);
		assertTrue(cleric1.getActiveSpells().get(0) == clw1);
		
		Spell dummySpell = new Spell(){
			public boolean isActive() {
				return true;
			}
			public void implementEffect(Character caster, DnDAgent target) {
				// Nothing
			}
		};
		
		dummySpell.cast(cleric1, cleric1);
		assertEquals(cleric1.getActiveSpells().size(), 2, 0);
		// Set up fight
		Party newParty = new Party(cleric1);
		newParty.addMember(fighter1);
		
		Fight f = new Fight(newParty, goblin1);
		// then cast some spells
		new Bless().cast(cleric1, newParty);
		new BullsStrength().cast(cleric1, fighter1);
		
		fighter1.setCombatDecider(TestFight.alwaysAttack);
		goblin1.setCombatDecider(TestFight.alwaysAttack);
		cleric1.setCombatDecider(TestFight.alwaysDefend);
		
		f.resolve();
		// check spell count
		ArrayList<CombatCondition> currentConditions = fcca1.getCombatConditions();
		for (CombatCondition cc : currentConditions) {
			if (cc instanceof BullsStrength) assertFalse(true);
		}
		assertFalse(cleric1.isDead());
		assertEquals(cleric1.getActiveSpells().size(), 1, 0);
		
		// and the one remaining should be Dummy Spell
		
		// and also check that the fighter does not have Bless or Bulls Strength as combat conditions

		
		
	}

}
