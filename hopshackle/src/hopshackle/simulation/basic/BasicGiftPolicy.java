package hopshackle.simulation.basic;

import hopshackle.simulation.*;

import java.util.*;

public class BasicGiftPolicy extends GiftPolicy {
	
	@Override
	public <T extends Agent> List<Gift> getListOfItemsGiven(T g) {
		if (!(g instanceof BasicAgent))
			return new ArrayList<Gift>();
		BasicAgent giver = (BasicAgent) g;
		List<Gift> listOfGifts = new ArrayList<Gift>();
		int hutsToKeep = 1;
		if (giver.isFemale() && giver.isMarried())
			hutsToKeep = 0;

		int identifiedHutToKeep = 0;
		if (giver.getNumberInInventoryOf(BuildingType.HUT) > hutsToKeep) {
			List<Agent> children = giver.getChildren();
			List<Artefact> inv = giver.getInventory();
			List<Hut> hutsToGive = new ArrayList<Hut>();
			for (Artefact item : inv) {
				if (item instanceof Hut) {
					if (identifiedHutToKeep < hutsToKeep) {
						if (((Hut)item).getLocation() == giver.getLocation()) {
							identifiedHutToKeep++;
							continue;
						}
					}
					hutsToGive.add((Hut)item);
				}
			}
			int nextHutAvailable = (hutsToKeep - identifiedHutToKeep);
			for (Agent child : children) {
				if (child.isDead())
					continue;
				if (child.getNumberInInventoryOf(BuildingType.HUT) == 0) {
					Gift gift = new Gift(child, hutsToGive.get(nextHutAvailable));
					listOfGifts.add(gift);
					nextHutAvailable++;
				}
				if (nextHutAvailable >= hutsToGive.size())
					break;
			}
		}
		
		return listOfGifts;
	}
}
