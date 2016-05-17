package hopshackle.simulation.basic;

import java.util.*;

import hopshackle.simulation.*;

public class Marry extends BasicAction {
	
	BasicAgent p1, p2;
	
	public Marry(List<BasicAgent> partners) {
		super(partners, new ArrayList<BasicAgent>(), ActionPlan.timeUntilAllAvailable(partners), 1000, true);
		p1 = mandatoryActors.get(0);
		p2 = mandatoryActors.get(1);
	}

	public Marry(BasicAgent ba, BasicAgent partner) {
		this(BasicUtilities.partnersAsList(ba, partner));
	}

	@Override
	public void initialisation() {
		assert(mandatoryActors.size() == 2) : "Marriage without two participants " + mandatoryActors; 
		if (!p1.isMarried() && !p2.isMarried()) {
			new Marriage(p1, p2);
		} else {
			this.cancel();
		}
	}
	
	@Override
	public void doNextDecision(BasicAgent a) {
		if (a.isFemale()) {
			a.purgeActions(false);
			return;
		}
		super.doNextDecision(a);
	}
	
	public String toString() {
		return "MARRY";
	}
	
}
