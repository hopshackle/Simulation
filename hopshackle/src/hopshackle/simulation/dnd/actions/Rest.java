package hopshackle.simulation.dnd.actions;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.DnDAgent;

public class Rest extends Action {

	private boolean wanderingMonster = false;
	
	public Rest(DnDAgent c, boolean recordAction){
		super(c, recordAction);
		c.setStationary(true);
	}
	
	protected void doStuff() {
		((DnDAgent)actor).setStationary(false);
		// check is interrupted
		// if interrupted, then fight. Else gains hp
		Square s = (Square) actor.getLocation();
		int CR = s.getX() + s.getY();
		if (Dice.roll(1, 6) == 6 && (CR > 0)) {
			actor.log("Rest is interrupted by a wandering monster");
			wanderingMonster = true;
		} else {
			((DnDAgent)actor).rest(1);
		}
	}
	protected void doNextDecision() {
		if (wanderingMonster) {
			Action adventure = new Adventure((DnDAgent)actor, 0,-2, false);
			actor.addAction(adventure);
		} else {
			super.doNextDecision();
		}
	}
	
	public String toString() {return "REST";}
}
