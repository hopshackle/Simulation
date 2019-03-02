package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;

import java.util.*;
import java.util.stream.IntStream;

public class OpenLoopTreeTracker<A extends Agent> implements GameListener<A> {

    public final String treeType;
    private Map<Integer, MCStatistics<A>> currentNodes = new HashMap<>();

    public OpenLoopTreeTracker(String treeSetting, Map<Integer, MonteCarloTree<A>> startingTrees, Game<A, ActionEnum<A>> game) {

        game.registerListener(this);
        switch (treeSetting) {
            // In all cases we initialise all players to be at the root of their respective trees (which might be the same one)
            case "single":
            case "perPlayer":
            case "ignoreOthers":
                IntStream.rangeClosed(1, game.getPlayerCount())
                        .filter(startingTrees.keySet()::contains)
                        .forEach(
                                p -> currentNodes.put(p, startingTrees.get(p).rootNode)
                        );
                break;
            default:
                throw new AssertionError("Unknown treeSetting " + treeSetting);
        }
        treeType = treeSetting;
    }

    /*
    Returns null if we are not in the tree
     */
    public MCStatistics<A> getCurrentNode(int decidingAgent) {
        return (currentNodes.getOrDefault(decidingAgent, null));
        //     Set<Integer> actors = currentNodes.get(agent.getActorRef()).actorsFrom();
        //      int actingAgent = actors.size() == 1 ? actors.iterator().next() : agent.getActorRef();
    }

    public boolean hasLeftTree(int player) {
        return currentNodes.get(player) == null;
    }

    @Override
    public void processGameEvent(GameEvent<A> event) {
        switch (event.type) {
            case MOVE:
                boolean gameLevelEvent = (event.actionTaken.agentRef == -1);
                int agentRef = gameLevelEvent ? event.visibleTo().get(0) : event.actionTaken.agentRef;
                MCStatistics<A> currentNode = currentNodes.getOrDefault(agentRef, null);
                switch (treeType) {
                    case "single":
                        // technically a single tree is only really valid if we have perfect information moves
                        // in practise we progress all nodes according to the active player
                        if (currentNode != null) {
                            MCStatistics<A> newNode = currentNode.getSuccessorNode(event.actionTaken);
                            currentNodes.keySet().stream()
                                    //                   .filter(event.visibleTo()::contains)
                                    .forEach(
                                            p -> currentNodes.put(p, newNode)
                                    );
                        } // else we are not in the tree
                        break;
                    case "perPlayer":
                        // we progress all players according to the action taken in their own tree (which may be none)
                        currentNodes.keySet().stream()
                                .filter(event.visibleTo()::contains)
                                .forEach(
                                        p -> {
                                            MCStatistics<A> node = currentNodes.getOrDefault(p, null);
                                            if (node != null) {
                                                MCStatistics<A> newNode = node.getSuccessorNode(event.actionTaken);
                                                currentNodes.put(p, newNode);
                                            }
                                        }
                                );
                        break;
                    case "ignoreOthers":
                        // we just progress the one node for the acting player
                        if (!gameLevelEvent && currentNode != null) {
                            MCStatistics<A> newNode = currentNode.getSuccessorNode(event.actionTaken);
                            currentNodes.put(event.actionTaken.agentRef, newNode);
                        } // else we are not in the tree
                        break;
                    default:
                        throw new AssertionError("Unknown treeSetting " + treeType);
                }
                break;
            case GAME_OVER:
                currentNodes = new HashMap<>();
        }
    }
}
