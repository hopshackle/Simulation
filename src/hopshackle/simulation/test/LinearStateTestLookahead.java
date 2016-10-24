package hopshackle.simulation.test;

import java.util.List;

import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;

/*
 * A test LookaheadState that simply wraps an internal LinearState and does not actually do any lookahead
 */
public class LinearStateTestLookahead implements LookaheadState<BasicAgent> {
	
	private LinearState<BasicAgent> wrappedState;
	
	public LinearStateTestLookahead(LinearState<BasicAgent> toWrap) {
		wrappedState = toWrap;
	}

	@Override
	public double[] getAsArray() {
		return wrappedState.getAsArray();
	}

	@Override
	public LookaheadState<BasicAgent> apply(ActionEnum<BasicAgent> proposedAction) {
		return this;
	}

	@Override
	public LookaheadState<BasicAgent> clone() {
		return this;
	}

}

class LookaheadTestFunction<A extends Agent> implements LookaheadFunction<A> {

	@Override
	public LookaheadState<A> apply(LookaheadState<A> currentState, ActionEnum<A> option) {
		return currentState;
	}

	@Override
	public LookaheadState<A> getCurrentState(A agent) {
		// TODO Auto-generated method stub
		return null;
	}
}

class LookaheadTestDecider extends LookaheadDecider<BasicAgent> {

	public LookaheadTestDecider(StateFactory<BasicAgent> stateFactory,
			LookaheadFunction<BasicAgent> lookahead,
			List<ActionEnum<BasicAgent>> actions) {
		super(stateFactory, lookahead, actions);
	}

	@Override
	public void learnFrom(ExperienceRecord<BasicAgent> exp, double maxResult) {
	}

	@Override
	public double value(LookaheadState<BasicAgent> state) {
		return 0;
	}
	
}
