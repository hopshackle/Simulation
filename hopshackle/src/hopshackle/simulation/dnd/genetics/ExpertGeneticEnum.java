package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.GeneticVariable;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

public enum ExpertGeneticEnum implements GeneticVariable {

	GOLD,
	LEVEL,
	AGE;

	private static String descriptor = "GEN4";

	public String getDescriptor() {return descriptor;}

	public double getValue(Object a1, Object a2) {
		return getValue(a1, 0.00);
	}

	public double getValue(Object a, double var) {

		if (!(a instanceof Character)) return 0.0;
		Character c = (Character) a;
		switch (this) {
		case LEVEL:
			int skillLevel = c.getSkill(Skill.skills.CRAFT).getLevel() + c.getIntelligence().getMod();
			return ((double)(skillLevel - 1) / 7.5) - 1.0;	
			//			return (c.getLevel()-3.0) / 3.0;
		case GOLD:
			double gold = c.getGold();
			if (gold >= 0.00) gold = 1.00;
			if (gold < 0.00) gold = -1.00;
			double absGold = Math.abs(c.getGold());
			if (absGold < 1.0) {
				absGold = 1.0;
				absGold = Math.sqrt(absGold);
			}
			absGold = 1.0 - (1.0 / absGold);
			if (gold >= 0.00) return absGold;
			return -absGold;
		case AGE:
			double retValue =  ((double)(c.getMaxAge()-c.getAge()))/((double)c.getMaxAge());
			return 2.0 * retValue - 1.0;
		}
		return 0.0;
	}

	public boolean unitaryRange() {
		return true;
	}

}
