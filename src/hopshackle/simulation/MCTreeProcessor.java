package hopshackle.simulation;

import hopshackle.utilities.*;

import java.util.*;

import org.encog.ml.data.MLDataPair;
import org.encog.neural.data.basic.*;

public class MCTreeProcessor<A extends Agent> {

    private int minVisits, maxNeurons;
    private boolean oneHot, controlSignal, logisticModel, linearModel, normalise;
    private boolean debug = true;
    private DeciderProperties properties;
    private LinkedList<double[]> inputData;
    private LinkedList<double[]> outputData;
    private List<String> actionsInOutputLayer = new ArrayList<String>();
    private int windowSize = 1000;
    private String name;
    //	private Map<ActionEnum<A>, Integer> actionCount = new HashMap<ActionEnum<A>, Integer>();

    public MCTreeProcessor(DeciderProperties prop, String nameForDeciders) {
        oneHot = prop.getProperty("MonteCarloRolloutTarget", "basic").equals("oneHot");
        normalise = prop.getProperty("MonteCarloRolloutTarget", "basic").equals("normalise");
        controlSignal = prop.getProperty("NeuralControlSignal", "false").equals("true");
        logisticModel = prop.getProperty("MonteCarloRolloutModel", "neural").equals("logistic");
        linearModel = prop.getProperty("MonteCarloRolloutModel", "neural").equals("linear");
        minVisits = prop.getPropertyAsInteger("MonteCarloMinVisitsForRolloutTraining", "50");
        maxNeurons = prop.getPropertyAsInteger("NeuralMaxOutput", "100");
        windowSize = prop.getPropertyAsInteger("MonteCarloRolloutTrainingWindow", "1000");
        properties = prop;
        inputData = new LinkedList<double[]>();
        outputData = new LinkedList<double[]>();
        name = nameForDeciders + "_MTP";
    }

