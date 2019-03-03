package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;
import org.javatuples.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public abstract class MonteCarloTree<P extends Agent> {

    protected String UCTType;
    protected Map<String, Integer> stateRefs = new HashMap<String, Integer>();
    protected int updatesLeft, maxActors;
    private List<Map<String, MCData>> actionValues;
    private static AtomicLong idFountain = new AtomicLong(1);
    protected int nextRef = 1;
    protected MCStatistics<P> rootNode;
    private long id;
    private EntityLog entityLogger;
    protected boolean debug = false;
    protected DeciderProperties properties;
    private BaseStateDecider<P> offlineHeuristic = new noHeuristic<>();
    protected String treeSetting;
    protected boolean MAST, RAVE, singleTree, multiTree, ignoreOthers;
    protected double RAVE_C, gamma, timePeriod;
    protected Optional<MonteCarloTree<P>> parent = Optional.empty();

    /**
     * Basic constructor. Note the mandatory injection of properties.
     * The only property currently used locally (31-May-17) is UCTType to allow for MC, V or Q style value updates.
     * (Experimental evidence suggests that only MC is significantly better than the others.)
     * However it is also used for injection downstream into the MCstatistics and MCData objects that track action
     * statistics. (The injection into MCStatistics is not explicit – any MCStatistics object requires a
     * Monte Carlo Tree at instantiation, which is really only for access to properties.)
     */
    public MonteCarloTree(DeciderProperties properties) {
        this(properties, 1);
    }


    /**
     * This is used to instantiate a tree with a different rootNode
     * Almost certainly only relevant for OpenLoop trees, in which the tree structure is
     * represented by pointers from node to node, and not a HashMap as in a Transposition Table tree
     * <p>
     * All the MCStatistics nodes in the tree will point to the parentTree for shared functions such as:
     * - updatesLeft
     * - MAST and other heuristics
     */
    public MonteCarloTree(MonteCarloTree<P> parentTree, MCStatistics<P> subTreeRoot) {
        this(parentTree.properties, parentTree.maxActors);
        parent = Optional.of(parentTree);
        actionValues = parentTree.actionValues;
        offlineHeuristic = parentTree.offlineHeuristic;
        entityLogger = parentTree.entityLogger;
        stateRefs = parentTree.stateRefs;
        nextRef = parentTree.nextRef;
        rootNode = subTreeRoot;
    }

    /**
     * Constructor that specifies additionally the number of agents in the game.
     * This determines the length of the reward vectors that are tracked.
     * The main purpose of this is to support a single tree to be used for all players, so that at each node a
     * decision can be made based on the prospective reward to the deciding agent.
     */
    public MonteCarloTree(DeciderProperties properties, int numberOfAgents) {
        maxActors = numberOfAgents;
        actionValues = new ArrayList(numberOfAgents);       // for MAST Heuristic
        for (int i = 0; i < numberOfAgents; i++)
            actionValues.add(i, new HashMap());
        this.properties = properties;
        UCTType = properties.getProperty("MonteCarloUCTType", "MC");
        MAST = properties.getProperty("MonteCarloMAST", "false").equals("true");
        RAVE = properties.getProperty("MonteCarloRAVE", "false").equals("true");
        RAVE_C = properties.getPropertyAsDouble("MonteCarloRAVEExploreConstant", "0.0");
        treeSetting = properties.getProperty("MonteCarloTree", "single");
        singleTree = treeSetting.equals("single");
        multiTree = treeSetting.equals("perPlayer");
        ignoreOthers = treeSetting.endsWith("ignoreOthers");
        // Now we add in the heuristic to use, if any
        if (MAST) {
            offlineHeuristic = new MASTHeuristic<>(this);
        } else if (RAVE) {
            offlineHeuristic = new RAVEHeuristic<>(this, RAVE_C);
        } else {
            offlineHeuristic = new noHeuristic<P>();
        }
        id = idFountain.getAndIncrement();
        timePeriod = properties.getPropertyAsDouble("TimePeriodForGamma", "1000");
        gamma = properties.getPropertyAsDouble("Gamma", "1.0");

    }

    /**
     * This resets all statistics to null except for the action values and counts (if using the CADIA heuristic here).
     */
    public void reset() {
        stateRefs.clear();
        if (entityLogger != null) {
            entityLogger.close();
            entityLogger = null;
        }
        id = idFountain.getAndIncrement();
        nextRef = 1;
    }

    /**
     * Sets the number of updates still to be made on the tree during a rollout.
     * The count is automatically decremented every time insertState() is called.
     */
    public void setUpdatesLeft(int n) {
        if (parent.isPresent())
            parent.get().setUpdatesLeft(n);
        else
            updatesLeft = n;
    }

    public int updatesLeft() {
        if (parent.isPresent())
            return parent.get().updatesLeft;
        else
            return updatesLeft;
    }

    public abstract void processTrajectory(List<Triplet<State<P>, ActionWithRef<P>, Long>> trajectory,
                                           double[] finalScores, MCStatistics<P> startNode, MCStatistics<P> stopNode);

    protected void updateActionValues(ActionEnum<P> action, int actingPlayer, double reward) {
        String actionAsString = action.toString();
        double[] actionReward = new double[1];
        actionReward[0] = reward;
        Map<String, MCData> av = actionValues.get(actingPlayer - 1);
        if (av.containsKey(actionAsString)) {
            av.put(actionAsString, new MCData(av.get(actionAsString), actionReward));
        } else {
            av.put(actionAsString, new MCData(actionAsString, actionReward, properties));
        }
    }

    // for test only
    public ActionEnum<P> getNextAction(State<P> state, List<ActionEnum<P>> possibleActions) {
        return getNextAction(state, possibleActions, state.getActorRef());
    }

    public abstract ActionEnum<P> getNextAction(State<P> state, List<ActionEnum<P>> possibleActions, int decidingAgent);

    public ActionEnum<P> getBestAction(List<ActionEnum<P>> possibleActions, int decidingAgent) {
        return rootNode.getBestAction(possibleActions, decidingAgent);

    }

    public abstract List<MCStatistics<P>> getAllNodesWithMinVisits(int minV);

    public double getActionValue(String k, int playerRef) {
        if (actionValues.get(playerRef - 1).containsKey(k)) {
            return actionValues.get(playerRef - 1).get(k).mean[0];
        }
        return 0.0;
    }

    public abstract int numberOfStates();

    public abstract void pruneTree(MCStatistics<P> newRoot);

    public abstract void insertRoot(State<P> state);

    public MCStatistics getRootStatistics() {
        return rootNode;
    }

    public abstract MCStatistics<P> getStatisticsFor(State<P> state);

    public abstract boolean withinTree(State<P> state);

    public abstract int[] getDepths();

    public void setOfflineHeuristic(BaseStateDecider<P> newHeuristic) {
        offlineHeuristic = newHeuristic;
    }

    public BaseStateDecider<P> getOfflineHeuristic() {
        return offlineHeuristic;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public abstract String toString(boolean full);

    protected String stateRef(String stateDescription) {
        return stateRefs.getOrDefault(stateDescription, 0).toString();
    }

    public void log(String s) {
        if (entityLogger == null) {
            entityLogger = new EntityLog("MCTree_" + id, null);
        }
        entityLogger.log(s);
    }

    protected double[] toArray(double single) {
        double[] retValue = new double[1];
        retValue[0] = single;
        return retValue;
    }

}

class noHeuristic<P extends Agent> extends BaseStateDecider<P> {

    public noHeuristic() {
        super(null);
    }

    @Override
    public double valueOption(ActionEnum<P> option, State<P> state) {
        return 0;
    }

    @Override
    public List<Double> valueOptions(List<ActionEnum<P>> options, State<P> state) {
        List<Double> retValue = new ArrayList<Double>(options.size());
        for (int i = 0; i < options.size(); i++)
            retValue.add(0.0);
        return retValue;
    }

    @Override
    public void learnFrom(ExperienceRecord<P> exp, double maxResult) {
    }

}