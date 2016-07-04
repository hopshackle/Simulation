package hopshackle.simulation;

import java.util.List;

public abstract class LookaheadDecider<A extends Agent, B extends LookaheadState<A>> extends BaseDecider <A> {
	
	private LookaheadFunction<A, B> lookahead;

	public LookaheadDecider(LookaheadFunction<A, B> lookahead, List<ActionEnum<A>> actions, List<GeneticVariable<A>> variables) {
		super(actions, variables);
		this.lookahead = lookahead;
	}

	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent, Agent contextAgent) {
		B currentState = lookahead.getCurrentState(decidingAgent);
		B futureState = lookahead.apply(currentState, option);
		return value(futureState);
	}
	
	public abstract double value(B state);
	
	public double[] getState(LookaheadState<A> ps, List<GeneticVariable<A>> variableSet) {
		double[] inputs = new double[variableSet.size()];
		for (int i = 0; i < variableSet.size(); i ++) {
			GeneticVariable<A> gv = variableSet.get(i);
			inputs[i] = gv.getValue(ps);
		}

		return inputs;
	}
}
