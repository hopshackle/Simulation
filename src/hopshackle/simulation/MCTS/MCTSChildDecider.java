package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;

import java.util.*;

public class MCTSChildDecider<P extends Agent> extends BaseAgentDecider<P> {

	private Decider<P> rolloutDecider;
	private MonteCarloTree<P> tree;

	public MCTSChildDecider(StateFactory<P> stateFactory, MonteCarloTree<P> tree, Decider<P> rolloutDecider, DeciderProperties prop) {
		super(stateFactory);
		injectProperties(prop);
		localDebug = false;
		this.rolloutDecider = rolloutDecider;
		this.tree = tree;
	}

	@Override
	public ActionEnum<P> makeDecision(P decidingAgent, List<ActionEnum<P>> chooseableOptions) {
		if (decidingAgent.isDead()) return null;

		State<P> state = stateFactory.getCurrentState(decidingAgent);
		if (chooseableOptions.isEmpty()) {
			return null;
		}

        int decidingAgentRef = decidingAgent.getActorRef();
		if (state != null) { // within tree
			return tree.getNextAction(state, chooseableOptions, decidingAgentRef);
		} else {
			return rolloutDecider.makeDecision(decidingAgent, chooseableOptions);
		}
	}


	@Override
	public double valueOption(ActionEnum<P> option, P decidingAgent) {
		// we never actually value options
		// we override the makeDecision() method to implement MCTS
		return 0.0;
	}
	@Override
	public double valueOption(ActionEnum<P> option, State<P> state) {
		// we never actually value options
		// we override the makeDecision() method to implement MCTS
		return 0.0;
	}

	@Override
	public void learnFrom(ExperienceRecord<P> exp, double maxResult) {}

	public void setRolloutDecider(Decider<P> rollout) {
		rolloutDecider = rollout;
	}
}
