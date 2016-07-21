package hopshackle.simulation;

import java.util.List;

public class QuadraticStateFactory<A extends Agent> implements StateFactory<A> {
	
	private List<GeneticVariable<A>> variables;
	private List<GeneticVariable<A>> constructedVariables;
	private int gvLength, variableLength;
	
	public QuadraticStateFactory(List<GeneticVariable<A>> var) {
		variables = var;
		gvLength = variables.size();
		variableLength = gvLength * (gvLength + 1) / 2;
		// Now construct Quadratic variables
		for (int i = 0; i < gvLength; i++) {
			for (int j = i; j < gvLength; j++) {
				constructedVariables.add(new QuadraticVariable<A>(variables.get(i), variables.get(j)));
			}
		}
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

	/*
	 * TODO: Not currently used - cruft that may be useful later. 
	 * Quadratic States are inefficient currently in that they make O(n^2) calls to getValue()
	 * rather than the O(n) actually required, followed by some multiplication.
	 * But..let's see if we actually need to improve the performance here first.
	 */
	public double[] convertStateToQuadraticRepresentation(double[] baseState) {
		double[] stateDescriptor = new double[variableLength];
		for (int i = 0; i < gvLength; i++) {
			double val1 = baseState[i];
			for (int j = i; j < gvLength; j++) {
				stateDescriptor[i * gvLength + j] = val1 * baseState[j];
			}
		}
		return stateDescriptor;
	}

	@Override
	public StateFactory<A> cloneWithNewVariables(List<GeneticVariable<A>> newVar) {
		return new QuadraticStateFactory<A>(newVar);
	}
}

class QuadraticVariable<A extends Agent> implements GeneticVariable<A> {
	
	private GeneticVariable<A> gv1, gv2;
	
	public QuadraticVariable(GeneticVariable<A> gv1, GeneticVariable<A> gv2) {
		this.gv1 = gv1;
		this.gv2 = gv2;
	}
	
	@Override
	public String toString() {
		return gv1.toString() + " : " + gv2.toString();
	}

	@Override
	public double getValue(A agent) {
		return gv1.getValue(agent) * gv2.getValue(agent);
	}

	@Override
	public String getDescriptor() {
		return gv1.getDescriptor();
	}

	@Override
	public boolean unitaryRange() {
		return gv1.unitaryRange() && gv2.unitaryRange();
	}
	
}