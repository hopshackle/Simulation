package hopshackle.simulation;

import java.util.*;

public class MCTSChildDecider<P extends Agent, A extends ActionEnum<P>> extends BaseDecider<P> {

	private Decider<P> rolloutDecider;
	private  MonteCarloTree<P, A> tree;
	
	public MCTSChildDecider(StateFactory<P> stateFactory, List<A> actions, MonteCarloTree<P, A> tree, Decider<P> rolloutDecider) {
		super(stateFactory, actions);
		this.rolloutDecider = rolloutDecider;
		this.tree = tree;
	}
	
	public ActionEnum<P> makeDecision(P decidingAgent, Game<P, A> game) {
		if (decidingAgent.isDead()) return null;

		State<P> state = stateFactory.getCurrentState(decidingAgent);
	
		if (tree.containsState(state)) {
			return tree.getNextAction(state);
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

	@SuppressWarnings("unchecked")
	@Override
	public void learnFrom(ExperienceRecord<P> exp, double maxResult) {
		// 'Learning' in this context means updating the MonteCarloTree
		if (tree.containsState(exp.getStartState())) {
			tree.updateState(exp.getStartState(), (A) exp.getActionTaken().actionType, exp.getReward());
			if (tree.updatesLeft() > 0 && !tree.containsState(exp.getEndState())) {
				tree.insertState(exp.getEndState(), (List<A>) exp.getPossibleActionsFromEndState());
			}
		}
		// This relies on us going forward through the ER (if backwards, then we still insert the additional state, 
		// but do not update its statistics
	}

}
