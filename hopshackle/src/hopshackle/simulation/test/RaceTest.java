package hopshackle.simulation.test;
import static org.junit.Assert.*;
import hopshackle.simulation.World;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

import org.junit.*;

public class RaceTest {
	
	private World w;
	
	@Before
	public void setUp() {
		w = new World();
	}

	@Test
	public void testGetRace() {
		Race r;
		r = Race.getRace("ORC");
		assertTrue(r.equals(Race.ORC));
		
		r = Race.getRace("GOBLIN");
		assertTrue(r.equals(Race.GOBLIN));
		
		r = null;
		r = Race.getRace("OOPS");
		assertTrue(r.equals(Race.GOBLIN));
	}
	
	@Test
	public void testDretch() {
		Character dretch = new Character(Race.DRETCH, CharacterClass.WARRIOR, w);
		Character dretch2 = new Character(Race.DRETCH, CharacterClass.WARRIOR, w);
		dretch.levelUp();
		
		int str = dretch.getStrength().getMod();
		int dex = dretch.getDexterity().getMod();
		int con = dretch.getConstitution().getMod();
		CombatAgent cad = dretch.getCombatAgent();
		CombatModifier cm = cad.getCurrentCM();
		CombatModifier cmDef = new CombatModifier(dretch2.getCombatAgent(), cad, null);
		
		// AC, Damage, hp, level
		assertEquals(cmDef.getArmour(), 4, 0);
		assertEquals(cmDef.getShield(), 1, 0);
		assertEquals(cmDef.getSize(), 0, 0);
		assertEquals(cm.getTotalDamageBonus(), str, 0);
		assertEquals(cmDef.getTotalDefenseBonus(), dex+5, 0);
		assertEquals(dretch.getHp(), 9+con*2, 7);
		assertEquals(cm.getTotalAttackBonus(), str+2, 0);
		
		// CombatConditions (Small, DamageReduction)
		assertEquals(dretch.getCombatConditions().size(), 2, 0);
		boolean isSmall = false, hasDR = false;
		for (CombatCondition cc : cad.getCombatConditions()) {
			if (cc instanceof Small) isSmall = true;
			if (cc instanceof DamageReduction) hasDR = true;
		}
		assertTrue(isSmall);
		assertTrue(hasDR);
		
	}
	
	@Test
	public void testGrick() {
		Character dretch = new Character(Race.DRETCH, CharacterClass.WARRIOR, w);
		Character grick = new Character(Race.GRICK, CharacterClass.WARRIOR, w);
		dretch.levelUp();
		grick.levelUp();
		
		int str = grick.getStrength().getMod();
		int dex = grick.getDexterity().getMod();
		if (dex > 4) dex = 4;
		int con = grick.getConstitution().getMod();
		CombatAgent cad = dretch.getCombatAgent();
		CombatAgent cag = grick.getCombatAgent();

		Fight f = new Fight(dretch, grick);
		f.setTarget(cad, cag);
		f.setTarget(cag, cad);
		
		CombatModifier cmg = grick.getCurrentCM();
		CombatModifier cmd = dretch.getCurrentCM();
		
		// AC, Damage, hp, level
		assertEquals(cmd.getArmour(), 4, 0);
		assertEquals(cmd.getShield(), 0, 0);
		assertEquals(cmd.getSize(), -1, 0);
		assertEquals(cmg.getTotalDamageBonus(), str-5, 0);  // -5 from dretch's damage reduction
		assertEquals(cmd.getTotalDefenseBonus(), dex+4-1, 0); // +4 from armour, -1 Medium to Small att
		assertEquals(cmg.getTotalAttackBonus(), str+2, 0);
		assertEquals(grick.getHp(), 9+con*2, 7);
		
		// CombatConditions (Small, DamageReduction)
		assertEquals(grick.getCombatConditions().size(), 1, 0);
		boolean isSmall = false, hasDR = false;
		for (CombatCondition cc : cag.getCombatConditions()) {
			if (cc instanceof Small) isSmall = true;
			if (cc instanceof DamageReduction) hasDR = true;
		}
		assertFalse(isSmall);
		assertTrue(hasDR);
		
	}
}
