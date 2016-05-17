package hopshackle.simulation.basic;

import java.util.*;
import hopshackle.simulation.*;

public class Breed extends BasicAction {

	private BasicAgent child, mother, father;
	private boolean parentsAreMarried;
	
	public Breed(BasicAgent parent1, BasicAgent parent2) {
		this(BasicUtilities.partnersAsList(parent1, parent2));
	}

	public Breed(List<BasicAgent> parents) {
		super(parents, new ArrayList<BasicAgent>(), ActionPlan.timeUntilAllAvailable(parents), 5000, true);
		for (int i = 0; i < mandatoryActors.size(); i++) {
			BasicAgent parent = (BasicAgent) mandatoryActors.get(i);
			if (parent.isFemale()) {
				mother = parent;
			} else {
				father = parent;
			}
		}
		parentsAreMarried = mother.getPartner() == father;
	}	

	public void doStuff() {
		child = new BasicAgent(father, mother);
		int childGeneration = Math.min(father.getGeneration(), mother.getGeneration()) + 1;
		child.setGeneration(childGeneration);
		child.updatePlan();
	}
	
	@Override protected void doNextDecision(BasicAgent actor) {
		if (actor == mother && parentsAreMarried) {
			// Do nothing
		} else {
			super.doNextDecision(actor);
		}
	}

	public String toString() {
		return "BREED";
	}
}
