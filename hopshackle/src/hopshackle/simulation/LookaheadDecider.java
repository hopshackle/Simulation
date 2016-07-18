package hopshackle.simulation;

import java.util.List;

public abstract class LookaheadDecider<A extends Agent> extends BaseDecider<A> {
	
	private LookaheadFunction<A> lookahead;

	public LookaheadDecider(StateFactory<A> stateFactory, LookaheadFunction<A> lookahead, List<ActionEnum<A>> actions) {
		super(stateFactory, actions);
		this.lookahead = lookahead;
	}

	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent) {
		LookaheadState<A> currentState = lookahead.getCurrentState(decidingAgent);
		LookaheadState<A> futureState = lookahead.apply(currentState, option);
		return value(futureState);
	}
	
	public abstract double value(LookaheadState<A> state);
	
}
