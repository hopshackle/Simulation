package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.*;

import java.util.EnumSet;

public enum CareerActionsI implements ActionEnum {

	FIGHTER,
	EXPERT,
	CLERIC;
	
	public Action getAction(Agent a) {
		return getAction(a, a);
	}
	
	public boolean isChooseable(Agent a) {
		return true;
	}

	public Action getAction(Agent a1, Agent a2) {
		return null;
	}

	public String getChromosomeDesc() {
		return "CAREER1";
	}

	public EnumSet<CareerGeneticEnum> getGeneticVariables() {
		return EnumSet.allOf(CareerGeneticEnum.class);
	}
	
	@Override
	public Enum<CareerActionsI> getEnum() {
		return this;
	}
	
}
