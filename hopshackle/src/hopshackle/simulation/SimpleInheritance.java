package hopshackle.simulation;

import java.util.*;

public class SimpleInheritance implements InheritancePolicy {

	@Override
	public <T extends Agent> void bequeathEstate(T testator) {
		List<Artefact> tempInv = testator.getInventory();
		List<T> heirs = this.getInheritorsInOrder(testator);
		for (T heir : heirs)
			heir.log("Inherits from estate of " + testator + ":");
		distributeArtefactsToHeirs(testator, tempInv, heirs);
	}

	@SuppressWarnings("unchecked")
	protected <T extends Agent> List<T> getInheritorsInOrder(T testator) {
		List<T> inheritors = new ArrayList<T>();
		for (Agent child : testator.getChildren()) {
			T inheritor = (T) child;
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
