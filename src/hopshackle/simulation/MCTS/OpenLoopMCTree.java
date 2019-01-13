package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;
import org.javatuples.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

public class OpenLoopMCTree<P extends Agent> extends MonteCarloTree<P> {

    public OpenLoopMCTree(DeciderProperties properties, int numberOfAgents) {
        super(properties, numberOfAgents);
        reset();
    }

    @Override
    public void processTrajectory(List<Triplet<State<P>, ActionWithRef<P>, Long>> trajectory, double[] finalScores) {
        MCStatistics<P> currentPointer = rootNode;
        List<MCStatistics<P>> nodeTrajectory = new ArrayList();
        nodeTrajectory.add(currentPointer);

        long endTime = trajectory.get(trajectory.size() - 1).getValue2();
        for (int i = 0; i < trajectory.size(); i++) {  // traverse through trajectory
            Triplet<State<P>, ActionWithRef<P>, Long> tuple = trajectory.get(i);
            ActionEnum<P> actionTaken = tuple.getValue1().actionTaken;
            int actingPlayer = tuple.getValue1().agentRef;
            long time = tuple.getValue2();
            double discountFactor = gamma;
            if (gamma < 1.0) discountFactor = Math.pow(gamma, (endTime - time) / timePeriod);
            double[] discountedScores = new double[finalScores.length];
            for (int j = 0; j < discountedScores.length; j++)
                discountedScores[j] = finalScores[j] * discountFactor;

            // we update, and get the next node
            if (currentPointer != null) {
                currentPointer = currentPointer.update(actionTaken, discountedScores, actingPlayer);
                if (currentPointer != null) nodeTrajectory.add(currentPointer);
            }
            if (MAST && actingPlayer > 0) {
                updateActionValues(actionTaken, actingPlayer, discountedScores[actingPlayer - 1]);
            }
            if (RAVE && actingPlayer > 0) {
                for (MCStatistics node : nodeTrajectory) { // all previous actions (plus this one) in the trajectory have their RAVE stats updated
                    node.updateRAVE(actionTaken, finalScores, actingPlayer);
                }
            }
        }
    }

    @Override
    public ActionEnum<P> getNextAction(State<P> state, List<ActionEnum<P>> possibleActions, int decidingAgent) {
        MCStatistics<P> currentPointer = null;
        if (state != null && state instanceof OpenLoopState) {
            OpenLoopState<P> OLState = (OpenLoopState) state;
            currentPointer = OLState.currentNodesByPlayer.get(decidingAgent);
        }
        if (currentPointer != null) {
            if (currentPointer.hasUntriedAction(possibleActions, decidingAgent)) {
                ActionEnum<P> action = currentPointer.getRandomUntriedAction(possibleActions, decidingAgent);
                return action;
            } else {
                ActionEnum<P> action = currentPointer.getUCTAction(possibleActions, decidingAgent);
                return action;
            }
        } else {
            return null;
        }
    }


    @Override
    public void insertRoot(State<P> state) {
        // do nothing
    }

    @Override
    public void reset() {
        rootNode = new MCStatistics(this, null);
    }

    @Override
    public MCStatistics<P> getStatisticsFor(State<P> state) {
        if (state instanceof OpenLoopState) {
            OpenLoopState<P> OLState = (OpenLoopState) state;
            return (OLState.currentNodesByPlayer.get(OLState.currentPlayer));
        } else {
            throw new AssertionError("Invalid type of state " + state.getClass());
        }
    }

    @Override
    public boolean withinTree(State<P> state) {
        if (state != null && state instanceof OpenLoopState) return true;
        return false;
    }

    @Override
    public List<MCStatistics<P>> getAllNodesWithMinVisits(int minV) {
        return getAllNodesThatMeetCriteria(node -> node.getVisits() >= minV);
    }

    private List<MCStatistics<P>> getAllNodesThatMeetCriteria(Predicate<MCStatistics<P>> predicate) {
        List<MCStatistics<P>> retValue = new ArrayList<>();
        Queue<MCStatistics<P>> processQueue = new LinkedList<>();
        processQueue.add(rootNode);

        do {
            MCStatistics<P> nextNode = processQueue.poll();
            if (predicate.test(nextNode)) {
                retValue.add(nextNode);
            }
            for (ActionWithRef<P> action : nextNode.getPossibleActions()) {
                MCStatistics<P> successor = nextNode.getSuccessorNode(action);
                if (successor != null)
                    processQueue.add(successor);
            }
            // if node has visits below threshold, then with open loop, so muct all its children
        } while (!processQueue.isEmpty());
        return retValue;
    }

    @Override
    public int numberOfStates() {
        return getAllNodesThatMeetCriteria(x -> true).size();
    }

    @Override
    public void pruneTree(MCStatistics<P> newRoot) {
        rootNode = newRoot;
    }

    @Override
    public int[] getDepths() {
        double[] visitDepth = new double[11];
        int[] atDepth = new int[11];
        Queue<MCStatistics<P>> q = new LinkedBlockingQueue();
        Queue<MCStatistics<P>> nextQ = new LinkedBlockingQueue();
        if (rootNode != null) q.add(rootNode);
        int currentDepth = 0;
        do {
            do {
                MCStatistics<P> state = q.poll();
                for (ActionWithRef<P> action : state.getPossibleActions()) {
                    MCStatistics<P> toCopy = state.getSuccessorNode(action);
                    if (toCopy != null) {
                        nextQ.add(toCopy);
                        visitDepth[currentDepth] += toCopy.getVisits();
                        atDepth[currentDepth]++;
                    }
                }
            } while (!q.isEmpty());
            q = nextQ;
            nextQ = new LinkedBlockingQueue();
            currentDepth++;
        } while (!q.isEmpty() && currentDepth < 11);

        int[] retValue = new int[21];
        for (int i = 0; i < 11; i++) {
            retValue[i] = atDepth[i];
        }
        double totalVisits = rootNode.getVisits();
        for (int i = 0; i < 10; i++) {
            retValue[i + 11] = (int) (100.0 * visitDepth[i] / totalVisits);
        }
        return retValue;
    }

    @Override
    public String toString(boolean full) {
        StringBuffer retValue = new StringBuffer("Monte Carlo Open Loop Tree ");
        List<MCStatistics<P>> allStats = getAllNodesThatMeetCriteria(x -> true);
        retValue.append("with " + numberOfStates() + " states.\n");
        for (MCStatistics<P> stats : allStats) {
            retValue.append("\t" + stats.getVisits() + " visits\n");
            if (full) {
                retValue.append("------------------\n");
                retValue.append(stats.toString(debug));
                retValue.append("------------------\n");
            }
        }
        return retValue.toString();
    }
}
