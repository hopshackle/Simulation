package hopshackle.simulation.basic;
import hopshackle.simulation.*;

import java.util.*;

public class PartnerFinder {

	private BasicAgent lonelyHeart;
	private ScoringFunction scoreFunction;

	public PartnerFinder(BasicAgent seniorPartner, ScoringFunction scoreFunction) {
		lonelyHeart = seniorPartner;
		this.scoreFunction = scoreFunction;
	}

	public BasicAgent getPartner() {
		Location currentLocation = lonelyHeart.getLocation();
		List<BasicAgent> shortList = new ArrayList<BasicAgent>();
		double highestScoreSoFar = 0;
		for (Agent pp : currentLocation.getAgents()) {
			if (!(pp instanceof BasicAgent))
				continue;
			double newScore = scoreFunction.getScore(pp);
			if (newScore > highestScoreSoFar) {
				shortList.clear();
				highestScoreSoFar = newScore;
			}
			if (newScore == highestScoreSoFar)
				shortList.add((BasicAgent)pp);
		}

		if (shortList.isEmpty()) return null;
		return (shortList.get(Dice.roll(1, shortList.size())-1));
	}

}
