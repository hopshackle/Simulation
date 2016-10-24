package hopshackle.simulation.basic;

import hopshackle.simulation.*;

public class BreedingPartnerScoringFunction implements ScoringFunction {

	private BasicAgent lonelyHeart;

	public BreedingPartnerScoringFunction(BasicAgent suitor) {
		lonelyHeart = suitor;
	}

	@Override
	public <T extends Agent> double getScore(T a) {
		if (!(a instanceof BasicAgent))
			return -1;
		BasicAgent possiblePartner = (BasicAgent) a;
		if (possiblePartner.equals(lonelyHeart.getPartner()) 
				&& lonelyHeart.ableToBreed() 
				&& possiblePartner.ableToBreed()
			)
				return 1;
		return -1;
	}
}
