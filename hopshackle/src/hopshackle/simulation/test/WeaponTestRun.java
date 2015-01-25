package hopshackle.simulation.test;


import static org.junit.Assert.*;
import hopshackle.simulation.World;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

import org.junit.*;
public class WeaponTestRun {

	World w;
	Character cleric, fighter, armouredFighter1, armouredFighter2;
	CombatAgent clr, ftr, aFtr1, aFtr2;
	Weapon shortSword = Weapon.SHORT_SWORD;
	Weapon longSword = Weapon.LONG_SWORD;
	Weapon bastardSword = Weapon.BASTARD_SWORD;
	Weapon lightMace = Weapon.LIGHT_MACE;
	Weapon heavyMace = Weapon.HEAVY_MACE;
	Weapon mLongSword = Weapon.MASTERWORK_LONG_SWORD;
	Weapon mBastardSword = Weapon.MASTERWORK_BASTARD_SWORD;
	
	@Before
	public void setUp() throws Exception {
		w = new World();
		cleric = new Character(Race.HUMAN, CharacterClass.CLERIC, w);
		cleric.setStrength(new Attribute(10));
		cleric.setDexterity(new Attribute(9));
		fighter = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		fighter.setStrength(new Attribute(14));
		fighter.setDexterity(new Attribute(13));
		clr = cleric.getCombatAgent();
		ftr = fighter.getCombatAgent();
		
		clr.setCurrentTarget(ftr);
		ftr.setCurrentTarget(clr);
		
		armouredFighter1 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		armouredFighter1.setStrength(new Attribute(14));
		armouredFighter1.setDexterity(new Attribute(13));
		armouredFighter2 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		armouredFighter2.setStrength(new Attribute(14));
		armouredFighter2.setDexterity(new Attribute(13));
		aFtr1 = armouredFighter1.getCombatAgent();
		aFtr2 = armouredFighter2.getCombatAgent();
		
		armouredFighter1.addItem(Armour.CHAIN_SHIRT);
		armouredFighter1.addItem(Shield.LARGE_SHIELD);
		armouredFighter2.addItem(Armour.CHAIN_SHIRT);
		armouredFighter2.addItem(Shield.LARGE_SHIELD);
		armouredFighter1.addItem(Weapon.LONG_SWORD);
		armouredFighter2.addItem(Weapon.LONG_SWORD);
		
		aFtr1.setCurrentTarget(aFtr2);
		// but not the other way round
		
	}

