package hopshackle.simulation.games;

import hopshackle.simulation.*;

import java.util.*;

public interface GameActionEnum<A extends Agent> extends ActionEnum<A> {

    public default List<Integer> isVisibleTo(int actorRef, Game<A, ActionEnum<A>> game) {
        List<Integer> retValue = new ArrayList<>();
        for (int i = 1; i <= game.getAllPlayers().size(); i++)
            retValue.add(i);
        return retValue;
    }

    public default List<Integer> actorVisibilityOnly(int actorRef) {
        List<Integer> retValue = new ArrayList<>();
        retValue.add(actorRef);     // only visible to the actor
        return retValue;
    }
}
