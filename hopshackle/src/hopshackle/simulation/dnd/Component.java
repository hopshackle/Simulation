package hopshackle.simulation.dnd;

import hopshackle.simulation.*;

public enum Component implements DnDArtefact {

	ORC_HEART,
	GRICK_BEAK;

	public double costToMake(Agent a) {
		return Long.MAX_VALUE;
	}

	public long getTimeToMake(Agent a) {
		return Long.MAX_VALUE;
	}

	public int getACChange(Agent owner) {
		return 0;
	}
	public double getAvgDmgChange(Agent owner) {
		return 0;
	}
	public int getHitBonus(Agent owner) {
		return 0;
	}
	public double getOneOffHeal() {return 0.0;}

	public int getMakeDC() {
		return 50;
	}

	public Recipe getRecipe() {
		return null;
	}

	@Override
	public double getMagicAttack() {
		return 0;
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
