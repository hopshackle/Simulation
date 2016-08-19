package hopshackle.simulation;

import java.util.*;

public class SimpleInheritance<T extends Agent> extends Policy<T> {

	public SimpleInheritance() {
		super("inheritance");
	}

	@Override
	public void apply(Agent testator) {
		List<Artefact> tempInv = testator.getInventory();
		List<Agent> heirs = this.getInheritorsInOrder(testator);
		for (Agent heir : heirs)
			heir.log("Inherits from estate of " + testator + ":");
		distributeArtefactsToHeirs(testator, tempInv, heirs);
	}

	protected List<Agent> getInheritorsInOrder(Agent testator) {
		List<Agent> inheritors = new ArrayList<Agent>();
		for (Agent child : testator.getChildren()) {
			Agent inheritor = child;
			if (inheritor != null && !inheritor.isDead())
				inheritors.add(inheritor);
		}
		return inheritors;
	}

	public static <T extends Agent> void distributeArtefactsToHeirs(T testator, List<? extends Artefact> items, List<T> heirs) {
		int loop = 1;
		for (Artefact item : items) {
			if (item.isInheritable()) {
				if (!heirs.isEmpty()) {
					int heirNumber = (loop-1)%heirs.size();
					Agent inheritor = heirs.get(heirNumber);
					testator.removeItem(item);
					inheritor.addItem(item);
				}
				loop++;
			}
		}
	}

}
