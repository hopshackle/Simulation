package hopshackle.simulation.test;

import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;

/*
 * A test LookaheadState that simply wraps an internal LinearState and does not actually do any lookahead
 */
public class LinearStateTestLookahead implements State<BasicAgent> {
	
	private LinearState<BasicAgent> wrappedState;
	
	public LinearStateTestLookahead(LinearState<BasicAgent> toWrap) {
		wrappedState = toWrap;
	}

	@Override
	public double[] getAsArray() {
		return wrappedState.getAsArray();
	}
	@Override
	public String getAsString() {
		return wrappedState.getAsString();
	}
	
	@Override
	public State<BasicAgent> apply(ActionEnum<BasicAgent> proposedAction) {
		return this;
	}

	@Override
	public State<BasicAgent> clone() {
		return this;
	}

	@Override
	public int getActorRef() {
		return 0;
	}

	@Override
	public double[] getScore() {
		return new double[1];
	}
}

class LookaheadTestDecider extends LookaheadDecider<BasicAgent> {

	public LookaheadTestDecider(StateFactory<BasicAgent> stateFactory) {
		super(stateFactory);
	}

	@Override
	public void learnFrom(ExperienceRecord<BasicAgent> exp, double maxResult) {
	}

	@Override
	public double value(State<BasicAgent> state) {
		return 0;
	}
	
}
