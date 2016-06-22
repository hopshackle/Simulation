package hopshackle.simulation.basic;

import hopshackle.simulation.*;

public class MarriagePartnerScoringFunction implements ScoringFunction {
	
	private BasicAgent lonelyHeart;
	
	public MarriagePartnerScoringFunction(BasicAgent suitor) {
		lonelyHeart = suitor;
	}

	@Override
	public <T extends Agent> double getScore(T a) {
		if (lonelyHeart.isMarried()) 
			return -1;
		if (lonelyHeart.getActionPlan().contains(BasicActions.MARRY)) 
			return -1;
		if (!(a instanceof BasicAgent))
			return -1;
		BasicAgent possiblePartner = (BasicAgent) a;
		if (possiblePartner.isMale() == lonelyHeart.isMale())
			return -1;
		if (possiblePartner == lonelyHeart)
			return -1;
		if (possiblePartner.isMarried())
			return -1;
		if (possiblePartner.getActionPlan().contains(BasicActions.MARRY)) 
			return -1;
		if (possiblePartner.getAge() < 1000)
			return -1;
		return possiblePartner.getHealth() + breedingYearsRemaining(possiblePartner);
	}

	private double breedingYearsRemaining(BasicAgent possiblePartner) {
		// Initial implementation just assume breeding age max is 100 seconds
		return (100000 - possiblePartner.getAge())/1000;
	}
}
