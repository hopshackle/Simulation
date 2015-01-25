package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.GeneticVariable;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

public enum DnDAgentInteractionGeneticSet implements GeneticVariable {

	WOUND_US,
	WOUND_THEM,
	LEVEL_DIFF,
	NUMBER_DIFF,
	ENTROPY_US,
	ENTROPY_THEM,
	FTR_DOMINANT_THEM,
	CLR_DOMINANT_THEM;

	public String getDescriptor() {
		return "GEN2";
	}

	public double getValue(DnDAgent a) {
		return getValue(a, a);
	}

	public double getValue(Object o1, Object o2) {
		if (!(o1 instanceof DnDAgent)) return 0.00;
		if (!(o2 instanceof DnDAgent)) return 0.00;
		DnDAgent d1 = (DnDAgent) o1;
		DnDAgent d2 = (DnDAgent) o2;

		switch (this) {
		case WOUND_US:
			return d1.getWound();
		case WOUND_THEM:
			return d2.getWound();
		case LEVEL_DIFF:
			return (d2.getLevel() - d1.getLevel()) /5.0;
		case NUMBER_DIFF:
			return (double) d1.getSize() / (double) d2.getSize();
		case ENTROPY_US:
			return entropy(d1);
		case ENTROPY_THEM:
			return entropy(d2);
		case FTR_DOMINANT_THEM:
			if (getDominantClass(d2) == CharacterClass.FIGHTER) return 1.0;
			return 0.0;
		case CLR_DOMINANT_THEM:
			if (getDominantClass(d2) == CharacterClass.CLERIC) return 1.0;
			return 0.0;
		}
		return 0.0;
	}

	public static CharacterClass getDominantClass(DnDAgent group) {
		int clericCount = 0;
		int fighterCount = 0;
		for (DnDAgent d : group.getMembers()) {
			Character c = (Character) d;
			if (c.getChrClass() == CharacterClass.CLERIC)
				clericCount += 1;
			if (c.getChrClass() == CharacterClass.FIGHTER)
				fighterCount += 1;
		}
		if (clericCount > fighterCount)
			return CharacterClass.CLERIC;
		if (fighterCount > clericCount)
			return CharacterClass.FIGHTER;
		return CharacterClass.EXPERT;
	}
	
	public static double entropy(DnDAgent group) {
		double clericCount = 0;
		double fighterCount = 0;
		for (DnDAgent d : group.getMembers()) {
			Character c = (Character) d;
			if (c.getChrClass() == CharacterClass.CLERIC)
				clericCount += 1;
			if (c.getChrClass() == CharacterClass.FIGHTER)
				fighterCount += 1;
		}
		clericCount = clericCount / group.getMembers().size();
		fighterCount = fighterCount / group.getMembers().size();
		double entropy = 0;
		if (clericCount > 0)
			entropy += clericCount * Math.log(clericCount);
		if (fighterCount > 0) 
			entropy += fighterCount * Math.log(fighterCount);
		entropy = - entropy;
		
		return entropy;
	}

	public double getValue(Object a, double var) {
		return 0;
	}

	public boolean unitaryRange() {
		return true;
	}
}
