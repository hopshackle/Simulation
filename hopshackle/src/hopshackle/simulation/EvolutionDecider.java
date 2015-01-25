package hopshackle.simulation;

import java.util.*;

public class EvolutionDecider extends VDecider {

	protected static double temperature;
	protected double maxNoise;
	protected static String name;

	public EvolutionDecider(List<ActionEnum> actions, List<GeneticVariable> variables) {
		super(actions, variables);
		maxNoise = SimProperties.getPropertyAsDouble("EvolutionNoise", "0.20");
	}

	public void setName(String newName) {
		super.setName(newName);
		double newMaxNoise = SimProperties.getPropertyAsDouble("EvolutionNoise." + newName, "-99.00");
		if (newMaxNoise > -98.00)
			maxNoise = newMaxNoise;
	}
	
	@Override
	public double valueOption(ActionEnum option, Agent decidingAgent, Agent contextAgent) {
		Genome g = decidingAgent.getGenome();
		if (g==null) return 0.0;

		double retValue = g.getValue(option, decidingAgent, contextAgent, variableSet);

		temperature = SimProperties.getPropertyAsDouble("Temperature", "1.0");
		return retValue *= (1.0 + (Math.random()-0.5)*temperature*maxNoise);
	}
	
	@Override
	public double valueState(double[] state) {
		throw new AssertionError("Not currently implemented");
	}

}
