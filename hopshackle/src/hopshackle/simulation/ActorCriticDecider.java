package hopshackle.simulation;

import java.io.*;
import java.util.*;

import org.encog.neural.data.basic.*;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.training.propagation.back.Backpropagation;

public class ActorCriticDecider<A extends Agent> extends NeuralDecider<A> {

	protected BasicNetwork stateEvaluationBrain;
	private static double gamma = SimProperties.getPropertyAsDouble("Gamma", "0.99");

	public ActorCriticDecider(List<ActionEnum<A>> actions, List<GeneticVariable<A>> variables) {
		super(actions, variables);
		stateEvaluationBrain = initialiseBrain(variables);
	}
	
	@Override
	/* 
	 * 	For Neural Deciders, we can cross them with another
	 *  as long as the action and variable Sets are identical
	 *  (or at least have the same number of items across the two Deciders)
	 */
	public Decider<A> crossWith(Decider<A> otherDecider) {
		if (!(otherDecider instanceof ActorCriticDecider))
			return super.crossWith(otherDecider);
		if (this.variableSet.size() != otherDecider.getVariables().size())
			return super.crossWith(otherDecider);
		if (this.actionSet.size() != otherDecider.getActions().size())
			return super.crossWith(otherDecider);
		if (this == otherDecider) return this;

		ActorCriticDecider<A> retValue = new ActorCriticDecider<A>(actionSet, variableSet);
		BasicNetwork[] newBrain = new BasicNetwork[actionSet.size()];
		BasicNetwork newStateBrain = null;
		for (int n = 0; n < actionSet.size(); n++) {
			// 50:50 chance for each action which Network we take
			if (Math.random() < 0.5) {
				newBrain[n] = (BasicNetwork) this.brain.get(actionSet.get(n).toString()).clone();
			} else {
				newBrain[n] = (BasicNetwork) ((NeuralDecider<A>)otherDecider).brain.get(actionSet.get(n).toString()).clone();
			}

			if (Math.random() < 0.5) {
				newStateBrain = (BasicNetwork) this.stateEvaluationBrain.clone();
			} else {
				newStateBrain = (BasicNetwork) ((ActorCriticDecider<A>)otherDecider).stateEvaluationBrain.clone();
			}
		}

		retValue.setBrain(newBrain);
		retValue.setName(toString());
		retValue.setStateBrain(newStateBrain);
		return retValue;
	}

	private void setStateBrain(BasicNetwork newStateBrain) {
		stateEvaluationBrain = newStateBrain;
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
		BasicNeuralData inputData = new BasicNeuralData(getCurrentState(agent, agent, null));
		double retValue = stateEvaluationBrain.compute(inputData).getData()[0];

		temperature = SimProperties.getPropertyAsDouble("Temperature", "1.0");
		return retValue += (1.0 + (Math.random()-0.5)*temperature*maxNoise);
	}
	
	@Override
	public void learnFrom(ExperienceRecord<A> exp, double maxResult) {
		BasicNetwork brainToTrain = brain.get(exp.getActionTaken().toString());
		if (exp.getVariables().size() != brainToTrain.getInputCount()) {
			logger.severe("Input data in ExperienceRecord not the same as input neurons " 
					+ exp.getVariables().size() + " : " + brainToTrain.getInputCount());
		}

		if (baseLearningCoefficient < 0.000001)
			return;	// no learning to take place

		double output = exp.getReward()/maxResult;
		double modifiedLearningCoefficient = baseLearningCoefficient / (double)brain.size() / getPercentageOfOption(exp.getActionTaken().getType());

		BasicNeuralData endState = new BasicNeuralData(exp.getEndState());
		double currentValue = (stateEvaluationBrain.compute(endState)).getData()[0];
		if (exp.isInFinalState()) currentValue = 0.0;
		double updatedStartValue = output + gamma * currentValue;
		if (updatedStartValue > 1.0) output = 1.0;
		if (updatedStartValue < -1.0) output = -1.0;

		double[][] outputValues = new double[1][1];
		double[][] inputValues = new double[1][brainToTrain.getInputCount()];
		double[][] inputValuesForStateBrain = new double[1][stateEvaluationBrain.getInputCount()];

		outputValues[0][0] = updatedStartValue;
		double[] subLoop = exp.getStartState();
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