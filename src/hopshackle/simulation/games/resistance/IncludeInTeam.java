package hopshackle.simulation.games.resistance;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;

public class IncludeInTeam implements GameActionEnum<ResistancePlayer> {

    public final int playerToInclude;

    public IncludeInTeam(int player) {
        playerToInclude = player;
    }

    @Override
    public Action<ResistancePlayer> getAction(ResistancePlayer resistancePlayer) {
        return new IncludeInTeamAction(this, resistancePlayer);
    }

    @Override
    public String toString() {
        return "INCLUDE_" + playerToInclude;
    }

    @Override
    public int hashCode() {
        return playerToInclude * 149 + 73609;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof IncludeInTeam) {
            if (((IncludeInTeam) other).playerToInclude == playerToInclude) return true;
        }
        return false;
    }
}

class IncludeInTeamAction extends GameAction<ResistancePlayer> {

    public int toInclude;

    public IncludeInTeamAction(IncludeInTeam actionEnum, ResistancePlayer player) {
        super(actionEnum, player);
        toInclude = actionEnum.playerToInclude;
    }

    public void doStuff() {
        actor.getGame().includeInTeam(toInclude);
    }

    public String toString() {
        return String.format("%d includes %d in proposed mission team", actor.getPlayerNumber(), toInclude);
    }
}
