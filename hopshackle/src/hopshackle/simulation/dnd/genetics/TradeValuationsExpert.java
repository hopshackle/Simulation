package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.*;

public enum TradeValuationsExpert implements ActionEnum {

	BASIC,
	RESERVE,
	VALUATION,
	TRADE_TIME;

	// BASIC is used for the price you're willing to pay on market grounds
	// RESERVE is used for the minumum price you will accept when selling 
	// VALUATION is the price used when determining P&L, and what item to make
	// i.e. the price you think it will sell for
	
	public boolean isChooseable (Agent a) {return true;}
	public Action getAction(Agent a) {
		// not relevant 
		return null;
	}
	public Action getAction(Agent a1, Agent a2) {
		return null;
	}

	public String getChromosomeDesc() {
		return "TRADE1";
	}
	@Override
	public Enum<TradeValuationsExpert> getEnum() {
		return this;
	}

}
