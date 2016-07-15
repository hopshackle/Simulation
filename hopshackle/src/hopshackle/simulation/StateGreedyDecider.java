package hopshackle.simulation;

import java.util.ArrayList;

public abstract class StateGreedyDecider<A extends Agent, S extends State<A>> extends StateDecider<A, S> {

	public StateGreedyDecider(ArrayList<ActionEnum<A>> actions, ArrayList<GeneticVariable<A, S>> variables) {
		super(actions, variables);
	}
	
	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent) {
		double retValue = 0.0;
		HopshackleState agentState = getState(decidingAgent);
		retValue = agentState.valueOptionWithoutExploration(option);
		return retValue;
	}

}
