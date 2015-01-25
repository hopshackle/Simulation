package hopshackle.simulation.dnd.actions;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.Character;

public class LeaveParty extends Action {
	public LeaveParty(Agent c, boolean recordAction) {
		super(c, 0, recordAction);
	}
	protected void doStuff() {
		Character c = (Character) actor;
		if (c.getParty() != null) {
			c.getParty().removeMember(c);
			c.purgeActions();
			// no new action kicked off. That will occur when Party
			// finishes current action
		}
	}
	protected void doNextDecision() {}
	public String toString() {return "LEAVE";}
}

