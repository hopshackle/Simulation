package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;
import java.util.*;

public class OpenLoopState<A extends Agent> implements State<A> {

    public final Map<Integer, MCStatistics<A>> currentNodesByPlayer = new HashMap<>();
    public final int currentPlayer;

    public OpenLoopState(int playerToAct, Map<Integer, MCStatistics<A>> currentNodes) {
        currentPlayer = playerToAct;
        currentNodes.keySet().stream().forEach(p -> currentNodesByPlayer.put(p, currentNodes.get(p)));
    }

    @Override
    public double[] getAsArray() {
        throw new AssertionError("Not implemented");
    }

    @Override
    public int getActorRef() {
        return currentPlayer;
    }

    @Override
    public double[] getScore() {
        return currentNodesByPlayer.get(currentPlayer).getV(currentPlayer);
    }

    @Override
    public String getAsString() {
        return "OpenLoopState";
    }

    @Override
    public State<A> apply(ActionEnum<A> proposedAction) {
        throw new AssertionError("Not implemented");
    }

    @Override
    public OpenLoopState<A> clone() {
        return new OpenLoopState(currentPlayer, currentNodesByPlayer);
    }
}
