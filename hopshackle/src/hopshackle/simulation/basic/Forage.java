package hopshackle.simulation.basic;

import hopshackle.simulation.*;

public class Forage extends Action {

	public Forage(Agent agent) {
		super(agent, 1000, true);
	}

	public void doStuff() {
		BasicAgent agent = (BasicAgent) actor;
		BasicHex currentLocation = (BasicHex) agent.getLocation();
		if (currentLocation == null) {
			logger.warning("Null location for " + agent + " while foraging.");
			return;
		}
		int numberOfAgents = 1;
		if (agent.isMarried()) numberOfAgents++;
		for (int loop = 0; loop < numberOfAgents; loop++) {
			switch (currentLocation.getTerrainType()) {
			case PLAINS:
				if (currentLocation.getCarryingCapacity() > 0) {
					agent.addItem(Resource.FOOD);
					currentLocation.changeCarryingCapacity(-1);
				}
				break;
			case FOREST:
				if (currentLocation.getCarryingCapacity() > 0) {
					agent.addItem(Resource.WOOD);
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
