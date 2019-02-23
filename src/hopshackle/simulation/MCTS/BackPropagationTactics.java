package hopshackle.simulation.MCTS;

import java.util.*;

public class BackPropagationTactics {

    /*
    This encapsulates some data structures to define where we start and stop back-propagating in MCTS
     */

    private Map<Integer, MCStatistics> startNodes;
    private Map<Integer, MCStatistics> stopNodes;

    /*
    This assumes we are back-propagating from the end of a trajectory back towards the root of the tree
    Hence a startNode is lower down the trajectory than any stopNode.
            stopNode == null  -> we back-propagate all the way to the root node
            startNode == null -> we back-propagate all the way from the leaf node
     */
    public BackPropagationTactics(Map<Integer, MCStatistics> startNodes, Map<Integer, MCStatistics> stopNodes, int playerCount) {
        this.startNodes = startNodes;
        this.stopNodes = stopNodes;
        if (startNodes.keySet().size() !=0 && startNodes.keySet().size() != playerCount) {
            throw new AssertionError("If a startNode is specified, it must be for all players");
        }
        if (stopNodes.keySet().size() !=0 && stopNodes.keySet().size() != playerCount) {
            throw new AssertionError("If a stopNode is specified, it must be for all players");
        }
    }

    public void updateStartNodes(Map<Integer, MCStatistics> newNodes) {
        this.startNodes = newNodes;
    }
    public void updateStopNodes(Map<Integer, MCStatistics> newNodes) {
        this.stopNodes = newNodes;
    }

    public MCStatistics getStartNode(int playerRef) {
        return startNodes.getOrDefault(playerRef, null);
    }
    public MCStatistics getStopNode(int playerRef) {
        return stopNodes.getOrDefault(playerRef, null);
    }
}
