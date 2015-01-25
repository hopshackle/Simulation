package hopshackle.simulation.dnd.actions;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

public class Move extends Action {

	private Square destination;
	private DnDAgent c;
	private boolean wanderingMonster = false;
	
	public Move(DnDAgent a, Location destination) {
		super(a, true);
		this.destination = (Square)destination;
		c = (DnDAgent) actor;
	}

	protected void doStuff() {
		// all we do is move the Agent to the new Location
		if (c instanceof Character) {
			Character ch = (Character) c;
			ch.getChrClass().move(ch);
		}
		
		actor.setLocation(destination);
		c.log("Moves to " + destination.toString());
		if ((destination.getX() + destination.getY()) > 0 && Dice.roll(1,20) < 10) {
			// encounter on the way
			actor.log("While travelling, you encounter a wandering monster");
			wanderingMonster = true;
		}
	}
	protected void doNextDecision() {
		if (wanderingMonster) {
			Action adventure = new Adventure(c, 0,-2, false);
			actor.addAction(adventure);	
		} else {
			super.doNextDecision();
		}
	}
	
	public String toString() {return "MOVE";}
}
