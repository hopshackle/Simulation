package hopshackle.simulation.basic;
import hopshackle.simulation.*;

import java.util.*;

public class MarriagePartnerFinder {

	private BasicAgent lonelyHeart;
	
	public MarriagePartnerFinder(BasicAgent seniorPartner) {
		lonelyHeart = seniorPartner;
	}

	public BasicAgent getPartner() {
		if (lonelyHeart.isMarried()) return null;
		Location currentLocation = lonelyHeart.getLocation();
		List<BasicAgent> shortList = new ArrayList<BasicAgent>();
		long lowestAgeSoFar = 180*60*1000;
		for (Agent pp : currentLocation.getAgents()) {
			if (!(pp instanceof BasicAgent))
				continue;
			BasicAgent possiblePartner = (BasicAgent) pp;
			if (possiblePartner.isMale() == lonelyHeart.isMale())
				continue;
			if (possiblePartner == lonelyHeart)
				continue;
			if (possiblePartner.isMarried())
				continue;
			if (possiblePartner.getHealth() < BasicAgent.FULL_HEALTH)
				continue;
			if (possiblePartner.getAge() > lowestAgeSoFar)
				continue;
			if (possiblePartner.getAge() < lowestAgeSoFar) {
				shortList.clear();
				lowestAgeSoFar = possiblePartner.getAge();
			}
			shortList.add(possiblePartner);
		}
		
		if (shortList.isEmpty()) return null;
		return (shortList.get(Dice.roll(1, shortList.size())-1));
	}

}
