package hopshackle.simulation;

import java.util.*;

public class GeneralQuadraticQDecider<A extends Agent> extends GeneralLinearQDecider<A> {

	private int gvLength;

	public GeneralQuadraticQDecider(List<? extends ActionEnum<A>> actions, List<GeneticVariable> variables) {
		super(actions, variables);
		actionLength = actions.size();
		gvLength = variables.size();
		variableLength = gvLength * gvLength;
		weights = new double[actionLength+1][variableLength];
		// convention is that the first V entries are for all variables * first variable
		// then the V+1 to 2V for all variables * second variable etc....
		for (int i=0; i<actionLength+1; i++)
			for (int j=0; j<variableLength; j++) 
				weights[i][j] = 1.0;		// initialise to 1.0 to promote optimistic exploration
	}

	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent, Agent contextAgent) {
		double[] stateDescriptor = new double[variableLength];
		for (int i = 0; i < gvLength; i++) {
			GeneticVariable var1 = variableSet.get(i);
			stateDescriptor[i] = var1.getValue(decidingAgent, contextAgent);
		}
		return valueOption(option, stateDescriptor);
	}

	@Override
	public double valueOption(ActionEnum<A> option, double[] state) {
		int optionIndex = actionSet.indexOf(option)+1;
		double retValue = 0.0;

		for (int i = 0; i < gvLength; i++) {
			for (int j = i; j < gvLength; j++){
				retValue += weights[0][i*gvLength + j] * state[i] * state[j];
				retValue += weights[optionIndex][i*gvLength + j] * state[i] * state[j];
			}
		}

		return retValue;
	}

	public void updateWeight(GeneticVariable var1, GeneticVariable var2, ActionEnum<A> option, double delta) {
		int optionIndex = actionSet.indexOf(option)+1;
		int varIndex = variableSet.indexOf(var1) * gvLength + variableSet.indexOf(var2);
		weights[0][varIndex] += delta - weights[0][varIndex] * lambda;
		weights[optionIndex][varIndex] += delta - weights[optionIndex][varIndex] * lambda;
	}

	public double[] getWeightOf(GeneticVariable input1, GeneticVariable input2, ActionEnum<A> option) {
		int optionIndex = actionSet.indexOf(option)+1;
		int varIndex = variableSet.indexOf(input1) * gvLength + variableSet.indexOf(input2);
		double[] retValue = new double[2];
		retValue[0] = weights[0][varIndex];
		retValue[1] = weights[optionIndex][varIndex];
		return retValue;
	}
	
	@Override
	public void learnFrom(ExperienceRecord<A> exp, double maxResult) {
		double bestNextAction = valueOfBestAction(exp);
		ActionEnum<A> actionTaken = exp.getActionTaken().actionType;
		double[] startState = exp.getStartState();
		double observedResult = exp.getReward();
		double predictedValue = valueOption(actionTaken, startState);
		double delta = observedResult + gamma * bestNextAction - predictedValue;
		for (int i = 0; i < gvLength; i++) {
			GeneticVariable var1 = variableSet.get(i);
			for (int j = i; j < gvLength; j++) {
				GeneticVariable var2 = variableSet.get(j);
				double value = startState[i] * startState[j];
				updateWeight(var1, var2, actionTaken, value * delta * alpha);
			}
		}
	}
}
