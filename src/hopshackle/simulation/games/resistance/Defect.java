package hopshackle.simulation.games.resistance;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;

public class Defect implements ActionEnum<ResistancePlayer> {
    @Override
    public Action<ResistancePlayer> getAction(ResistancePlayer resistancePlayer) {
        return new DefectAction(resistancePlayer);
    }

    public String toString() {
        return "DEFECT";
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