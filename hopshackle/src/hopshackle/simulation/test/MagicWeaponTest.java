package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.genetics.CombatNeuronInputs;

import java.io.File;
import java.util.ArrayList;

import org.junit.*;
public class MagicWeaponTest {
	
	private Character fighter1, fighter2, cleric1;
	private Character dretch;
	private static String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");

	@Before
	public void setUp() throws Exception {
		
		World w = new World();
		Genome clrGenome = new Genome(new File(baseDir + "\\Genomes\\Standard\\CLR_TEST_SPELL.txt"));
		fighter1 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		fighter1.addItem(Weapon.SHORT_SWORD);
		fighter1.setStrength(new Attribute(14));

		fighter2 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		fighter2.addItem(Weapon.SHORT_SWORD);
		fighter2.addItem(Shield.LARGE_SHIELD);
		fighter2.setStrength(new Attribute(14));

		cleric1 = new Character(Race.HUMAN, CharacterClass.CLERIC, null, clrGenome, w);
		cleric1.setWisdom(new Attribute(14));
		cleric1.addItem(Weapon.HEAVY_MACE);
		cleric1.levelUp();
		cleric1.rest(1); // will set magic points to max mp
		assertEquals(cleric1.getMp(), 3, 0);
		
		dretch = new Character(Race.DRETCH, CharacterClass.WARRIOR, w);
		dretch.setDexterity(new Attribute(10));
		dretch.levelUp();
	}
	
	@Test
	public void testMagicWeaponCast() {
		Party p = new Party(cleric1);
		p.addMember(fighter1);
		p.addMember(fighter2);
		
		cleric1.setCombatDecider(TestFight.alwaysDefend);
		fighter1.setCombatDecider(TestFight.alwaysDefend);
		fighter2.setCombatDecider(TestFight.alwaysDefend);
		
		dretch.setCombatDecider(TestFight.alwaysDefend);
		
		Fight f = new Fight(p, dretch);
		f.oneRound();
		cleric1.setCombatDecider(TestFight.alwaysBuff);
		
		f.setTarget(fighter2.getCombatAgent(), dretch.getCombatAgent());
		
		double f1ToHit = fighter2.getCombatAgent().getHitChance(dretch.getCombatAgent(), null);
		double f1dmg = fighter2.getCombatAgent().getAvgDmgPerRound(dretch.getCombatAgent(), null);
		assertFalse(fighter2.getCombatAgent().getCurrentCM().isMagicAttack());
		assertEquals(CombatNeuronInputs.CANNOT_ATTACK.getValue(fighter2), 1.0, 0.001);
		
		f.oneRound();
		
		assertEquals(cleric1.getMp(), 2, 0);
		ArrayList<Spell> spells = cleric1.getActiveSpells();
		assertTrue(spells.get(0) instanceof MagicWeapon);
		
		assertTrue(fighter2.getCombatAgent().getCurrentCM().isMagicAttack());
		assertEquals(CombatNeuronInputs.CANNOT_ATTACK.getValue(fighter2), 0.0, 0.001);
		double newf1ToHit = fighter2.getCombatAgent().getHitChance(dretch.getCombatAgent(), null);
		double newf1dmg = fighter2.getCombatAgent().getAvgDmgPerRound(dretch.getCombatAgent(), null);
		
		assertEquals(f1ToHit - newf1ToHit, -0.05, 0.001);
		assertEquals(f1dmg - newf1dmg, -6.3*newf1ToHit, 0.05);
		
		// then in the next round, the cleric should choose to Bless, as no other Character is attacking
		// the Dretch
		
		f.oneRound();
		assertEquals(cleric1.getMp(), 1, 0);
		spells = cleric1.getActiveSpells();
		assertTrue(spells.get(1) instanceof Bless);
		
	}
	
	@Test
	public void testStackingWithMasterwork() {
		Party p = new Party(cleric1);
		p.addMember(fighter1);
		p.addMember(fighter2);
		
		Fight f = new Fight(p, dretch);
		f.setTarget(fighter1.getCombatAgent(), dretch.getCombatAgent());
		
		fighter1.addItem(Weapon.MASTERWORK_SHORT_SWORD);
		
		double f1ToHit = fighter1.getCombatAgent().getHitChance(dretch.getCombatAgent(), null);
		double f1dmg = fighter1.getCombatAgent().getAvgDmgPerRound(dretch.getCombatAgent(), null);

		new MagicWeapon().cast(cleric1, fighter1);
		
		double f1ToHit2 = fighter1.getCombatAgent().getHitChance(dretch.getCombatAgent(), null);
		double f1dmg2 = fighter1.getCombatAgent().getAvgDmgPerRound(dretch.getCombatAgent(), null);
		
		assertEquals(f1ToHit2 - f1ToHit, 0.00, 0.0001);
		assertEquals(f1dmg2 - f1dmg, 6.0*f1ToHit, 0.05);
		
		fighter1.removeItem(Weapon.MASTERWORK_SHORT_SWORD);
		
		double f1ToHit3 = fighter1.getCombatAgent().getHitChance(dretch.getCombatAgent(), null);
		double f1dmg3 = fighter1.getCombatAgent().getAvgDmgPerRound(dretch.getCombatAgent(), null);
		
		assertEquals(Weapon.SHORT_SWORD.getAvgDmgChange(fighter1), 0.0, 0.001);
		assertTrue(fighter1.getWeapon() == Weapon.SHORT_SWORD);
		assertEquals(f1ToHit2, f1ToHit3, 0.00001);
		assertEquals(f1dmg2, f1dmg3, 0.00001);
		
	}

}
