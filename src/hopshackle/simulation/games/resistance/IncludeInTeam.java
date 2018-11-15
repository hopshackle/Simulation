package hopshackle.simulation.games.resistance;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;

public class IncludeInTeam implements ActionEnum<ResistancePlayer> {

    public final int playerToInclude;

    public IncludeInTeam(int player) {
        playerToInclude = player;
    }

    @Override
    public Action<ResistancePlayer> getAction(ResistancePlayer resistancePlayer) {
        return new IncludeInTeamAction(this, resistancePlayer);
    }

    public String toString() {
        return "INCLUDE_" + playerToInclude;
    }
}

class IncludeInTeamAction extends GameAction<ResistancePlayer> {

    public int toInclude;

    public IncludeInTeamAction(IncludeInTeam actionEnum, ResistancePlayer player) {
        super(actionEnum, player);
        toInclude = actionEnum.playerToInclude;
    }

    public void doStuff(){
        actor.getGame().includeInTeam(toInclude);
    }

    public String toString() {
        return String.format("%d includes %d in proposed mission team", actor.getPlayerNumber(), toInclude);
    }
}
