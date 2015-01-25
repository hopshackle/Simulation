package hopshackle.simulation;

import java.util.*;

public class StateSoftMaxDecider extends StateDecider {


	public StateSoftMaxDecider(ArrayList<ActionEnum> actions, ArrayList<GeneticVariable> variables) {
		super(actions, variables);
	}

	@Override
	public ActionEnum decide(Agent decidingAgent, Agent contextAgent) {
		List<ActionEnum> possibleActions = getChooseableOptions(decidingAgent, contextAgent);
		double[] optionWeightings = new double[possibleActions.size()];
		double sumOfActionValues = 0.0;
		double baseValue = 0.0;
		ActionEnum chosenAction = null;

		double temperature = SimProperties.getPropertyAsDouble("Temperature", "1.0");
		if (temperature < 0.01) temperature = 0.01;
		
		ActionEnum highValueAction = decideWithoutLearning(decidingAgent, contextAgent);
		baseValue = valueOption(highValueAction, decidingAgent, contextAgent) / 3.0;
		// so that powers of e are about 3 in Boltzmann distribution

		int index = 0;
		for (ActionEnum option : possibleActions) {
			double valueOfThisOption = Math.exp(valueOption(option, decidingAgent, contextAgent) / baseValue / temperature);
			sumOfActionValues += valueOfThisOption;
			optionWeightings[index] = valueOfThisOption;
			index++;
		}

		double randomNumber = Math.random() * sumOfActionValues;
		double runningSum = 0.0;
		for (int loop = 0; loop < possibleActions.size(); loop++) {
			runningSum += optionWeightings[loop];
			if (runningSum > randomNumber) {
				chosenAction = possibleActions.get(loop);
				break;
			}
		}
		if (chosenAction == null) {
			logger.warning("No Action chosen in StateSoftMaxDecider for " + decidingAgent);
			chosenAction = highValueAction;
		}

		learnFromDecision(decidingAgent, contextAgent, chosenAction);

		return chosenAction;
	}

	@Override
	public double valueOption(ActionEnum option, Agent decidingAgent, Agent contextAgent) {
		// this has no noise - the noise is applied in the softmax action determination
		double retValue = 0.0;
		HopshackleState agentState = getState(decidingAgent, contextAgent);
		retValue = agentState.valueOption(option, 0.0);
		return retValue;
	}

}
