package hopshackle.simulation;

public class UnexploredLocationMatcher implements GoalMatcher<Location> {

	private MapKnowledge map;
	
	public UnexploredLocationMatcher(MapKnowledge knownLocations) {
		map = knownLocations;
	}
	
	@Override
	public boolean matches(Location loc) {
		if (!map.isKnown(loc)) return true;
		for (Location adjacentLocation : loc.accessibleLocations) {
			if (!map.isKnown(adjacentLocation))
				return true;
		}
		return false;
	}

	public String toString() {
		return "UNKNOWN";
	}
	
	public boolean equals(Object o) {
		if (o instanceof UnexploredLocationMatcher) {
			UnexploredLocationMatcher ulm = (UnexploredLocationMatcher) o;
			return (ulm.map == map);
		}
		return false;
	}
	
	public int hashCode() {
		return map.hashCode();
	}

	@Override
	public boolean supercedes(GoalMatcher<Location> competitor) {
		return false;
	}
}
