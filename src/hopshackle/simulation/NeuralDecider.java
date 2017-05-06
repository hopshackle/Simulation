package hopshackle.simulation;

import java.io.*;
import java.util.*;

import org.encog.ml.data.MLDataPair;
import org.encog.neural.data.basic.*;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.structure.NeuralStructure;
import org.encog.neural.networks.training.propagation.Propagation;
import org.encog.neural.networks.training.propagation.back.Backpropagation;
import org.encog.neural.networks.training.propagation.quick.QuickPropagation;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;

public class NeuralDecider<A extends Agent> extends QDecider<A> {

	protected BasicNetwork brain;
	protected Map<String, Integer> outputKey = new HashMap<String, Integer>();
	protected int maxActionIndex = -1;
	protected static double temperature;
	protected boolean monteCarlo = getProperty("MonteCarloReward", "false").equals("true");
	protected double baseMomentum = getPropertyAsDouble("NeuralLearningMomentum", "0.0");
	protected String propagationType = getProperty("NeuralPropagationType", "back");
	protected boolean applyTemperatureToLearning = getProperty("NeuralAnnealLearning", "false").equals("true");
	protected int learningIterations = Integer.valueOf(getProperty("NeuralLearningIterations", "1"));
	protected boolean learnWithValidation = getProperty("NeuralLearnUntilValidationError", "false").equals("true");
	protected boolean shuffleTrainingData = getProperty("NeuralShuffleData", "true").equals("true");
	protected boolean logTrainingErrors = getProperty("NeuralLogTrainingErrors", "false").equals("true");
	protected double overrideLearningCoefficient, overrideMomentum, scaleFactor;
	private boolean controlSignal = getProperty("NeuralControlSignal", "false").equals("true");
	private int maximumOutputOptions = getPropertyAsInteger("NeuralMaxOutput", "100");
	private static boolean extraDebug = true;

	public NeuralDecider(StateFactory<A> stateFactory, double scaleFactor){
		super(stateFactory);
		this.scaleFactor = scaleFactor;
	}

	@Override 
	public void injectProperties(DeciderProperties dp) {
		super.injectProperties(dp);
		monteCarlo = getProperty("MonteCarloReward", "false").equals("true");
		baseMomentum = getPropertyAsDouble("NeuralLearningMomentum", "0.0");
		propagationType = getProperty("NeuralPropagationType", "back");
		applyTemperatureToLearning = getProperty("NeuralAnnealLearning", "false").equals("true");
		learningIterations = Integer.valueOf(getProperty("NeuralLearningIterations", "1"));
		learnWithValidation = getProperty("NeuralLearnUntilValidationError", "false").equals("true");
		shuffleTrainingData = getProperty("NeuralShuffleData", "true").equals("true");
		logTrainingErrors = getProperty("NeuralLogTrainingErrors", "false").equals("true");
		maximumOutputOptions = getPropertyAsInteger("NeuralMaxOutput", "100");
		controlSignal = getProperty("NeuralControlSignal", "false").equals("true");
		if (controlSignal) {
			brain = BrainFactory.initialiseBrain(stateFactory.getVariables().size() + maximumOutputOptions, 1, decProp);
		} else {
			brain = BrainFactory.initialiseBrain(stateFactory.getVariables().size(), maximumOutputOptions, decProp);
		}
		overrideLearningCoefficient = getPropertyAsDouble("NeuralLearningCoefficient." + toString(), "-99.0");
		overrideMomentum = getPropertyAsDouble("NeuralLearningMomentum." + toString(), "-99.0");
		if (overrideLearningCoefficient > -98) alpha = overrideLearningCoefficient;
		if (overrideMomentum > -98) baseMomentum = overrideMomentum;
	}

	/*
	 * allVar is a list of all possible genetic variables that could be used
	 * We then remove a variable from the current set, or add one in that is not currently used
	 * Repeating for the number of required mutations
	 */
	public NeuralDecider<A> mutate(int mutations, List<GeneticVariable<A>> allVar) {
		List<GeneticVariable<A>> variableSet = HopshackleUtilities.cloneList(stateFactory.getVariables());
		for (int i = 0; i < mutations; i++) {
			int numberOfInputs = variableSet.size();
			if (Math.random() > (numberOfInputs - 5) * 0.1) {
				// then add a new one
				boolean variableFound = false;
				do {
					int roll = Dice.roll(1, allVar.size()) -1;
					GeneticVariable<A> choice = allVar.get(roll);
					if (!variableSet.contains(choice)) {
						variableFound = true;
						variableSet.add(choice);
					}
				} while (!variableFound);
			} else {
				int roll = Dice.roll(1, numberOfInputs) -1;
				variableSet.remove(roll);
			}
		}
		StateFactory<A> newFactory = stateFactory.cloneWithNewVariables(variableSet);
		return new NeuralDecider<A>(newFactory, scaleFactor);
	}

