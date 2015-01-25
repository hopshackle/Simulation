package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.*;

import java.util.EnumSet;

public enum PartyJoinActions implements ActionEnum {
	APPLY_TO_PARTY;

	public Action getAction(Agent a) {
		return getAction(a, a);
	}

	public boolean isChooseable(Agent a) {
		return true;
	}
	public Action getAction(Agent a1, Agent a2) {
		return new ApplyToPartyAction(a1, true);
	}

	public String getChromosomeDesc() {
		return "PJOIN1";
	}
	public EnumSet getGeneticVariables() {
		return EnumSet.allOf(DnDAgentInteractionGeneticSet.class);
	}

	@Override
	public Enum<PartyJoinActions> getEnum() {
		return this;
	}
}

class ApplyToPartyAction extends Action {
	
	ApplyToPartyAction(Agent a, boolean b){
		super(a, b);
	}
}