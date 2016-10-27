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

	@Override
	public double[] getAsArray() {
		return values;
	}

	@Override
	public String getAsString() {
		StringBuffer retValue = new StringBuffer();
		for (double d : values) {
			retValue.append(String.format("%.2f|", d));
		}
		return retValue.toString();
	}

}
