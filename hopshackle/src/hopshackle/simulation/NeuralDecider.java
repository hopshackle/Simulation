package hopshackle.simulation;

import java.io.*;
import java.util.*;

import org.encog.neural.data.basic.*;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.training.propagation.back.Backpropagation;
public class NeuralDecider<A extends Agent> extends BaseDecider<A> {

	protected BasicNetwork brain;
	protected static double temperature;
	protected double maxNoise = SimProperties.getPropertyAsDouble("NeuralNoise", "0.20");
	protected double baseLearningCoefficient = SimProperties.getPropertyAsDouble("NeuralLearningCoefficient", "0.02");
	protected double baseMomentum = SimProperties.getPropertyAsDouble("NeuralLearningMomentum", "0.0");
	private double overrideLearningCoefficient, overrideMomentum; 

	public NeuralDecider(StateFactory<A> stateFactory, List<? extends ActionEnum<A>> actions){
		super(stateFactory, actions);
		brain = BrainFactory.initialiseBrain(actionSet.size(), stateFactory.getVariables().size());
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

	/*
	 * (non-Javadoc)
	 * @see hopshackle.simulation.Decider#valueOption(hopshackle.simulation.ActionEnum, hopshackle.simulation.Agent)
	 * TODO: valueOption will work, but is |A| times slower than the ideal approach, as it calls the NN once for each possible
	 * action, given the main loop in BaseDecider. So, if performance is a problem here (which it probably shouldn't be...), then 
	 * getValuesPerOption and getNormalisedValuesPerOption can be overridden. However that would be non-standard and hence confusing.
	 */
	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent) {
		State<A> state = getCurrentState(decidingAgent);
		BasicNeuralData inputData = new BasicNeuralData(state.getAsArray());
		double[] retValue = brain.compute(inputData).getData();
		temperature = SimProperties.getPropertyAsDouble("Temperature", "1.0");
		int index = actionSet.indexOf(option);
		if (localDebug)
			decidingAgent.log("Option " + option.toString() + " has base Value of " + retValue);
		return retValue[index] += (1.0 + (Math.random()-0.5)*temperature*maxNoise);
	}
	
	public void saveBrain(String name, String directory) {
		File saveFile = new File(directory + "\\" + name + "_" + 
				actionSet.get(0).getChromosomeDesc() + "_" + stateFactory.getVariables().get(0).getDescriptor() + ".brain");

		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(saveFile));

			oos.writeObject(actionSet);
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
		if (network.getInputCount() != stateFactory.getVariables().size()) {
			logger.info("Brain supplied wrong size for NeuralDecider " + toString() +
					". Has " + network.getInputCount() + 
					" neuron inputs instead of " + stateFactory.getVariables().size());
			return false;
		}
		if (network.getOutputCount() != actionSet.size()) {
			logger.info("Brain supplied wrong size for NeuralDecider " + toString() +
					". Has " + network.getOutputCount() + 
					" neuron outputs instead of " + actionSet.size());
			return false;
		}
		return true;
	}

	@Override
	public void learnFrom(ExperienceRecord<A> exp, double maxResult) {
		if (exp.getStartStateAsArray().length != brain.getInputCount()) {
			logger.severe("Input data in ExperienceRecord not the same as input neurons " 
					+ exp.getStartStateAsArray().length + " : " + brain.getInputCount());
		}

		if (baseLearningCoefficient < 0.000001)
			return;	// no learning to take place

		double[][] outputValues = new double[1][brain.getOutputCount()];
		double[][] inputValues = new double[1][brain.getInputCount()];
	
		double[] startState = exp.getStartStateAsArray();
		for (int n=0; n<startState.length; n++) {
			inputValues[0][n] = startState[n];
		}

		double output = exp.getReward()/maxResult;
		if (output > 1.0) output = 1.0;
		if (output < -1.0) output = -1.0;
		BasicNeuralData inputData = new BasicNeuralData(startState);
		double[] prediction = brain.compute(inputData).getData();
		for (int n=0; n < exp.getEndStateAsArray().length; n++) {
			outputValues[0][n] = prediction[n];
		}
		int actionIndex = actionSet.indexOf(exp.actionTaken.getType());
		outputValues[0][actionIndex] = output;
		// So only the action chosen has an updated target value - the others assume the prediction was correct.

		BasicNeuralDataSet trainingData = new BasicNeuralDataSet(inputValues, outputValues);
		Backpropagation trainer = new Backpropagation(brain, trainingData, baseLearningCoefficient, baseMomentum);
		trainer.iteration();
	}
	
	@SuppressWarnings("unchecked")
	public static <A extends Agent> NeuralDecider<A> createNeuralDecider(StateFactory<A> stateFactory, File saveFile) {
		NeuralDecider<A> retValue = null;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(saveFile));
	
			ArrayList<ActionEnum<A>> actionSet = (ArrayList<ActionEnum<A>>) ois.readObject();
			// Not used...but written away for...so we read it in to make the code comparable
			ArrayList<GeneticVariable<A>> variableSet = (ArrayList<GeneticVariable<A>>) ois.readObject();
			retValue = new NeuralDecider<A>(stateFactory, actionSet);
	
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
}
