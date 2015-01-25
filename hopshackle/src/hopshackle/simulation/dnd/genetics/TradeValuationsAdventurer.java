package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.*;

public enum TradeValuationsAdventurer implements ActionEnum {
	AC,
	AVG_DAMAGE,
	HEAL,
	MAGIC;

	public boolean isChooseable (Agent a) {return true;}
	public Action getAction(Agent a) {
		// not relevant 
		return null;
	}
	public Action getAction(Agent a1, Agent a2) {
		return null;
	}

	public String getChromosomeDesc() {
		return "TRADE2";
	}
	
	@Override
	public Enum<TradeValuationsAdventurer> getEnum() {
		return this;
	}
}
