package hopshackle.simulation.dnd;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.Skill.skills;
import hopshackle.simulation.dnd.actions.Heal;
import hopshackle.simulation.dnd.genetics.*;

import java.io.Serializable;
import java.util.*;

public enum CharacterClass implements Serializable{
	FIGHTER (10, false, 1, "FTR"),
	CLERIC (8, false, 1.333, "CLR"),
	WARRIOR (8, true, 1, "WAR"),
	EXPERT (6, true, 2, "EXP");

	private int hitDie;
	private boolean NPC;
	private double hitProgression;
	private String abbrev;
	private static HashMap<CharacterClass, BaseDecider> defaultDeciders = new HashMap<CharacterClass, BaseDecider>();
	private static HashMap<CharacterClass, BaseDecider> defaultCombatDeciders = new HashMap<CharacterClass, BaseDecider>();
	private static HashMap<CharacterClass, TradeDecider> defaultTradeDeciders = new HashMap<CharacterClass, TradeDecider>();
	private static ArrayList<GeneticVariable> clericSpellVarSet =  new ArrayList<GeneticVariable>(EnumSet.allOf(TradeGeneticEnumAdventurer.class));
	private boolean startWithPotion;

	static {
		clericSpellVarSet.remove(TradeGeneticEnumAdventurer.GOLD);
	}

	CharacterClass (int hitDie, boolean NPC, double hitProgression, String abb) {
		this.hitDie = hitDie;
		this.NPC = NPC;
		this.hitProgression = hitProgression;
		abbrev = abb;
	}

	public int getHitDie() {
		return hitDie;
	}
	public boolean getNPC() {
		return NPC;
	}
	public void setHitDie(int hitDie) {
		this.hitDie = hitDie;
	}
	public double hitProgression() {
		return hitProgression;
	}
	public int baseHit(int l) {
		// base hit modifier for level l
		return (int)((double) l / hitProgression);
	}
	public String getAbbrev() {
		return abbrev;
	}
	public void rest(Character c) {
		switch (this) {
		case WARRIOR:
		case FIGHTER:
		case EXPERT:
			break;
		case CLERIC:
			// use up magic points
			new Heal(c, false).run();
		}
	}
	public void move(Character c) {
		rest(c);
		// for the moment this is the same as rest - i.e. Clerics heal
	}
	/*
	 *  The start() method does any set up required for the Character that is not
	 *  directly associated with levelling up.
	 *  Specifically, it covers nay clean-up required once all of:
	 *  - Genome
	 *  - Career
	 *  
	 *  are known.
	 */

	public void levelUp(Character c) {

		if (c.getDecider() == null) c.setDecider(defaultDeciders.get(this));
		if (c.getCombatDecider() == null) c.setCombatDecider(defaultCombatDeciders.get(this));
		if (c.getTradeDecider() == null) c.setTradeDecider(defaultTradeDeciders.get(this));

		startWithPotion = (SimProperties.getProperty("StartWithPotion", "false").equals("true"));

		switch (this) {
		case WARRIOR:
			switch ((int)c.getLevel()) {
			case 1:
				new Skill(c, skills.WEAPON_PROFICIENCY_MARTIAL);
				c.setCombatDecider(new HardCodedDecider(CombatActionsI.ATTACK));
			}
			break;
		case CLERIC:
			c.setMaxMp(getMaxMP(c));
			setSpells(c);
			switch ((int)c.getLevel()) {
			case 1:
				if (startWithPotion)
					c.addItem(Potion.CURE_LIGHT_WOUNDS);	// starting kit
				break;
			case 2:
				break;
			case 3:
				break;
			case 4:
				break;
			}
			break;
		case FIGHTER:
			switch ((int)c.getLevel()) {
			case 1:
				// Martial weapon proficiency
				if (startWithPotion)
					c.addItem(Potion.CURE_LIGHT_WOUNDS);	// starting kit
				new Skill(c, skills.WEAPON_PROFICIENCY_MARTIAL);
				// Weapon Focus in a martial weapon
				new WeaponFocus(c, skills.WEAPON_FOCUS_MARTIAL);
				// plus Dodge
				new Dodge(c);
				break;
			case 2:
				// Weapon Proficiency - Bastard Sword
				new Skill(c, skills.WEAPON_PROFICIENCY_BASTARD_SWORD);
				break;
			case 3:
				// Weapon Focus - Bastard Sword
				new WeaponFocus(c, skills.WEAPON_FOCUS_BASTARD_SWORD);
				break;
			case 4:
				// Weapon Specialisation 
				new WeaponSpecialization(c, skills.WEAPON_SPECIALIZATION);
				break;
			}
			break;
		case EXPERT:
			Skill s = c.getSkill(Skill.skills.CRAFT);
			Craft craftSkill =null;
			if (s!=null) craftSkill = (Craft)s;
			switch ((int)c.getLevel()) {
			case 1:
				craftSkill = new Craft(c);
				craftSkill.addItem(Shield.SMALL_SHIELD);
				craftSkill.addItem(Armour.LEATHER_ARMOUR);
				craftSkill.addItem(Weapon.LIGHT_MACE);
				craftSkill.addItem(Weapon.HEAVY_MACE);
				craftSkill.addItem(Armour.STUDDED_LEATHER_ARMOUR);
				craftSkill.addItem(Shield.LARGE_SHIELD);
				craftSkill.addItem(Weapon.SHORT_SWORD);
				craftSkill.addItem(Weapon.LONG_SWORD);
				craftSkill.addItem(Armour.CHAIN_SHIRT);
				craftSkill.addItem(Armour.SPLINT_MAIL);
				craftSkill.addItem(Weapon.BASTARD_SWORD);
				craftSkill.level = 4;

				//originally 2
				craftSkill.addItem(Armour.CHAIN_MAIL);
				craftSkill.addItem(Armour.BANDED_MAIL);
				craftSkill.addItem(Shield.TOWER_SHIELD);

				// originally 3
				craftSkill.addItem(Potion.CURE_LIGHT_WOUNDS);
				craftSkill.addItem(Armour.HALF_PLATE);
				craftSkill.addItem(Weapon.MASTERWORK_BASTARD_SWORD);
				craftSkill.addItem(Weapon.MASTERWORK_SHORT_SWORD);
				craftSkill.addItem(Weapon.MASTERWORK_LONG_SWORD);

				// originally 4
				craftSkill.addItem(Armour.FULL_PLATE);

				//originally 5
				craftSkill.addItem(Weapon.LONG_SWORD_PLUS_1);
				craftSkill.addItem(Weapon.BASTARD_SWORD_PLUS_1);

				break;
			case 2: 
				craftSkill.level++;
				break;
			case 3:
				craftSkill.level += 3; // assume skill focus
				break;
			case 4:
				craftSkill.level++;
				break;
			default:
				craftSkill.level++;
			}
		}
	}

