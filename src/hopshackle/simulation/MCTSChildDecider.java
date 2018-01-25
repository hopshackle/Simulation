package hopshackle.simulation;

import java.util.*;

public class MCTSChildDecider<P extends Agent> extends BaseAgentDecider<P> {

	private Decider<P> rolloutDecider;
	private MonteCarloTree<P> tree;
	private boolean RAVE, useLookahead;
	
	public MCTSChildDecider(StateFactory<P> stateFactory, MonteCarloTree<P> tree, Decider<P> rolloutDecider, DeciderProperties prop) {
		super(stateFactory);
		injectProperties(prop);
		localDebug = false;
		this.rolloutDecider = rolloutDecider;
		this.tree = tree;
		useLookahead = getProperty("LookaheadQLearning", "false").equals("true");
		RAVE = !getProperty("MonteCarloRAVE", "false").equals("false");
	}

	@Override
	public ActionEnum<P> makeDecision(P decidingAgent, List<ActionEnum<P>> chooseableOptions) {
		if (decidingAgent.isDead()) return null;

		State<P> state = stateFactory.getCurrentState(decidingAgent);
		if (chooseableOptions.isEmpty()) {
			return null;
		}

		if (tree.containsState(state)) {
			int decidingAgentRef = decidingAgent.getActorRef();
			if (decidingAgent.getGame() != null) {
				P masterAgent = (P) decidingAgent.getGame().getMasterOf(decidingAgent);
				decidingAgentRef = decidingAgent.getGame().getMasterNumber(masterAgent);
			}
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
	public void learnFrom(ExperienceRecord<P> exp, double maxResult) {
		// 'Learning' in this context means updating the MonteCarloTree
		if (tree.containsState(exp.getStartState(useLookahead))) {
			if (!tree.containsState(exp.getEndState()) && tree.updatesLeft() > 0 && !exp.isFinalState) {
				// this will insert the state, so that the update will use the base value...the s', a, s'' update
				// will then update this state using the reward
				tree.insertState(exp.getEndState());
			}
			tree.updateState(exp.getStartState(useLookahead), exp.getActionTaken().getType(), exp.getEndState(), exp.getMonteCarloReward(), exp.getMasterNumber());
		} else if (tree.updatesLeft() > 0) {
			System.out.println("Action Taken: " + exp.actionTaken);
			for (ActionEnum<P> poss : exp.getPossibleActionsFromStartState())
				System.out.println("\tPossible Action: " + poss);
			throw new AssertionError("Tree should contain previous state");
		}
		// This relies on us going forward through the ER (if backwards, then we will error as state is not found, and we
		// have updates left).
		
		if (RAVE) updateRAVE(exp);
		// We do this here, as we need to traverse the chain or ER, and the ER itself is not passed
		// to the MonteCarloTree update.
	}

	private void updateRAVE(ExperienceRecord<P> exp) {
		ActionEnum<P> action = exp.getActionTaken().getType();
		double reward[] = exp.getMonteCarloReward();
		ExperienceRecord<P> previousER = exp.getPreviousRecord();
		while (previousER != null) {
			tree.updateRAVE(previousER.getStartState(useLookahead), action, reward, exp.getMasterNumber());
			previousER = previousER.getPreviousRecord();
		}
	}
	
	@Override
	public void injectProperties(DeciderProperties prop) {
		super.injectProperties(prop);
		RAVE = getProperty("MonteCarloRAVE", "false").equals("true");
	}

	public void setRolloutDecider(Decider<P> rollout) {
		rolloutDecider = rollout;
	}
}
