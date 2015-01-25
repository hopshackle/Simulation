/**
 * 
 */
package hopshackle.simulation.basic;

import hopshackle.simulation.*;

public enum BuildingType implements Artefact {
	HUT, 
	VILLAGE;

	@Override
	public double costToMake(Agent a) {
		return 0;
	}

	@Override
	public int getMakeDC() {
		return 0;
	}

	@Override
	public Recipe getRecipe() {
		return null;
	}

	@Override
	public long getTimeToMake(Agent a) {
		return 0;
	}

	@Override
	public boolean isA(Artefact item) {
		return item.equals(this);
	}

	@Override
	public void changeOwnership(Agent newOwner) {
	}

	@Override
	public boolean isInheritable() {
		return true;
	}
}