	@Override
	public double valueOption(ActionEnum<A> option, State<A> state) {
		int optionIndex = outputKey.getOrDefault(option.toString(), maxActionIndex);
		BasicNeuralData inputData = extractInput(state, option);
		double[] valuation = brain.compute(inputData).getData();
		if (controlSignal) optionIndex = 0;
		return valuation[optionIndex] * scaleFactor;
	}

	@Override
	public List<Double> valueOptions(List<ActionEnum<A>> options, State<A> state) {
		if (controlSignal) return valueOptionsWithControlSignal(options, state);
		List<Double> retValue = new ArrayList<Double>(options.size());
		for (int i = 0; i < options.size(); i++) retValue.add(0.0); 
		BasicNeuralData inputData = new BasicNeuralData(state.getAsArray());
		double[] valuation = brain.compute(inputData).getData();
		for (int i = 0; i < options.size(); i++) {
			ActionEnum<A> option = options.get(i);
			if (!outputKey.containsKey(option.toString())) addNewAction(option);
			int optionIndex = outputKey.getOrDefault(option.toString(), maxActionIndex);
			retValue.set(i, valuation[optionIndex] * scaleFactor);
		}
		return retValue;
	}

	private List<Double> valueOptionsWithControlSignal(List<ActionEnum<A>> options, State<A> state) {
		// we still only getArray from State once, but we need to call brain.compute for each option
		List<Double> retValue = new ArrayList<Double>(options.size());
		for (int i = 0; i < options.size(); i++) retValue.add(0.0); 
		BasicNeuralData inputData = extractInput(state, options.get(0));
		int offset = inputData.size() - maximumOutputOptions;
		int oldIndex = -1;
		for (int i = 0; i < options.size(); i++) {
			ActionEnum<A> option = options.get(i);
			if (!outputKey.containsKey(option.toString())) addNewAction(option);
			int optionIndex = outputKey.getOrDefault(option.toString(), maxActionIndex);
			if (oldIndex > -1) inputData.setData(oldIndex + offset, 0.0);
			oldIndex = optionIndex;
			inputData.setData(optionIndex + offset, 1.0);
			double[] valuation = brain.compute(inputData).getData();
			retValue.set(i, valuation[0] * scaleFactor);
		}
		return retValue;
	}

	private BasicNeuralData extractInput(State<A> state, ActionEnum<A> option) {
		if (!outputKey.containsKey(option.toString())) addNewAction(option);
		int optionIndex = outputKey.getOrDefault(option.toString(), maxActionIndex);
		double[] rawInput = state.getAsArray();
		int actionOffset = rawInput.length;
		if (controlSignal) {
			double[] rawInput2 = new double[rawInput.length + maximumOutputOptions];
			for (int i = 0; i < actionOffset; i++) rawInput2[i] = rawInput[i];
			rawInput = rawInput2;
			rawInput[optionIndex + actionOffset] = 1.0;	// we indicate which option is being considered
		}
		return new BasicNeuralData(rawInput);
	}

	public void addNewAction(ActionEnum<A> action) {
		maxActionIndex++;
		if (maxActionIndex < maximumOutputOptions)
			outputKey.put(action.toString(), maxActionIndex);
		else {
			maxActionIndex--;
			//		logger.severe("Action " + action.toString() + " cannot be allocated to output neuron");
		}
	}


	public void saveBrain(String name, String directory) {
		File saveFile = new File(directory + "\\" + name + "_" + "_" + stateFactory.getVariables().get(0).getDescriptor() + ".brain");

		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(saveFile));

			//	oos.writeObject(actions.actionSet);
			oos.writeObject(stateFactory.getVariables());
			oos.writeObject(brain);