	@After
	public void tearDown() throws Exception {
	}
		
	
	@Test
	public void testWeaponStatistics01() {		
		assertEquals("Cleric Shortsword", clr.getHitChance(ftr, shortSword), 0.25, 0.0001);
		// -1 Dodge, -1 Dex, -4 Not Proficient
	}
	@Test
	public void testWeaponStatistics02() {
		assertEquals("Fighter Shortsword", ftr.getHitChance(clr, shortSword), 0.80, 0.001);
		// +2 Str, +1 Dex, +1 Base, +1 Weapon Focus
	}
	@Test
	public void testWeaponStatistics03() {
		assertEquals("Cleric Light Mace", clr.getHitChance(ftr, lightMace), 0.45, 0.001);
		// -1 Dodge, -1 Dex
	}
	@Test
	public void testWeaponStatistics04() {
		assertEquals("Fighter Light Mace", ftr.getHitChance(clr, lightMace), 0.75, 0.001);
		// +2 Str, +1 Dex, +1 Base 
	}
	@Test
	public void testWeaponStatistics05() {
		assertEquals("Cleric Heavy Mace", clr.getHitChance(ftr, heavyMace), 0.45, 0.001);
		// -1 Dodge, -1 Dex
	}
	@Test
	public void testWeaponStatistics06() {		
		assertEquals("Fighter Heavy Mace", ftr.getHitChance(clr, heavyMace), 0.75, 0.001);
		// +2 Str, +1 Dex, +1 Base 
	}
	@Test
	public void testWeaponStatistics07() {		
		assertEquals("Cleric Longsword", clr.getHitChance(ftr, longSword), 0.25, 0.001);
		// -1 Dodge, -1 Dex, -4 Not Proficient		
	}
	@Test
	public void testWeaponStatistics08() {		
		assertEquals("Fighter Longsword", ftr.getHitChance(clr, longSword), 0.80, 0.001);
		// +2 Str, +1 Dex, +1 Base, +1 Weapon Focus
	}
	@Test
	public void testWeaponStatistics09() {		
		assertEquals("Cleric Bastard Sword", clr.getHitChance(ftr, bastardSword), 0.25, 0.001);
		// -1 Dodge, -1 Dex, -4 Not Proficient
	}
	@Test
	public void testWeaponStatistics10() {		
		assertEquals("Fighter Bastard Sword", ftr.getHitChance(clr, bastardSword), 0.55, 0.001);
		// +2 Str, +1 Dex, +1 Base, -4 Not Proficient
	}
	@Test
	public void testWeaponStatistics11() {		
		assertEquals("Cleric Masterwork Longsword", clr.getHitChance(ftr, mLongSword), 0.30, 0.001);
		// -1 Dodge, -1 Dex, -4 Not Proficient, +1 Masterwork
	}
	@Test
	public void testWeaponStatistics12() {		
		assertEquals("Fighter Masterwork Longsword", ftr.getHitChance(clr, mLongSword), 0.85, 0.001);
		// +2 Str, +1 Dex, +1 Base, +1 Weapon Focus, +1 Masterwork
	}
	@Test
	public void testWeaponStatistics13() {		
		assertEquals("Cleric Masterwork Bastardsword", clr.getHitChance(ftr, mBastardSword), 0.30, 0.001);
		// -1 Dodge, -1 Dex, -4 Not Proficient, +1 Masterwork
	}
	@Test
	public void testWeaponStatistics14() {		
		assertEquals("Fighter Masterwork Bastardsword", ftr.getHitChance(clr, mBastardSword), 0.60, 0.001);
		// +2 Str, +1 Dex, +1 Base, +1 Masterwork, -4 Not Proficient
	}
	@Test
	public void testWeaponStatistics21() {		
		assertEquals("Cleric Shortsword", clr.getAvgDmgPerRound(ftr, shortSword), 0.9625, 0.00001);
		// -1 Dodge, -1 Dex, -4 Not Proficient
	}
	@Test
	public void testWeaponStatistics22() {
		assertEquals("Fighter Shortsword", ftr.getAvgDmgPerRound(clr, shortSword), 4.84, 0.00001);
		// +2 Str, +1 Dex, +1 Base, +1 Weapon Focus
	}
	@Test
	public void testWeaponStatistics23() {
		assertEquals("Cleric Light Mace", clr.getAvgDmgPerRound(ftr, lightMace), 1.65375, 0.00001);
		// -1 Dodge, -1 Dex
	}
	@Test
	public void testWeaponStatistics24() {
		assertEquals("Fighter Light Mace", ftr.getAvgDmgPerRound(clr, lightMace), 4.33125, 0.00001);
		// +2 Str, +1 Dex, +1 Base 
	}
	@Test
	public void testWeaponStatistics25() {
		assertEquals("Cleric Heavy Mace", clr.getAvgDmgPerRound(ftr, heavyMace), 2.12625, 0.00001);
		// -1 Dodge, -1 Dex
	}
	@Test
	public void testWeaponStatistics26() {		
		assertEquals("Fighter Heavy Mace", ftr.getAvgDmgPerRound(clr, heavyMace), 5.11875, 0.00001);
		// +2 Str, +1 Dex, +1 Base 
	}
	@Test
	public void testWeaponStatistics27() {		
		assertEquals("Cleric Longsword", clr.getAvgDmgPerRound(ftr, longSword), 1.2375, 0.00001);
		// -1 Dodge, -1 Dex, -4 Not Proficient		
	}
	@Test
	public void testWeaponStatistics28() {		
		assertEquals("Fighter Longsword", ftr.getAvgDmgPerRound(clr, longSword), 5.72, 0.00001);
		// +2 Str, +1 Dex, +1 Base, +1 Weapon Focus
	}
	@Test
	public void testWeaponStatistics29() {		
		assertEquals("Cleric Bastard Sword", clr.getAvgDmgPerRound(ftr, bastardSword), 1.5125, 0.00001);
		// -1 Dodge, -1 Dex, -4 Not Proficient
	}
	@Test
	public void testWeaponStatistics30() {		
		assertEquals("Fighter Bastard Sword", ftr.getAvgDmgPerRound(clr, bastardSword), 4.5375, 0.00001);
		// +2 Str, +1 Dex, +1 Base, -4 Not Proficient
	}
	@Test
	public void testWeaponStatistics31() {		
		assertEquals("Cleric Masterwork Longsword", clr.getAvgDmgPerRound(ftr, mLongSword), 1.485, 0.00001);
		// -1 Dodge, -1 Dex, -4 Not Proficient, +1 Masterwork
	}
	@Test
	public void testWeaponStatistics32() {		
		assertEquals("Fighter Masterwork Longsword", ftr.getAvgDmgPerRound(clr, mLongSword), 6.0775, 0.00001);
		// +2 Str, +1 Dex, +1 Base, +1 Weapon Focus, +1 Masterwork
	}
	@Test
	public void testWeaponStatistics33() {		
		assertEquals("Cleric Masterwork Bastardsword", clr.getAvgDmgPerRound(ftr, mBastardSword), 1.815, 0.00001);
		// -1 Dodge, -1 Dex, -4 Not Proficient, +1 Masterwork
	}
	@Test
	public void testWeaponStatistics34() {		
		assertEquals("Fighter Masterwork Bastardsword", ftr.getAvgDmgPerRound(clr, mBastardSword), 4.95, 0.00001);
		// +2 Str, +1 Dex, +1 Base, +1 Masterwork, -4 Not Proficient
	}

