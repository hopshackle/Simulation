package hopshackle.simulation;

public enum Resource implements Artefact {

	METAL,
	WOOD,
	FOOD;

	public int getMakeDC() {
		return 0;
	}
	
	public Recipe getRecipe() {
		return null;
	}

	public double costToMake(Agent a) {
		return 1;
	}
	public long getTimeToMake(Agent a) {
		return Long.MAX_VALUE;
	}

	@Override
	public boolean isA(Artefact item) {
		return (item.equals(this));
	}

	@Override
	public void changeOwnership(Agent newOwner) {
	}

	@Override
	public boolean isInheritable() {
		return false;
	}

}
