package hopshackle.simulation;

import java.util.*;

import org.encog.neural.data.basic.BasicNeuralData;

public class NeuralLookaheadDecider<A extends Agent> extends LookaheadDecider<A> {

	private NeuralDecider<A> internalNeuralDecider;

	public NeuralLookaheadDecider(StateFactory<A> stateFactory,	LookaheadFunction<A> lookahead, List<ActionEnum<A>> actions, double scaleFactor) {
		super(stateFactory, lookahead, actions);
		internalNeuralDecider = new NeuralDecider<A>(stateFactory, dummyActionSet, scaleFactor);
	}
	
	@Override
	public void learnFrom(ExperienceRecord<A> exp, double maxResult) {
		ExperienceRecord<A> expRecAfterLookahead = preProcessExperienceRecord(exp);
		internalNeuralDecider.learnFrom(expRecAfterLookahead, maxResult);
	}
	
	@Override
	public void learnFromBatch(List<ExperienceRecord<A>> allExperience, double maxResult) {
		List<ExperienceRecord<A>> processedER = new ArrayList<ExperienceRecord<A>>();
		for (ExperienceRecord<A> er : allExperience) {
			processedER.add(preProcessExperienceRecord(er));
		}
		internalNeuralDecider.learnFromBatch(processedER, maxResult);
	}

	@Override
	public double value(LookaheadState<A> state) {
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



