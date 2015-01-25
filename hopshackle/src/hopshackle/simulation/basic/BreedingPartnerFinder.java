package hopshackle.simulation.basic;
import hopshackle.simulation.*;

import java.util.*;

public class BreedingPartnerFinder {

	private BasicAgent lonelyHeart;
	private static double MINIMUM_HEALTH_FOR_BREEDING = SimProperties.getPropertyAsDouble("MinimumHealthForBreeding", "11.0");
	
	public BreedingPartnerFinder(BasicAgent testAgent1) {
		lonelyHeart = testAgent1;
	}

	public BasicAgent getPartner() {
		if (lonelyHeart.isMarried())
			return lonelyHeart.getPartner();
		
		Location currentLocation = lonelyHeart.getLocation();
		List<BasicAgent> shortList = new ArrayList<BasicAgent>();
		List<BasicAgent> shorterList = new ArrayList<BasicAgent>();
		double highestHealthSoFar = 0.0;
		double highestFoodSoFar = 0.0;
		for (Agent pp : currentLocation.getAgents()) {
			if (!(pp instanceof BasicAgent))
				continue;
			BasicAgent possiblePartner = (BasicAgent) pp;
			if (possiblePartner == lonelyHeart)
				continue;
			if (possiblePartner.getHealth() < MINIMUM_HEALTH_FOR_BREEDING)
				continue;
			if (possiblePartner.getHealth() < highestHealthSoFar)
				continue;
			if (possiblePartner.getHealth() > highestHealthSoFar) {
				shortList.clear();
				highestHealthSoFar = possiblePartner.getHealth();
			}
			shortList.add(possiblePartner);
		}
		for (BasicAgent possiblePartner : shortList) {
			if (possiblePartner.getNumberInInventoryOf(Resource.FOOD) < highestFoodSoFar)
				continue;
			if (possiblePartner.getNumberInInventoryOf(Resource.FOOD) > highestFoodSoFar) {
				shorterList.clear();
				highestFoodSoFar = possiblePartner.getNumberInInventoryOf(Resource.FOOD);
			}
			highestFoodSoFar = possiblePartner.getNumberInInventoryOf(Resource.FOOD);
			shorterList.add(possiblePartner);
		}
		
		if (shorterList.isEmpty()) return null;
		return (shorterList.get(Dice.roll(1, shorterList.size())-1));
	}

}
