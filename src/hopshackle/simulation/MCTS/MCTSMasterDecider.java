package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;
import hopshackle.simulation.metric.StatsCollator;

import java.util.*;

public class MCTSMasterDecider<A extends Agent> extends BaseAgentDecider<A> {

    protected Map<Integer, MonteCarloTree<A>> treeMap = new HashMap<>();
    protected Game<A, ActionEnum<A>> masterGame;
    protected BaseStateDecider<A> rolloutDecider;
    protected Decider<A> opponentModel;
    private int maxRollouts = getPropertyAsInteger("MonteCarloRolloutCount", "99");
    private int maxRolloutsPerOption = getPropertyAsInteger("MonteCarloRolloutPerOption", "50");
    protected boolean useAVDForRollout = getProperty("MonteCarloActionValueRollout", "false").equals("true");
    protected boolean useAVDForOpponent = getProperty("MonteCarloActionValueOpponentModel", "false").equals("true");
    protected boolean reuseOldTree = getProperty("MonteCarloRetainTreeBetweenActions", "false").equals("true");
    private boolean trainRolloutDeciderOverGames, trainRolloutDeciderUsingAllPlayerExperiences;
    protected String treeSetting = getProperty("MonteCarloTree", "single");
    protected boolean singleTree = treeSetting.equals("single");
    protected boolean multiTree = treeSetting.equals("perPlayer");
    protected boolean openLoop, MRISRootConsistency;
    private long millisecondsPerMove;
    protected boolean deciderAsHeuristic;
    private boolean writeGameLog;
    protected boolean debug = false;
    private MCTreeProcessor<A> treeProcessor;
    private DatabaseWriter<MCTSDecision> writer;
    private String writerSuffix;

    public MCTSMasterDecider(Game<A, ActionEnum<A>> masterGame, StateFactory<A> stateFactory, BaseStateDecider<A> rolloutDecider, Decider<A> opponentModel) {
        super(stateFactory);
        this.rolloutDecider = rolloutDecider;
        if (rolloutDecider == null)
            this.rolloutDecider = new RandomDecider<>(stateFactory);
        this.opponentModel = opponentModel;
        if (opponentModel == null)
            this.opponentModel = new RandomDecider<>(stateFactory);
        this.masterGame = masterGame;
    }

    public void setWriter(DatabaseWriter<MCTSDecision> dbw, String suffix) {
        if (writer != null)
            writer.writeBuffer();
        writerSuffix = suffix;
        writer = dbw;
    }

    public MCTSChildDecider<A> createChildDecider(Game clonedGame, MonteCarloTree<A> tree, int currentPlayer, boolean opponent) {
        MCTSChildDecider<A> retValue;

        if ((useAVDForRollout && !opponent) || (useAVDForOpponent && opponent))
            retValue = new MCTSChildDecider<>(stateFactory, tree, new MCActionValueDecider<>(tree, stateFactory, currentPlayer), decProp);
        else
            retValue = new MCTSChildDecider<>(stateFactory, tree, rolloutDecider, decProp);

        retValue.setName("Child_MCTS");
        return retValue;
    }