	public int getMaxMP(Character c) {
		int retValue = 0;
		switch (this) {
		case FIGHTER:
		case WARRIOR:
		case EXPERT:
			break;
		case CLERIC:
			int wisMod = c.getWisdom().getMod();
			switch ((int)c.getLevel()) {
			case 9-20:
			case 8:
			case 7:
			case 6:
				retValue+=8;
			case 5:
				retValue += 5;
				if (wisMod>2)
					retValue +=5;
			case 4:
				retValue +=4;
			case 3:
				retValue+=3;
				if (wisMod>1)
					retValue +=3;
			case 2:
				retValue +=1;
			case 1:
				retValue += 1;
				if (wisMod>0) retValue++;
				if (wisMod<0) retValue = 0;
				break;
			}
			break;
		}
		return retValue;
	}

	public void setSpells(Character c) {
		switch (this) {
		case WARRIOR:
		case FIGHTER:
		case EXPERT:
			return;
		case CLERIC:
			int wisdom = c.getWisdom().getValue();
			switch ((int)c.getLevel()) {
			case 5-20:
			case 4: 
				if (wisdom > 11) {
					if (!c.hasSpell("Spiritual Hammer")) c.addSpell(new SpiritualHammer());
				}
			case 3:
				if (wisdom > 11) {
					if (!c.hasSpell("Cure Moderate Wounds")) c.addSpell(new CureModerateWounds());
					if (!c.hasSpell("Bulls Strength")) c.addSpell(new BullsStrength());
				}
			case 2:
				if (wisdom > 10) 
					if (howManySpells(c, 1) < 3)
						pickClericL1Spell(c);
			case 1:
				if (wisdom > 10) {
					switch (howManySpells(c, 1)) {
					case 0:
						pickClericL1Spell(c);
					case 1:
						pickClericL1Spell(c);
					}
				}
			}
		}
	}

	private int howManySpells(Character c, int level) {
		int retValue = 0;
		for (Spell s : c.getSpellList()) 
			if (s.getLevel() == level) retValue++;
		return retValue;
	}

	private void pickClericL1Spell(Character c) {
		double valueBless = c.getGenome().getValue(ClericSpellValuations.CS_BLESS, c, clericSpellVarSet);
		if (c.hasSpell("Bless")) valueBless = Double.NEGATIVE_INFINITY; 
		double valueMagicWeapon = c.getGenome().getValue(ClericSpellValuations.CS_MAGIC, c, 1.0, clericSpellVarSet);
		if (c.hasSpell("Magic Weapon")) valueMagicWeapon = Double.NEGATIVE_INFINITY; 
		double valueHeal =  c.getGenome().getValue(ClericSpellValuations.CS_HEAL, c, 5.5, clericSpellVarSet);
		if (c.hasSpell("Cure Light Wounds")) valueHeal = Double.NEGATIVE_INFINITY; 
		double valueAC =  c.getGenome().getValue(ClericSpellValuations.CS_AC, c, 2.0, clericSpellVarSet);
		if (c.hasSpell("Shield of Faith")) valueAC = Double.NEGATIVE_INFINITY; 

		double maxValuation = Math.max(valueBless, Math.max(valueMagicWeapon, Math.max(valueHeal, valueAC))) - 0.001;

		boolean pickedSpell = false;
		if (valueMagicWeapon > maxValuation && !c.hasSpell("Magic Weapon")) {
			c.addSpell(new MagicWeapon());
			pickedSpell = true;
		}
		if (valueBless > maxValuation && !pickedSpell && !c.hasSpell("Bless")){
			c.addSpell(new Bless());
			pickedSpell = true;
		}
		if (valueAC > maxValuation && !pickedSpell && !c.hasSpell("Shield of Faith")){
			c.addSpell(new ShieldOfFaith());
			pickedSpell = true;
		}
		if (valueHeal > maxValuation && !pickedSpell && !c.hasSpell("Cure Light Wounds")){
			c.addSpell(new CureLightWounds());
			pickedSpell = true;
		}
	}

	public boolean setDefaultDecider(BaseDecider d) {
		defaultDeciders.put(this, d);
		return true;
	}
	
	public boolean setDefaultCombatDecider(BaseDecider d) {
		defaultCombatDeciders.put(this, d);
		return true;
	}

	public boolean setDefaultTradeDecider(TradeDecider d) {
		defaultTradeDeciders.put(this, d);
		return true;
	}
}

