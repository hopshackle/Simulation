package hopshackle.simulation.dnd;

import hopshackle.simulation.Dice;

public enum Race {
	HUMAN (	0,	0,	0,	0,	0,	0,  4),
	ORC (	4,	-2,	2,	-4,	-2,	-4, 8),
	GOBLIN( -2,	3,	0,	-1,	-2,	-4, 6),
	DRETCH(	2, 	0, 	4, 	-5, 0,	0,	6),
	GRICK(	4,	4,	0,	-7,	4,	-5,	6);

	final int strMod;
	final int dexMod;
	final int conMod;
	final int intMod;
	final int wisMod;
	final int chaMod;
	final int dmgDie;

	Race(int s, int d, int c, int i, int w, int cha, int dmg) {
		strMod = s;
		dexMod = d;
		conMod = c;
		intMod = i;
		wisMod = w;
		chaMod = cha;
		dmgDie = dmg;
	}

	public void applyRacialModifiers(Character c) {
		if (c.getRace() != this) return;

		switch(this) {
		case GRICK:
			setStandardMods(c, false);
			c.addCondition(new DamageReduction(c, 10));
			setStandardMods(c, false);
			c.addSkill(new Skill(c, Skill.skills.UNARMED_COMBAT));
			c.addItem(Armour.CHAIN_SHIRT);
			break;
		case DRETCH:
			setStandardMods(c, false);
			new Small(c);
			c.addCondition(new DamageReduction(c, 5));
			c.addSkill(new Skill(c, Skill.skills.UNARMED_COMBAT));
			c.addItem(Armour.CHAIN_SHIRT);
			c.addItem(Shield.SMALL_SHIELD);
			break;
		case GOBLIN:
			new Small(c);
		case ORC:
			setStandardMods(c, false);
			break;
		case HUMAN:
			setStandardMods(c, true);
			break;
		}
	}

	private void setStandardMods(Character c, boolean bestOf6) {
		if (bestOf6) {
			c.setStrength(new Attribute(Dice.bestOf(6, 4, 3) + strMod));
			c.setDexterity(new Attribute(Dice.bestOf(6, 4, 3) + dexMod));
			c.setConstitution(new Attribute(Dice.bestOf(6, 4, 3) + conMod));
			c.setIntelligence(new Attribute(Dice.bestOf(6, 4, 3) + intMod));
			c.setWisdom(new Attribute(Dice.bestOf(6, 4, 3) + wisMod));
			c.setCharisma(new Attribute(Dice.bestOf(6, 4, 3) + chaMod));
		} else {
			c.setStrength(new Attribute((Dice.roll(3,6)) + strMod));
			c.setDexterity(new Attribute((Dice.roll(3,6)) + dexMod));
			c.setConstitution(new Attribute((Dice.roll(3,6)) + conMod));
			c.setIntelligence(new Attribute((Dice.roll(3,6)) + intMod));
			c.setWisdom(new Attribute((Dice.roll(3,6)) + wisMod));
			c.setCharisma(new Attribute((Dice.roll(3,6)) + chaMod));
		}
	}

	public static Race getRace(String string) {
		// 		Returns the Race that corresponds to the String
		if (string.equals("GOBLIN")) {
			return Race.GOBLIN;
		}
		if (string.equals("ORC")) {
			return Race.ORC;
		}
		if (string.equals("HUMAN")) {
			return Race.HUMAN;
		}
		if (string.equals("DRETCH")) {
			return Race.DRETCH;
		}
		if (string.equals("GRICK")) {
			return Race.GRICK;
		}
		return Race.GOBLIN;
	}


}
