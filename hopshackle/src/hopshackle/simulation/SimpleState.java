package hopshackle.simulation;

import java.util.List;

public class SimpleState<A extends Agent> implements State<A> {
	
	private List<GeneticVariable<A>> variables;
	private double[] values;
	
	public SimpleState(A agent, List<GeneticVariable<A>> var) {
		variables = var;
		values = new double[variables.size()];
		for (int i = 0; i < variables.size(); i ++) {
			GeneticVariable<A> gv = variables.get(i);
			values[i] = gv.getValue(agent, agent);
		}
	}

	@Override
	public double[] asArray(List<GeneticVariable<A>> ignoredArgument) {
		return values;
	}

}
