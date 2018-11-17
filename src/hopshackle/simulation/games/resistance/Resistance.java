package hopshackle.simulation.games.resistance;

import hopshackle.simulation.*;
import hopshackle.simulation.games.Game;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.*;

public class Resistance extends Game<ResistancePlayer, ActionEnum<ResistancePlayer>> {

    public enum Phase {ASSEMBLE, VOTE, MISSION;}
    private static AtomicLong idFountain = new AtomicLong(1);
    private static int[][] teamMembersByMissionandPlayercount = new int[][]{
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 2, 2, 2, 3, 3, 3},
            {0, 0, 0, 0, 0, 3, 3, 3, 4, 4, 4},
            {0, 0, 0, 0, 0, 2, 4, 3, 4, 4, 4},
            {0, 0, 0, 0, 0, 3, 3, 4, 5, 5, 5},
            {0, 0, 0, 0, 0, 3, 4, 4, 5, 5, 5}};

    private int playerCount, traitorCount;
    private boolean[] traitorIdentities;
    private ResistancePlayer[] allPlayers;
    private int mission = 0, defectorsOnMission = 0, failedMissions = 0, failedVotes = 0, successfulMissions = 0, firstPlayer = 0, currentPlayer;
    private List<Integer> currentMissionTeam;
    private boolean[] missionTeamVotes;
    private Phase currentPhase;
    private World world;
    private long refID = idFountain.getAndIncrement();

    /*
    In a game of Resistance we have five rounds (missions). The first player selects the people to go on the mission.
    All players then vote on that team. If not approved my a majority, then first player shifts round and we repeat.
    In the event of 5 failed votes, the Spies win.
    If the team is approved we move to the Mission phase. All players on that mission decide whether to Cooperate or Defect.
    any Defect means the mission fails.
     */

    public Resistance(int numberOfPlayers, int numberOfTraitors, World world) {
        debug = true;
        playerCount = numberOfPlayers;
        traitorCount = numberOfTraitors;
        traitorIdentities = new boolean[playerCount + 1];
        allPlayers = new ResistancePlayer[playerCount + 1];
        for (int i = 1; i <= playerCount; i++) {
            allPlayers[i] = new ResistancePlayer(world, this, i);
        }
        scoreCalculator = new ResistanceScoreCalculator();
        initialise();
    }

    private void initialise() {
        int traitorsAllocated = 0;
        do {
            int t = Dice.roll(1, playerCount);
            if (!traitorIdentities[t]) {
                traitorIdentities[t] = true;
                log(String.format("Player %d is traitor", t));
                traitorsAllocated++;
            }
        } while (traitorsAllocated < traitorCount);

        for (int i = 1; i <= playerCount; i++) {
            if (traitorIdentities[i] == true) {
                allPlayers[i].setTraitor(true);
                for (int j = 1; j < playerCount; j++) {
                    if (j != i && traitorIdentities[j]) allPlayers[i].setOtherTraitor(j);
                }
            }
        }

        mission = 1;
        firstPlayer = 1;
        currentPlayer = 1;
        currentPhase = Phase.ASSEMBLE;
        currentMissionTeam = new ArrayList();
    }


    @Override
    public Game<ResistancePlayer, ActionEnum<ResistancePlayer>> clone(ResistancePlayer perspectivePlayer) {
        return null;
    }

    @Override
    public String getRef() {
        return String.valueOf(refID);
    }

    @Override
    public ResistancePlayer getCurrentPlayer() {
        return allPlayers[currentPlayer];
    }

    @Override
    public List<ResistancePlayer> getAllPlayers() {
        return Arrays.stream(allPlayers).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public int getPlayerNumber(ResistancePlayer player) {
        for (int i = 1; i < playerCount; i++)
            if (allPlayers[i] == player) return i;
        throw new AssertionError("No such player in this game");
    }

    @Override
    public ResistancePlayer getPlayer(int n) {
        return allPlayers[n];
    }

    @Override
    public List<ActionEnum<ResistancePlayer>> getPossibleActions() {
        List<ActionEnum<ResistancePlayer>> retValue = new ArrayList();
        switch (currentPhase) {
            case ASSEMBLE:
                retValue = IntStream.rangeClosed(1, playerCount)
                        .filter(i -> !currentMissionTeam.contains(i))
                        .mapToObj(player -> new IncludeInTeam(player))
                        .collect(Collectors.toList());
                break;
            case VOTE:
                retValue.add(new SupportTeam());
                retValue.add(new RejectTeam());
                break;
            case MISSION:
                if (traitorIdentities[currentPlayer])
                    retValue.add(new Defect());
                retValue.add(new Cooperate());
                break;
        }
        return retValue;
    }

    @Override
    public boolean gameOver() {
        return failedMissions >= 3 || successfulMissions >= 3 || failedVotes == 5;
    }

    public boolean spiesHaveWon() {
        return failedMissions >= 3 || failedVotes == 5;
    }

    private void updateCurrentPlayer() {
        currentPlayer = (currentPlayer + 1) % playerCount;
        if (currentPlayer == 0) currentPlayer = playerCount;
    }

    private void updateFirstPlayer() {
        firstPlayer = (firstPlayer + 1) % playerCount;
        if (firstPlayer == 0) firstPlayer = playerCount;
    }

    @Override
    public void updateGameStatus() {
        /*
        Called after every action has been executed. Responsible for moving the whole game state forward to the
       next phase, and updating the next player.
         */
        switch (currentPhase) {
            case ASSEMBLE:
                if (currentMissionTeam.size() == teamMembersByMissionandPlayercount[mission][playerCount]) {
                    currentPhase = Phase.VOTE;
                    missionTeamVotes = new boolean[playerCount + 1];
                    currentPlayer = firstPlayer;
                    failedVotes = 0;
                    if (debug)
                        log(String.format("Mission Team proposal: %s", HopshackleUtilities.formatList(currentMissionTeam, ", ", null)));
                } else {
                    updateCurrentPlayer();
                }
                break;
            case VOTE:
                updateCurrentPlayer();
                if (currentPlayer == firstPlayer) {
                    // everyone has now voted
                    int votesInFavour = 0;
                    for (int i = 1; i < missionTeamVotes.length; i++) {
                        if (missionTeamVotes[i]) votesInFavour++;
                    }
                    if (debug)
                        log(String.format("%d out of %d votes in favour of proposed team", votesInFavour, playerCount));
                    if (votesInFavour > playerCount / 2.0) {
                        currentPhase = Phase.MISSION;
                        currentPlayer = currentMissionTeam.get(0);
                        defectorsOnMission = 0;
                    } else {
                        updateFirstPlayer();
                        currentPlayer = firstPlayer;
                        failedVotes++;
                        missionTeamVotes = new boolean[playerCount + 1];
                        currentPhase = Phase.ASSEMBLE;
                        currentMissionTeam = new ArrayList();
                    }
                }
                break;
            case MISSION:
                if (currentPlayer == currentMissionTeam.get(teamMembersByMissionandPlayercount[mission][playerCount] - 1)) {
                    if (defectorsOnMission == 0) {
                        successfulMissions++;
                    } else {
                        failedMissions++;
                    }
                    if (debug) log(String.format("%d total defectors on mission", defectorsOnMission));
                    currentPhase = Phase.ASSEMBLE;
                    mission++;
                    updateFirstPlayer();
                    currentPlayer = firstPlayer;
                    currentMissionTeam = new ArrayList();
                } else {
                    currentPlayer = currentMissionTeam.get(currentMissionTeam.indexOf(currentPlayer) + 1);
                }
                break;
        }
    }

    // no method for cooperation, as this is the default assumption
    public void defect(int player) {
        if (currentPlayer == player && currentPhase == Phase.MISSION && traitorIdentities[player]) {
            defectorsOnMission++;
        } else {
            throw new AssertionError("Defect not a possible action at this point");
        }
    }
    public void includeInTeam(int player) {
        if (currentPhase == Phase.ASSEMBLE && !currentMissionTeam.contains(player)) {
            currentMissionTeam.add(player);
        } else {
            throw new AssertionError("Cannot include a team member in phase " + currentPhase);
        }
    }
    public void voteForMission(int player, boolean vote) {
        if (currentPhase == Phase.VOTE) {
            missionTeamVotes[player] = vote;
        } else {
            throw new AssertionError("Cannot vote in phase " + currentPhase);
        }
    }

    public List<Integer> getTraitors() {
        List<Integer> retValue = new ArrayList();
        for (int i = 1; i < traitorIdentities.length; i++) {
            if (traitorIdentities[i]) retValue.add(i);
        }
        return retValue;
    }

    public String toString() {
        return String.format("Resistance Game %d: Phase %s Mission %d, with %d failed missions so far", refID, currentPhase, mission, failedMissions);
    }
    public Phase getPhase() { return currentPhase;}
}
