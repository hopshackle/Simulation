package hopshackle.simulation;

import java.util.*;

import org.encog.neural.data.basic.*;

public class MCTreeProcessor<A extends Agent> {

	private int minVisits, maxNeurons;
	private boolean oneHot, controlSignal;
	private DeciderProperties properties;
	private List<double[]> inputData;
	private List<double[]> outputData;
	private List<ActionEnum<A>> actionsInOutputLayer = new ArrayList<ActionEnum<A>>();
//	private Map<ActionEnum<A>, Integer> actionCount = new HashMap<ActionEnum<A>, Integer>();

	public MCTreeProcessor(DeciderProperties prop) {
		oneHot = prop.getProperty("MonteCarloOneHotRolloutTraining", "false").equals("true");
		controlSignal = prop.getProperty("NeuralControlSignal", "false").equals("true");
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
//		actionCount.put(bestAction, actionCount.getOrDefault(bestAction, 0));
		double[] retValue = new double[actionsInOutputLayer.size()];
		if (controlSignal)
			for (int i = 0; i < retValue.length; i++) retValue[i] = Double.NaN;
		for (ActionEnum<A> action : actionsInStats) {
			double visits = stats.getVisits(action);
			if (visits == 0) continue;
			double value = stats.getMean(action)[refAgent-1];
			if (oneHot && action.equals(bestAction)) value = 1.0;
			if (oneHot && !action.equals(bestAction)) value = 0.0;
			retValue = setValueForAction(action, retValue, value);
		}
		if (controlSignal) {
			List<Integer> indicesToRemove = new ArrayList<Integer>();
			for (int i = 0; i < retValue.length; i++) {
				if (Double.isNaN(retValue[i])) {
					indicesToRemove.add(i);
				}
			}
			double[] newRetValue = new double[retValue.length - indicesToRemove.size()];
			int removed = 0;
			for (int i = 0; i < retValue.length; i++) {
				if (indicesToRemove.contains(i)) {
					removed++;
				} else {
					newRetValue[i-removed] = retValue[i];
				}
			}
			retValue = newRetValue;
		}
		return retValue;
	}

	private double[] setValueForAction(ActionEnum<A> action, double[] initialArray, double value) {
		double[] retValue = initialArray;
		int index = actionsInOutputLayer.indexOf(action);
		if (index == -1 && actionsInOutputLayer.size() < maxNeurons) {
			actionsInOutputLayer.add(action);
			double[] oldArray = retValue;
			retValue = new double[actionsInOutputLayer.size()];
			for (int i = 0; i < oldArray.length; i++) retValue[i] = oldArray[i];
			index = actionsInOutputLayer.size() - 1;
		} 
		if (index > -1) retValue[index] = value;
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
		// the earlier training data will not have seen all possible actions, so the arrays will not extend to cover the full requirements of the NN
		if (controlSignal) return finalTrainingDataWithControlSignal();
		BasicNeuralDataSet retValue = new BasicNeuralDataSet();
		for (int i = 0; i < inputData.size(); i++) {
			double[] currentOut = outputData.get(i);
			double[] currentIn = inputData.get(i);
			if (currentOut.length < maxNeurons) {
				double[] newOut = new double[maxNeurons];	// will default to 0.0
				for (int j = 0; j < currentOut.length; j++)
					newOut[j] = currentOut[j];
				currentOut = newOut;
			}
			retValue.add(new BasicNeuralData(currentIn), new BasicNeuralData(currentOut));
		}
		return retValue;
	}

	private BasicNeuralDataSet finalTrainingDataWithControlSignal() {
		BasicNeuralDataSet retValue = new BasicNeuralDataSet();
		for (int i = 0; i < inputData.size(); i++) {
			double[] startInput = inputData.get(i);
			double[] startOutput = outputData.get(i);
			for (int a = 0; a < startOutput.length; a++) {
				double[] finalInput = new double[startInput.length + maxNeurons];
				for (int j = 0; j < startInput.length; j++) finalInput[j] = startInput[j];
				double[] finalOutput = new double[1];
				// make this the single output, and set the control signal in the input
				finalOutput[0] = startOutput[a];
				finalInput[a + startInput.length] = 1.0;
				retValue.add(new BasicNeuralData(finalInput), new BasicNeuralData(finalOutput));
			}
		}
		return retValue;
	}
}
