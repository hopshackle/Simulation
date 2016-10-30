package hopshackle.simulation;

import java.util.*;

public class MCTSChildDecider<P extends Agent> extends BaseDecider<P> {

	private Decider<P> rolloutDecider;
	private MonteCarloTree<P, ActionEnum<P>> tree;
	
	public MCTSChildDecider(StateFactory<P> stateFactory, List<ActionEnum<P>> actions, MonteCarloTree<P, ActionEnum<P>> tree, Decider<P> rolloutDecider) {
		super(stateFactory, actions);
		this.rolloutDecider = rolloutDecider;
		this.tree = tree;
		boolean monteCarlo = SimProperties.getProperty("MonteCarloReward", "false").equals("true");
		if (!monteCarlo) {
			throw new AssertionError("MCTS Deciders should only be used with MonteCarlo switched on! Update GeneticProperties.txt");
		}
	}

	@Override
	public ActionEnum<P> makeDecision(P decidingAgent) {
		if (decidingAgent.isDead()) return null;

		State<P> state = stateFactory.getCurrentState(decidingAgent);

		if (tree.containsState(state)) {
			return tree.getNextAction(state, getChooseableOptions(decidingAgent));
		} else {
			return rolloutDecider.makeDecision(decidingAgent);
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
			tree.updateState(exp.getStartState(), exp.getActionTaken().actionType, exp.getReward());
			if (tree.updatesLeft() > 0 && !tree.containsState(exp.getEndState()) && !exp.isFinalState) {
				tree.insertState(exp.getEndState(), exp.getPossibleActionsFromEndState());
			}
		}
		// This relies on us going forward through the ER (if backwards, then we still insert the additional state, 
		// but do not update its statistics
	}

}
