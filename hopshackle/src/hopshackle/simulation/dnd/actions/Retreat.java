package hopshackle.simulation.dnd.actions;

import hopshackle.simulation.dnd.*;


public class Retreat extends CombatAction {

	private CombatAgent retreater;
	
	public Retreat(CombatAgent a) {
		super(a.getAgent(), true);
		retreater = a;
	}
	
	protected void doStuff() {
		Fight f = retreater.getCurrentFight();
		f.moveToReserve(retreater);
	}

	public String toString() {return "RETREAT";}
}
