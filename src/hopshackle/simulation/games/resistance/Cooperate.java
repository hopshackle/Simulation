package hopshackle.simulation.games.resistance;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;
import java.util.*;

public class Cooperate implements GameActionEnum<ResistancePlayer> {

    @Override
    public Action<ResistancePlayer> getAction(ResistancePlayer resistancePlayer) {
        return new CooperateAction(resistancePlayer);
    }

    public String toString() {return "COOPERATE";}

    public boolean equals(Object other) {
        return (other instanceof Cooperate);
    }

    public int hashCode() {
        return 31039;
    }

    @Override
    public List<Integer> isVisibleTo(int actorRef, Game game) {
        return actorVisibilityOnly(actorRef);
    }

}

class CooperateAction extends GameAction<ResistancePlayer> {

    private static Cooperate coop = new Cooperate();

    public CooperateAction(ResistancePlayer agent) {
        super(coop, agent);
    }

    @Override
    public void doStuff() {
        // do nothing...default is to cooperate...we only need to say if we defect
    }

    public String toString() {return "COOPERATE";}

}