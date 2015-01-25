package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.*;

import java.util.EnumSet;

public enum PartyJoinActionsII implements ActionEnum {
	ACCEPT_APPLICANT,
	REFUSE_APPLICANT;

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
		return "PJOIN2";
	}
	public EnumSet<DnDAgentInteractionGeneticSet> getGeneticVariables() {
		return EnumSet.allOf(DnDAgentInteractionGeneticSet.class);
	}
	
	@Override
	public Enum<PartyJoinActionsII> getEnum() {
		return this;
	}
}
