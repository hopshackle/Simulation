package hopshackle.simulation.basic;

import hopshackle.simulation.Action;

public class Breed extends Action {

	private BasicAgent child;

	public Breed(BasicAgent parent1, BasicAgent parent2) {
		super(parent1, 5000, true);
		if (!breedingPartnersAreMarried(parent1, parent2)) {
			parent2.purgeActions();
			parent2.addAction(new Rest(parent2, 4990));
		}
		child = new BasicAgent(parent1, parent2);
		int childGeneration = Math.min(parent1.getGeneration(), parent2.getGeneration()) + 1;
		child.setGeneration(childGeneration);
		child.addAction(new Rest(child, 5010));
	}
	
	private boolean breedingPartnersAreMarried(BasicAgent parent1, BasicAgent parent2) {
		return (parent1.getPartner() == parent2);
	}

	public void doStuff() {
		// nothing To Do
	}

	public String toString() {
		return "BREED";
	}
}
