package hopshackle.simulation;

import java.util.*;

public class GeneralLinearQDecider<A extends Agent> extends BaseStateDecider<A> implements RawDecider<A> {

	/*
	 * We need to have a Map to record what actions we have seen so far
	 * For each action we then maintain a set of weights (initialised at 0.0)
	 * 
	 */
	protected Map<String, double[]> weights = new HashMap<String, double[]>();
	protected int variableLength;
	protected int actionLength = 0;
	public static String newline = System.getProperty("line.separator");

	public GeneralLinearQDecider(StateFactory<A> stateFactory) {
		super(stateFactory);
		variableLength = stateFactory.getVariables().size();
	}

	protected double[] getWeightsFor(ActionEnum<A> action) {
		String actionKey = action.toString();
		return getWeightsFor(actionKey);
	}

	protected double[] getWeightsFor(String actionAsString) {
		if (weights.containsKey(actionAsString)) return weights.get(actionAsString);
		double[] w = new double[variableLength+1];
		for (int j = 0; j < variableLength; j++) w[j] = 1.0;
		weights.put(actionAsString, w);
		actionLength++;
		return w;
	}

	@Override
	public double valueOption(ActionEnum<A> option, State<A> state) {
		double[] w = getWeightsFor(option);
		double[] stateArray = state.getAsArray(); 
		return dotProduct(w, stateArray);
	}
	
	private double dotProduct(double[] weights, double[] features) {
		double retValue = 0.0;
		for (int i = 0; i < features.length; i++) {
			retValue += weights[i] * features[i];
		}
		return retValue;
	}
	
	@Override
	public List<Double> valueOptions(List<ActionEnum<A>> options, State<A> state) {
		List<Double> retValue = new ArrayList<Double>(options.size());
		for (int i = 0; i < options.size(); i++) retValue.add(0.0); 
		double[] stateArray = state.getAsArray(); 
		for (int i = 0; i < options.size(); i++) {
			ActionEnum<A> option = options.get(i);
			double[] w = getWeightsFor(option);
			retValue.set(i, dotProduct(w, stateArray));
		}
		return retValue;
	}

	@Override
	public double[] valueOptions(double[] stateAsArray) {
		double[] retValue = new double[weights.size()];
		int count = 0;
		for (String option : weights.keySet()) {
			double[] w = getWeightsFor(option);
			retValue[count] = dotProduct(w, stateAsArray);
			count++;
		}
		return retValue;
	}

	public void updateWeight(int varIndex, ActionEnum<A> option, double delta) {
		double[] w = getWeightsFor(option);
		w[varIndex] += delta - w[varIndex] * lambda;
	}

	public void setWeights(String option, double[] w) {
		// for testing only
		weights.put(option, w);
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
		double[] startState = exp.getStartStateAsArray(useLookahead);
		double[] endState = exp.getEndStateAsArray();
		double[] featureTrace = exp.getFeatureTrace();
		double predictedValue = valueOption(exp.getActionTaken().getType(), exp.getStartState(useLookahead));
		double discountPeriod = exp.getDiscountPeriod();
		int actingAgentNumber = exp.getAgentNumber();
		double reward = exp.getMonteCarloReward()[actingAgentNumber];
		double delta = Math.pow(gamma, discountPeriod) * reward - predictedValue;
		double bestNextAction = -1.0;
		if (!monteCarlo) {
			bestNextAction = valueOfBestAction(exp);
			reward = exp.getReward()[actingAgentNumber];
			delta = reward + Math.pow(gamma, discountPeriod) * bestNextAction - predictedValue;
		}
		if (localDebug) {
			ActionEnum<A> nextAction = getBestActionFrom(exp.getPossibleActionsFromEndState(), exp.getEndState());
			String message = String.format("Learning:\t%-15sReward: %.2f, NextValue: %.2f, Predicted: %.2f, Delta: %.4f, NextAction: %s", 
					exp.getActionTaken(), reward, bestNextAction, predictedValue, delta, nextAction == null ? "NULL" : nextAction.toString());
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
						getWeightOf(i, exp.getActionTaken().getType()));
				log(message);
				exp.getAgent().log(message);
			}
			updateWeight(i, exp.getActionTaken().getType(), weightChange);
		}
	}
}
