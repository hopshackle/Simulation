package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.*;

public enum ClericSpellValuations implements ActionEnum {
	CS_BLESS,
	CS_HEAL,
	CS_AC,
	CS_MAGIC;

	public boolean isChooseable (Agent a) {return true;}
	public Action getAction(Agent a) {
		// not relevant 
		return null;
	}
	public Action getAction(Agent a1, Agent a2) {
		return null;
	}

	public String getChromosomeDesc() {
		return "CLR_SPELL";
	}
	
	@Override
	public Enum<ClericSpellValuations> getEnum() {
		return this;
	}
}
