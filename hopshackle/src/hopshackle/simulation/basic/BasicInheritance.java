package hopshackle.simulation.basic;

import java.util.List;

import hopshackle.simulation.*;

public class BasicInheritance extends SimpleInheritance {


	@SuppressWarnings("unchecked")
	@Override
	protected <T extends Agent> List<T> getInheritorsInOrder(T testator) {
		List<T> inheritors = super.getInheritorsInOrder(testator);
		if (inheritors.isEmpty()) {
			BasicAgent t = ((BasicAgent) testator);
			List<BasicAgent> spouses = t.getAllPartners();
			if (!spouses.isEmpty())
				inheritors.add((T) spouses.get(spouses.size()-1));
		}
		return inheritors;
	}
}
