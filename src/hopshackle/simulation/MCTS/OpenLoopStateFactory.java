package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;

import java.util.*;

public class OpenLoopStateFactory<A extends Agent> implements StateFactory<A>, GameListener<A> {

    private String treeType;
    private Map<Integer, MCStatistics<A>> currentNodes = new HashMap<>();

    public OpenLoopStateFactory(String treeSetting, Map<Integer, MonteCarloTree<A>> startingTrees, Game<A, ActionEnum<A>> game) {
        game.registerListener(this);
        switch (treeSetting) {
            // In all cases we initialise all players to be at the root of their respective trees (which might be the same one)
            case "single":
            case "perPlayer":
            case "ignoreOthers":
                game.getAllPlayers().stream()
                        .mapToInt(game::getPlayerNumber)
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
    @Override
    public State<A> getCurrentState(A agent) {
        if (currentNodes.get(agent.getActorRef()) == null) return null;
        Set<Integer> actors = currentNodes.get(agent.getActorRef()).actorsFrom();
        int actingAgent = actors.size() == 1 ? actors.iterator().next() : agent.getActorRef();
        return new OpenLoopState<>(actingAgent, HopshackleUtilities.cloneMap(currentNodes));
    }

    @Override
    public <V extends GeneticVariable<A>> List<V> getVariables() {
        return new ArrayList<>();
    }

    @Override
    public StateFactory<A> cloneWithNewVariables(List<GeneticVariable<A>> newVar) {
        return this;
    }

    @Override
    public void processGameEvent(GameEvent<A> event) {
        switch (event.type) {
            case MOVE:
                MCStatistics<A> currentNode = currentNodes.getOrDefault(event.actionTaken.agentRef, null);
                switch (treeType) {
                    case "single":
                        // we progress all nodes according to the active player
                        if (currentNode != null) {
                            MCStatistics<A> newNode = currentNode.getSuccessorNode(event.actionTaken);
                            currentNodes.keySet().stream()
                                    .forEach(
                                            p -> currentNodes.put(p, newNode)
                                    );
                        } // else we are not in the tree
                        break;
                    case "perPlayer":
                        // we progress all players according to the action taken in their own tree
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
                        if (currentNode != null) {
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