    @Override
    public ActionEnum<A> makeDecision(A agent, List<ActionEnum<A>> chooseableOptions) {

        long startTime = System.currentTimeMillis();
        Game<A, ActionEnum<A>> game = agent.getGame();
        int currentPlayer = game.getPlayerNumber(agent);

        // We initialise a new tree, and then rollout N times
        // once the game is finished, we process the trajectory to update the MCTree
        // This is not using any Lookahead; just the record of state to state transitions
        MonteCarloTree<A> tree = getTree(agent);

        State<A> currentState = stateFactory.getCurrentState(agent);
        if (reuseOldTree) {
            pruneTree(agent);
        } else {
            treeMap.values().forEach(MonteCarloTree::reset);
        }

        tree.insertRoot(currentState);

        int N = Math.min(maxRollouts, maxRolloutsPerOption * chooseableOptions.size());
        int nodesAtStart = tree.numberOfStates();
        if (chooseableOptions.size() == 1) {
            agent.log("Only one action possible...skipping MCTS");
            // return chooseableOptions.get(0);
            // TODO: If we have a time budget, then in the future it may make sense to
            // construct a Tree anyway, as parts may be of relevance in later turns
            N = 1;
        }

        int actualI = 0;
        for (int i = 0; i < N; i++) {
            Game<A, ActionEnum<A>> clonedGame = game.clone();
            preIterationProcessing(clonedGame, game);
            executeSearch(clonedGame);
            postIterationProcessing(clonedGame);
            long now = System.currentTimeMillis();
            actualI = i;
            if (millisecondsPerMove > 0 && now >= startTime + millisecondsPerMove) break;
        }

        if (trainRolloutDeciderOverGames) {
            treeProcessor.processTree(tree);
            if (trainRolloutDeciderUsingAllPlayerExperiences) {
                for (A p : game.getAllPlayers()) {
                    Decider<A> decider = p.getDecider();
                    if (decider != this && decider instanceof MCTSMasterDecider<?>) {
                        MCTSMasterDecider<A> otherD = (MCTSMasterDecider<A>) decider;
                        otherD.treeProcessor.processTree(tree);
                    }
                }
            }
        }

        ActionEnum<A> best = tree.getBestAction(chooseableOptions, currentPlayer);
        if (best == null) {
            throw new AssertionError("No action chosen");
        }
        int nodesExpanded = tree.numberOfStates() - nodesAtStart;
        MCTSDecision decision = new MCTSDecision(tree, actualI, best, this.name, nodesExpanded, game.getTime());
        if (writer != null) writer.write(decision, writerSuffix);

        // Then we look at the statistics in the tree for the current state to make a decision
        int[] atDepth = decision.depths;
        agent.log(tree.getRootStatistics().toString(debug));
        agent.log(String.format("Tree depths: (%d) %d %d %d %d %d %d %d %d %d %d", atDepth[10], atDepth[0], atDepth[1], atDepth[2], atDepth[3], atDepth[4], atDepth[5], atDepth[6], atDepth[7], atDepth[8], atDepth[9]));
        agent.log(String.format("Visit depths: %d %d %d %d %d %d %d %d %d %d", atDepth[11], atDepth[12], atDepth[13], atDepth[14], atDepth[15], atDepth[16], atDepth[17], atDepth[18], atDepth[19], atDepth[20]));
        Map<String, Double> newStats = new HashMap<>();
        newStats.put(toString() + "|MAX_DEPTH", (double) decision.maxDepth);
        newStats.put(toString() + "|ITERATIONS", (double) actualI);
        newStats.put(toString() + "|NODES", (double) nodesExpanded);
        StatsCollator.addStatistics(newStats);

        treeMap.put(agent.getActorRef(), tree);
        if (writeGameLog) {
            String message = agent.toString() + " " + best.toString() + " (after " + (actualI + 1) + " rollouts)";
            //      System.out.println(message);
            game.log(tree.getRootStatistics().toString(debug));
            game.log(String.format("Tree depths: (%d) %d %d %d %d %d %d %d %d %d %d", atDepth[10], atDepth[0], atDepth[1], atDepth[2], atDepth[3], atDepth[4], atDepth[5], atDepth[6], atDepth[7], atDepth[8], atDepth[9]));
            game.log(String.format("Visit depths: %d %d %d %d %d %d %d %d %d %d", atDepth[11], atDepth[12], atDepth[13], atDepth[14], atDepth[15], atDepth[16], atDepth[17], atDepth[18], atDepth[19], atDepth[20]));
            game.log(message);
        }

        return best;
    }

    protected void pruneTree(A agent) {
        State<A> currentState = stateFactory.getCurrentState(agent);
        MonteCarloTree<A> tree = treeMap.get(agent.getActorRef());
        int before = tree.numberOfStates();
        tree.pruneTree(tree.getStatisticsFor(currentState));
        if (debug) {
            log("Pruning reduces states in tree from " + before + " to " + tree.numberOfStates());
        }
    }