	@Test
	public void testArmourStatistics01() {		
		assertTrue(armouredFighter1.getShield().equals(Shield.LARGE_SHIELD));
	}
	@Test
	public void testArmourStatistics02() {
		assertTrue(armouredFighter2.getArmour().equals(Armour.CHAIN_SHIRT));
	}
	@Test
	public void testWeaponStatistics41() {		
		assertEquals(aFtr1.getHitChance(aFtr2, shortSword), 0.40, 0.0001);
		// +2 Str, -1 Dex, +1 Base, +1 Weapon Focus, -6 Armour
	}
	@Test
	public void testWeaponStatistics42() {
		assertEquals(aFtr2.getHitChance(aFtr1, shortSword), 0.35, 0.001);
		// +2 Str, -1 Dex, +1 Base, +1 Weapon Focus, -1 Dodge, -6 Armour
	}
	@Test
	public void testWeaponStatistics43() {		
		assertEquals(aFtr1.getHitChance(aFtr2, bastardSword), 0.15, 0.001);
		// -1 Dex, -4 Not Proficient, -6 Armour, +1 Base, +2 Str
	}
	@Test
	public void testWeaponStatistics44() {		
		assertEquals(aFtr2.getHitChance(aFtr1, bastardSword), 0.10, 0.001);
		// -1 Dex, -4 Not Proficient, -6 Armour, +1 Base, +2 Str, -1 Dodge
	}
	@Test
	public void testWeaponStatistics45() {		
		assertEquals(aFtr1.getHitChance(aFtr2, mLongSword), 0.45, 0.001);
		// +2 Str, -1 Dex, +1 Base, +1 Weapon Focus, -6 Armour, +1 Masterwork
	}
	@Test
	public void testWeaponStatistics46() {		
		assertEquals(aFtr2.getHitChance(aFtr1, mLongSword), 0.40, 0.001);
		// +2 Str, -1 Dex, +1 Base, +1 Weapon Focus, -1 Dodge, -6 Armour,  +1 Masterwork
	}
	@Test
	public void testWeaponStatistics51() {	
		fighter.addItem(Weapon.SHORT_SWORD);
		assertEquals(shortSword.getAvgDmgChange(fighter), 0, 0.0001);
		fighter.removeItem(Weapon.SHORT_SWORD);
	}
	@Test
	public void testWeaponStatistics52() {	
		assertEquals(shortSword.getAvgDmgChange(fighter), 1.645, 0.00001);
		// Test goblin has AC of 18 (3 armour, 1 size, 2 shield, 2 dex)
		// To hit is +4 with Sword, -1 with Fist (as for Sword, without Weaponm Focus or Proficiency)
	}
	@Test
	public void testWeaponStatistics53() {	
		fighter.addItem(Weapon.LIGHT_MACE);
		assertEquals(shortSword.getAvgDmgChange(fighter), 0.385, 0.00001);
		fighter.removeItem(Weapon.LIGHT_MACE);
		assertTrue(fighter.getWeapon() == Weapon.FIST);
	}
	@Test
	public void testWeaponStatistics54() {	
		fighter.addItem(Weapon.LIGHT_MACE);
		assertEquals(mLongSword.getAvgDmgChange(fighter), 1.1275, 0.00001);
		fighter.removeItem(Weapon.LIGHT_MACE);
	}
	@Test
	public void testWeaponStatistics55() {	
		fighter.addItem(Weapon.HEAVY_MACE);
		fighter.addItem(Weapon.BASTARD_SWORD);
		/// this should not be selected
		assertTrue(fighter.getWeapon() == Weapon.HEAVY_MACE);
		fighter.removeItem(Weapon.HEAVY_MACE);
		fighter.addItem(Weapon.LIGHT_MACE);
		assertTrue(fighter.getWeapon() == Weapon.LIGHT_MACE);
		fighter.removeItem(Weapon.LIGHT_MACE);
		assertTrue(fighter.getWeapon() == Weapon.BASTARD_SWORD);
	}
	@Test
	public void testWeaponStatistics56() {	
		fighter.addItem(Weapon.LIGHT_MACE);
		fighter.addItem(Weapon.BASTARD_SWORD);
		assertEquals(mLongSword.getAvgDmgChange(fighter), 1.1275, 0.00001);
		assertEquals(lightMace.getAvgDmgChange(fighter), 0, 0.00001);
		assertEquals(bastardSword.getAvgDmgChange(fighter), -0.9075, 0.00001);
		fighter.removeItem(Weapon.LIGHT_MACE);
		assertEquals(mLongSword.getAvgDmgChange(fighter), 2.035, 0.00001);
		assertEquals(lightMace.getAvgDmgChange(fighter), 0.9075, 0.00001);
		assertEquals(Weapon.BASTARD_SWORD.getAvgDmgChange(fighter), 0, 0.00001);
	}
	
