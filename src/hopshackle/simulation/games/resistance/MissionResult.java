package hopshackle.simulation.games.resistance;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;
import java.util.*;

public class MissionResult implements GameActionEnum<ResistancePlayer> {

    private int totalDefectors;
    private List<Integer> teamMembers;

    public MissionResult(List<Integer> members, int defectors) {
        teamMembers = HopshackleUtilities.cloneList(members);
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
            MissionResult omr = (MissionResult) other;
            if (omr.totalDefectors != totalDefectors)
                return false;
            if (omr.teamMembers.size() != teamMembers.size())
                return false;
            for (int tm : omr.teamMembers) {
                if (!teamMembers.contains(tm))
                    return false;
            }
            return true;
        }
        return false;
    }

    public int getDefections() {
        return totalDefectors;
    }
    public List<Integer> getTeam() {
        return teamMembers;
    }

    public int hashCode() {
        int teamHash = teamMembers.stream().mapToInt(i -> i * 17).sum();
        return 44371 + totalDefectors * 58573 + teamHash;
    }
}
