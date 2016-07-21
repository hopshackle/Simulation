package hopshackle.simulation;

import java.util.*;

import org.encog.neural.data.basic.BasicNeuralData;

public class NeuralLookaheadDecider<A extends Agent> extends LookaheadDecider<A> {

	private NeuralDecider<A> internalNeuralDecider;

	public NeuralLookaheadDecider(StateFactory<A> stateFactory,	LookaheadFunction<A> lookahead, List<ActionEnum<A>> actions) {
		super(stateFactory, lookahead, actions);
		internalNeuralDecider = new NeuralDecider<A>(stateFactory, dummyActionSet);
	}

	@Override
	public void learnFrom(ExperienceRecord<A> exp, double maxResult) {
		ExperienceRecord<A> expRecAfterLookahead = preProcessExperienceRecord(exp);
		internalNeuralDecider.learnFrom(expRecAfterLookahead, maxResult);
	}

	@Override
	public double value(LookaheadState<A> state) {
		BasicNeuralData inputData = new BasicNeuralData(state.getAsArray());
		return internalNeuralDecider.brain.compute(inputData).getData(0);
	}
}



