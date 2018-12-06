package hopshackle.simulation.games.resistance;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;

public class SupportTeam implements ActionEnum<ResistancePlayer> {
    @Override
    public Action<ResistancePlayer> getAction(ResistancePlayer resistancePlayer) {
        return new SupportTeamAction(resistancePlayer);
    }

    public String toString() {
        return "SUPPORT";
    }

    public boolean equals(Object other) {
        return (other instanceof SupportTeam);
    }

    public int hashCode() {
        return 88819;
    }
}

class SupportTeamAction extends GameAction<ResistancePlayer> {

    private static SupportTeam actionEnum = new SupportTeam();

    public SupportTeamAction(ResistancePlayer voter) {
        super(actionEnum, voter);
    }
    public void doStuff() {
        actor.getGame().voteForMission(actor.getPlayerNumber(), true);
    }

    public String toString() {
        return String.format("%d votes for mission team", actor.getPlayerNumber());
    }
}
