package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.genetics.CareerDecisionHardCoded;

import java.io.File;
import java.util.ArrayList;

import org.junit.*;
public class CharacterTest {

	World w;
	Character clr1, clr2, ftr1, ftr2, exp1, exp2, goblin1, goblin2;
	private static String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");
	
	@Before
	public void setUp() throws Exception {
		w = new World();
		SimProperties.setProperty("Temperature", "0.00");
		Genome clrGenome = new Genome(new File(baseDir + "\\Genomes\\Standard\\CLR_TEST_SPELL.txt"));
		ftr1 = new Character(Race.HUMAN, new CareerDecisionHardCoded(CharacterClass.FIGHTER), null, null, w);
		ftr2 = new Character(Race.HUMAN, CharacterClass.FIGHTER, null, null, w);
		clr1 = new Character(Race.HUMAN, new CareerDecisionHardCoded(CharacterClass.CLERIC), null, clrGenome, w);
		clr2 = new Character(Race.HUMAN, CharacterClass.CLERIC, null, clrGenome, w);
		clr1.setWisdom(new Attribute(12));
		clr2.setWisdom(new Attribute(12));
		ftr1.start();
		clr1.start();
		goblin1 = new Character(Race.GOBLIN, CharacterClass.WARRIOR, null, null, w);
		goblin2 = new Character(Race.GOBLIN, new CareerDecisionHardCoded(CharacterClass.WARRIOR), null, null, w);
		goblin2.start();
	}
	
	@Test
	public void testItemRemoval() {
		assertFalse(ftr1.removeItem(Weapon.BASTARD_SWORD));
		ftr1.addItem(Weapon.BASTARD_SWORD);
		assertTrue(ftr1.removeItem(Weapon.BASTARD_SWORD));
		assertFalse(ftr1.removeItem(Weapon.BASTARD_SWORD));
	}

	@Test
	public void testCleric() {	
		assertEquals(clr1.getLevel(), 1, 0.0001);
		assertEquals(clr2.getLevel(), 1, 0.0001);
		
		assertEquals(clr1.getHp(), 8+clr1.getConstitution().getMod(), 0);
		assertEquals(clr2.getHp(), 8+clr2.getConstitution().getMod(), 0);
		
		assertTrue(clr1.hasSkill(Skill.skills.WEAPON_PROFICIENCY_SIMPLE));
		assertFalse(clr1.hasSkill(Skill.skills.DODGE));
		assertTrue(clr1.hasSpell("Cure Light Wounds"));
		assertTrue(clr1.hasSpell("Bless"));
		assertFalse(clr1.hasSpell("Shield of Faith"));
		assertFalse(clr1.hasSpell("Magic Weapon"));
		
		assertTrue(clr2.hasSkill(Skill.skills.WEAPON_PROFICIENCY_SIMPLE));
		assertFalse(clr2.hasSkill(Skill.skills.WEAPON_FOCUS_MARTIAL));
		assertTrue(clr2.hasSpell("Cure Light Wounds"));
		assertTrue(clr2.hasSpell("Bless"));
		assertFalse(clr2.hasSpell("Shield of Faith"));
		assertFalse(clr1.hasSpell("Magic Weapon"));
	}
	
