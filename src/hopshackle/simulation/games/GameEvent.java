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

    @Override
    public boolean equals(Object other) {
        if (other instanceof GameEvent) {
            GameEvent o = (GameEvent) other;
            if (!o.type.equals(type)) return false;
            if (o.time != time) return false;
            if (!o.actionTaken.equals(actionTaken)) return false;
            if (o.visibleTo.size() != visibleTo.size()) return false;
            if (!o.visibleTo.stream().allMatch(visibleTo::contains)) return false;
            if (!visibleTo.stream().allMatch(o.visibleTo::contains)) return false;
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 36343 + (int) time * 13 + actionTaken.hashCode() * 4057 + type.hashCode() * 4229 + visibleTo.hashCode();
    }
}
