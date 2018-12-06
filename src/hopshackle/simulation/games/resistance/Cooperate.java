package hopshackle.simulation.games.resistance;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;

public class Cooperate implements ActionEnum<ResistancePlayer> {

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