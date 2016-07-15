package hopshackle.simulation;

import java.util.List;

public class SimpleStateBaseDecider<A extends Agent> extends BaseDecider<A, SimpleState<A>> {

	public SimpleStateBaseDecider(List<? extends ActionEnum<A>> actions, List<GeneticVariable<A, SimpleState<A>>> variables) {
		super(actions, variables);
	}

	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent) {
		return 0;
	}

	@Override
	public void learnFrom(ExperienceRecord<A, SimpleState<A>> exp, double maxResult) {
	}

	@Override
	public SimpleState<A> getCurrentState(A decidingAgent) {
		return new SimpleState<A>(decidingAgent, variableSet);
	}

}
