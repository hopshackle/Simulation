package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.GeneticVariable;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

public enum CombatNeuronInputsReduced implements GeneticVariable {

	WOUND_US,
	WOUND_PARTY,
	TARGETTED,
	ODDS;

	public String getDescriptor() {
		return "CBT2";
	}

	public double getValue(Object a) {
		if (!(a instanceof Character)) return 0.00;
		Character c = (Character)a;
		CombatAgent opp = c.getCurrentTarget();
		if (opp == null) 
			opp = c.getCombatAgent();
		return getValue(a, (DnDAgent) opp.getAgent());
	}

	public double getValue(Object o1, Object o2) {
		if (!(o1 instanceof DnDAgent)) return 0.00;
		if (!(o2 instanceof DnDAgent)) return 0.00;
		DnDAgent a1 = (DnDAgent) o1;
		DnDAgent a2 = (DnDAgent) o2;
		switch (this) {
		case WOUND_US:
			return CombatNeuronInputs.WOUND_US.getValue(a1, a2);
		case WOUND_PARTY:
			return CombatNeuronInputs.WOUND_PARTY.getValue(a1, a2);
		case TARGETTED:
			return CombatNeuronInputs.TARGETTED.getValue(a1, a2);
		case ODDS:
			return CombatNeuronInputs.TARGETTED.getValue(a1, a2);
		}
		return 0.0;
	}

	public double getValue(Object a, double var) {
		return 0;
	}


	public boolean unitaryRange() {
		return true;
	}

}
