package hopshackle.simulation.basic;
import hopshackle.simulation.*;

import java.util.*;

public class HutsOwnedByMatcher implements GoalMatcher<Location> {

	private Agent owner;
	List<Location> locationsWithHuts;

	public HutsOwnedByMatcher(Agent owner) {
		this.owner = owner;
		updateHutLocations();
	}

	private void updateHutLocations() {
		locationsWithHuts = new ArrayList<Location>();
		for (Artefact item : owner.getInventory()) {
			if (item instanceof Hut) {
				Hut hut = (Hut)item;
				locationsWithHuts.add(hut.getLocation());
			}
		}
	}

	@Override
	public boolean matches(Location loc) {
		updateHutLocations();
		return locationsWithHuts.contains(loc);
	}

	public String toString() {
		return "HUT";
	}

	public boolean equals(Object o) {
		if (o instanceof HutsOwnedByMatcher) {
			HutsOwnedByMatcher hom = (HutsOwnedByMatcher) o;
			return (hom.owner == owner);
		}
		return false;
	}

	public int hashCode() {
		return owner.hashCode();
	}

	@Override
	public boolean supercedes(GoalMatcher<Location> competitor) {
		return false;
	}
}
