package hopshackle.simulation.games.resistance;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;

import java.util.List;

public class SupportTeam implements GameActionEnum<ResistancePlayer> {
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

    @Override
    public List<Integer> isVisibleTo(int actorRef, Game game) {
        return actorVisibilityOnly(actorRef);
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
