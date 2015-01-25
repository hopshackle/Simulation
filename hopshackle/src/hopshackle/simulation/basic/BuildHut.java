package hopshackle.simulation.basic;

import hopshackle.simulation.*;

public class BuildHut extends Action {

	public BuildHut(Agent a) {
		super(a, 5000, true);
		Location hutLocation = a.getLocation();
		if (hutLocation instanceof BasicHex) {
			BasicHex hex = (BasicHex) hutLocation;
			hex.changeMaxCarryingCapacity(-1);
		}
	}
	
	public void doStuff() {
		new Hut(actor);
	}
	
	public String toString() {
		return "BUILD_HUT";
	}
}
