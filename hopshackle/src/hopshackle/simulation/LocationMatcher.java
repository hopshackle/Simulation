package hopshackle.simulation;

public class LocationMatcher implements GoalMatcher<Location> {

	private Location target;

	public LocationMatcher(Location target) {
		this.target = target;
	}

	@Override
	public boolean matches(Location loc) {
		if (loc == target) {
				return true;
		}
		return false;
	}

	public String toString() {
		return target.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof LocationMatcher) {
			LocationMatcher lm = (LocationMatcher) o;
			if (target.equals(lm))
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		if (target == null) return 0;
		return target.hashCode();
	}

	@Override
	public boolean supercedes(GoalMatcher<Location> competitor) {
		return false;
	}
}