	@Test
	public void testNegativeDmgMod() {
		fighter.addItem(heavyMace);
		cleric.addItem(Armour.CHAIN_MAIL);
		double baseDmg = ftr.getAvgDmgPerRound(clr, null);
		double baseHit = ftr.getHitChance(clr, null);
		
		fighter.setStrength(new Attribute(5));
		// sets str from 14, so change in modifier from +2 to -3
		
		cleric.removeItem(Armour.CHAIN_MAIL);	// this offsets the toHit chance
		
		ftr.setCurrentCM(null);
		double newDmg = ftr.getAvgDmgPerRound(clr, null);
		double newHit = ftr.getHitChance(clr, null);
		
		assertEquals(baseHit, newHit, 0.0001);
		assertEquals(newDmg, baseDmg * (5.0/8.0 * 3.0)/6.5, 0.0001);
		// previous was 6.5 hp per hit (d8+2), not it is 5/8 * d5
		
	}
	
	@Test
	public void testGoblinAttack() {
		// have a goblin attack the Fighter
		Character goblin = new Character(Race.GOBLIN, CharacterClass.WARRIOR, w);
		goblin.setStrength(new Attribute(8));
		goblin.addItem(Weapon.SHORT_SWORD);
		// +1 Base, -1 Strength, -1 from Dexterity, +1 from Small hitting Medium
		CombatAgent goblinCA = goblin.getCombatAgent();
		assertEquals(goblinCA.getHitChance(ftr, null), 0.55, 0.001) ;
		
		goblin.removeItem(Weapon.SHORT_SWORD);
		goblin.addItem(Weapon.LIGHT_MACE);
		assertEquals(goblinCA.getHitChance(ftr, null), 0.55, 0.001) ;
		// and after the fighter has declared Dodge
	}
}

