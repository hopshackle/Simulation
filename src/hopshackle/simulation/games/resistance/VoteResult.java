package hopshackle.simulation.games.resistance;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;

public class VoteResult implements GameActionEnum<ResistancePlayer> {

    private String votesAsString;
    private boolean voteSuccessful;
    private boolean[] votes;

    public VoteResult(boolean[] votes) {
        StringBuilder temp = new StringBuilder();
        this.votes = votes;
        int votesInFavour = 0;
        for (int i = 1; i < votes.length; i++) {
            temp.append(votes[i] ? "1" : "0");
            if (votes[i]) votesInFavour++;
        }
        voteSuccessful = votesInFavour > votes.length / 2;
        votesAsString = temp.toString();
    }

    public boolean isPassed() {
        return voteSuccessful;
    }
    public boolean voteOfPlayer(int playerNumber) {
        return votes[playerNumber];
    }

    @Override
    public Action<ResistancePlayer> getAction(ResistancePlayer resistancePlayer) {
        throw new AssertionError("Not implemented : only used for GameEvent messaging");
    }

    public String toString() {
        return String.format("VOTE_RESULT: %s", votesAsString);
    }

    public boolean equals(Object other) {
        if (other instanceof hopshackle.simulation.games.resistance.VoteResult) {
            if (((VoteResult) other).votesAsString.equals(votesAsString));
                return true;
        }
        return false;
    }

    public int hashCode() {
        return 58321 + votesAsString.hashCode();
    }
}

