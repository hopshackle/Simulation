package hopshackle.simulation;

import java.util.*;

public class MCTSChildDecider<P extends Agent> extends BaseDecider<P> {

	private Decider<P> rolloutDecider;
	private MonteCarloTree<P> tree;

	public MCTSChildDecider(StateFactory<P> stateFactory, MonteCarloTree<P> tree, Decider<P> rolloutDecider) {
		super(stateFactory);
		localDebug = false;
		this.rolloutDecider = rolloutDecider;
		this.tree = tree;
		boolean monteCarlo = getProperty("MonteCarloReward", "false").equals("true");
		if (!monteCarlo) {
			throw new AssertionError("MCTS Deciders should only be used with MonteCarlo switched on! Update GeneticProperties.txt");
		}
	}

	@Override
	public ActionEnum<P> makeDecision(P decidingAgent, List<ActionEnum<P>> chooseableOptions) {
		if (decidingAgent.isDead()) return null;

		State<P> state = stateFactory.getCurrentState(decidingAgent);
		if (chooseableOptions.isEmpty()) {
			return null;
		}

		if (tree.containsState(state)) {
			return tree.getNextAction(state, chooseableOptions);
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
	public void learnFrom(ExperienceRecord<P> exp, double maxResult) {
		// 'Learning' in this context means updating the MonteCarloTree
		if (tree.containsState(exp.getStartState())) {
			if (!tree.containsState(exp.getEndState()) && tree.updatesLeft() > 0 && !exp.isFinalState) {
				// this will insert the state, so that the update will use the base value...the s', a, s'' update
				// will then update this state using the reward
				tree.insertState(exp.getEndState(), exp.getPossibleActionsFromEndState());
			}
			tree.updateState(exp.getStartState(), exp.getActionTaken().actionType, exp.getEndState(), exp.getReward());
		} else if (tree.updatesLeft() > 0) {
			throw new AssertionError("Tree should contain previous state");
		}
		// This relies on us going forward through the ER (if backwards, then we will error as state is not found, and we
		// have updates left).
	}

	public Decider<P> getRolloutDecider() {
		return rolloutDecider;
	}

}
