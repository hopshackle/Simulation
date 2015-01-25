package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.GeneticVariable;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

public enum CareerGeneticEnum implements GeneticVariable {
	STR_MOD,
	DEX_MOD,
	CON_MOD,
	INT_MOD,
	WIS_MOD,
	CHA_MOD;

	public String getDescriptor() {
		return "CAREER01";
	}

	public double getValue(DnDAgent a) {
		
		if (!(a instanceof Character)) return 0.0;
		
		Character c = (Character) a;
		switch (this) {
		case STR_MOD:
			return c.getStrength().getMod()*0.20;
		case DEX_MOD:
			return c.getDexterity().getMod()*0.20;
		case CON_MOD:
			return c.getConstitution().getMod()*0.20;
		case INT_MOD:
			return c.getIntelligence().getMod()*0.20;
		case WIS_MOD:
			return c.getWisdom().getMod()*0.20;
		case CHA_MOD:
			return c.getCharisma().getMod()*0.20;
		}
		return 0.0;
	}

	public double getValue(Object o1, Object o2) {
		if (!(o1 instanceof DnDAgent)) return 0.00;
		DnDAgent a1 = (DnDAgent) o1;
		return getValue(a1);
	}

	public double getValue(Object a, double var) {
		return 0;
	}

	public boolean unitaryRange() {
		return true;
	}
	
}
