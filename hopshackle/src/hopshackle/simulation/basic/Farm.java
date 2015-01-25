package hopshackle.simulation.basic;

import hopshackle.simulation.*;

public class Farm extends Action {

	public Farm(Agent a) {
		super(a, 4000, true);
	}
	
	public void doStuff() {
		actor.addItem(Resource.FOOD);
		actor.addItem(Resource.FOOD);
		BasicAgent ba = (BasicAgent) actor;
		if (ba.isMarried()) {
			actor.addItem(Resource.FOOD);
			actor.addItem(Resource.FOOD);
		}
	}
	
	public String toString() {
		return "FARM";
	}
}
