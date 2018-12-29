package hopshackle.simulation.games.resistance;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;

public class MissionResult implements GameActionEnum<ResistancePlayer> {

    private int totalDefectors;

    public MissionResult(int defectors) {
        totalDefectors = defectors;
    }

    @Override
    public Action<ResistancePlayer> getAction(ResistancePlayer resistancePlayer) {
        throw new AssertionError("Not implemented : only used for GameEvent messaging");
    }

    public String toString() {
        return String.format("MISSION_RESULT: %d", totalDefectors);
    }

    public boolean equals(Object other) {
        if (other instanceof MissionResult) {
            if (((MissionResult)other).totalDefectors == totalDefectors)
                return true;
        }
        return false;
    }

    public int hashCode() {
        return 44371 + totalDefectors * 58573;
    }
}
