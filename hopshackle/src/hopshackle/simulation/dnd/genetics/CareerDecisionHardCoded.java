package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.CharacterClass;

public class CareerDecisionHardCoded extends BaseDecider {

	private ActionEnum hardCode;
	
	public CareerDecisionHardCoded(ActionEnum decision) {
		super(null, null);
		hardCode = decision;
	}
	
	public CareerDecisionHardCoded(CharacterClass chrClass) {
		super(null, null);
		switch (chrClass) {
		case CLERIC:
			hardCode = CareerActionsI.CLERIC;
			break;
		case EXPERT:
			hardCode = CareerActionsI.EXPERT;
			break;
		case FIGHTER:
			hardCode = CareerActionsI.FIGHTER;
			break;
		case WARRIOR:
			break;
		default:
			logger.warning("Invalid ChrClass in CareerDecisionHardCoded: " + chrClass);
			hardCode = CareerActionsI.FIGHTER;
		}
	}
	
	@Override
	public ActionEnum decide(Agent a) {
		return hardCode;
	}
	
	@Override
	public ActionEnum decideWithoutLearning(Agent a, Agent b) {
		return hardCode;
	}

	@Override
	public double valueOption(ActionEnum option, Agent decidingAgent, Agent contextAgent) {
		return 0;
	}
}
