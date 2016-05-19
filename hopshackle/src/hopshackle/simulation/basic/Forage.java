package hopshackle.simulation.basic;

import hopshackle.simulation.*;

public class Forage extends BasicAction {

	public Forage(BasicAgent agent) {
		super(BasicActions.FORAGE, agent, 1000, true);
	}

	public void doStuff() {
		BasicHex currentLocation = (BasicHex) actor.getLocation();
		if (currentLocation == null) {
			logger.warning("Null location for " + actor + " while foraging.");
			return;
		}
		int numberOfAgents = 1;
		if (actor.isMarried()) numberOfAgents++;
		for (int loop = 0; loop < numberOfAgents; loop++) {
			switch (currentLocation.getTerrainType()) {
			case PLAINS:
				if (currentLocation.getCarryingCapacity() > 0) {
					actor.addItem(Resource.FOOD);
					currentLocation.changeCarryingCapacity(-1);
				}
				break;
			case FOREST:
				if (currentLocation.getCarryingCapacity() > 0) {
					actor.addItem(Resource.WOOD);
					currentLocation.changeCarryingCapacity(-1);
				}
				break;
			default:
			}
		}
	}

	public String toString() {
		return "FORAGE";
	}
}
