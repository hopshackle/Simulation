package hopshackle.simulation.MCTS;

import com.mysql.cj.x.protobuf.MysqlxExpect;
import hopshackle.simulation.*;
import hopshackle.simulation.AgentEvent.Type;
import hopshackle.simulation.games.Game;
import org.javatuples.Triplet;

import java.util.*;
import java.util.stream.Collectors;

public class MCTSMasterDecider<A extends Agent> extends BaseAgentDecider<A> {

    protected Map<Integer, MonteCarloTree<A>> treeMap = new HashMap<>();
    protected BaseStateDecider<A> rolloutDecider;
    private Decider<A> opponentModel;
    private MCTSChildDecider<A> childDecider;
    private int maxRollouts = getPropertyAsInteger("MonteCarloRolloutCount", "99");
    private int maxRolloutsPerOption = getPropertyAsInteger("MonteCarloRolloutPerOption", "50");
    private boolean useAVDForRollout = getProperty("MonteCarloActionValueRollout", "false").equals("true");
    private boolean useAVDForOpponent = getProperty("MonteCarloActionValueOpponentModel", "false").equals("true");
    private boolean reuseOldTree = getProperty("MonteCarloRetainTreeBetweenActions", "false").equals("true");
    private boolean trainRolloutDeciderOverGames, trainRolloutDeciderUsingAllPlayerExperiences;
    private String treeSetting = getProperty("MonteCarloSingleTree", "single");
    private boolean singleTree = treeSetting.equals("single");
    private boolean multiTree = treeSetting.equals("perPlayer");
    private boolean openLoop;
    private long millisecondsPerMove;
    private boolean deciderAsHeuristic;
    private int rolloutLimit;
    private boolean writeGameLog;
    private boolean debug = true;
    private MCTreeProcessor<A> treeProcessor;

    public MCTSMasterDecider(StateFactory<A> stateFactory, BaseStateDecider<A> rolloutDecider, Decider<A> opponentModel) {
        super(stateFactory);
        this.rolloutDecider = rolloutDecider;
        if (rolloutDecider == null)
            this.rolloutDecider = new RandomDecider<A>(stateFactory);
        this.opponentModel = opponentModel;
        if (opponentModel == null)
            this.opponentModel = new RandomDecider<A>(stateFactory);
    }


    public MCTSChildDecider<A> createChildDecider(StateFactory<A> stateFactoryForChild, MonteCarloTree<A> tree, int currentPlayer, boolean opponent) {
        MCTSChildDecider<A> retValue;
        if ((useAVDForRollout && !opponent) || (useAVDForOpponent && opponent))
            retValue = new MCTSChildDecider<>(stateFactoryForChild, tree, new MCActionValueDecider<>(tree, stateFactory, currentPlayer), decProp);
        else
            retValue = new MCTSChildDecider<>(stateFactoryForChild, tree, rolloutDecider, decProp);

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
            if (!openLoop) {
                int before = tree.numberOfStates();
                tree.pruneTree(tree.getStatisticsFor(currentState));
                if (debug) {
                    agent.log("Pruning reduces states in tree from " + before + " to " + tree.numberOfStates());
                }
            } else {
                // openLoop reuse old tree is processed after we have made the next decision
            }
        } else {
            tree.reset();
        }

        tree.insertRoot(currentState);

        int N = Math.min(maxRollouts, maxRolloutsPerOption * chooseableOptions.size());
        if (chooseableOptions.size() == 1) {
            agent.log("Only one action possible...skipping MCTS");
            // return chooseableOptions.get(0);
            // TODO: If we have a time budget, then in the future it may make sense to
            // construct a Tree anyway, as parts may be of relevance in later turns
            N = 1;
        }

        childDecider = createChildDecider(stateFactory, tree, currentPlayer, false);