			oos.close();

		} catch (IOException e) {
			logger.severe("Error saving brain: " + e.toString());
			for ( StackTraceElement s : e.getStackTrace()) {
				logger.info(s.toString());
			}
		} 
	}

	public void setBrain(BasicNetwork newBrain) {
		if (isBrainCompatible(newBrain)) {
			brain = newBrain;
		}
	}
	protected boolean isBrainCompatible(BasicNetwork network) {
		if (network.getInputCount() != brain.getInputCount()) {
			logger.info("Brain supplied wrong size for NeuralDecider " + toString() +
					". Has " + network.getInputCount() + 
					" neuron inputs instead of " + brain.getInputCount());
			return false;
		}
		if (network.getOutputCount() != brain.getOutputCount()) {
			logger.info("Brain supplied wrong size for NeuralDecider " + toString() +
					". Has " + network.getOutputCount() + 
					" neuron outputs instead of " + brain.getOutputCount());
			return false;
		}
		return true;
	}

	@Override
	public void learnFrom(ExperienceRecord<A> exp, double maxResult) {
		if (exp.getStartStateAsArray(useLookahead).length != brain.getInputCount()) {
			logger.severe("Input data in ExperienceRecord not the same as input neurons " 
					+ exp.getStartStateAsArray(useLookahead).length + " : " + brain.getInputCount());
		}

		if (alpha < 0.000001)
			return;	// no learning to take place

		double[][] outputValues = new double[1][brain.getOutputCount()];
		double[][] inputValues = new double[1][brain.getInputCount()];

		double[] startState = exp.getStartStateAsArray(useLookahead);
		for (int n=0; n<startState.length; n++) {
			inputValues[0][n] = startState[n];
		}
		outputValues[0] = getTarget(exp);

		// So only the action chosen has an updated target value - the others assume the prediction was correct.
		/*		if (localDebug) {
			for (int i = 0; i < inputValues.length; i++) {
				log(Arrays.toString(inputValues[i]));
				log(Arrays.toString(outputValues[i]));
				exp.getAgent().log(Arrays.toString(inputValues[i]));
				exp.getAgent().log(Arrays.toString(outputValues[i]));
			}
		}
		 */
		BasicNeuralDataSet trainingData = new BasicNeuralDataSet(inputValues, outputValues);
		teach(trainingData);
	}

	protected double[] getTarget(ExperienceRecord<A> exp) {
		// our target is the reward observed, plus gamma times the predicted value of the end state
		// which in turn is given by the best action value 
		double[] retValue = new double[brain.getOutputCount()];
		int actingAgentNumber = exp.getAgentNumber();
		double output = exp.getMonteCarloReward()[actingAgentNumber] / scaleFactor;
		if (!monteCarlo) {
			double bestQValue = valueOfBestAction(exp) / scaleFactor;
			double discountPeriod = exp.getDiscountPeriod();
			output = exp.getReward()[actingAgentNumber] / scaleFactor + Math.pow(gamma, discountPeriod) * bestQValue;
		}
		int actionIndex = outputKey.getOrDefault(exp.actionTaken.getType().toString(), -1);
		if (actionIndex == -1) {	// we have not seen this action before, so allocate an output neuron for it
			addNewAction(exp.actionTaken.getType());
			actionIndex = outputKey.get(exp.getActionTaken().getType().toString());
		}
		if (output > 1.0) output = 1.0;
		if (output < -1.0) output = -1.0;
		// for all outputs other than the target one, we use the prediction from the network (so that there is no weight update)
		BasicNeuralData inputData = new BasicNeuralData(exp.getStartStateAsArray(useLookahead));
		double[] prediction = brain.compute(inputData).getData();
		for (int n=0; n < brain.getOutputCount(); n++) {
			retValue[n] = prediction[n];
		}
		retValue[actionIndex] = output;
		return retValue;
	}

	protected BasicNeuralDataSet shuffle(BasicNeuralDataSet preshuffle) {
		int length = (int) preshuffle.getRecordCount();
		int inputLength = preshuffle.getInputSize();
		int outputLength = preshuffle.getIdealSize();
		double[][] shuffledInput = new double[length][inputLength];
		double[][] shuffledOutput = new double[length][outputLength];

		List<Integer> remainingIndices = new ArrayList<Integer>(length);
		for (int i = 0; i < length; i++) remainingIndices.add(i);
		for (int i = 0; i < length; i++) {
			int nextIndex = Dice.roll(1, remainingIndices.size()) - 1;
			int nextElement = remainingIndices.get(nextIndex);
			remainingIndices.remove(nextIndex);
			shuffledInput[i] = preshuffle.get(nextElement).getInputArray();
			shuffledOutput[i] = preshuffle.get(nextElement).getIdealArray();
		}

		return new BasicNeuralDataSet(shuffledInput, shuffledOutput);
	}

	// TODO: This does not properly support validation error checking. I think I can implement that using the Strategy
	// concept in Encog 3.3. After each iteration I check the validation error, and stop training if this has increased.
	protected double teach(BasicNeuralDataSet allData) {
		
		if (shuffleTrainingData) 
			allData = shuffle(allData);
		
		BasicNeuralDataSet validationData = new BasicNeuralDataSet();
		BasicNeuralDataSet trainingData = new BasicNeuralDataSet();
		
		if (learnWithValidation && allData.size() >= 10) {
			int dataSize = allData.size();
			for (int i = 0; i < dataSize; i++) {
				MLDataPair next = allData.get(i);
				if (i % 5 == 4) {
					validationData.add(next);
				} else {
					trainingData.add(next);
				}
			}
		} else {
			trainingData = allData;
		}
		
		double temperature = SimProperties.getPropertyAsDouble("Temperature", "1.0");
		double updatedLearningCoefficient = alpha;
		if (applyTemperatureToLearning)
			updatedLearningCoefficient *= temperature;

		Propagation trainer = null;
		switch (propagationType) {
		case "back":
			trainer = new Backpropagation(brain, trainingData, updatedLearningCoefficient, baseMomentum);
			break;
		case "quick":
			trainer = new QuickPropagation(brain, trainingData, updatedLearningCoefficient);
			break;
		case "resilient":
			trainer = new ResilientPropagation(brain, trainingData);
			break;
		default:
			throw new AssertionError(propagationType + " is not a known type. Must be back/quick/resilient.");
		}

		if (validationData.size() == 0) {
			trainer.iteration(learningIterations);
		} else {
			double trainingError = 0.0;
			double valError = Double.MAX_VALUE;
			int iteration = 0;
			boolean terminateLearning = false;
			BasicNetwork brainCopy = (BasicNetwork) brain.clone();
			do {
				double lastTrainingError = trainingError;
				trainer.iteration(1);
				trainingError = trainer.getError();
				double newValError = brain.calculateError(validationData);
				if (localDebug) {
					log(String.format("Iteration %d on %s has validation error of %.5f and training error of %.5f", iteration, this.toString(), newValError, trainingError));
				}
				if (newValError >= valError || iteration > learningIterations) {
					terminateLearning = true;
					brain = brainCopy;
					if (logTrainingErrors)
						System.out.println(String.format("%d iterations on %s has validation error of %.5f and training error of %.5f", iteration-1, this.toString(), valError, lastTrainingError));
				} else {
					brainCopy = (BasicNetwork) brain.clone();
					valError = newValError;
				}
				iteration++;
			} while (!terminateLearning);		
		}
		trainer.finishTraining();
		if (lambda > 0.00)
			applyLambda();
		return trainer.getError();
	}

	private void applyLambda() {
		double temperature = SimProperties.getPropertyAsDouble("Temperature", "1.0");
		double updatedLambda = lambda;
		if (applyTemperatureToLearning)
			updatedLambda *= temperature;
		for (int layerNumber = 0; layerNumber < brain.getLayerCount()-1; layerNumber++) {	
			for (int fromNeuron = 0; fromNeuron < brain.getLayerTotalNeuronCount(layerNumber); fromNeuron++) {
				for (int toNeuron = 0; toNeuron < brain.getLayerNeuronCount(layerNumber+1); toNeuron++) {
					// The last Neuron in each layer is a bias neuron, which has no incoming connections
					if (brain.isConnected(layerNumber, fromNeuron, toNeuron)) {
						double currentWeight = brain.getWeight(layerNumber, fromNeuron, toNeuron);
						brain.setWeight(layerNumber, fromNeuron, toNeuron, currentWeight * (1.0 - updatedLambda));
					} else {
						System.out.println(String.format("No Connection from layer %d, %d -> %d",layerNumber, fromNeuron, toNeuron));
					}
				}
			}
		}
	}

	@Override
	public void learnFromBatch(ExperienceRecord<A>[] expArray, double maxResult) {
		if (alpha < 0.000001)
			return;	// no learning to take place

		int inputLength = brain.getInputCount();
		int outputLength = brain.getOutputCount();
		double[][] batchOutputData = new double[expArray.length][outputLength];
		double[][] batchInputData = new double[expArray.length][inputLength];

		int count = 0;
		for (ExperienceRecord<A> exp : expArray) {
			double[] startState = exp.getStartStateAsArray(useLookahead);
			for (int n=0; n<startState.length; n++) {
				batchInputData[count][n] = startState[n];
			}
			batchOutputData[count] = getTarget(exp);
			if (localDebug && extraDebug) {
				String message = HopshackleUtilities.formatArray(batchInputData[count], " ", "%+.3f");
				log(message);
				exp.getAgent().log(message);
				message = HopshackleUtilities.formatArray(batchOutputData[count], " ", "%+.3f");
				log(message);
				exp.getAgent().log(message);
			}
			count++;
		}

		BasicNeuralDataSet trainingData = new BasicNeuralDataSet(batchInputData, batchOutputData);
		double error = teach(trainingData);
		if (logTrainingErrors)
			System.out.println(String.format("%s has training error of %.4f", this.toString(), error));
	}

	@SuppressWarnings("unchecked")
	public static <A extends Agent> NeuralDecider<A> createNeuralDecider(StateFactory<A> stateFactory, File saveFile, double scaleFactor) {
		NeuralDecider<A> retValue = null;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(saveFile));

			//	ArrayList<ActionEnum<A>> actionSet = (ArrayList<ActionEnum<A>>) ois.readObject();
			// Not used...but written away for...so we read it in to make the code comparable
			ArrayList<GeneticVariable<A>> variableSet = (ArrayList<GeneticVariable<A>>) ois.readObject();
			retValue = new NeuralDecider<A>(stateFactory.cloneWithNewVariables(variableSet), scaleFactor);

			BasicNetwork actionNN = (BasicNetwork) ois.readObject();

			ois.close();
			retValue.setBrain(actionNN);
			String name = saveFile.getName();
			String end = ".brain";
			name = name.substring(0, name.indexOf(end));
			retValue.setName(name);

		} catch (Exception e) {
			logger.severe("Error reading combat brain: " + e.toString());
			for ( StackTraceElement s : e.getStackTrace()) {
				logger.info(s.toString());
			}
		}

		return retValue;
	}

	@Override
	public Decider<A> crossWith(Decider<A> otherDecider) {
		if (otherDecider == null) {
			NeuralDecider<A> retValue = new NeuralDecider<A>(stateFactory, scaleFactor);
			retValue.brain = (BasicNetwork) brain.clone();
			// i.e. descendant retains weights from learning so far
			return retValue;
		}
		if (!(otherDecider instanceof NeuralDecider))
			return super.crossWith(otherDecider);

		List<GeneticVariable<A>> newInputs = combineAndReduceInputs(otherDecider.getVariables(), 4);
		NeuralDecider<A> retValue = new NeuralDecider<A>(stateFactory.cloneWithNewVariables(newInputs), scaleFactor);
		retValue.setName(this.toString());
		return retValue;
	}

	@SuppressWarnings("unchecked")
	public <V extends GeneticVariable<A>> List<V> combineAndReduceInputs(List<V> list, int mutations) {
		Set<V> retValue1 = new HashSet<V>();
		for (GeneticVariable<A> gv : stateFactory.getVariables()) {
			if (Math.random() > 0.50)
				retValue1.add((V) gv);
		}
		for (GeneticVariable<A> gv : list) {
			if (Math.random() > 0.50)
				retValue1.add((V) gv);
		}
		List<V> retValue = new ArrayList<V>();
		for (GeneticVariable<A> gv : retValue1) 
			retValue.add((V) gv);
		for (int i = 0; i < mutations; i++) {
			int numberOfInputs = retValue.size();
			if (Math.random() > (numberOfInputs - 5) * 0.1) {
				// then add a new one
				//but we can't
			} else {
				int roll = Dice.roll(1, numberOfInputs) -1;
				retValue.remove(roll);
			}
		}
		return retValue;
	}

	public void mutateWeights(int mutations) {
		NeuralStructure structure = brain.getStructure();
		if (brain.getLayerCount() != 3) 
			throw new AssertionError("Hard-coded assumption that NN has just one hidden layer.");
		int totalWeights = structure.calculateSize();
		int inputNeurons = brain.getLayerNeuronCount(1);
		int hiddenNeurons = brain.getLayerTotalNeuronCount(2);
		int outputNeurons = brain.getLayerTotalNeuronCount(3);
		totalWeights = inputNeurons * (hiddenNeurons - 1) + hiddenNeurons * outputNeurons;
		// -1 in the above is for the bias neuron in the hidden layer, which has no connections from input layer

		for (int i = 0; i < mutations; i++) {
			int weightToChange = Dice.roll(1, totalWeights);
			int fromLayer = 1;
			int toNeuronCount = hiddenNeurons - 1;
			if (weightToChange > inputNeurons * (hiddenNeurons - 1)) {
				fromLayer = 2;
				weightToChange -= inputNeurons * (hiddenNeurons - 1);
				toNeuronCount = outputNeurons;
			}
			int fromNeuron = weightToChange / toNeuronCount + 1;
			int toNeuron = weightToChange % toNeuronCount + 1;

			brain.addWeight(fromLayer, fromNeuron, toNeuron, Math.random() / 10.0);
		}
	}

	public BasicNetwork getBrain() {
		return brain;
	}
}
