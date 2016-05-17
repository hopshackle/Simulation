package hopshackle.simulation.basic;

import java.util.*;
import hopshackle.simulation.*;

public class Breed extends BasicAction {

	private BasicAgent child, mother, father;
	
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
	}	

	public void doStuff() {
		child = new BasicAgent(father, mother);
		int childGeneration = Math.min(father.getGeneration(), mother.getGeneration()) + 1;
		child.setGeneration(childGeneration);
		child.updatePlan();
	}

	public String toString() {
		return "BREED";
	}
}
