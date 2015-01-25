package hopshackle.simulation;

import java.io.*;
import java.util.*;

import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.neural.data.basic.*;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.back.Backpropagation;
public class NeuralDecider extends QDecider {

	protected Hashtable<String, BasicNetwork> brain;
	protected static double temperature;
	protected double maxNoise;
	private HashMap<ActionEnum, Integer> last100Choices;
	private HashMap<ActionEnum, Integer> previous100Choices;
	private int decisionCounter = 0;
	private boolean nd_debug = false;
	private static boolean initialOptimismTraining = SimProperties.getProperty("NeuralDeciderInitialOptimismTraining", "true").equals("true");
	protected double baseLearningCoefficient = SimProperties.getPropertyAsDouble("NeuralLearningCoefficient", "0.02");
	protected double baseMomentum = SimProperties.getPropertyAsDouble("NeuralLearningMomentum", "0.0");
	private double overrideLearningCoefficient, overrideMomentum; 

	public NeuralDecider(List<? extends ActionEnum> actions, List<GeneticVariable> variables){
		super(actions, variables);
		maxNoise = SimProperties.getPropertyAsDouble("NeuralNoise", "0.20");
		// Each NeuralDecider will have a brain consisting of:
		//	A Neural Network for each Option, containing 1 output Neuron,
		//  and a number of input Neurons equal to the Variables
		brain = initialiseFullBrain(actionSet, variableSet);
		last100Choices = new HashMap<ActionEnum, Integer>();
		previous100Choices = new HashMap<ActionEnum, Integer>();
		overrideLearningCoefficient = SimProperties.getPropertyAsDouble("NeuralLearningCoefficient." + toString(), "-99.0");
		overrideMomentum = SimProperties.getPropertyAsDouble("NeuralLearningMomentum." + toString(), "-99.0");
		if (overrideLearningCoefficient > -98) baseLearningCoefficient = overrideLearningCoefficient;
		if (overrideMomentum > -98) baseMomentum = overrideMomentum;
	}

	public void setName(String newName) {
		super.setName(newName);
		double newMaxNoise = SimProperties.getPropertyAsDouble("NeuralNoise." + newName, "-99.00");
		if (newMaxNoise > -98.00)
			maxNoise = newMaxNoise;
	}

	public void setVariables(List<GeneticVariable> genVar) {
		super.setVariables(genVar);
		brain = initialiseFullBrain(actionSet, variableSet);
	}
	public void setActions(List<? extends ActionEnum> actions) {
		super.setActions(actions);
		brain = initialiseFullBrain(actionSet, variableSet);
	}

	protected static Hashtable<String, BasicNetwork> initialiseFullBrain(List<ActionEnum> actionSet, List<GeneticVariable> varSet) {
		Hashtable<String, BasicNetwork> retValue = new Hashtable<String, BasicNetwork>();
		if (actionSet == null || varSet == null) return retValue;
		for (ActionEnum ae : actionSet) {
			retValue.put(ae.toString(), initialiseBrain(varSet));
		}
		return retValue;
	}

	public static BasicNetwork initialiseBrain(List<GeneticVariable> varSet) {
		int neuronLayers = Integer.valueOf(SimProperties.getProperty("NeuralDeciderHiddenLayers", "1"));
		int[] layers = new int[neuronLayers+2];
		if (varSet != null) {
			int inputNeurons = varSet.size();
			layers[0] = inputNeurons;
			for (int hiddenLayer = 1; hiddenLayer <= neuronLayers; hiddenLayer++) {
				layers[hiddenLayer] = inputNeurons / 2 + 1;	 // all hidden layers have same number of neurons
			}
			layers[neuronLayers+1] = 1;	// output neuron
			return newFFNetwork(layers);
		}
		return null;
	}

	@Override
	public double valueOption(ActionEnum option, Agent decidingAgent, Agent contextAgent) {
		double[] state = getCurrentState(decidingAgent, contextAgent);
		double retValue = valueOption(option, state);
		if (nd_debug)
			decidingAgent.log("Option " + option.toString() + " has base Value of " + retValue);
		return retValue;
	}
	
