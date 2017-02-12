package hopshackle.simulation;

import hopshackle.simulation.AgentEvent.Type;

import java.util.*;

public class MCTSMasterDecider<A extends Agent> extends BaseDecider<A> {

	protected Map<A, MonteCarloTree<A>> treeMap = new HashMap<A, MonteCarloTree<A>>();
	protected Decider<A> rolloutDecider;
	private Decider<A> opponentModel;
	private MCTSChildDecider<A> childDecider;
	private static int maxRollouts = SimProperties.getPropertyAsInteger("MonteCarloRolloutCount", "99");
	private static int maxRolloutsPerOption = SimProperties.getPropertyAsInteger("MonteCarloRolloutPerOption", "50");
	private static boolean useAVDForRollout = SimProperties.getProperty("MonteCarloActionValueRollout", "false").equals("true");
	private static boolean useAVDForOpponent = SimProperties.getProperty("MonteCarloActionValueOpponentModel", "false").equals("true");
	private static boolean reuseOldTree = SimProperties.getProperty("MonteCarloRetainTreeBetweenActions", "false").equals("true");
	private static boolean debug = true;

	public MCTSMasterDecider(StateFactory<A> stateFactory, Decider<A> rolloutDecider, Decider<A> opponentModel) {
		super(stateFactory);
		this.rolloutDecider = rolloutDecider;
		this.opponentModel = opponentModel;
	}


	protected MCTSChildDecider<A> createChildDecider(MonteCarloTree<A> tree) {
		if (useAVDForRollout)
			return new MCTSChildDecider<A>(stateFactory, tree, new MCActionValueDecider<A>(tree, stateFactory));
		else 
			return new MCTSChildDecider<A>(stateFactory, tree, rolloutDecider);
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
						//				agent.log("Removing Tree from MCTS decider");
						treeMap.remove(agent);
					}
				}
			});
		}
		MonteCarloTree<A> tree = treeMap.getOrDefault(agent, new MonteCarloTree<A>());
		if (reuseOldTree) {
			int before = tree.numberOfStates();
			tree.pruneTree(currentState.getAsString());
			if (debug) {
				agent.log("Pruning reduces states in tree from " + before + " to " + tree.numberOfStates());
			}
		} else {
			tree.reset();
		}

		OnInstructionTeacher<A> teacher = new OnInstructionTeacher<A>();
		childDecider = createChildDecider(tree);
		teacher.registerDecider(childDecider);
		class FollowOnEventFilter implements EventFilter {
			@Override
			public boolean ignore(AgentEvent event) {
				return (event.getAction() == null) ? false : event.getAction().isFollowOnAction();
			}
		}
		ExperienceRecordCollector<A> erc = new ExperienceRecordCollector<A>(new StandardERFactory<A>(), new FollowOnEventFilter());
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
					if (useAVDForOpponent)
						player.setDecider(new MCActionValueDecider<A>(tree, this.stateFactory));
					else 
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
		agent.log(tree.getStatisticsFor(currentState).toString());
		
		String logFile = agent.toString() + "_" + agent.getWorld().getCurrentTime() + ".log";
		tree.exportToFile(logFile, currentState.getAsString());
		
		agent.log("Now Sweeping...");
		for (int i = 0; i < 10; i++) {
			tree.sweep();
		};
		agent.log(tree.getStatisticsFor(currentState).toString());
		
		ActionEnum<A> best = tree.getBestAction(currentState, chooseableOptions);
		if (best == null) {
			throw new AssertionError("No action chosen");
		}

		
		treeMap.put(agent, tree);
		return best;
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
		treeMap.put(agent, new MonteCarloTree<A>());
		return treeMap.get(agent);
	}

	public Decider<A> getRolloutDecider() {
		return rolloutDecider;
	}

}
