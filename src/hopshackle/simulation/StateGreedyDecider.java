package hopshackle.simulation;

public class StateGreedyDecider<A extends Agent> extends StateDecider<A> {

	public StateGreedyDecider(StateFactory<A> stateFactory) {
		super(stateFactory);
	}
	
	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent) {
		double retValue = 0.0;
		HopshackleState agentState = getState(decidingAgent);
		retValue = agentState.valueOptionWithoutExploration(option);
		return retValue;
	}

}
