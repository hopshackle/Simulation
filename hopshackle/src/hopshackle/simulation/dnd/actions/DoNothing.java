package hopshackle.simulation.dnd.actions;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.Character;

public class DoNothing extends Action {

	public DoNothing (Agent c, int pause) {
		super(c, pause, true);
	}
	
	protected void doStuff() {
		if (actor instanceof Character) {
			Character ch = (Character)actor;
			ch.getChrClass().move(ch);
		}
	}
	
	public String toString() {return "STAY";}
}
