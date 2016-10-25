package hopshackle.simulation;

import java.util.*;

public class GeneralLinearQDecider<A extends Agent> extends QDecider<A> {

	protected double[][] weights;
	protected int variableLength;
	protected int actionLength;
	public static String newline = System.getProperty("line.separator");

	public GeneralLinearQDecider(StateFactory<A> stateFactory, List<? extends ActionEnum<A>> actions) {
		super(stateFactory, actions);
		actionLength = actionSet.size();
		variableLength = stateFactory.getVariables().size();
		weights = new double[actionLength][variableLength];
		initialiseWeights();
	}

	protected void initialiseWeights() {
		for (int i=0; i<actionLength; i++) {
			weights[i][0] = 1.0;	// the constant term. To slightly encourage exploration
			for (int j=1; j<variableLength; j++) 
				weights[i][j] = 0.0;	
		}
	}

	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent) {
		State<A> currentState = stateFactory.getCurrentState(decidingAgent);
		return valueOption(option, currentState);
	}

	@Override
	public double valueOption(ActionEnum<A> option, State<A> state) {
		int optionIndex = actionSet.indexOf(option);
		if (optionIndex == -1) logger.severe(option + " not found in actionSet in GLQDecider.valueOption");
		double retValue = 0.0;

		double[] stateArray = state.getAsArray(); 
		for (int i = 0; i < variableLength; i++) {
			retValue += weights[optionIndex][i] * stateArray[i];
		}
		return retValue;
	}

	public void updateWeight(int varIndex, ActionEnum<A> option, double delta) {
		int optionIndex = actionSet.indexOf(option);
		weights[optionIndex][varIndex] += delta - weights[optionIndex][varIndex] * lambda;
	}
	
	protected void setWeights(double[][] newWeights) {
		// for testing only
		weights = newWeights;
	}

	public double getWeightOf(int varIndex, ActionEnum<A> option) {
		int optionIndex = actionSet.indexOf(option);
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
	public void learnFrom(ExperienceRecord<A> exp, double maxResult) {
		double bestNextAction = valueOfBestAction(exp);
		ActionEnum<A> nextAction = getBestActionFrom(exp.getPossibleActionsFromEndState(), exp.getEndState());
		double[] startState = exp.getStartStateAsArray();
		double[] endState = exp.getEndStateAsArray();
		double[] featureTrace = exp.getFeatureTrace();
		double predictedValue = valueOption(exp.getActionTaken().actionType, exp.getStartState());
		double discountPeriod = exp.getDiscountPeriod();
		double delta = exp.getReward() + Math.pow(gamma, discountPeriod) * bestNextAction - predictedValue;
		if (localDebug) {
			String message = String.format("Learning:\t%-15sReward: %.2f, NextValue: %.2f, Predicted: %.2f, Delta: %.4f, NextAction: %s", 
					exp.getActionTaken(), exp.getReward(), bestNextAction, predictedValue, delta, nextAction == null ? "NULL" : nextAction.toString());
			log(message);
			exp.getAgent().log(message);
			StringBuffer logMessage = new StringBuffer("StartState -> EndState (FeatureTrace) :" + newline);
			for (int i = 0; i < startState.length; i++) {
				if (startState[i] != 0.0 || endState[i] != 0.0 || Math.abs(featureTrace[i]) >= 0.01)
					logMessage.append(String.format("\t%.2f -> %.2f (%.2f) %s %s", startState[i], endState[i], featureTrace[i], stateFactory.getVariables().get(i).toString(), newline));
			}
			message = logMessage.toString();
			log(message);
			exp.getAgent().log(message);
		}
		for (int i = 0; i < variableLength; i++) {
			double value = featureTrace[i];
			if (value == 0.0) continue;
			double weightChange = value * delta * alpha;
			if (localDebug) {
				GeneticVariable<A> input = stateFactory.getVariables().get(i);
				String message = String.format("\t\t%-15s Value: %.2f, WeightChange: %.4f, Current Weight: %.2f", input.toString(), value, weightChange, 
					getWeightOf(i, exp.getActionTaken().actionType));
				log(message);
				exp.getAgent().log(message);
			}
			updateWeight(i, exp.getActionTaken().actionType, weightChange);
		}
	}
}