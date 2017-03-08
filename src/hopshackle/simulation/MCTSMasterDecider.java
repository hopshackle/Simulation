package hopshackle.simulation;

import hopshackle.simulation.AgentEvent.Type;

import java.util.*;

public class MCTSMasterDecider<A extends Agent> extends BaseDecider<A> {

	protected Map<A, MonteCarloTree<A>> treeMap = new HashMap<A, MonteCarloTree<A>>();
	protected Decider<A> rolloutDecider;
	private Decider<A> opponentModel;
	private MCTSChildDecider<A> childDecider;
	private int maxRollouts = getPropertyAsInteger("MonteCarloRolloutCount", "99");
	private int maxRolloutsPerOption = getPropertyAsInteger("MonteCarloRolloutPerOption", "50");
	private boolean useAVDForRollout = getProperty("MonteCarloActionValueRollout", "false").equals("true");
	private boolean useAVDForOpponent = getProperty("MonteCarloActionValueOpponentModel", "false").equals("true");
	private boolean reuseOldTree = getProperty("MonteCarloRetainTreeBetweenActions", "false").equals("true");
	private String sweepMethodology = getProperty("MonteCarloSweep", "terminal");
	private int sweepIterations = getPropertyAsInteger("MonteCarloSweepIterations", "0");
	private boolean singleTree = getProperty("MonteCarloSingleTree", "false").equals("true");
	private boolean debug = false;

	public MCTSMasterDecider(StateFactory<A> stateFactory, Decider<A> rolloutDecider, Decider<A> opponentModel) {
		super(stateFactory);
		this.rolloutDecider = rolloutDecider;
		if (rolloutDecider == null)
			this.rolloutDecider = new RandomDecider<A>(stateFactory);
		this.opponentModel = opponentModel;
		if (opponentModel == null)
			this.opponentModel = new RandomDecider<A>(stateFactory);
	}


	protected MCTSChildDecider<A> createChildDecider(MonteCarloTree<A> tree, int currentPlayer, boolean opponent) {
		MCTSChildDecider<A> retValue = null;
		if ((useAVDForRollout && !opponent) || (useAVDForOpponent && opponent))
			retValue = new MCTSChildDecider<A>(stateFactory, tree, new MCActionValueDecider<A>(tree, stateFactory, currentPlayer, decProp), decProp);
		else 
			retValue = new MCTSChildDecider<A>(stateFactory, tree, rolloutDecider, decProp);

		retValue.setName("Child_MCTS");
		return retValue;
	}

