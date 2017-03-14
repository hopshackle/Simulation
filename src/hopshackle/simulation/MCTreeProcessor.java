package hopshackle.simulation;

import java.util.*;

import org.encog.neural.data.basic.*;

public class MCTreeProcessor<A extends Agent> {

	private int minVisits, maxNeurons;
	private DeciderProperties properties;
	private List<double[]> inputData;
	private List<double[]> outputData;
	private List<ActionEnum<A>> actionsInOutputLayer = new ArrayList<ActionEnum<A>>();

	public MCTreeProcessor(DeciderProperties prop) {
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
		double[] retValue = new double[actionsInStats.size()];
		for (ActionEnum<A> action : actionsInStats) {
			double visits = stats.getVisits(action);
			double value = 	stats.getMean(action)[refAgent-1];
			if (visits == 0) value = 0.00;	// this will be true anyway. Added for emphasis.
				// what we really want to do in this case is signal to the NeuralDecider that the value should be overridden by the prediction
				// TODO: could be done using Double.NaN, and then modifying NeuralDecider
			int index = actionsInOutputLayer.indexOf(action);
			if (index == -1) {
				actionsInOutputLayer.add(action);
				index = actionsInOutputLayer.size() - 1;
			} 
			retValue[index] = value;
		}
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
		System.out.println("Training decider with " + trainingData.size() + " states");
		retValue.teach(trainingData);
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
