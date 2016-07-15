package hopshackle.simulation;

import java.io.*;
import java.util.*;

import org.encog.neural.data.basic.*;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.training.propagation.back.Backpropagation;

public abstract class ActorCriticDecider<A extends Agent, S extends State<A>> extends NeuralDecider<A, S> {

	protected BasicNetwork stateEvaluationBrain;
	private static double gamma = SimProperties.getPropertyAsDouble("Gamma", "0.99");

	public ActorCriticDecider(List<ActionEnum<A>> actions, List<GeneticVariable<A, S>> variables) {
		super(actions, variables);
		stateEvaluationBrain = initialiseBrain(variables);
	}

	public void saveBrain(String descriptor) {
		String directory = SimProperties.getProperty("BaseDirectory", "C:");
		directory = directory + "\\ActorCriticBrains";

		File saveFile = new File(directory + "\\" + descriptor + "_" + name.substring(0, 4) + ".brain");
		
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(saveFile));

			oos.writeObject(actionSet);
			oos.writeObject(variableSet);
			for (ActionEnum<A> ae : actionSet)
				oos.writeObject(brain.get(ae.toString()));
			oos.writeObject(stateEvaluationBrain);
			oos.close();

		} catch (IOException e) {
			logger.severe("Error saving brain: " + e.toString());
			for ( StackTraceElement s : e.getStackTrace()) {
				logger.info(s.toString());
			}
		} 
	}

	public double getNoise() {
		return maxNoise;
	}

	public double valueState(A agent) {
		BasicNeuralData inputData = new BasicNeuralData(HopshackleUtilities.stateToArray(getCurrentState(agent), variableSet));
		double retValue = stateEvaluationBrain.compute(inputData).getData()[0];

		temperature = SimProperties.getPropertyAsDouble("Temperature", "1.0");
		return retValue += (1.0 + (Math.random()-0.5)*temperature*maxNoise);
	}
	
	@Override
	public void learnFrom(ExperienceRecord<A, S> exp, double maxResult) {
		BasicNetwork brainToTrain = brain.get(exp.getActionTaken().toString());
		if (exp.getVariables().size() != brainToTrain.getInputCount()) {
			logger.severe("Input data in ExperienceRecord not the same as input neurons " 
					+ exp.getVariables().size() + " : " + brainToTrain.getInputCount());
		}

		if (baseLearningCoefficient < 0.000001)
			return;	// no learning to take place

		double output = exp.getReward()/maxResult;
		double modifiedLearningCoefficient = baseLearningCoefficient / (double)brain.size() / getPercentageOfOption(exp.getActionTaken().getType());

		BasicNeuralData endState = new BasicNeuralData(HopshackleUtilities.stateToArray(exp.getEndState(), variableSet));
		double currentValue = (stateEvaluationBrain.compute(endState)).getData()[0];
		if (exp.isInFinalState()) currentValue = 0.0;
		double updatedStartValue = output + gamma * currentValue;
		if (updatedStartValue > 1.0) output = 1.0;
		if (updatedStartValue < -1.0) output = -1.0;

		double[][] outputValues = new double[1][1];
		double[][] inputValues = new double[1][brainToTrain.getInputCount()];
		double[][] inputValuesForStateBrain = new double[1][stateEvaluationBrain.getInputCount()];

		outputValues[0][0] = updatedStartValue;
		double[] subLoop = HopshackleUtilities.stateToArray(exp.getStartState(), variableSet);
		for (int n=0; n<subLoop.length; n++) {
			inputValues[0][n] = subLoop[n];
			inputValuesForStateBrain[0][n] = subLoop[n];
		}

		BasicNeuralDataSet trainingData = new BasicNeuralDataSet(inputValues, outputValues);
		Backpropagation trainer = new Backpropagation(brainToTrain, trainingData, modifiedLearningCoefficient, baseMomentum);
		trainer.iteration();
		trainingData = new BasicNeuralDataSet(inputValuesForStateBrain, outputValues);
		trainer = new Backpropagation(stateEvaluationBrain, trainingData, baseLearningCoefficient / (double)brain.size(), baseMomentum);
		trainer.iteration();
	}
}