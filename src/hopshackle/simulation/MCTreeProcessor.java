package hopshackle.simulation;

import java.util.*;

import org.encog.neural.data.basic.*;

public class MCTreeProcessor<A extends Agent> {

	private int minVisits, maxNeurons;
	private boolean oneHot;
	private DeciderProperties properties;
	private List<double[]> inputData;
	private List<double[]> outputData;
	private int maximumOutputOptions;
	private List<ActionEnum<A>> actionsInOutputLayer = new ArrayList<ActionEnum<A>>();

	public MCTreeProcessor(DeciderProperties prop) {
		oneHot = prop.getProperty("MonteCarloOneHotRolloutTraining", "false").equals("true");
		maximumOutputOptions = prop.getPropertyAsInteger("NeuralMaxOutput", "100");
		minVisits = prop.getPropertyAsInteger("MonteCarloMinVisitsForRolloutTraining", "50");
		maxNeurons = prop.getPropertyAsInteger("NeuralMaxOutput", "100");
		properties = prop;
		inputData = new ArrayList<double[]>();
		outputData = new ArrayList<double[]>();
	}

	public void processTree(MonteCarloTree<A> tree, int refAgent) {
		for (String key : tree.getAllStatesWithMinVisits(minVisits)) {
			MCStatistics<A> stats = tree.getStatisticsFor(key);
			double[] input = convertStateAsStringToArray(key);
			inputData.add(input);
			double[] targetOutput = getOutputValuesAsArray(stats, refAgent);
			outputData.add(targetOutput);
		}
	}

	protected double[] convertStateAsStringToArray(String stringRepresentation) {
		String[] stepOne = stringRepresentation.split("\\|");
		double[] retValue = new double[stepOne.length];
		for (int i = 0; i < stepOne.length; i++) {
			double value = Integer.valueOf(stepOne[i]) / 100.0;
			retValue[i] = value;
		}
		return retValue;
	}

	protected double[] getOutputValuesAsArray(MCStatistics<A> stats, int refAgent) {
		List<ActionEnum<A>> actionsInStats = stats.getPossibleActions();
		ActionEnum<A> bestAction = stats.getBestAction(actionsInStats);	// this assumes refAgent is the same as stats.actingAgent
		double[] retValue = new double[actionsInOutputLayer.size()];
		if (oneHot) {	// set 1.0 for the best action according to the Tree, and 0.0 is left as the default for all others
			retValue = setValueForAction(bestAction, retValue, 1.0);
		} else {
			for (ActionEnum<A> action : actionsInStats) {
				double visits = stats.getVisits(action);
				double value = stats.getMean(action)[refAgent-1];
				if (visits == 0) value = 0.00;	// this will be true anyway. Added for emphasis.
				// what we really want to do in this case is signal to the NeuralDecider that the value should be overridden by the prediction
				// TODO: could be done using Double.NaN, and then modifying NeuralDecider
				retValue = setValueForAction(action, retValue, value);
			}
		}
		return retValue;
	}
	
	private double[] setValueForAction(ActionEnum<A> action, double[] initialArray, double value) {
		double[] retValue = initialArray;
		int index = actionsInOutputLayer.indexOf(action);
		if (index == -1 && actionsInOutputLayer.size() < maximumOutputOptions) {
			actionsInOutputLayer.add(action);
			double[] oldArray = retValue;
			retValue = new double[actionsInOutputLayer.size()];
			for (int i = 0; i < oldArray.length; i++) retValue[i] = oldArray[i];
			index = actionsInOutputLayer.size() - 1;
		} 
		retValue[index] = value;
		return retValue;
	}

	public NeuralDecider<A> generateDecider(StateFactory<A> stateFactory, double scaleFactor) {
		NeuralDecider<A> retValue = new NeuralDecider<A>(stateFactory, scaleFactor);
		retValue.injectProperties(properties);
		for (ActionEnum<A> action : actionsInOutputLayer) {
			retValue.addNewAction(action);
			// we inject the actions in the same order as the training data (or else all hell will break loose)
		}
		BasicNeuralDataSet trainingData = finalTrainingData();
		double error = retValue.teach(trainingData);
		System.out.println(String.format("Trained decider with %s states, %s options and error of %.4f", trainingData.size(), actionsInOutputLayer.size(), error));
		return retValue;
	}

	protected BasicNeuralDataSet finalTrainingData() {
		// the earlier training data will not have seen all possible actions, so the arrays will not extend all the
		BasicNeuralDataSet retValue = new BasicNeuralDataSet();
		for (int i = 0; i < inputData.size(); i++) {
			double[] currentOut = outputData.get(i);
			double[] input = inputData.get(i);
			if (currentOut.length < maxNeurons) {
				double[] newOut = new double[maxNeurons];	// will default to 0.0
				for (int j = 0; j < currentOut.length; j++)
					newOut[j] = currentOut[j];
				currentOut = newOut;
			}
			retValue.add(new BasicNeuralData(input), new BasicNeuralData(currentOut));
		}
		return retValue;
	}

}
