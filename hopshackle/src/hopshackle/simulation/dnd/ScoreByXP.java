package hopshackle.simulation.dnd;

import hopshackle.simulation.*;

public class ScoreByXP implements ScoringFunction {

	@Override
	public <T extends Agent> double getScore(T a) {
		double retValue = 0.0;
		if (!(a instanceof Character)) return retValue;
		retValue = ((Character)a).getXp();
		return retValue;
	}

}
