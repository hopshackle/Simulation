package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;
import hopshackle.simulation.AgentEvent.Type;
import hopshackle.simulation.games.Game;
import org.javatuples.Triplet;

import java.util.*;
import java.util.stream.Collectors;

public class MCTSMasterDecider<A extends Agent> extends BaseAgentDecider<A> {

    protected Map<A, MonteCarloTree<A>> treeMap = new HashMap<A, MonteCarloTree<A>>();
    protected Set<String> processedGames = new HashSet<String>();
    protected BaseStateDecider<A> rolloutDecider;
    private Decider<A> opponentModel;
    private MCTSChildDecider<A> childDecider;
    private int maxRollouts = getPropertyAsInteger("MonteCarloRolloutCount", "99");
    private int maxRolloutsPerOption = getPropertyAsInteger("MonteCarloRolloutPerOption", "50");
    private boolean useRolloutForOpponent = getProperty("MonteCarloUseRolloutForOpponent", "false").equals("true");
    private boolean useAVDForRollout = getProperty("MonteCarloActionValueRollout", "false").equals("true");
    private boolean useAVDForOpponent = getProperty("MonteCarloActionValueOpponentModel", "false").equals("true");
    private boolean reuseOldTree = getProperty("MonteCarloRetainTreeBetweenActions", "false").equals("true");
    private boolean trainRolloutDeciderOverGames, trainRolloutDeciderUsingAllPlayerExperiences;
    private boolean singleTree = getProperty("MonteCarloSingleTree", "false").equals("true");
    private boolean openLoop;
    private long millisecondsPerMove;
    private boolean deciderAsHeuristic;
    private int rolloutLimit;
    private boolean writeGameLog;
    private boolean debug = true;
    private double rolloutTemp, rolloutTempChange;
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

    public MCTSChildDecider<A> createChildDecider(MonteCarloTree<A> tree, int currentPlayer, boolean opponent) {
        MCTSChildDecider<A> retValue = null;
        if ((useAVDForRollout && !opponent) || (useAVDForOpponent && opponent))
            retValue = new MCTSChildDecider<A>(stateFactory, tree, new MCActionValueDecider<A>(tree, stateFactory, currentPlayer), decProp);
        else
            retValue = new MCTSChildDecider<A>(stateFactory, tree, rolloutDecider, decProp);

        retValue.setName("Child_MCTS");
        return retValue;
    }