    public void processTree(MonteCarloTree<A> tree) {
        for (String key : tree.getAllStatesWithMinVisits(minVisits)) {
            MCStatistics<A> stats = tree.getStatisticsFor(key);
            if (stats.getPossibleActions().size() < 2) continue;
            double[] input = convertStateAsStringToArray(key);
            inputData.add(input);
            double[] targetOutput = getOutputValuesAsArray(stats);
            outputData.add(targetOutput);
        }
        int currentSize = inputData.size();
        if (currentSize > windowSize) {
            do {
                inputData.remove();
                outputData.remove();
            } while (inputData.size() > windowSize);
        }
        if (inputData.size() != outputData.size())
            throw new AssertionError("Input and Output queues must be same size");
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

    protected double[] getOutputValuesAsArray(MCStatistics<A> stats) {
        List<ActionEnum<A>> actionsInStats = stats.getPossibleActions();
        int actingAgent = stats.getActorRef();
        ActionEnum<A> bestAction = stats.getBestAction(actionsInStats, actingAgent);
        double[] retValue = new double[actionsInOutputLayer.size()];
        for (int i = 0; i < retValue.length; i++) retValue[i] = Double.NaN;
        for (ActionEnum<A> action : actionsInStats) {
            double visits = stats.getVisits(action);
            if (visits == 0) continue;
            double value = stats.getMean(action)[actingAgent];
            if (oneHot && action.equals(bestAction)) value = 1.0;
            if (oneHot && !action.equals(bestAction)) value = 0.0;
            retValue = setValueForAction(action, retValue, value);
        }

        if (normalise) {
            retValue = Normalise.range(retValue, 0, 1);
        }
        return retValue;
    }

    private double[] setValueForAction(ActionEnum<A> action, double[] initialArray, double value) {
        String actionAsString = action.toString();
        if (action.toString().trim().equals(""))
            throw new AssertionError("no name");
        double[] retValue = initialArray;
        int index = actionsInOutputLayer.indexOf(actionAsString);
        if (index == -1 && actionsInOutputLayer.size() < maxNeurons) {
            actionsInOutputLayer.add(actionAsString);
            double[] oldArray = retValue;
            retValue = new double[actionsInOutputLayer.size()];
            for (int i = 0; i < oldArray.length; i++) retValue[i] = oldArray[i];
            for (int i = oldArray.length; i < retValue.length; i++) retValue[i] = Double.NaN;
            index = actionsInOutputLayer.size() - 1;
        }
        if (index > -1) retValue[index] = value;
        return retValue;
    }

    public Decider<A> generateDecider(StateFactory<A> stateFactory, double scaleFactor, double temp, RawDecider<A> baselineDecider) {
        properties.setProperty("Temperature", String.valueOf(temp));
        Decider<A> retValue;
        StringBuffer message = new StringBuffer();
        if (debug) {
            message.append(String.format("%d total states, T = %.3f\n", outputData.size(), properties.getPropertyAsDouble("Temperature", "0.0")));
            int[] actionCounts = getValidActionCountsFromData();
            int[] chosenCounts = getTopActionCountsFromData();
            if (actionCounts.length != chosenCounts.length) throw new AssertionError("Should be same length");
            int threshold = outputData.size() / 500;
            for (int i = 0; i < actionsInOutputLayer.size(); i++) {
                if (chosenCounts[i] >= threshold) {
                    message.append(String.format("%-20s %d of %d (%.0f%%)\n", actionsInOutputLayer.get(i), chosenCounts[i], actionCounts[i], (chosenCounts[i] * 100.0) / (double) actionCounts[i]));
                }
            }
        }
        BasicNeuralDataSet trainingData = finalTrainingData(baselineDecider);
        if (linearModel) {
            GeneralLinearQDecider<A> ld = new GeneralLinearQDecider<A>(stateFactory);
            ld.injectProperties(properties);
            int count = 0;
            double totalError = 0.0;
            for (String action : actionsInOutputLayer) {
                LinearRegression regressor = LinearRegression.createFrom(trainingData, count);
                ld.setWeights(action, regressor.getWeights());
                count++;
                totalError += regressor.getError();
            }
            System.out.println(String.format("Trained " + name + " linear decider with %s states, %s options, and error %.4f",
                    trainingData.size(), actionsInOutputLayer.size(), totalError / (double) trainingData.getIdealSize()));
            retValue = ld;
        } else if (logisticModel) {
            LogisticDecider<A> ld = new LogisticDecider<A>(stateFactory);
            ld.injectProperties(properties);
            int count = 0;
            for (String action : actionsInOutputLayer) {
                LogisticRegression regressor = new LogisticRegression();
                regressor.train(trainingData, count);
                ld.addRegressor(action, regressor);
                count++;
                // we inject the actions in the same order as the training data (or else all hell will break loose)
            }
            double totalError = 0.0;
            for (MLDataPair d : trainingData.getData()) {
                double[] input = d.getInputArray();
                double[] target = d.getIdealArray();
                double[] estimate = new double[d.getIdealArray().length];
                for (int i = 0; i < actionsInOutputLayer.size(); i++)
                    estimate[i] = ld.getRegressor(actionsInOutputLayer.get(i)).classify(input);
                double error = 0.0;
                for (int i = 0; i < target.length; i++) {
                    error += Math.pow((target[i] - estimate[i]), 2);
                }
                totalError = totalError + error / (double) target.length;
            }
            totalError = totalError / (double) trainingData.getRecordCount();
            System.out.println(String.format("Trained " + name + " logistic decider with %s states, %s options, and error %.4f",
                    trainingData.size(), actionsInOutputLayer.size(), totalError));
            retValue = ld;
        } else {
            NeuralDecider<A> nd = new NeuralDecider<A>(stateFactory, scaleFactor);
            nd.injectProperties(properties);
            for (String action : actionsInOutputLayer) {
                nd.addNewAction(action);
                // we inject the actions in the same order as the training data (or else all hell will break loose)
            }
            // ok...the problem here is that we are starting from a completely empty decider. This means that
            // OffPolicy learning won't work terribly well...and we will be setting random targets on options
            // that were not picked, instead of using the values from the previous iteration.
            // Hence, here we really want to provide another decider to be used for both of these tasks, in order
            // to generate the training data...which is then used to train a de novo decider as now.

            double error = nd.teach(trainingData);
            System.out.println(String.format("Trained " + name + " neural decider with %s states, %s options and error of %.4f", trainingData.size(), actionsInOutputLayer.size(), error));
            retValue = nd;
        }
        retValue.setName(name);
        if (debug) {
            retValue.log(message.toString());
            retValue.flushLog();
        }
        return retValue;
    }

    protected BasicNeuralDataSet finalTrainingData(RawDecider<A> baselineDecider) {
        if (controlSignal) return finalTrainingDataWithControlSignal();
        BasicNeuralDataSet retValue = new BasicNeuralDataSet();
        for (int i = 0; i < inputData.size(); i++) {
            double[] currentIn = inputData.get(i);
            double[] baseline = new double[maxNeurons];
            if (baselineDecider != null) {
                baseline = baselineDecider.valueOptions(currentIn);
            }
            double[] currentOut = outputData.get(i).clone();
            for (int k = 0; k < currentOut.length; k++)
                if (Double.isNaN(currentOut[k]))
                    currentOut[k] = baseline[k];
            if (currentOut.length < baseline.length) {
                for (int j = 0; j < currentOut.length; j++)
                    baseline[j] = currentOut[j];
                currentOut = baseline;
            }
            retValue.add(new BasicNeuralData(currentIn), new BasicNeuralData(currentOut));
        }
        return retValue;
    }

    public int[] getValidActionCountsFromData() {
        int[] retValue = new int[maxNeurons];
        for (int i = 0; i < outputData.size(); i++) {
            double[] output = outputData.get(i);
            for (int j = 0; j < output.length; j++) {
                if (!Double.isNaN(output[j]))
                    retValue[j]++;
            }
        }
        return retValue;
    }

    public int[] getTopActionCountsFromData() {
        int[] retValue = new int[maxNeurons];
        for (int i = 0; i < outputData.size(); i++) {
            double[] output = outputData.get(i);
            for (int j = 0; j < output.length; j++) {
                if (!Double.isNaN(output[j]) && output[j] > 0.5)
                    retValue[j]++;
            }
        }
        return retValue;
    }
    /*
	public double[] getRarityOfValidActionsFromData() {
		// and then we can amend the frequency of records 
		int[] actionCounts = getValidActionCountsFromData();
		double totalRecordsToUseInTraining = 10000;
		double actualRecords = outputData.size();
		double[] rarityOfOutputOptions = new double[(int) actualRecords];
		for (int i = 0; i < actualRecords; i++) {
			double[] output = outputData.get(i);
			for (int j = 0; j < output.length; j++) {
				if (!Double.isNaN(output[j]))
					rarityOfOutputOptions[i] -= Math.log(actionCounts[j]);
			}
		}
		double maxValue = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < actualRecords; i++) {
			if (rarityOfOutputOptions[i] > maxValue) maxValue = rarityOfOutputOptions[i];
		}
		for (int i = 0; i < actualRecords; i++) {
			if (rarityOfOutputOptions[i] < 0.0) {
				rarityOfOutputOptions[i] = Math.exp(rarityOfOutputOptions[i] - maxValue);
			} else {
				throw new AssertionError("Should not be possible for rarity to be zero");
			}
		}
		return Normalise.asProbabilityDistribution(rarityOfOutputOptions);
	}
	 */

    private BasicNeuralDataSet finalTrainingDataWithControlSignal() {
        BasicNeuralDataSet retValue = new BasicNeuralDataSet();
        for (int i = 0; i < inputData.size(); i++) {
            double[] startInput = inputData.get(i);
            double[] startOutput = outputData.get(i);
            for (int a = 0; a < startOutput.length; a++) {
                if (Double.isNaN(startOutput[a])) continue;
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