	@Override
	public double valueOption(ActionEnum option, double[] state) {
		BasicNetwork brainSection = brain.get(option.toString());
		if (brainSection == null) {
			logger.severe("Action reference for " + option.toString() + " not found in Brain");
			return -1.0;
		}
		BasicNeuralData inputData = new BasicNeuralData(state);
		double retValue = brainSection.compute(inputData).getData()[0];
		temperature = SimProperties.getPropertyAsDouble("Temperature", "1.0");
		return retValue += (1.0 + (Math.random()-0.5)*temperature*maxNoise);
	}
	
	/**
	 * This static function returns a new Feedforward Network. 
	 * The parameter taken is a integer array indicating the number of neurons to
	 * have in each layer. The first entry in the array is the input layer; the last layer is the output layer.
	 * 
	 * @author $James Goodman$
	 * @version $1$
	 */
	public static BasicNetwork newFFNetwork(int[] layers) {

		BasicNetwork network = new BasicNetwork();

		int maxLoop = layers.length;
		for (int loop = 0; loop < maxLoop; loop++)
			network.addLayer(new BasicLayer(new ActivationSigmoid(), loop < maxLoop-1, layers[loop]));
		// No bias neuron for output layer

		network.getStructure().finalizeStructure();
		network.reset();
		if (initialOptimismTraining)
			initialOptimismTraining(network, 1.0);

		return network;
	}

	private static void initialOptimismTraining(BasicNetwork network, double maxValue) {
		// Optimistically train this array
		int inputNeurons = network.getInputCount();
		double[][] trainingInputData = new double[100][inputNeurons];
		double[][] trainingOutputData = new double[100][1];
		for (int n=0; n<100; n++) {
			for (int m=0; m<inputNeurons; m++)
				trainingInputData[n][m] = Math.random()*2.0 - 1.0;
			trainingOutputData[n][0] = (Math.random()/4.0)+ maxValue - 0.25;
		}

		BasicNeuralDataSet trainingSet = new BasicNeuralDataSet(trainingInputData, trainingOutputData);
		Backpropagation trainer = new Backpropagation(network, trainingSet, 0.2, 0.00);
		trainer.iteration();
	}


	public static NeuralDecider createNeuralDecider(File saveFile) {
		NeuralDecider retValue = null;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(saveFile));

			ArrayList actionSet = (ArrayList) ois.readObject();
			ArrayList variableSet = (ArrayList) ois.readObject();
			retValue = new NeuralDecider(actionSet, variableSet);

			BasicNetwork[] actionNN = new BasicNetwork[actionSet.size()];
			for (int n=0; n<actionSet.size(); n++)
				actionNN[n] = (BasicNetwork) ois.readObject();

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

	public void saveBrain(String name) {
		String directory = SimProperties.getProperty("BaseDirectory", "C:");
		directory = directory + "\\NeuralBrains";
		saveBrain(name, directory);
	}

	public void saveBrain(String name, String directory) {
		File saveFile = new File(directory + "\\" + name + "_" + 
				actionSet.get(0).getChromosomeDesc() + "_" + variableSet.get(0).getDescriptor() + ".brain");

		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(saveFile));

			oos.writeObject(actionSet);
			oos.writeObject(variableSet);
			for (ActionEnum ae : actionSet)
				oos.writeObject(brain.get(ae.toString()));