	@Override
	public ActionEnum<A> makeDecision(A agent, List<ActionEnum<A>> chooseableOptions) {
		Game<A, ActionEnum<A>> game = agent.getGame();
		int currentPlayer = game.getPlayerNumber(agent);
		State<A> currentState = stateFactory.getCurrentState(agent);
		// We initialise a new tree, and then rollout N times
		// We also listen to the ER stream for the cloned agent, and then
		// once the game is finished, we use this to update the MonteCarloTree
		// using an OnInstructionTeacher.
		// This is not using any Lookahead; just the record of state to state transitions
		if (!treeMap.containsKey(agent)) {
			// we need to listen to the agent, so that on its death we can remove the tree from the map
			// and avoid nasty memory leaks
			agent.addListener(new AgentListener() {
				@Override
				public void processEvent(AgentEvent event) {
					if (event.getEvent() == Type.DEATH) {
						treeMap.remove(agent);
						//			agent.log("Removing Tree from MCTS decider, leaving " + treeMap.size());
					}
				}
			});
		}
		MonteCarloTree<A> tree = treeMap.get(agent);
		if (tree == null) tree = new MonteCarloTree<A>(decProp, game.getAllPlayers().size());
		if (reuseOldTree) {
			int before = tree.numberOfStates();
			tree.pruneTree(currentState.getAsString());
			if (debug) {
				agent.log("Pruning reduces states in tree from " + before + " to " + tree.numberOfStates());
			}
		} else {
			tree.reset();
		}

		class FollowOnEventFilter implements EventFilter {
			@Override
			public boolean ignore(AgentEvent event) {
				return (event.getAction() == null) ? false : event.getAction().isFollowOnAction();
			}
		}
		ExperienceRecordCollector<A> erc = new ExperienceRecordCollector<A>(new StandardERFactory<A>(decProp), new FollowOnEventFilter());

		OnInstructionTeacher<A> teacher = new OnInstructionTeacher<A>();
		childDecider = createChildDecider(tree, currentPlayer, false);
		teacher.registerDecider(childDecider);
		teacher.registerToERStream(erc);

		if (chooseableOptions.size() == 1) {
			agent.log("Only one action possible...skipping MCTS");
			return chooseableOptions.get(0);
			// TODO: If we have a time budget, then in the future it may make sense to 
			// construct a Tree anyway, as parts may be of relevance in later turns
		}
		tree.insertState(currentState, chooseableOptions);
		int N = Math.min(maxRollouts, maxRolloutsPerOption * chooseableOptions.size());
		for (int i = 0; i < N; i++) {
			tree.setUpdatesLeft(1);
			Game<A, ActionEnum<A>> clonedGame = game.clone(agent);
			A clonedAgent = clonedGame.getPlayer(currentPlayer);
			for (A player : clonedGame.getAllPlayers()) {
				if (player != clonedAgent) {
					if (singleTree)
						player.setDecider(createChildDecider(tree, game.getPlayerNumber(player), true));
					else if (useAVDForOpponent)
						player.setDecider(new MCActionValueDecider<A>(tree, this.stateFactory, currentPlayer, this.decProp));
					else 
						player.setDecider(opponentModel);
				} else {
					player.setDecider(childDecider);
				}
				// For each other player in the game, we have to model their behaviour in some way
				// For this player (from whose perspective the MonteCarloTree is being constructed)
				// we use MCTSChildDecider
			}

			if (singleTree) {
				for (A player : clonedGame.getAllPlayers()) 
					erc.registerAgentWithReference(player, clonedAgent);
			} else {
				erc.registerAgent(clonedAgent);
			}
			clonedGame.playGame();
			teacher.teach();

			if (sweepMethodology.equals("iteration")) {
				sweep(tree);
			}
		}

		if (sweepMethodology.equals("terminal")) {
			sweep(tree);
			//			logFile = agent.toString() + "_" + agent.getWorld().getCurrentTime() + "_postSweep.log";
			//			tree.exportToFile(logFile, currentState.getAsString());
		}
		// Then we look at the statistics in the tree for the current state to make a decision
		agent.log(tree.getStatisticsFor(currentState).toString(debug));

		ActionEnum<A> best = tree.getBestAction(currentState, chooseableOptions);
		if (best == null) {
			throw new AssertionError("No action chosen");
		}

		treeMap.put(agent, tree);
		return best;
	}

	private void sweep(MonteCarloTree<A> tree) {
		for (int i = 0; i < sweepIterations; i++) {
			tree.sweep();
		};
	}

	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent) {
		return 0;
	}

	@Override
	public void learnFrom(ExperienceRecord<A> exp, double maxResult) {
	}

	public MonteCarloTree<A> getTree(A agent) {
		if (treeMap.containsKey(agent)) {
			return treeMap.get(agent);
		}
		treeMap.put(agent, new MonteCarloTree<A>(decProp, agent.getGame().getAllPlayers().size()));
		return treeMap.get(agent);
	}

	public Decider<A> getRolloutDecider() {
		return rolloutDecider;
	}

	@Override
	public void injectProperties(DeciderProperties dp) {
		super.injectProperties(dp);
		maxRollouts = getPropertyAsInteger("MonteCarloRolloutCount", "99");
		maxRolloutsPerOption = getPropertyAsInteger("MonteCarloRolloutPerOption", "50");
		useAVDForRollout = getProperty("MonteCarloActionValueRollout", "false").equals("true");
		useAVDForOpponent = getProperty("MonteCarloActionValueOpponentModel", "false").equals("true");
		reuseOldTree = getProperty("MonteCarloRetainTreeBetweenActions", "false").equals("true");
		sweepMethodology = getProperty("MonteCarloSweep", "terminal");
		sweepIterations = getPropertyAsInteger("MonteCarloSweepIterations", "0");
		singleTree = getProperty("MonteCarloSingleTree", "false").equals("true");
	}

}
