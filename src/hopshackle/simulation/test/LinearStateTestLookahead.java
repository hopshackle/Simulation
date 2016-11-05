package hopshackle.simulation.test;

import java.util.List;

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
}

class LookaheadTestDecider extends LookaheadDecider<BasicAgent> {

	public LookaheadTestDecider(StateFactory<BasicAgent> stateFactory,
			List<ActionEnum<BasicAgent>> actions) {
		super(stateFactory, actions);
	}

	@Override
	public void learnFrom(ExperienceRecord<BasicAgent> exp, double maxResult) {
	}

	@Override
	public double value(State<BasicAgent> state) {
		return 0;
	}
	
}
