package hopshackle.simulation.games.resistance;

import hopshackle.simulation.Action;
import hopshackle.simulation.ActionEnum;
import hopshackle.simulation.games.*;

import java.util.List;

public class RejectTeam implements GameActionEnum<ResistancePlayer> {
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

    @Override
    public List<Integer> isVisibleTo(int actorRef, Game game) {
        return actorVisibilityOnly(actorRef);
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