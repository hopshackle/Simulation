package hopshackle.simulation.basic;

import hopshackle.simulation.*;

public class Farm extends BasicAction {

	public Farm(BasicAgent a) {
		super(BasicActions.FARM, a, 4000, true);
	}
	
	public void doStuff() {
		actor.addItem(Resource.FOOD);
		actor.addItem(Resource.FOOD);
		if (actor.isMarried()) {
			actor.addItem(Resource.FOOD);
			actor.addItem(Resource.FOOD);
		}
	}
	
	public String toString() {
		return "FARM";
	}
}
