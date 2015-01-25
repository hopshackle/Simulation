package hopshackle.simulation;

import java.util.ArrayList;

public class StateGreedyDecider extends StateDecider {

	public StateGreedyDecider(ArrayList<ActionEnum> actions, ArrayList<GeneticVariable> variables) {
		super(actions, variables);
	}
	
	@Override
	public double valueOption(ActionEnum option, Agent decidingAgent, Agent contextAgent) {
		double retValue = 0.0;
		HopshackleState agentState = getState(decidingAgent, contextAgent);
		retValue = agentState.valueOptionWithoutExploration(option);
		return retValue;
	}

}