			oos.close();

		} catch (IOException e) {
			logger.severe("Error saving brain: " + e.toString());
			for ( StackTraceElement s : e.getStackTrace()) {
				logger.info(s.toString());
			}
		} 
	}

	protected void updateBrain(NeuralDecider parent) {
		this.brain = parent.brain;
		setName(parent.toString());
	}

	public void setBrain(BasicNetwork[] actionNN2) {
		if (isBrainCompatible(actionNN2)) {
			brain.clear();
			for (int n=0; n<actionNN2.length; n++)	
				brain.put(actionSet.get(n).toString(), actionNN2[n]);
		}
	}
	protected boolean isBrainCompatible(BasicNetwork[] network) {
		if (network.length != actionSet.size()) {
			logger.info("Brain supplied wrong size for NeuralDecider " + this.toString() + ". " + 
					actionSet.size() + "expected, " + network.length + " received.");
			return false;
		}
		for (int n=0; n<network.length; n++){
			if (network[n].getInputCount() != variableSet.size()) {
				logger.info("Brain supplied wrong size for NeuralDecider " + toString() +
						". Action " + n + " has " + network[n].getInputCount() + 
						" neuron inputs instead of " + variableSet.size());
				return false;
			}
		}
		return true;
	}

	@Override
	/* 
	 * 	For Neural Deciders, we can cross them with another
	 *  as long as the action and variable Sets are identical
	 *  (or at least have the same number of items across the two Deciders)
	 */
	public Decider crossWith(Decider otherDecider) {
		if (!(otherDecider instanceof NeuralDecider))
			return super.crossWith(otherDecider);
		if (this.variableSet.size() != otherDecider.getVariables().size())
			return super.crossWith(otherDecider);
		if (this.actionSet.size() != otherDecider.getActions().size())
			return super.crossWith(otherDecider);
		if (this == otherDecider) return this;

		NeuralDecider retValue = new NeuralDecider(actionSet, variableSet);
		retValue.setTeacher(this.getTeacher());
		BasicNetwork[] newBrain = new BasicNetwork[actionSet.size()];
		for (int n = 0; n < actionSet.size(); n++) {
			// 50:50 chance for each action which Network we take
			if (Math.random() < 0.5) {
				newBrain[n] = this.brain.get(actionSet.get(n).toString());
			} else {
				newBrain[n] = ((NeuralDecider)otherDecider).brain.get(actionSet.get(n).toString());
			}
		}

		retValue.setBrain(newBrain);
		retValue.setName(toString());
		return retValue;
	}

	public double getNoise() {
		return maxNoise;
	}

	public void addDecision(ActionEnum option) {
		if (decisionCounter >= 100) {
			previous100Choices = last100Choices;
			last100Choices = new HashMap<ActionEnum, Integer>();
			decisionCounter = 0;
		}
		Integer number = last100Choices.get(option);
		if (number == null) number = 0;
		number++;
		last100Choices.put(option, number);
		decisionCounter++;
	}

	@Override
	public ActionEnum decide(Agent decidingAgent) {
		ActionEnum retValue = super.decide(decidingAgent);
		addDecision(retValue);
		return retValue;
	}

	public double getPercentageOfOption(ActionEnum option) {
		Integer number1 = last100Choices.get(option);
		if (number1 == null) number1 = 0;
		Integer number2 = previous100Choices.get(option);
		if (number2 == null) number2 = 0;
		double retValue = ((double)number1 + (double)number2) / (100.0 + (double) decisionCounter);
		if (retValue < 0.01) retValue = 0.01;
		if (previous100Choices.size() == 0) return 0.20;
		return retValue;
	}
	
	@Override
	public void learnFrom(ExperienceRecord exp, double maxResult) {
		BasicNetwork brainToTrain = brain.get(exp.getActionTaken().toString());
		if (exp.getVariables().size() != brainToTrain.getInputCount()) {
			logger.severe("Input data in ExperienceRecord not the same as input neurons " 
					+ exp.getVariables().size() + " : " + brainToTrain.getInputCount());
		}

		if (baseLearningCoefficient < 0.000001)
			return;	// no learning to take place

		double[][] outputValues = new double[1][1];
		double[][] inputValues = new double[1][brainToTrain.getInputCount()];
		
		double output = exp.getReward()/maxResult;
		if (output > 1.0) output = 1.0;
		if (output < -1.0) output = -1.0;
		outputValues[0][0] = output;
		double[] subLoop = exp.getStartState();
		for (int n=0; n<subLoop.length; n++) {
			inputValues[0][n] = subLoop[n];
		}

		double modifiedLearningCoefficient = baseLearningCoefficient / (double)brain.size() / getPercentageOfOption(exp.getActionTaken());

		BasicNeuralDataSet trainingData = new BasicNeuralDataSet(inputValues, outputValues);
		Backpropagation trainer = new Backpropagation(brainToTrain, trainingData, modifiedLearningCoefficient, baseMomentum);
		trainer.iteration();
	}
}
