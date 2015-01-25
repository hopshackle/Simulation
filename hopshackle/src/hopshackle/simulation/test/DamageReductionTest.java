package hopshackle.simulation.test;
import static org.junit.Assert.*;
import hopshackle.simulation.World;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

import org.junit.*;

public class DamageReductionTest {
	
	private World w;
	private Character fighter, orc;
	private CombatAgent ftr, oa;
	private Fight f;

	@Before
	public void setUp() throws Exception {
		w = new World();
		fighter = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		fighter.setStrength(new Attribute(14));
		fighter.setDexterity(new Attribute(13));
		ftr = fighter.getCombatAgent();

		fighter.addItem(Armour.CHAIN_SHIRT);
		fighter.addItem(Shield.LARGE_SHIELD);
		fighter.addItem(Weapon.LONG_SWORD);
		
		orc = new Character(Race.ORC, CharacterClass.WARRIOR, w);
		orc.setStrength(new Attribute(16));
		orc.setDexterity(new Attribute(13));
		
		f = new Fight(fighter, orc);
		
		oa = orc.getCombatAgent();
		
		f.setTarget(oa, ftr);
		f.setTarget(ftr, oa);
		
		
	}

	@Test
	public void testDamageReduced() {
		CombatModifier cmo = oa.getCurrentCM();
		CombatModifier cmf = ftr.getCurrentCM();
		assertEquals(cmo.getTotalDamageBonus(), 3);
		assertEquals(cmf.getTotalDamageBonus(), 2);
		
		orc.addCondition(new DamageReduction(orc, 5));
		
		cmo = oa.getCurrentCM();
		cmf = ftr.getCurrentCM();
		assertEquals(cmo.getTotalDamageBonus(), 3);
		assertEquals(cmf.getTotalDamageBonus(), -3);
	}
	
	@Test
	public void testZeroDamage() {
		fighter.setCombatDecider(TestFight.alwaysAttack);
		orc.setCombatDecider(TestFight.alwaysDefend);
		
		double dmg1 = ftr.getAvgDmgPerRound(oa, null);
		assertEquals(dmg1, 5.005, 0.001);
		double dmg2 = ftr.getAvgDmgPerRound(oa, Weapon.FIST);
		assertEquals(dmg2, 2.12625, 0.001);
		
		orc.addCondition(new DamageReduction(orc, 20));
		int orcHp = orc.getHp();
		
		assertEquals(ftr.getAvgDmgPerRound(oa, null), 0.00, 0.05);
		
		for (int loop=0; loop < 20; loop++) {
			f.oneRound();
			assertEquals(orc.getHp(), orcHp);
		}
	}
	
	@Test
	public void testNonZeroDamage() {
		fighter.setCombatDecider(TestFight.alwaysAttack);
		orc.setCombatDecider(TestFight.alwaysDefend);
		
		Character cleric = new Character(Race.HUMAN, CharacterClass.CLERIC, w);
		cleric.setWisdom(new Attribute(16));
		cleric.setCombatDecider(TestFight.alwaysDefend);
		cleric.levelUp();
		cleric.rest(1);
		
		f.addCombatant(cleric.getCombatAgent(), ftr);

		orc.addCondition(new DamageReduction(orc, 20));
		
		assertEquals(ftr.getAvgDmgPerRound(oa, null), 0.00, 0.05);
		Spell magicWeapon = new MagicWeapon();
		magicWeapon.cast(cleric, fighter);
		assertTrue(magicWeapon.isActive());
		assertTrue(ftr.getAvgDmgPerRound(oa, null) > 0.5);
		
		f.resolve();
		
		assertTrue(orc.isDead());
	}

}
