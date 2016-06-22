package hopshackle.simulation;

import java.util.*;

public class MapKnowledge {

	private Set<Location> knownLocations;
	private Set<Location> newLocationsSinceLastUpdate;
	private HashMap<String, JourneyPlan> journeyTrackers;
	private Agent agent;

	public MapKnowledge(Agent agent) {
		this.agent = agent;
		knownLocations = new HashSet<Location>();
		newLocationsSinceLastUpdate = new HashSet<Location>();
		journeyTrackers = new HashMap<String, JourneyPlan>();
	}

	public boolean addLocation(Location newLocation) {
		newLocationsSinceLastUpdate.add(newLocation);
		if (isKnown(newLocation)) {
			return false;
		} else {
			knownLocations.add(newLocation);
			agent.setHasUnexploredLocations(true);
			return true;
		}
	}

	public boolean isKnown(Location l) {
		return (knownLocations.contains(l));
	}

	public void addLocationMap(LocationMap fullMap) {
		for (Location l : fullMap.getAllLocations()) {
			addLocation(l);
		}
	}

	public void addMapKnowledge(MapKnowledge mapKnowledge) {
		for (Location l : mapKnowledge.knownLocations)
			this.addLocation(l);
	}

	public boolean addJourneyTracker(String key, GoalMatcher locationMatcher, MovementCostCalculator movementCalculator) {
		if (!journeyTrackers.containsKey(key)) {
			JourneyPlan tracker = new JourneyPlan(agent, locationMatcher, movementCalculator);
			journeyTrackers.put(key, tracker);
			return true;
		}
		return false;
	}

	public JourneyPlan getJourneyTracker(String key) {
		if (journeyTrackers.containsKey(key)) {
			return journeyTrackers.get(key);
		} else {
			return new JourneyPlan(agent, null, null);
		}
	}

	public void updateJourneyTrackers() {
		Set<String> keySet = journeyTrackers.keySet();
		for (String key : keySet) {
			JourneyPlan oldTracker = journeyTrackers.get(key);
			boolean dataChangeWarrantsJourneyTrackerUpdate = true;
			if (oldTracker.isEmpty()) {
				dataChangeWarrantsJourneyTrackerUpdate = false;;
				GoalMatcher oldLocMatcher = oldTracker.getLocationMatcher();
				for (Location newLoc : newLocationsSinceLastUpdate) {
					if (oldLocMatcher.matches(newLoc)) {
						dataChangeWarrantsJourneyTrackerUpdate = true;
						break;
					}
				}
			}
			if (dataChangeWarrantsJourneyTrackerUpdate) {
				JourneyPlan newPlan = new JourneyPlan(agent, oldTracker.getLocationMatcher(), oldTracker.getMovementCalculator());
				journeyTrackers.put(key, newPlan);
			}
		}
		newLocationsSinceLastUpdate.clear();
	}

	public int getSize() {
		return knownLocations.size();
	}
	public void prune() {
		Set<Location> toPrune = new HashSet<Location>();
		for (Location l : knownLocations) {
			if (Math.random() < 0.05 ) {
				toPrune.add(l);
			}
		}
		knownLocations.removeAll(toPrune);
		newLocationsSinceLastUpdate.clear();
		journeyTrackers.clear();
//		agent.log("Pruned " + toPrune.size() + " locations from memory. " + knownLocations.size() + " remaining.");
	}

}
