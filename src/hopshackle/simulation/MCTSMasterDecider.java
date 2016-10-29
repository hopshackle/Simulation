package hopshackle.simulation;

import java.util.List;

public class MCTSMasterDecider<A extends Agent> extends BaseDecider<A> {
	
	private MonteCarloTree<A, ActionEnum<A>> tree;
	private Decider<A> rolloutDecider, opponentModel;
	private MCTSChildDecider<A> childDecider;
	private static int N = SimProperties.getPropertyAsInteger("MonteCarloRolloutCount", "99");

	public MCTSMasterDecider(StateFactory<A> stateFactory, List<? extends ActionEnum<A>> actions, 
			Decider<A> rolloutDecider, Decider<A> opponentModel) {
		super(stateFactory, actions);
		this.rolloutDecider = rolloutDecider;
		this.opponentModel = opponentModel;
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
		childDecider = new MCTSChildDecider<A>(stateFactory, actionSet, tree, rolloutDecider);
		teacher.registerDecider(childDecider);
		ExperienceRecordCollector<A> erc = new ExperienceRecordCollector<A>(new StandardERFactory<A>());
		teacher.registerToERStream(erc);
		State<A> currentState = stateFactory.getCurrentState(agent);
		tree.insertState(currentState, getChooseableOptions(agent));
		for (int i = 0; i < N; i++) {
			tree.setUpdatesLeft(1);
			Game<A, ActionEnum<A>> clonedGame = game.clone(agent);
			A clonedAgent = clonedGame.getPlayer(currentPlayer);
			for (A player : clonedGame.getAllPlayers()) {
				if (player != clonedAgent) {
					player.setDecider(opponentModel);
				} else {
					player.setDecider(childDecider);
				}
				// For each other player in the game, we have to model their behaviour in some way
				// For this player (from whose perspective the MonteCarloTree is being constructed)
				// we use MCTSChildDecider
			}
			erc.registerAgent(clonedAgent);
			clonedGame.playGame();
			teacher.teach();
		}
	
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
	
	public MonteCarloTree<A, ActionEnum<A>> getTree() {
		return tree;
	}

}
