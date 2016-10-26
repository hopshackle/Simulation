package hopshackle.simulation;

import java.util.List;

public class MCTSMasterDecider<A extends Agent> extends BaseDecider<A> {
	
	private MonteCarloTree<A, ActionEnum<A>> tree;
	private Decider<A> rolloutDecider;
	private MCTSChildDecider<A, ActionEnum<A>> childDecider;
	private static int N = SimProperties.getPropertyAsInteger("MonteCarloRolloutCount", "99");

	public MCTSMasterDecider(StateFactory<A> stateFactory, List<? extends ActionEnum<A>> actions, Decider<A> rolloutDecider) {
		super(stateFactory, actions);
		this.rolloutDecider = rolloutDecider;
	}
	
	@Override
	public ActionEnum<A> makeDecision(A agent) {
		// Hmm. We therefore need a way to get the agent's game context
		Game<A, ActionEnum<A>> game = agent.getGame();
		int currentPlayer = game.getPlayerNumber(agent);
		if (currentPlayer != game.getCurrentPlayerNumber()) 
			throw new AssertionError("Incorrect current player for Game");
		// We initialise a new tree, and then rollout N times
		// We also listen to the ER stream for the cloned agent, and then
		// once the game is finished, we use this to update the MonteCarloTree
		// using an OnInstructionTeacher.
		// This is not using any Lookahead; just the record of state to state transitions
		tree = new MonteCarloTree<A, ActionEnum<A>>();
		OnInstructionTeacher<A> teacher = new OnInstructionTeacher<A>();
		childDecider = new MCTSChildDecider<A, ActionEnum<A>>(stateFactory, actionSet, tree, rolloutDecider);
		// TODO: Need to think here. This only works if all the other players are using a model decider.
		// SO I might need a way to inject the assumed opponent model here. If in the master game all the players
		// are actually using their own MCTS (or other) learning engines.
		teacher.registerDecider(childDecider);
		ExperienceRecordCollector<A> erc = new ExperienceRecordCollector<A>(new StandardERFactory<A>());
		agent.setDecider(childDecider);		// We replace this decider for the agent
		State<A> currentState = stateFactory.getCurrentState(agent);
		for (int i = 0; i < N; i++) {
			tree.setUpdatesLeft(1);
			Game<A, ActionEnum<A>> clonedGame = game.clone(agent);
			A clonedAgent = clonedGame.getPlayer(currentPlayer);
			erc.registerAgent(clonedAgent);
			clonedGame.playGame();
			// TODO: I need this to be properly MonteCarlo; i.e. just use the final score for all training!
			teacher.teach();
		}
		agent.setDecider(this); // we reset this decider for the agent
		
		// Then we look at the statistics in the tree for the current state to make a decision
		return tree.getBestAction(currentState);
	}

	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent) {
		return 0;
	}

	@Override
	public void learnFrom(ExperienceRecord<A> exp, double maxResult) {
	}

}