	@Test
	public void testFighter() {
		assertEquals(ftr1.getLevel(), 1, 0.00001);
		assertEquals(ftr2.getLevel(), 1, 0.00001);
		
		assertEquals(ftr1.getHp(), 10+ftr1.getConstitution().getMod(), 0);
		assertEquals(ftr2.getHp(), 10+ftr2.getConstitution().getMod(), 0);
		
		assertTrue(ftr1.hasSkill(Skill.skills.WEAPON_PROFICIENCY_SIMPLE));
		assertTrue(ftr1.hasSkill(Skill.skills.DODGE));
		assertTrue(ftr1.hasSkill(Skill.skills.WEAPON_FOCUS_MARTIAL));
		assertFalse(ftr1.hasSkill(Skill.skills.CRAFT));
		assertFalse(ftr1.hasSpell("Cure Light Wounds"));
		
		assertTrue(ftr2.hasSkill(Skill.skills.WEAPON_PROFICIENCY_SIMPLE));
		assertTrue(ftr2.hasSkill(Skill.skills.DODGE));
		assertTrue(ftr2.hasSkill(Skill.skills.WEAPON_FOCUS_MARTIAL));
		assertFalse(ftr2.hasSkill(Skill.skills.CRAFT));
		assertFalse(ftr2.hasSpell("Cure Light Wounds"));
	
		ArrayList<CombatCondition> fca1, fca2;
		fca1 = ftr1.getCombatConditions();
		fca2 = ftr2.getCombatConditions();
		
		boolean ftr1HasWeaponFocus = false;
		boolean ftr1HasDodge = false;
		boolean ftr1HasBless = false;
		for (CombatCondition cc : fca1) {
			if (cc.toString().equals("WEAPON_FOCUS_MARTIAL"))
				ftr1HasWeaponFocus = true;
			if (cc.toString().equals("DODGE"))
				ftr1HasDodge = true;
			if (cc.toString().equals("BLESS"))
				ftr1HasBless = true;
		}
		assertTrue(ftr1HasWeaponFocus);
		assertTrue(ftr1HasDodge);
		assertFalse(ftr1HasBless);
		
		boolean ftr2HasWeaponFocus = false;
		boolean ftr2HasDodge = false;
		boolean ftr2HasBless = false;
		for (CombatCondition cc : fca1) {
			if (cc.toString().equals("WEAPON_FOCUS_MARTIAL"))
				ftr2HasWeaponFocus = true;
			if (cc.toString().equals("DODGE"))
				ftr2HasDodge = true;
			if (cc.toString().equals("BLESS"))
				ftr2HasBless = true;
		}
		assertTrue(ftr2HasWeaponFocus);
		assertTrue(ftr2HasDodge);
		assertFalse(ftr2HasBless);
		
	}

	@Test
	public void testWarrior() {
		assertEquals(goblin1.getLevel(), 1, 0.00001);
		assertEquals(goblin2.getLevel(), 1, 0.00001);
		
		assertTrue(goblin1.getHp() < 9+goblin1.getConstitution().getMod());
		assertTrue(goblin2.getHp() < 9+goblin2.getConstitution().getMod());
		
		assertTrue(goblin1.hasSkill(Skill.skills.WEAPON_PROFICIENCY_SIMPLE));
		assertFalse(goblin1.hasSkill(Skill.skills.DODGE));
		assertFalse(goblin1.hasSkill(Skill.skills.WEAPON_FOCUS_MARTIAL));
		assertFalse(goblin1.hasSkill(Skill.skills.CRAFT));
		assertFalse(goblin1.hasSpell("Cure Light Wounds"));
		
		assertTrue(goblin2.hasSkill(Skill.skills.WEAPON_PROFICIENCY_SIMPLE));
		assertFalse(goblin2.hasSkill(Skill.skills.DODGE));
		assertFalse(goblin2.hasSkill(Skill.skills.WEAPON_FOCUS_MARTIAL));
		assertFalse(goblin2.hasSkill(Skill.skills.CRAFT));
		assertFalse(goblin2.hasSpell("Cure Light Wounds"));
	
		ArrayList<CombatCondition> fca1, fca2;
		fca1 = goblin1.getCombatConditions();
		fca2 = goblin2.getCombatConditions();
		
		boolean goblin1HasWeaponFocus = false;
		boolean goblin1HasDodge = false;
		boolean goblin1HasBless = false;
		boolean goblin1HasSmall = false;
		for (CombatCondition cc : fca1) {
			if (cc.toString().equals("WEAPON_FOCUS_MARTIAL"))
				goblin1HasWeaponFocus = true;
			if (cc.toString().equals("DODGE"))
				goblin1HasDodge = true;
			if (cc.toString().equals("BLESS"))
				goblin1HasBless = true;
			if (cc instanceof Small)
				goblin1HasSmall = true;
		}
		assertFalse(goblin1HasWeaponFocus);
		assertFalse(goblin1HasDodge);
		assertFalse(goblin1HasBless);
		assertTrue(goblin1HasSmall);
		
		boolean goblin2HasWeaponFocus = false;
		boolean goblin2HasDodge = false;
		boolean goblin2HasBless = false;
		boolean goblin2HasSmall = false;
		for (CombatCondition cc : fca1) {
			if (cc.toString().equals("WEAPON_FOCUS_MARTIAL"))
				goblin2HasWeaponFocus = true;
			if (cc.toString().equals("DODGE"))
				goblin2HasDodge = true;
			if (cc.toString().equals("BLESS"))
				goblin2HasBless = true;
			if(cc instanceof Small)
				goblin2HasSmall = true;
		}
		assertFalse(goblin2HasWeaponFocus);
		assertFalse(goblin2HasDodge);
		assertFalse(goblin2HasBless);
		assertTrue(goblin2HasSmall);
		
	}
	
}
