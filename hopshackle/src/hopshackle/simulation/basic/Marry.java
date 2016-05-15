package hopshackle.simulation.basic;

import java.util.*;

import hopshackle.simulation.*;

public class Marry extends Action {
	
	BasicAgent p1, p2;
	
	public Marry(List<Agent> partners) {
		super(partners, new ArrayList<Agent>(), ActionPlan.timeUntilAllAvailable(partners), 1000, true);
		p1 = (BasicAgent) mandatoryActors.get(0);
		p2 = (BasicAgent) mandatoryActors.get(1);
	}

	public Marry(BasicAgent ba, BasicAgent partner) {
		this(partnersAsList(ba, partner));
	}
	private static List<Agent> partnersAsList(BasicAgent a1, BasicAgent a2) {
		List<Agent> retValue = new ArrayList<Agent>();
		retValue.add(a1);
		retValue.add(a2);
		return retValue;
	}

	public void doStuff() {
		assert(mandatoryActors.size() == 2) : "Marriage without two participants " + mandatoryActors; 
		new Marriage(p1, p2);
	}
	
	@Override
	public void doNextDecision(Agent a) {
		BasicAgent ba = (BasicAgent)a;
		if (ba.isFemale()) {
			ba.purgeActions();
			return;
		}
		super.doNextDecision(a);
	}
	
	public String toString() {
		return "MARRY";
	}
	
}
