package hopshackle.simulation;

import java.util.List;

public class SimpleState<A extends Agent> implements State<A> {
	
	private List<GeneticVariable<A, SimpleState<A>>> variables;
	private double[] values;

	public SimpleState(A agent, List<GeneticVariable<A, SimpleState<A>>> var) {
		variables = var;
		values = new double[variables.size()];
		for (int i = 0; i < variables.size(); i ++) {
			GeneticVariable<A, SimpleState<A>> gv = variables.get(i);
			values[i] = gv.getValue(agent);
		}
	}

	public double getValue(GeneticVariable<A, SimpleState<A>> gv) {
		if (variables.contains(gv)) {
			return values[variables.indexOf(gv)];
		} else {
			return 0.0;
		}
	}

}
