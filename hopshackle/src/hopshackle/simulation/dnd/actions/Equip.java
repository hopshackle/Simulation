package hopshackle.simulation.dnd.actions;

import hopshackle.simulation.Action;
import hopshackle.simulation.dnd.DnDAgent;


public class Equip extends Action {
	private int wait;
	
	public Equip(DnDAgent c, int pause){
		super(c, pause, true);
		wait = pause;
		((DnDAgent)actor).rest(1);
		// We then rest in order to put orders on market
	}

	protected void doStuff() {
		// Then we do the same again after waiting
		((DnDAgent)actor).rest(1);
		actor.addAge(-wait);
		// the point here is simply to wait for market to clear
		// The assumption is that this is always called initially in order for a character
		// to have a chance of equipping themselves
	}

	public String toString() {return "EQUIP";}
}
