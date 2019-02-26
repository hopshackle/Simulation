package hopshackle.simulation.games;

import hopshackle.simulation.*;

import java.util.*;

public class GameEvent<A extends Agent> {

    public enum Type {
        MOVE,
        PLAYER_CHANGE,
        GAME_OVER
    }

    public final Type type;
    public final ActionWithRef<A> actionTaken;
    public final long time;
    private List<Integer> visibleTo = new ArrayList<>();

    public GameEvent(Type type, Game game) {
        this.type = type;
        time = game.getTime();
        actionTaken = null;
        if (type != Type.GAME_OVER)
            throw new AssertionError("Only GAME_OVER is valid with no further parameters");
        for (int i = 1; i <= game.getPlayerCount(); i++)
            visibleTo.add(i);
    }

    public GameEvent(ActionWithRef<A> actionTaken, Game game) {
        type = Type.MOVE;
        time = game.getTime();
        this.actionTaken = actionTaken;
        visibleTo = ((GameActionEnum) actionTaken.actionTaken).isVisibleTo(actionTaken.agentRef, game);
    }

    public GameEvent(int previousPlayer, Game game) {
        type = Type.PLAYER_CHANGE;
        time = game.getTime();
        actionTaken = new ActionWithRef<>(null, previousPlayer);
        // not visible to anyone...this is purely for technical use in MRIS-MCTS and variants
    }

    public List<Integer> visibleTo() {
        return visibleTo;
    }
}
