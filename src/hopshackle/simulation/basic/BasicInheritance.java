package hopshackle.simulation.basic;

import java.util.List;

import hopshackle.simulation.*;

public class BasicInheritance<T extends Agent> extends SimpleInheritance<T> {

	@Override
	protected List<Agent> getInheritorsInOrder(Agent testator) {
		List<Agent> inheritors = super.getInheritorsInOrder(testator);
		if (inheritors.isEmpty()) {
			BasicAgent t = ((BasicAgent) testator);
			List<BasicAgent> spouses = t.getAllPartners();
			if (!spouses.isEmpty())
				inheritors.add(spouses.get(spouses.size()-1));
		}
		return inheritors;
	}
}
