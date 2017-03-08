package hopshackle.simulation;

import java.util.*;

public class GeneralLinearQDecider<A extends Agent> extends QDecider<A> {

	/*
	 * We need to have a Map to record what actions we have seen so far
	 * For each action we then maintain a set of weights (initialised at 0.0)
	 * 
	 */
	protected Map<String, double[]> weights = new HashMap<String, double[]>();
	protected int variableLength;
	protected int actionLength = 0;
	public static String newline = System.getProperty("line.separator");

	public GeneralLinearQDecider(StateFactory<A> stateFactory, List<ActionEnum<A>> actions) {
		super(stateFactory);
		variableLength = stateFactory.getVariables().size();
		for (ActionEnum<A> action : actions) {
			getWeightsFor(action);
		}
	}

	protected double[] getWeightsFor(ActionEnum<A> action) {
		String actionKey = action.toString();
		if (weights.containsKey(actionKey)) return weights.get(actionKey);
		double[] w = new double[variableLength+1];
		for (int j = 0; j < variableLength; j++) w[j] = 1.0;	
		weights.put(actionKey, w);
		actionLength++;
		return w;
	}

	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent) {
		State<A> currentState = stateFactory.getCurrentState(decidingAgent);
		return valueOption(option, currentState);
	}

	@Override
	public double valueOption(ActionEnum<A> option, State<A> state) {
		double[] w = getWeightsFor(option);
		double retValue = 0.0;

		double[] stateArray = state.getAsArray(); 
		for (int i = 0; i < variableLength; i++) {
			retValue += w[i] * stateArray[i];
		}
		return retValue;
	}

	public void updateWeight(int varIndex, ActionEnum<A> option, double delta) {
		double[] w = getWeightsFor(option);
		w[varIndex] += delta - w[varIndex] * lambda;
	}

	protected void setWeights(ActionEnum<A> option, double[] w) {
		// for testing only
		weights.put(option.toString(), w);
	}

	public double getWeightOf(int varIndex, ActionEnum<A> option) {
		double[] w = getWeightsFor(option);
		return w[varIndex];
	}

	public double getLargestWeight() {
		double retValue = 0.0;
		for (double[] w : weights.values()) {
			for (int j = 0; j < variableLength; j++) {
				if (Math.abs(w[j]) > retValue) 
					retValue = Math.abs(w[j]);
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
		double delta = exp.getReward()[0] + Math.pow(gamma, discountPeriod) * bestNextAction - predictedValue;
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
