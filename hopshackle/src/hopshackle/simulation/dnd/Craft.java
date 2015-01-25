package hopshackle.simulation.dnd;

import hopshackle.simulation.Artefact;

import java.util.ArrayList;

public class Craft extends Skill {

	private ArrayList<Artefact> knownItems;

	public Craft(Character skillPossessor) {
		super(skillPossessor, skills.CRAFT);
		knownItems = new ArrayList<Artefact>();
	}

	public void addItem(Artefact item) {
		knownItems.add(item);
	}
	public void removeItem(Artefact item) {
		knownItems.remove(item);
	}
	public boolean canCraft(Artefact item) {
		return knownItems.contains(item);
	}

	public ArrayList<Artefact> getKnownItems() {
		ArrayList<Artefact> retValue = new ArrayList<Artefact>();
		for (Artefact i : knownItems) {
			retValue.add(i);
		}
		return retValue;
	}
	public ArrayList<Artefact> getMakeableItems() {
		ArrayList<Artefact> retValue = getKnownItems();
		for (Artefact i : knownItems) {
			if (timeToCreate(i) == Long.MAX_VALUE
					|| i.costToMake(skillPossessor) > skillPossessor.getGold()) {
				retValue.remove(i);
			}
		}
		return retValue;
	}

	public long timeToCreate(Artefact item) {
		double makeCost = 0.0;
		if (item instanceof HasBaseCost){
			makeCost = ((HasBaseCost)item).getBaseCost();
		} else {
			makeCost = item.costToMake(skillPossessor);
		}
		
		if (canCraft(item)) {
			int DC = item.getMakeDC();
			int take10 = 10 + level + skillPossessor.getIntelligence().getMod();
			if (take10<DC) {
				// can't actually create this item on a take 10 basis
				return Long.MAX_VALUE;
			}
			double progress = take10 * DC;
			double target = makeCost * 10;
			return (int)((target / progress)*500);
			// assumes 500ms = 1 week
			// so lifetime = 200 weeks; or 4 years
		} else return Long.MAX_VALUE;
	}

	public boolean create(Artefact item) {
		// Actually creates item, and deducts relevant raw material costs. 
		// Returns false if not successful for some reason
		double costToMake = item.costToMake(skillPossessor);
		if (skillPossessor.getGold() > costToMake) {
			if (timeToCreate(item) < Long.MAX_VALUE) {
				skillPossessor.addGold(-costToMake);
				skillPossessor.addItem(item);
			} else return false;
		} else return false;
		return true;
	}
	public String toString() {
		return "CRAFT";
	}

}
