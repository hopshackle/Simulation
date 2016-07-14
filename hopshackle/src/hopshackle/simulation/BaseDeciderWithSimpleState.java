package hopshackle.simulation;

import java.util.List;

public abstract class BaseDeciderWithSimpleState<A extends Agent> extends BaseDecider<A, SimpleState<A>> {

	public BaseDeciderWithSimpleState(List<? extends ActionEnum<A>> actions, List<GeneticVariable<A>> variables) {
		super(actions, variables);
	}

	@Override
	public SimpleState<A> getCurrentState(A decidingAgent, Agent contextAgent, Action<A> action) {
		return new SimpleState<A>(decidingAgent, variableSet);
	}

}