        int actualI = 0;
        for (int i = 0; i < N; i++) {
            Game<A, ActionEnum<A>> clonedGame = game.clone(agent);
            A clonedAgent = clonedGame.getPlayer(currentPlayer);
            if (openLoop) {
                OpenLoopStateFactory<A> factory = new OpenLoopStateFactory<>(treeSetting, treeMap, clonedGame);
                childDecider = createChildDecider(factory, tree, currentPlayer, false);
            } else {
                // we can use the default one as we do not need to re-initialise the tree pointers
            }
            for (A player : clonedGame.getAllPlayers()) {
                // For each other player in the game, we have to model their behaviour in some way
                if (player != clonedAgent && !singleTree && !multiTree) {
                    if (useAVDForOpponent)
                        player.setDecider(new MCActionValueDecider<A>(tree, this.stateFactory, currentPlayer));
                    else
                        player.setDecider(opponentModel);
                } else {
                    // always use the childDecider if we are openLoop with anything other than ignoreOthers
                    player.setDecider(childDecider);
                }
            }

            clonedGame.playGame(rolloutLimit);
            List<Triplet<State<A>, ActionWithRef<A>, Long>> trajectoryToUse = tree.filterTrajectory(clonedGame.getTrajectory(), agent.getActorRef());
            if (multiTree) {
                treeMap.values().stream().forEach(
                        t -> {
                            t.setUpdatesLeft(1);
                            t.processTrajectory(trajectoryToUse, clonedGame.getFinalScores());
                        }
                );
            } else {
                tree.setUpdatesLeft(1);
                tree.processTrajectory(trajectoryToUse, clonedGame.getFinalScores());
            }
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

        // Then we look at the statistics in the tree for the current state to make a decision
        agent.log(tree.getRootStatistics().toString(debug));
        int[] atDepth = tree.getDepths();
        agent.log(String.format("Tree depths: (%d) %d %d %d %d %d %d %d %d %d %d", atDepth[10], atDepth[0], atDepth[1], atDepth[2], atDepth[3], atDepth[4], atDepth[5], atDepth[6], atDepth[7], atDepth[8], atDepth[9]));
        agent.log(String.format("Visit depths: %d %d %d %d %d %d %d %d %d %d", atDepth[11], atDepth[12], atDepth[13], atDepth[14], atDepth[15], atDepth[16], atDepth[17], atDepth[18], atDepth[19], atDepth[20]));

        ActionEnum<A> best = tree.getBestAction(chooseableOptions, currentPlayer);
        if (best == null) {
            throw new AssertionError("No action chosen");
        }

        treeMap.put(agent.getActorRef(), tree);
        if (writeGameLog) {
            String message = agent.toString() + " " + best.toString() + " (after " + (actualI + 1) + " rollouts)";
            System.out.println(message);
            game.log(tree.getRootStatistics().toString(debug));
            game.log(String.format("Tree depths: (%d) %d %d %d %d %d %d %d %d %d %d", atDepth[10], atDepth[0], atDepth[1], atDepth[2], atDepth[3], atDepth[4], atDepth[5], atDepth[6], atDepth[7], atDepth[8], atDepth[9]));
            game.log(String.format("Visit depths: %d %d %d %d %d %d %d %d %d %d", atDepth[11], atDepth[12], atDepth[13], atDepth[14], atDepth[15], atDepth[16], atDepth[17], atDepth[18], atDepth[19], atDepth[20]));
            game.log(message);
        }

        if (openLoop && reuseOldTree) {
            // we need to apply the decision taken to all relevant trees
            ActionWithRef<A> actionTaken = new ActionWithRef<>(best, agent.getActorRef());
                treeMap.keySet().stream()
                        .filter(p -> (p==1 && singleTree) || !singleTree)
                        .forEach(
                                // TODO: This does not currently take account of any action partial visibility
                                p -> {
                                    MCStatistics<A> rootStats = treeMap.get(p).rootNode;
                                    MCStatistics<A> successorStats = rootStats.getSuccessorNode(actionTaken);
                                    treeMap.get(p).rootNode = successorStats;
                                }
                        );
        }

        return best;
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

    public MonteCarloTree<A> getTree(A agent) {
        if (treeMap.containsKey(agent.getActorRef())) {
            return treeMap.get(agent.getActorRef());
        }
        MonteCarloTree<A> retValue;
        if (openLoop) {
            retValue = new OpenLoopMCTree<>(decProp, agent.getGame().getAllPlayers().size());
            if (multiTree) {
                // use a different tree for all players
                agent.getGame().getAllPlayers().stream()
                        .filter(p -> p != agent)
                        .forEach(p -> treeMap.put(p.getActorRef(), new OpenLoopMCTree<>(decProp, agent.getGame().getAllPlayers().size())));
            }
        } else {
            retValue = new TranspositionTableMCTree<>(decProp, agent.getGame().getAllPlayers().size());
        }
        if (singleTree) {
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
        treeSetting = getProperty("MonteCarloSingleTree", "single");
        singleTree = treeSetting.equals("single");
        multiTree = treeSetting.equals("perPlayer");
        trainRolloutDeciderOverGames = getProperty("MonteCarloTrainRolloutDecider", "false").equals("true");
        trainRolloutDeciderUsingAllPlayerExperiences = getProperty("MonteCarloTrainRolloutDeciderFromAllPlayers", "false").equals("true");
        openLoop = getProperty("MonteCarloOpenLoop", "false").equals("true");
        deciderAsHeuristic = getProperty("MonteCarloRolloutAsHeuristic", "false").equals("true");
        if (treeProcessor == null) treeProcessor = new MCTreeProcessor<A>(dp, this.name);
        rolloutLimit = getPropertyAsInteger("MonteCarloRolloutLimit", "0");
    }

}
