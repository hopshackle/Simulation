package hopshackle.simulation;

import java.util.ArrayList;

public class HardCodedDecider extends BaseDecider {

	private ActionEnum hardCode;

	public HardCodedDecider(ActionEnum decision) {
		super(new ArrayList<ActionEnum>(), new ArrayList<GeneticVariable>());
		hardCode = decision;
	}
	
	@Override 
	protected ActionEnum makeDecision(Agent decidingAgent, Agent contextAgent) {
		return hardCode;
	}

	@Override
	public double valueOption(ActionEnum option, Agent decidingAgent, Agent contextAgent) {
		return 0;
	}

	@Override
	public void learnFrom(ExperienceRecord exp, double maxResult) {
	}
}


