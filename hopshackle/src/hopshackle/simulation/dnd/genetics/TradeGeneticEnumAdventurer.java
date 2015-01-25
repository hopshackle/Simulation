package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.GeneticVariable;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

public enum TradeGeneticEnumAdventurer implements GeneticVariable {
	GOLD,
	FIVE,
	TEN,
	FIFTY,
	HUNDRED,
	VARIABLE;

	public String getDescriptor() {
		return "GEN6";
	}

	public double getValue(DnDAgent a) {
		return getValue(a, 0.00);
	}

	public double getValue(Object o1, Object o2) {
		return getValue(o1, 0.0);
	}

	public double getValue(Object a, double var) {
		if (!(a instanceof Character)) return 0;
		Character c = (Character)a;
		if (c.isDead()) return 0;

		switch (this) {
		case GOLD:
			return c.getGold();
		case FIVE:
			return 5.0;
		case TEN:
			return 10.0;
		case FIFTY:
			return 50.0;
		case HUNDRED:
			return 100.0;
		case VARIABLE:
			return var;
		}

		return 0.0;
	}

	public boolean unitaryRange() {
		return false;
	}
}
