package hopshackle.simulation.basic;

import hopshackle.simulation.*;

public class WildernessMatcher implements GoalMatcher<Location> {

	private int civLevel;

	public WildernessMatcher(Agent a) {
		Location currentLocation = a.getLocation();
		civLevel = getCivLevel((BasicHex)currentLocation);
	}
	private int getCivLevel(BasicHex currentLocation) {
		if (currentLocation.getTerrainType() != TerrainType.PLAINS) {
			return 20;
		}
		return CivilisationMatcher.getCivLevel(currentLocation);
	}
	@Override
	public boolean matches(Location loc) {
		if (loc instanceof BasicHex) {
			BasicHex bh = (BasicHex) loc;
			if (bh.getTerrainType() != TerrainType.PLAINS)
				return false;
			if (getCivLevel((BasicHex)loc) < civLevel) 
				return true;
		}
		return false;
	}

	@Override
	public boolean supercedes(GoalMatcher<Location> competitor) {
		if (competitor instanceof WildernessMatcher) {
			WildernessMatcher wlm = (WildernessMatcher)competitor;
			return civLevel < wlm.civLevel;
		}
		return false;
	}

	public String toString() {
		return "PLAINS";
	}

	public boolean equals(Object o) {
		if (o instanceof WildernessMatcher) {
			WildernessMatcher wm = (WildernessMatcher)o;
			return (wm.civLevel == civLevel);
		}
		return false;
	}

	public int hashCode() {
		return civLevel;
	}

}
