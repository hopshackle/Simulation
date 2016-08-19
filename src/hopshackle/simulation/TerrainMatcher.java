package hopshackle.simulation;

public class TerrainMatcher implements GoalMatcher<Location> {
	
	private TerrainType terrainToMatch;
	
	public TerrainMatcher(TerrainType terrain) {
		terrainToMatch = terrain;
	}

	@Override
	public boolean matches(Location loc) {
		if (loc instanceof Hex) {
			Hex h = (Hex) loc;
			if (h.getTerrainType() == terrainToMatch)
				return true;
		}
		return false;
	}
	
	public String toString() {
		return terrainToMatch.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof TerrainMatcher) {
			TerrainMatcher tm = (TerrainMatcher) o;
			if (tm.terrainToMatch == terrainToMatch)
				return true;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		if (terrainToMatch == null) return 0;
		return terrainToMatch.ordinal();
	}

	@Override
	public boolean supercedes(GoalMatcher<Location> competitor) {
		return false;
	}
}
