package hopshackle.simulation;

import java.util.*;

import org.encog.neural.data.basic.BasicNeuralData;

public class NeuralLookaheadDecider<A extends Agent> extends LookaheadDecider<A> {

	private NeuralDecider<A> internalNeuralDecider;

	public NeuralLookaheadDecider(StateFactory<A> stateFactory, SimpleWorldLogic<A> worldLogic, double scaleFactor) {
		super(stateFactory);
		internalNeuralDecider = new NeuralDecider<A>(stateFactory, worldLogic, scaleFactor);
	}
	
	@Override
	public void learnFrom(ExperienceRecord<A> exp, double maxResult) {
		logResult(exp);
		internalNeuralDecider.learnFrom(exp, maxResult);
	}
	
	@Override
	public void learnFromBatch(List<ExperienceRecord<A>> allExperience, double maxResult) {
		List<ExperienceRecord<A>> processedER = new ArrayList<ExperienceRecord<A>>();
		for (ExperienceRecord<A> er : allExperience) {			
			processedER.add(er);
			logResult(er);
		}
		internalNeuralDecider.learnFromBatch(processedER, maxResult);
	}
	
	private void logResult(ExperienceRecord<A> baseER) {
		if (localDebug) {
			double maxResult = baseER.getAgent().getMaxScore();
			double startValue = value(baseER.getStartState()) * maxResult; // projection
			double endValue = value(baseER.getEndState()) * maxResult; // projection
			String message = String.format("Learning:\t%-20sScore: %.2f -> %.2f, State Valuation: %.2f -> %.2f, EndGame: %s", 
					baseER.getActionTaken(), baseER.getStartScore(), baseER.getEndScore(), startValue, endValue, baseER.isInFinalState());
			log(message);
			baseER.getAgent().log(message);
			double[] startArray = baseER.getStartStateAsArray();
			double[] endArray = baseER.getEndStateAsArray();
			double[] featureTrace = baseER.getFeatureTrace();
			StringBuffer logMessage = new StringBuffer("StartState -> EndState (FeatureTrace) :" + newline);
			for (int i = 0; i < startArray.length; i++) {
				if (startArray[i] != 0.0 || endArray[i] != 0.0 || Math.abs(featureTrace[i]) >= 0.01)
					logMessage.append(String.format("\t%.2f -> %.2f (%.2f) %s %s", startArray[i], endArray[i], featureTrace[i], stateFactory.getVariables().get(i).toString(), newline));
			}
			message = logMessage.toString();
			log(message);
			baseER.getAgent().log(message);
		}
	}

	@Override
	public double value(State<A> state) {
		BasicNeuralData inputData = new BasicNeuralData(state.getAsArray());
		return internalNeuralDecider.brain.compute(inputData).getData(0);
	}

	public void setInternalNeuralNetwork(NeuralDecider<A> newND) {
		if (internalNeuralDecider.isBrainCompatible(newND.brain)) {
			internalNeuralDecider = newND;
		} else {
			throw new AssertionError("New Network is not compatible with the old one.");
		}
	}
	
	public void saveToFile(String descriptor, String directory) {
		internalNeuralDecider.saveBrain(descriptor, directory);
	}
	
	@Override
	public void setName(String newName) {
		super.setName(newName);
		internalNeuralDecider.setName(newName + "_ND");
	}
}



