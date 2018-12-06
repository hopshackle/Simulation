package hopshackle.simulation.games.resistance;

import hopshackle.simulation.Action;
import hopshackle.simulation.ActionEnum;
import hopshackle.simulation.games.*;

public class RejectTeam implements ActionEnum<ResistancePlayer> {
    @Override
    public Action<ResistancePlayer> getAction(ResistancePlayer resistancePlayer) {
        return new RejectTeamAction(resistancePlayer);
    }

    public String toString() {
        return "REJECT";
    }

    public boolean equals(Object other) {
        return (other instanceof RejectTeam);
    }

    public int hashCode() {
        return 88493;
    }

}

class RejectTeamAction extends GameAction<ResistancePlayer> {

    private static RejectTeam actionEnum = new RejectTeam();

    public RejectTeamAction(ResistancePlayer player) {
        super(actionEnum, player);
    }

    public void doStuff() {
        actor.getGame().voteForMission(actor.getPlayerNumber(), false);
    }

    public String toString() {
        return String.format("%d votes against mission team", actor.getPlayerNumber());
    }
}