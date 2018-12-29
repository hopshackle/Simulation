package hopshackle.simulation.games.resistance;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;

import java.util.List;

public class Defect implements GameActionEnum<ResistancePlayer> {
    @Override
    public Action<ResistancePlayer> getAction(ResistancePlayer resistancePlayer) {
        return new DefectAction(resistancePlayer);
    }

    public String toString() {
        return "DEFECT";
    }

    public boolean equals(Object other) {
        return (other instanceof Defect);
    }

    public int hashCode() {
        return 30983;
    }

    @Override
    public List<Integer> isVisibleTo(int actorRef, Game game) {
        return actorVisibilityOnly(actorRef);
    }
}

class DefectAction extends GameAction<ResistancePlayer> {

    private static Defect defect = new Defect();

    public DefectAction(ResistancePlayer agent) {
        super(defect, agent);
    }

    @Override
    public void doStuff() {
        actor.getGame().defect(actor.getPlayerNumber());
    }

    public String toString() {
        return "DEFECT";
    }

}