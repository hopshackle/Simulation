package hopshackle.simulation;

import java.util.List;

public class LinearStateFactory<A extends Agent> implements StateFactory<A> {
	
	private List<GeneticVariable<A>> variables;
	
	public LinearStateFactory(List<GeneticVariable<A>> var) {
		variables = var;
	}

	@Override
	public State<A> getCurrentState(A agent) {
		LinearState<A> retValue = new LinearState<A>(agent, variables);
		return retValue;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <V extends GeneticVariable<A>> List<V> getVariables() {
		return (List<V>) variables;
	}

}
