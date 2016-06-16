package hopshackle.simulation.basic;

import java.util.*;

import hopshackle.simulation.*;

public class Marry extends BasicAction {
	
	BasicAgent p1, p2;
	
	public Marry(List<BasicAgent> partners) {
		super(BasicActions.MARRY, partners, new ArrayList<BasicAgent>(), ActionPlan.timeUntilAllAvailable(partners), 1000, true);
		p1 = mandatoryActors.get(0);
		p2 = mandatoryActors.get(1);
	}

	public Marry(BasicAgent ba, BasicAgent partner) {
		this(BasicUtilities.partnersAsList(ba, partner));
	}

	@Override
	public void doStuff() {
		if (!p1.isMarried() && !p2.isMarried()) {
			new Marriage(p1, p2);
			if (p1.isFemale()) p1.purgeActions(false);
			if (p2.isFemale()) p2.purgeActions(false);
		} 
	}
	
	public String toString() {
		return "MARRY";
	}
	
}
