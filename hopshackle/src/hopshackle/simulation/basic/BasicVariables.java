package hopshackle.simulation.basic;

import hopshackle.simulation.*;

import java.util.List;

public enum BasicVariables implements GeneticVariable {

	CONSTANT,
	HEALTH,
	WATER,
	FOREST,
	PLAINS,
	POPULATION,
	HEX_CAPACITY,
	OWNS_HUT,
	HUT_PROXIMITY,
	FOOD_LEVEL,
	WOOD_LEVEL,
	CIV_LEVEL,
	MALE,
	MARRIED_STATUS,
	ENGAGED,
	AGE;

	private static String descriptor = "BASICGEN01";

	public String getDescriptor() {return descriptor;}

	@Override
	public double getValue(Object o, double var) {
		if (!(o instanceof BasicAgent)) return 0.00;
		BasicAgent agent = (BasicAgent)o;
		BasicHex agentLocation = (BasicHex)agent.getLocation();
		List<BuildingType> inventory = null;
		switch (this) {
		case CONSTANT:
			return 1.0;
		case HEALTH:
			return agent.getHealth() / agent.getMaxHealth();
		case POPULATION:
			int pop = agentLocation.getAgents().size();
			if (pop > 20) pop = 20;
			return (double) pop / 20.0;
		case WATER:
			for (Location l : agentLocation.getAccessibleLocations()) {
				Hex adjacentHex = (Hex) l;
				if (adjacentHex.getTerrainType() == TerrainType.OCEAN)
					return 1.0;
			}
			return getProximityToTerrain(agent);
		case FOREST:
			if (agentLocation.getTerrainType() == TerrainType.FOREST)
				return 1.0;
			return getProximityToTerrain(agent);
		case PLAINS:
			if (agentLocation.getTerrainType() == TerrainType.PLAINS)
				return 1.0;
			return getProximityToTerrain(agent);
		case OWNS_HUT:
			inventory = agent.getInventoryOf(BuildingType.HUT);
			if (inventory.size() > 0)
				return 1.0;
			return 0.0;
		case HUT_PROXIMITY:
			inventory = agent.getInventoryOf(BuildingType.HUT);
			if (inventory.size() == 0)
				return 0.0;
			for (Artefact bt : inventory) {
				Hut h = (Hut)bt;
				if (h.getLocation() == agentLocation)
					return 1.0;
			}
			return getProximityToTerrain(agent);
		case FOOD_LEVEL:
			int food = agent.getNumberInInventoryOf(Resource.FOOD);
			return (double)food / 10.0;
		case WOOD_LEVEL:
			int wood = agent.getNumberInInventoryOf(Resource.WOOD);
			return (double)wood / 3.0;
		case HEX_CAPACITY:
			int current = agentLocation.getCarryingCapacity();
			int maximum = agentLocation.getMaxCarryingCapacity();
			if (maximum > 0)
				return (double)current / (double)maximum;
			return 0;
		case CIV_LEVEL:
			int huts = agentLocation.getHuts().size();
			return ((double)huts) / 10.0;
		case MALE:
			if (agent.isMale())
				return 1.0;
			return 0.0;		
		case MARRIED_STATUS:
			if (agent.isMarried())
				return 1.0;
			return 0.0;
		case ENGAGED:
			ActionPlan ap = agent.getActionPlan();
			if (ap.contains(BasicActions.LOOK_FOR_PARTNER) || ap.contains(BasicActions.MARRY))
				return 1.0;
			return 0.0;
		case AGE:
			return ((double)agent.getAge() / (double)agent.getMaxAge());
		}
		return 0.0;
	}

	public double getProximityToTerrain(Agent agent) {
		JourneyPlan jPlan = agent.getMapKnowledge().getJourneyTracker(this.toString());
		if (jPlan.isEmpty()) {
			jPlan = addJourneyTracker(agent);
		}
		double distance = jPlan.distance();
		if (distance < -0.001 || distance > 20)
			return 0.0;		// i.e. we assume very far
		return (20.0 - distance) / 20.0;

	}

	private JourneyPlan addJourneyTracker(Agent agent) {
		GoalMatcher locMatcher = null;
		switch (this) {
		case WATER:
			locMatcher = new TerrainMatcher(TerrainType.OCEAN);
			break;
		case PLAINS:
			locMatcher = new TerrainMatcher(TerrainType.PLAINS);
			break;
		case HUT_PROXIMITY:
			locMatcher = new HutsOwnedByMatcher(agent);
			break;
		case FOREST:
			locMatcher = new TerrainMatcher(TerrainType.FOREST);
			break;
		default:
			assert(false);
		}
		agent.getMapKnowledge().addJourneyTracker(this.toString(), locMatcher, new TerrainMovementCalculator());
		return agent.getMapKnowledge().getJourneyTracker(this.toString());
	}

	@Override
	public double getValue(Object a1, Object a2) {
		return getValue(a1, 0.00);
	}

	@Override
	public boolean unitaryRange() {
		return true;
	}
}
