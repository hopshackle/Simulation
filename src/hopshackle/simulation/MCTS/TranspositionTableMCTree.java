package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class TranspositionTableMCTree<P extends Agent> extends MonteCarloTree<P> {

    private Map<String, MCStatistics<P>> tree;

    public TranspositionTableMCTree(DeciderProperties properties) {
        this(properties, 1);
    }

    public TranspositionTableMCTree(DeciderProperties properties, int numberOfAgents) {
        super(properties, numberOfAgents);
        tree = new HashMap<>();
    }

    @Override
    public void reset() {
        tree.clear();
        super.reset();
    }

    /**
     * Does the tree contain the specified state (as a string).
     * All internal maps use the string representation of the state as the key; so these need to be unique.
     *
     * @param stateAsString
     * @return true/false
     */
    public boolean containsState(String stateAsString) {
        return tree.containsKey(stateAsString);
    }

    public boolean containsState(State<?> state) {
        String stateAsString = state.getAsString();
        return containsState(stateAsString);
    }
    /**
     * Inserts the state into the tree, with the list of possible actions specified.
     * This does check that the state is not already present.
     * It does not check to see if the insertion is valid (based on the number of updates left).
     *
     * @param state
     */
    public void insertState(State<P> state) {
        String stateAsString = state.getAsString();
        if (tree.containsKey(stateAsString))
            return;
        tree.put(stateAsString, new MCStatistics<P>(this, state));
        stateRefs.put(stateAsString, nextRef);
        nextRef++;
        updatesLeft--;
    }
    @Override
    public void insertRoot(State<P> state) {
        insertState(state);
        rootNode = tree.get(state.getAsString());
    }

    @Override
    public void processTrajectory(List<Triplet<State<P>, ActionWithRef<P>, Long>> trajectory, double[] finalScores) {
        long endTime = trajectory.get(trajectory.size()-1).getValue2();
        for (int i = 0; i < trajectory.size(); i++) {  // traverse through trajectory
            Triplet<State<P>, ActionWithRef<P>, Long> tuple = trajectory.get(i);
            State<P> state = tuple.getValue0();
            ActionEnum<P> actionTaken = tuple.getValue1().actionTaken;
            State<P> nextState = i < trajectory.size() - 1 ? trajectory.get(i + 1).getValue0() : null;
            int actingPlayer = tuple.getValue1().agentRef;
            long time = tuple.getValue2();

            double discountFactor = gamma;
            if (gamma < 1.0) discountFactor = Math.pow(gamma, (endTime - time) / timePeriod);
            double[] discountedScores = new double[finalScores.length];
            for (int j = 0; j < discountedScores.length; j++)
                discountedScores[j] = finalScores[j] * discountFactor;

            if (containsState(state)) {
                // in tree, so update state
                updateState(state, actionTaken, nextState, discountedScores, actingPlayer);
            } else if (updatesLeft() > 0) {
                insertState(state);
                updateState(state, actionTaken, nextState, discountedScores, actingPlayer);
            }

            if (RAVE) {
                for (int j = i; j >= 0; j--) { // all previous actions in the trajectory have their RAVE stats updated
                    State<P> previousState = trajectory.get(j).getValue0();
                    updateRAVE(previousState, actionTaken, discountedScores, actingPlayer);
                }
            }
        }
    }

    //public for testing
    public void updateState(State<P> state, ActionEnum<P> action, State<P> nextState, double reward) {
        updateState(state, action, nextState, toArray(reward), 1);
    }

    // public for testing
    public void updateState(State<P> state, ActionEnum<P> action, State<P> nextState, double[] reward, int actingPlayer) {
        String stateAsString = state.getAsString();
        if (debug) {
            String rewardString = "";
            for (int i = 0; i < reward.length; i++) rewardString = String.format("%s|%.2f", rewardString, reward[i]);
            log(String.format("Updating State %s to State %s with Action %s and reward %s", stateRef(stateAsString), stateRef(nextState.getAsString()), action.toString(), rewardString));
        }
        if (tree.containsKey(stateAsString)) {
            MCStatistics<P> stats = tree.get(stateAsString);
            //			if (debug) log(String.format("Before update: MC:%.2f\tV:%.2f\tQ:%.2f", stats.getMean(action), stats.getV(), stats.getQ()));
            stats.update(action, nextState, reward, actingPlayer);
            if (debug) {
                String rewardString = "";
                for (int i = 0; i < reward.length; i++)
                    rewardString = String.format("%s|%.2f", rewardString, stats.getMean(action, actingPlayer)[i]);
                log("After update: " + rewardString);
                log("");
            }
        } else {
            if (debug) log("State not yet in tree");
        }
        if (MAST) {
            updateActionValues(action, actingPlayer, reward[actingPlayer - 1]);
        }
    }

    public void updateRAVE(State<P> state, ActionEnum<P> action, double[] reward, int agentRef) {
        String stateAsString = state.getAsString();
        if (tree.containsKey(stateAsString)) {
            if (debug) {
                String rewardString = "";
                for (int i = 0; i < reward.length; i++)
                    rewardString = String.format("%s|%.2f", rewardString, reward[i]);
                log(String.format("Updating RAVE State %s for Action %s and reward %.2f", stateRef(stateAsString), action.toString(), reward));
            }
            MCStatistics<P> stats = tree.get(stateAsString);
            stats.updateRAVE(action, reward, agentRef);
            if (debug) {
                String rewardString = "";
                for (int i = 0; i < reward.length; i++)
                    rewardString = String.format("%s|%.2f", rewardString, reward[i]);
                log(String.format("After update: MC:%.2f\tV:%.2f\tQ:%.2f\n", stats.getMean(action, agentRef), stats.getV(agentRef), stats.getQ(agentRef)));
            }
        } else {
            if (debug) log("State not yet in tree");
        }
    }

    @Override
    public ActionEnum<P> getNextAction(State<P> state, List<ActionEnum<P>> possibleActions, int decidingAgent) {
        String stateAsString = state.getAsString();
        if (tree.containsKey(stateAsString)) {
            MCStatistics<P> stats = tree.get(stateAsString);
            if (stats.hasUntriedAction(possibleActions, decidingAgent)) {
                return stats.getRandomUntriedAction(possibleActions, decidingAgent);
            } else {
                return stats.getUCTAction(possibleActions, decidingAgent);
            }
        } else {
            return null;
        }
    }

    @Override
    public MCStatistics<P> getStatisticsFor(State<P> state) {
        return tree.get(state.getAsString());
    }

    public MCStatistics<P> getStatisticsFor(String state) {
        return tree.get(state);
    }

    @Override
    public int numberOfStates() {
        return tree.size();
    }

    @Override
    public List<MCStatistics<P>> getAllNodesWithMinVisits(int minV) {
        List<MCStatistics<P>> retValue = new ArrayList();
        for (String key : tree.keySet()) {
            if (tree.get(key).getVisits() >= minV)
                retValue.add(tree.get(key));
        }
        return retValue;
    }

    @Override
    public void pruneTree(MCStatistics<P> newRoot) {
		/*
		- Create a new, empty tree as in refresh()
		- statesToCopy = empty Queue
		- statesToCopy.push(startState)
		- While statesToCopy not empty
			Pop state from statesToCopy
			Add all child states of state to statesToCopy
			Add state to new Tree
		 */
        Map<String, MCStatistics<P>> newTree = new HashMap<String, MCStatistics<P>>();
        Queue<String> q = new LinkedBlockingQueue<String>();
        Set<String> processed = new HashSet<String>();
        q.add(newRoot.getReferenceState().getAsString());
        do {
            String state = q.poll();
            processed.add(state);
            MCStatistics<P> toCopy = tree.get(state);
            if (toCopy != null) {
                newTree.put(state, toCopy);
                for (String successor : toCopy.getSuccessorStates())
                    if (!processed.contains(successor))
                        q.add(successor);
            }
        } while (!q.isEmpty());

        tree = newTree;
    }

    @Override
    public boolean withinTree(State<P> state) {
        return state == null ? false : tree.containsKey(state.getAsString());
    }

    @Override
    public int[] getDepths() {
        Map<String, Integer> depths = new HashMap<String, Integer>();
        double[] visitDepth = new double[10];
        int[] atDepth = new int[11];
        Queue<String> q = new LinkedBlockingQueue<String>();
        String root = rootNode.getReferenceState().toString();
        depths.put(root, 0);
        q.add(root);
        do {
            String state = q.poll();
            int currentDepth = depths.get(state);
            MCStatistics<P> toCopy = tree.get(state);
            if (toCopy != null) {
                for (String successor : toCopy.getSuccessorStates())
                    if (!depths.containsKey(successor)) {
                        depths.put(successor, currentDepth + 1);
                        int visits = tree.get(successor).getVisits();
                        if (currentDepth < 10) {
                            visitDepth[currentDepth] += visits;
                            atDepth[currentDepth]++;
                        }
                        if (currentDepth + 1 > atDepth[10]) atDepth[10] = currentDepth + 1;
                        q.add(successor);
                    }
            }
        } while (!q.isEmpty());

        int[] retValue = new int[21];
        for (int i = 0; i < 11; i++) {
            retValue[i] = atDepth[i];
        }
        double totalVisits = tree.get(root).getVisits();
        for (int i = 0; i < 10; i++) {
            retValue[i + 11] = (int) (100.0 * visitDepth[i] / totalVisits);
        }
        return retValue;
    }

    @Override
    public String toString(boolean full) {
        StringBuffer retValue = new StringBuffer("Monte Carlo Tree ");
        retValue.append("with " + tree.size() + " states.\n");
        for (String s : tree.keySet()) {
            MCStatistics<P> stats = tree.get(s);
            retValue.append("\t" + s + "\t" + stats.getVisits() + " visits\n");
            if (full) {
                retValue.append("------------------\n");
                retValue.append(stats.toString(debug));
                retValue.append("------------------\n");
            }
        }
        return retValue.toString();
    }
}
