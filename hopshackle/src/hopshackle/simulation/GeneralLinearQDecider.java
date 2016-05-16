package hopshackle.simulation;

import java.util.*;

public class GeneralLinearQDecider extends QDecider {

	protected double[][] weights;
	protected int actionLength, variableLength;

	public GeneralLinearQDecider(List<? extends ActionEnum> actions, List<GeneticVariable> variables) {
		super(actions, variables);
		actionLength = actions.size();
		variableLength = variables.size();
		weights = new double[actionLength][variableLength];
		for (int i=0; i<actionLength; i++)
			for (int j=0; j<variableLength; j++) 
				weights[i][j] = 1.0;		// initialise to 1.0 to promote optimistic exploration
	}

	@Override
	public double valueOption(ActionEnum option, Agent decidingAgent, Agent contextAgent) {
		double[] stateDescriptor = new double[variableLength];
		for (int i = 0; i < variableLength; i++) {
			stateDescriptor[i] = variableSet.get(i).getValue(decidingAgent, contextAgent);
		}
		return valueOption(option, stateDescriptor);
	}

	@Override
	public double valueOption(ActionEnum option, double[] state) {
		int optionIndex = actionSet.indexOf(option);
		assert (optionIndex != -1) : option + " not found in actionSet in GLQDecider.valueOption";
		double retValue = 0.0;

		for (int i = 0; i < variableLength; i++) {
			//		retValue += weights[0][i] * state[i];
			retValue += weights[optionIndex][i] * state[i];
		}
		return retValue;
	}

	public void updateWeight(GeneticVariable input, ActionEnum option, double delta) {
		int optionIndex = actionSet.indexOf(option);
		int varIndex = variableSet.indexOf(input);
		//		weights[0][varIndex] += delta - weights[0][varIndex] * lambda;
		weights[optionIndex][varIndex] += delta - weights[optionIndex][varIndex] * lambda;
	}
	
	protected void setWeights(double[][] newWeights) {
		// for testing only
		weights = newWeights;
	}

	public double getWeightOf(GeneticVariable input, ActionEnum option) {
		int optionIndex = actionSet.indexOf(option);
		int varIndex = variableSet.indexOf(input);
		return weights[optionIndex][varIndex];
	}

	public double getLargestWeight() {
		double retValue = 0.0;
		for (int i = 0; i < actionLength; i++) {
			for (int j = 0; j < variableLength; j++) {
				if (Math.abs(weights[i][j]) > retValue) 
					retValue = Math.abs(weights[i][j]);
			}
		}
		return retValue;
	}

	@Override
	public void learnFrom(ExperienceRecord exp, double maxResult) {
		double bestNextAction = valueOfBestAction(exp);
		ActionEnum nextAction = getBestActionFrom(exp.getPossibleActionsFromEndState(), exp.getEndState());
		double[] startState = exp.getStartState();
		double predictedValue = valueOption(exp.getActionTaken(), startState);
		double delta = exp.getReward() + gamma * bestNextAction - predictedValue;
		if (localDebug) {
			log(String.format("Learning:\t%-15sReward: %.2f, NextValue: %.2f, Predicted: %.2f, Delta: %.4f, NextAction: %s", 
					exp.getActionTaken(), exp.getReward(), bestNextAction, predictedValue, delta, nextAction.toString()));
			StringBuffer logMessage = new StringBuffer("Start state: ");
			double[] state = exp.getStartState();
			for (int i = 0; i < state.length; i++) 
				logMessage.append(String.format(" [%.2f] ", state[i]));
			log(logMessage.toString());
			logMessage = new StringBuffer("End state:   ");
			state = exp.getEndState();
			for (int i = 0; i < state.length; i++) 
				logMessage.append(String.format(" [%.2f] ", state[i]));
			log(logMessage.toString());
		}
		for (int i = 0; i < variableSet.size(); i++) {
			double value = startState[i];
			GeneticVariable input = variableSet.get(i);
			double weightChange = value * delta * alpha;
			if (localDebug) log(String.format("\t\t%-15s Value: %.2f, WeightChange: %.4f, Current Weight: %.2f", input.toString(), value, weightChange, 
					getWeightOf(input, exp.getActionTaken())));
			updateWeight(input, exp.getActionTaken(), weightChange);
		}
	}
}