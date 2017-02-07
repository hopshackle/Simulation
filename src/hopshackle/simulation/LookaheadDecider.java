package hopshackle.simulation;

public abstract class LookaheadDecider<A extends Agent> extends BaseDecider<A> {

	public LookaheadDecider(StateFactory<A> stateFactory) {
		super(stateFactory);
	}

	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent) {
		State<A> currentState = stateFactory.getCurrentState(decidingAgent);
		State<A> futureState = currentState.apply(option);
		double retValue = value(futureState);
		if (localDebug) {
			String message = "Option " + option.toString() + " has base Value of " + retValue; //+
				//	" with state representation of: \n \t" + Arrays.toString(futureState.getAsArray());
			decidingAgent.log(message);
			log(message);
		}
		return retValue;
	}
	
	public abstract double value(State<A> state);
	
}
