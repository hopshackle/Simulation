package hopshackle.simulation;

import java.util.*;

public class GeneralQuadraticQDecider<A extends Agent> extends GeneralLinearQDecider<A> {

	private int gvLength;

	public GeneralQuadraticQDecider(List<? extends ActionEnum<A>> actions, List<GeneticVariable> variables) {
		super(actions, variables);
		// Note that we then override everything that the super constructor does
		actionLength = actions.size();
		gvLength = variables.size();
		variableLength = gvLength * gvLength;
		weights = new double[actionLength][variableLength];
		// convention is that the first V entries are for all variables * first variable
		// then the V+1 to 2V for all variables * second variable etc....
		// With additional caveat that the 'diagonal' entries are the plain variables
		// So 1 is just the first variable, V+2 is just the second, 2V+3 the third ... and so on
		initialiseWeights();
	}

	@Override
	protected double[] getState(A decidingAgent, Agent contextAgent) {
		double[] base = super.getState(decidingAgent, contextAgent);
		return convertStateToQuadraticRepresentation(base);
	}
	
	private double[] convertStateToQuadraticRepresentation(double[] baseState) {
		double[] stateDescriptor = new double[variableLength];
		for (int i = 0; i < gvLength; i++) {
			double val1 = baseState[i];
			for (int j = i; j < gvLength; j++) {
				if (j == i) {
					stateDescriptor[i + j] = val1;
				} else {
					stateDescriptor[i + j] = val1 * baseState[j];
				}
			}
		}
		return stateDescriptor;
	}

	public void updateWeight(GeneticVariable var1, GeneticVariable var2, ActionEnum<A> option, double delta) {
		int optionIndex = actionSet.indexOf(option);
		int varIndex = variableSet.indexOf(var1) * gvLength + variableSet.indexOf(var2);
		weights[optionIndex][varIndex] += delta - weights[optionIndex][varIndex] * lambda;
	}

	public double getWeightOf(GeneticVariable input1, GeneticVariable input2, ActionEnum<A> option) {
		int optionIndex = actionSet.indexOf(option);
		int varIndex = variableSet.indexOf(input1) * gvLength + variableSet.indexOf(input2);
		return weights[optionIndex][varIndex];
	}
	
	@Override
	public void learnFrom(ExperienceRecord<A> exp, double maxResult) {
		double bestNextAction = valueOfBestAction(exp);
		ActionEnum<A> actionTaken = exp.getActionTaken().actionType;
		double[] startState = convertStateToQuadraticRepresentation(exp.getStartState());
		double[] endState = convertStateToQuadraticRepresentation(exp.getEndState());
		double observedResult = exp.getReward();
		double predictedValue = valueOption(actionTaken, startState);
		double delta = observedResult + gamma * bestNextAction - predictedValue;
		if (localDebug) {
			String message = String.format("Learning:\t%-15sReward: %.2f, NextValue: %.2f, Predicted: %.2f, Delta: %.4f, NextAction: %s", 
					exp.getActionTaken(), exp.getReward(), bestNextAction, predictedValue, delta, actionTaken == null ? "NULL" : actionTaken.toString());
			log(message);
			StringBuffer logMessage = new StringBuffer("StartState -> EndState :" + newline);
			for (int i = 0; i < startState.length; i++) {
				if (startState[i] != 0.0 || endState[i] != 0.0) {
					int firstVarComponent = i / gvLength;
					int secondVarComponent = i % gvLength;
					String variableName = variableSet.get(firstVarComponent).toString() + ":" + variableSet.get(secondVarComponent);
					if (firstVarComponent == secondVarComponent)
						variableName = variableSet.get(firstVarComponent).toString();
					logMessage.append(String.format("\t%.2f -> %.2f %s %s", startState[i], endState[i], variableName, newline));
				}
			}
			message = logMessage.toString();
			log(message);
		}
		for (int i = 0; i < variableLength; i++) {
			double value = startState[i];
			if (value == 0.0) continue;
			int firstVarComponent = i / gvLength;
			int secondVarComponent = i % gvLength;
			GeneticVariable var1 = variableSet.get(firstVarComponent);
			GeneticVariable var2 = variableSet.get(secondVarComponent);
			String variableName = variableSet.get(firstVarComponent).toString() + ":" + variableSet.get(secondVarComponent);
			if (firstVarComponent == secondVarComponent)
				variableName = variableSet.get(firstVarComponent).toString();
			double weightChange = value * delta * alpha;
			if (localDebug) {
				String message = String.format("\t\t%-15s Value: %.2f, WeightChange: %.4f, Current Weight: %.2f", variableName, value, weightChange, 
					getWeightOf(var1, var2, exp.getActionTaken().actionType));
				log(message);
			}
			updateWeight(var1, var2, exp.getActionTaken().actionType, weightChange);
		}
	}
}
