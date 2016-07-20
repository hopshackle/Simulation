package hopshackle.simulation;

import java.util.List;

import org.encog.neural.networks.BasicNetwork;

public class NeuralLookaheadDecider<A extends Agent> extends LookaheadDecider<A> {

	private BasicNetwork brain;
	protected double maxNoise = SimProperties.getPropertyAsDouble("NeuralNoise", "0.20");
	protected double baseLearningCoefficient = SimProperties.getPropertyAsDouble("NeuralLearningCoefficient", "0.02");
	protected double baseMomentum = SimProperties.getPropertyAsDouble("NeuralLearningMomentum", "0.0");
	
	public NeuralLookaheadDecider(StateFactory<A> stateFactory,	LookaheadFunction<A> lookahead, List<ActionEnum<A>> actions) {
		super(stateFactory, lookahead, actions);
		brain = BrainFactory.initialiseBrain(stateFactory.getVariables().size(), 1);
	}

	@Override
	public void learnFrom(ExperienceRecord<A> exp, double maxResult) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double value(LookaheadState<A> state) {
		// TODO Auto-generated method stub
		return 0;
	}

}