    protected void executeSearch(Game<A, ActionEnum<A>> clonedGame) {
        MonteCarloTree<A> tree = getTree(clonedGame.getCurrentPlayer());
        int currentPlayer = clonedGame.getCurrentPlayerRef();

        MCTSChildDecider<A> childDecider = createChildDecider(clonedGame, tree, currentPlayer, false);

        if (useAVDForOpponent)
            opponentModel = new MCActionValueDecider<>(tree, stateFactory, currentPlayer);

        MCTSUtilities.launchGame(treeMap, clonedGame, childDecider, opponentModel, decProp);
    }

    protected void preIterationProcessing(Game clonedGame, Game rootGame) {
        int perspective = clonedGame.getCurrentPlayerRef();
        clonedGame.redeterminise(perspective, perspective, Optional.empty());
        // IS-MCTS, we redeterminise once at the start of each iteration
    }

    protected void postIterationProcessing(Game clonedGame) {
        // Nothing as default
    }

    @Override
    public double valueOption(ActionEnum<A> option, A decidingAgent) {
        return 0;
    }

    @Override
    public double valueOption(ActionEnum<A> option, State<A> state) {
        return 0;
    }

    @Override
    public void learnFrom(ExperienceRecord<A> exp, double maxResult) {
    }

    protected MonteCarloTree<A> getEmptyTree(int playerCount) {
        return new TranspositionTableMCTree<>(decProp, playerCount);
    }

    public MonteCarloTree<A> getTree(A agent) {
        if (!treeMap.containsKey(agent.getActorRef())) {
            int playerCount = agent.getGame().getAllPlayers().size();
            MonteCarloTree<A> retValue = getEmptyTree(playerCount);
            if (multiTree) {
                // use a different tree for all players
                agent.getGame().getAllPlayers().stream()
                        .forEach(p -> treeMap.put(p.getActorRef(), getEmptyTree(playerCount)));
                if (deciderAsHeuristic)
                    treeMap.values().forEach(t -> t.setOfflineHeuristic(rolloutDecider));
            } else if (singleTree) {
                // use the same tree for all players
                Game<A, ActionEnum<A>> game = agent.getGame();
                for (A player : game.getAllPlayers())
                    treeMap.put(player.getActorRef(), retValue);
            } else {
                treeMap.put(agent.getActorRef(), retValue);
            }
            if (deciderAsHeuristic) {
                retValue.setOfflineHeuristic(rolloutDecider);
            }
        }
        return treeMap.get(agent.getActorRef());
    }

    @Override
    public void injectProperties(DeciderProperties dp) {
        super.injectProperties(dp);
        millisecondsPerMove = getPropertyAsInteger("MonteCarloTimePerMove", "1000");
        writeGameLog = getProperty("MonteCarloGameLog", "true").equals("true");
        maxRollouts = getPropertyAsInteger("MonteCarloRolloutCount", "99");
        maxRolloutsPerOption = getPropertyAsInteger("MonteCarloRolloutPerOption", "50");
        useAVDForRollout = getProperty("MonteCarloActionValueRollout", "false").equals("true");
        useAVDForOpponent = getProperty("MonteCarloActionValueOpponentModel", "false").equals("true");
        reuseOldTree = getProperty("MonteCarloRetainTreeBetweenActions", "false").equals("true");
        treeSetting = getProperty("MonteCarloTree", "single");
        singleTree = treeSetting.equals("single");
        multiTree = treeSetting.equals("perPlayer");
        trainRolloutDeciderOverGames = getProperty("MonteCarloTrainRolloutDecider", "false").equals("true");
        trainRolloutDeciderUsingAllPlayerExperiences = getProperty("MonteCarloTrainRolloutDeciderFromAllPlayers", "false").equals("true");
        openLoop = getProperty("MonteCarloOpenLoop", "false").equals("true");
        deciderAsHeuristic = getProperty("MonteCarloRolloutAsHeuristic", "false").equals("true");
        MRISRootConsistency = getProperty("MonteCarloMRISRootConsistency", "false").equals("true");
        if (treeProcessor == null) treeProcessor = new MCTreeProcessor<>(dp, this.name);
        masterGame.getAllPlayers().forEach(this::getTree);
    }

}
