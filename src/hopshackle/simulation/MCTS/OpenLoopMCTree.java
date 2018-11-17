package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.List;

public class OpenLoopMCTree<P extends Agent> extends MonteCarloTree<P> {

    private MCStatistics<P> currentPointer;

    public OpenLoopMCTree(DeciderProperties properties, int numberOfAgents) {
        super(properties, numberOfAgents);
        currentPointer = rootNode;
    }

    @Override
    public void processTrajectory(List<Pair<State<P>, ActionWithRef<P>>> trajectory, double[] finalScores) {
        currentPointer = rootNode;
        List<MCStatistics<P>> nodeTrajectory = new ArrayList();
        nodeTrajectory.add(currentPointer);

        // 'Learning' in this context means updating the MonteCarloTree
        for (int i = 0; i < trajectory.size(); i++) {  // traverse through trajectory
            Pair<State<P>, ActionWithRef<P>> tuple = trajectory.get(i);
            ActionEnum<P> actionTaken = tuple.getValue1().actionTaken;
            int actingPlayer = tuple.getValue1().agentRef;

            // we update, and get the next node
            currentPointer = currentPointer.update(actionTaken, finalScores, actingPlayer);
            if (currentPointer != null)
                nodeTrajectory.add(currentPointer);
            if (RAVE) {
                for (MCStatistics node : nodeTrajectory) { // all previous actions (plus this one) in the trajectory have their RAVE stats updated
                    node.updateRAVE(actionTaken, finalScores, actingPlayer);
                }
            }
        }
    }

    @Override
    public void updateState(State<P> state, ActionEnum<P> action, State<P> nextState, double[] reward, int actingPlayer) {

    }

    @Override
    public ActionEnum<P> getNextAction(State<P> state, List<ActionEnum<P>> possibleActions, int decidingAgent) {
        return null;
    }

    @Override
    public ActionEnum<P> getBestAction(State<P> state, List<ActionEnum<P>> possibleActions, int decidingAgent) {
        return null;
    }

    @Override
    public List<String> getAllStatesWithMinVisits(int minV) {
        return null;
    }

    @Override
    public int numberOfStates() {
        return 0;
    }

    @Override
    public void pruneTree(String newRoot) {

    }

    @Override
    public MCStatistics getStatisticsFor(State<P> state) {
        return null;
    }

    @Override
    public boolean withinTree(State<P> state) {
        return false;
    }

    @Override
    public int[] getDepthsFrom(String root) {
        return new int[0];
    }

    @Override
    public String toString(boolean full) {
        return null;
    }
}
