package hopshackle.simulation.basic;

import hopshackle.simulation.*;

public class CivilisationMatcher implements GoalMatcher<Location> {

	private int civLevel;

	public CivilisationMatcher(Agent a) {
		Location currentLocation = a.getLocation();
		civLevel = getCivLevel(currentLocation);
	}

	@Override
	public boolean matches(Location loc) {
		if (getCivLevel(loc) > civLevel) 
			return true;
		return false;
	}

	public static int getCivLevel(Location loc) {
		if (!(loc instanceof BasicHex))
			throw new AssertionError("Expecting BasicHex in CivilisationMatcher");
		return ((BasicHex)loc).getHuts().size();
	}
	public String toString() {
		return "CIV";
	}

	public boolean equals(Object o) {
		if (o instanceof CivilisationMatcher) {
			CivilisationMatcher cm = (CivilisationMatcher)o;
			return (cm.civLevel == civLevel);
		}
		return false;
	}

	public int hashCode() {
		return civLevel;
	}

	public boolean supercedes(GoalMatcher competitor) {
		if (competitor instanceof CivilisationMatcher) {
			CivilisationMatcher clm = (CivilisationMatcher)competitor;
			return civLevel > clm.civLevel;
		}
		return false;
	}

}
