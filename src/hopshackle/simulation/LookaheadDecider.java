package hopshackle.simulation;

import java.util.*;

public abstract class LookaheadDecider<A extends Agent> extends BaseDecider<A> {

	private LookaheadFunction<A> lookahead;
	protected List<ActionEnum<A>> dummyActionSet;

	public LookaheadDecider(StateFactory<A> stateFactory, LookaheadFunction<A> lookahead, List<ActionEnum<A>> actions) {
		super(stateFactory, actions);
		this.lookahead = lookahead;
		dummyActionSet = new ArrayList<ActionEnum<A>>();
		dummyActionSet.add(DummyAction.DUMMY);
	}

	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent) {
		LookaheadState<A> currentState = (LookaheadState<A>) stateFactory.getCurrentState(decidingAgent);
		LookaheadState<A> futureState = lookahead.apply(currentState, option);
		double retValue = value(futureState);
		if (localDebug) {
			String message = "Option " + option.toString() + " has base Value of " + retValue; //+
				//	" with state representation of: \n \t" + Arrays.toString(futureState.getAsArray());
			decidingAgent.log(message);
			log(message);
		}
		return retValue;
	}
	
	public abstract double value(LookaheadState<A> state);
	

}
