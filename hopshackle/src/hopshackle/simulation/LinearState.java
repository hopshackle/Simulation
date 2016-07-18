package hopshackle.simulation;

import java.util.List;

public class LinearState<A extends Agent> implements State<A> {
	
	private List<GeneticVariable<A>> variables;
	private double[] values;

	public LinearState(A agent, List<GeneticVariable<A>> var) {
		variables = var;
		values = new double[variables.size()];
		for (int i = 0; i < variables.size(); i ++) {
			GeneticVariable<A> gv = variables.get(i);
			values[i] = gv.getValue(agent);
		}
	}

	public double getValue(GeneticVariable<A> gv) {
		if (variables.contains(gv)) {
			return values[variables.indexOf(gv)];
		} else {
			return 0.0;
		}
	}

	@Override
	public double[] getAsArray() {
		return values;
	}

}