    @Override
    public ActionEnum<A> makeDecision(A agent, List<ActionEnum<A>> chooseableOptions) {

        if (chooseableOptions.size() == 1) {
            agent.log("Only one action possible...skipping MCTS");
            return chooseableOptions.get(0);
            // TODO: If we have a time budget, then in the future it may make sense to
            // construct a Tree anyway, as parts may be of relevance in later turns
        }

        long startTime = System.currentTimeMillis();
        Game<A, ActionEnum<A>> game = agent.getGame();
        int currentPlayer = game.getPlayerNumber(agent);
        State<A> currentState = stateFactory.getCurrentState(agent);
        // We initialise a new tree, and then rollout N times
        // We listen to the ER stream for the cloned agent, and
        // once the game is finished, we process the trajectory to update the MCTree
        // This is not using any Lookahead; just the record of state to state transitions
        if (!treeMap.containsKey(agent)) {
            // we need to listen to the agent, so that on its death we can remove the tree from the map
            // and avoid nasty memory leaks
            agent.addListener(new AgentListener() {
                @Override
                public void processEvent(AgentEvent event) {
                    if (event.getEvent() == Type.DEATH) {
                        treeMap.remove(agent);
                        if (trainRolloutDeciderOverGames && !processedGames.contains(agent.getGame().getRef())) {
                            processedGames.add(agent.getGame().getRef());
                            RawDecider<A> rawDecider = null;
                            if (rolloutDecider instanceof RawDecider) rawDecider = (RawDecider<A>) rolloutDecider;
                            rolloutDecider = treeProcessor.generateDecider(stateFactory, agent.getMaxScore(), rolloutTemp, rawDecider);
                            if (useRolloutForOpponent) opponentModel = rolloutDecider;
                            rolloutTemp *= rolloutTempChange;
                        }
                    }
                }
            });
        }
        MonteCarloTree<A> tree = treeMap.get(agent);
        if (tree == null) {    // i.e. agent has no tree in map, so must be their first turn in a new game
            if (openLoop) {
                tree = new OpenLoopMCTree<A>(decProp, game.getAllPlayers().size());
            } else {
                tree = new TranspositionTableMCTree<A>(decProp, game.getAllPlayers().size());
            }
            if (deciderAsHeuristic) {
                tree.setOfflineHeuristic(rolloutDecider);
            }
        }
        if (reuseOldTree) {
            int before = tree.numberOfStates();
            tree.pruneTree(tree.getStatisticsFor(currentState));
            if (debug) {
                agent.log("Pruning reduces states in tree from " + before + " to " + tree.numberOfStates());
            }
        } else {
            tree.reset();
        }

        //      ExperienceRecordCollector<A> erc = new ExperienceRecordCollector<A>(new StandardERFactory<A>(decProp), null);

        //      OnInstructionTeacher<A> teacher = new OnInstructionTeacher<A>();
        childDecider = createChildDecider(tree, currentPlayer, false);
        //      teacher.registerDecider(childDecider);
        //      teacher.registerToERStream(erc);

        tree.insertRoot(currentState);
        int N = Math.min(maxRollouts, maxRolloutsPerOption * chooseableOptions.size());

        int actualI = 0;
        for (int i = 0; i < N; i++) {
            tree.setUpdatesLeft(1);
            Game<A, ActionEnum<A>> clonedGame = game.clone(agent);
            A clonedAgent = clonedGame.getPlayer(currentPlayer);
            for (A player : clonedGame.getAllPlayers()) {
                // For each other player in the game, we have to model their behaviour in some way
                if (player != clonedAgent) {
                    if (singleTree)
                        player.setDecider(createChildDecider(tree, game.getPlayerNumber(player), true));
                    else if (useAVDForOpponent)
                        player.setDecider(new MCActionValueDecider<A>(tree, this.stateFactory, currentPlayer));
                    else
                        player.setDecider(opponentModel);
                } else {
                    player.setDecider(childDecider);
                }
            }
/*
            if (openLoop) {
                // set open loop references for the cloned game to inherit the same starting states
                ((OpenLoopStateFactory<A>) stateFactory).cloneGame(game, clonedGame);
            }

            if (singleTree) {
                for (A player : clonedGame.getAllPlayers())
                    erc.registerAgentWithReference(player, clonedAgent);
            } else {
                erc.registerAgent(clonedAgent);
            }
            */
            clonedGame.playGame(rolloutLimit);
            List<Triplet<State<A>, ActionWithRef<A>, Long>> trajectoryToUse = new ArrayList();
            if (!singleTree) {
                // filter trajectory to just our actions
                trajectoryToUse = clonedGame.getTrajectory().stream()
                        .filter(t -> t.getValue1().agentRef == clonedAgent.getActorRef()).collect(Collectors.toList());
            } else {
                trajectoryToUse = clonedGame.getTrajectory();
            }
            tree.processTrajectory(trajectoryToUse, clonedGame.getFinalScores());
            //        teacher.teach();

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

        treeMap.put(agent, tree);
        if (writeGameLog) {
            String message = agent.toString() + " " + best.toString() + " (after " + (actualI + 1) + " rollouts)";
            System.out.println(message);
            game.log(tree.getRootStatistics().toString(debug));
            game.log(String.format("Tree depths: (%d) %d %d %d %d %d %d %d %d %d %d", atDepth[10], atDepth[0], atDepth[1], atDepth[2], atDepth[3], atDepth[4], atDepth[5], atDepth[6], atDepth[7], atDepth[8], atDepth[9]));
            game.log(String.format("Visit depths: %d %d %d %d %d %d %d %d %d %d", atDepth[11], atDepth[12], atDepth[13], atDepth[14], atDepth[15], atDepth[16], atDepth[17], atDepth[18], atDepth[19], atDepth[20]));
            game.log(message);
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

    // test method only
    public MonteCarloTree<A> getTree(A agent) {
        if (treeMap.containsKey(agent)) {
            return treeMap.get(agent);
        }
        if (openLoop) {
            treeMap.put(agent, new OpenLoopMCTree<>(decProp, agent.getGame().getAllPlayers().size()));
        } else {
            treeMap.put(agent, new TranspositionTableMCTree<>(decProp, agent.getGame().getAllPlayers().size()));
        }
        return treeMap.get(agent);
    }

    @Override
    public void injectProperties(DeciderProperties dp) {
        super.injectProperties(dp);
        millisecondsPerMove = getPropertyAsInteger("MonteCarloTimePerMove", "1000");
        writeGameLog = getProperty("MonteCarloGameLog", "true").equals("true");
        maxRollouts = getPropertyAsInteger("MonteCarloRolloutCount", "99");
        maxRolloutsPerOption = getPropertyAsInteger("MonteCarloRolloutPerOption", "50");
        rolloutTemp = SimProperties.getPropertyAsDouble("MonteCarloRolloutStartTemperature", "1.0");
        rolloutTempChange = SimProperties.getPropertyAsDouble("MonteCarloRolloutTemperatureChange", "0.5");
        useAVDForRollout = getProperty("MonteCarloActionValueRollout", "false").equals("true");
        useAVDForOpponent = getProperty("MonteCarloActionValueOpponentModel", "false").equals("true");
        reuseOldTree = getProperty("MonteCarloRetainTreeBetweenActions", "false").equals("true");
        singleTree = getProperty("MonteCarloSingleTree", "false").equals("true");
        trainRolloutDeciderOverGames = getProperty("MonteCarloTrainRolloutDecider", "false").equals("true");
        trainRolloutDeciderUsingAllPlayerExperiences = getProperty("MonteCarloTrainRolloutDeciderFromAllPlayers", "false").equals("true");
        openLoop = getProperty("MonteCarloOpenLoop", "false").equals("true");
        /*
        if (openLoop && !(stateFactory instanceof OpenLoopStateFactory)) {
            // we override the provided state factory TODO: or, possibly keep both
            if (singleTree) {
                this.stateFactory = OpenLoopStateFactory.newInstanceGameLevelStates();
            } else {
                this.stateFactory = OpenLoopStateFactory.newInstance();
            }
        }
        */
        deciderAsHeuristic = getProperty("MonteCarloRolloutAsHeuristic", "false").equals("true");
        if (treeProcessor == null) treeProcessor = new MCTreeProcessor<A>(dp, this.name);
        rolloutLimit = getPropertyAsInteger("MonteCarloRolloutLimit", "0");
    }

}
