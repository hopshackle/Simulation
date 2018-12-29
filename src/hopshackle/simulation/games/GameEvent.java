package hopshackle.simulation.games;

import hopshackle.simulation.*;

import java.util.*;

public class GameEvent<A extends Agent> {

    public enum Type {
        MOVE,
        GAME_OVER
    }

    public final Type type;
    public final Game game;
    public final ActionWithRef<A> actionTaken;
    private List<Integer> visibleTo = new ArrayList<>();

    public GameEvent(Type type, Game game) {
        this.type = type;
        this.game = game;
        actionTaken = null;
        if (type != Type.GAME_OVER)
            throw new AssertionError("Only GAME_OVER is valid with no further parameters");
        for (int i = 1; i <= game.getAllPlayers().size(); i++)
            visibleTo.add(i);
    }

    public GameEvent(ActionWithRef<A> actionTaken, Game game) {
        type = Type.MOVE;
        this.game = game;
        this.actionTaken = actionTaken;
        visibleTo = ((GameActionEnum) actionTaken.actionTaken).isVisibleTo(actionTaken.agentRef, game);
    }

    public List<Integer> visibleTo() {
        return visibleTo;
    }
}
