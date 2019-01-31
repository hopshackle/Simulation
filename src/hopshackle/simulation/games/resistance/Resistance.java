package hopshackle.simulation.games.resistance;

import hopshackle.simulation.*;
import hopshackle.simulation.games.AllPlayerDeterminiser;
import hopshackle.simulation.games.Game;
import hopshackle.simulation.games.GameDeterminisationMemory;
import hopshackle.simulation.games.GameEvent;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.*;

public class Resistance extends Game<ResistancePlayer, ActionEnum<ResistancePlayer>> {

    public enum Phase {ASSEMBLE, VOTE, MISSION;}

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
    private int mission = 0, defectorsOnMission = 0, failedMissions = 0, failedVotes = 0, successfulMissions = 0, firstPlayer = 0;
    private List<List<Integer>> failedMissionDetails;
    private List<Integer> currentMissionTeam;
    private boolean[] missionTeamVotes;
    private Phase currentPhase;
    private World world;

    /*
    In a game of Resistance we have five rounds (missions). The first player selects the people to go on the mission.
    All players then vote on that team. If not approved my a majority, then first player shifts round and we repeat.
    In the event of 5 failed votes, the Spies win.
    If the team is approved we move to the Mission phase. All players on that mission decide whether to Cooperate or Defect.
    any Defect means the mission fails.
     */

    public Resistance(int numberOfPlayers, int numberOfTraitors, World world) {
        this(numberOfPlayers, numberOfTraitors, world, false);
    }

    private Resistance(int numberOfPlayers, int numberOfTraitors, World world, boolean cloned) {
        debug = false;
        playerCount = numberOfPlayers;
        traitorCount = numberOfTraitors;
        traitorIdentities = new boolean[playerCount + 1];
        missionTeamVotes = new boolean[playerCount + 1];
        failedMissionDetails = new ArrayList<>();
        allPlayers = new ResistancePlayer[playerCount + 1];
        for (int i = 1; i <= playerCount; i++) {
            allPlayers[i] = new ResistancePlayer(world, this, i);
        }
        this.world = world;
        scoreCalculator = new ResistanceScoreCalculator();
        if (!cloned) initialise();
    }

    private void initialise() {
        randomiseTraitorsExcluding(-1);
        updatePlayersWithTraitorInformation();

        mission = 1;
        firstPlayer = 1;
        changeCurrentPlayer(1);
        currentPhase = Phase.ASSEMBLE;
        currentMissionTeam = new ArrayList();
    }

    @Override
    public Resistance cloneLocalFields() {
        Resistance retValue = new Resistance(playerCount, traitorCount, world, true);
        retValue.debug = false;

        // now initialise
        retValue.mission = mission;
        retValue.successfulMissions = successfulMissions;
        retValue.failedMissions = failedMissions;
        retValue.failedMissionDetails = HopshackleUtilities.cloneList(failedMissionDetails);
        // note that the details of failed historic missions are immutable, so this shallow copy is legit
        retValue.failedVotes = failedVotes;
        retValue.firstPlayer = firstPlayer;
        retValue.currentPhase = currentPhase;
        for (int i = 0; i < missionTeamVotes.length; i++)
            retValue.missionTeamVotes[i] = missionTeamVotes[i];
        retValue.defectorsOnMission = defectorsOnMission;
        retValue.currentMissionTeam = HopshackleUtilities.cloneList(currentMissionTeam);

        for (int i = 0; i < traitorIdentities.length; i++)
            retValue.traitorIdentities[i] = traitorIdentities[i];
        retValue.updatePlayersWithTraitorInformation();

        return retValue;
    }

    @Override
    public void redeterminise(int perspective) {
        // traitor identities is the tricky bit, as this includes hidden information
        // if the perspective player is a traitor...then it's easy, we just copy as they have complete information
        // otherwise we need to sample...the one piece of inference we should use is that any determinisation
        // must be compatible with the mission failures to date. I.e. each failed mission must have had a number of
        // traitors at least equal to the observed defections
        if (traitorIdentities[perspective]) {
            // no change from basic clone
        } else {
            randomiseTraitorsExcluding(perspective);
            updatePlayersWithTraitorInformation();
        }

    }

    private void randomiseTraitorsExcluding(int exclude) {
        int redeterminisationCount = 0;
        do {
            for (int i = 0; i < traitorIdentities.length; i++) traitorIdentities[i] = false;
            int traitorsAllocated = 0;
            do {
                redeterminisationCount++;
                int t = Dice.roll(1, playerCount);
                if (t != exclude && !traitorIdentities[t]) {
                    traitorIdentities[t] = true;
                    if (debug) log(String.format("Player %d is traitor", t));
                    traitorsAllocated++;
                }
            } while (traitorsAllocated < traitorCount);
        } while (redeterminisationCount < 200 && !traitorsCompatibleWithHistory());
        if (redeterminisationCount > 10 && debug) {
            log(redeterminisationCount + " randomisations until compatible with defection history");
        }
    }

    protected void resetTraitors(ResistanceDeterminisationMemory memory) {
        boolean[] oldTraitors = memory.getTraitors();
        boolean[] currentTraitors = traitorIdentities;
        traitorIdentities = oldTraitors;
        if (!traitorsCompatibleWithHistory()) {
            throw new AssertionError("History is not compatible with suggested traitors");
        }
        updatePlayersWithTraitorInformation();
    }

    private boolean traitorsCompatibleWithHistory() {
        for (List<Integer> failedTeam : failedMissionDetails) {
            int observedDefections = failedTeam.get(failedTeam.size() - 1);
            int traitorsOnMission = 0;
            for (int i = 0; i < failedTeam.size() - 1; i++) {
                if (traitorIdentities[failedTeam.get(i)])
                    traitorsOnMission++;
            }
            if (observedDefections > traitorsOnMission) {
                return false;
            }
        }
        return true;
    }

    protected void updatePlayersWithTraitorInformation() {
        for (int i = 1; i <= playerCount; i++) {
            allPlayers[i].setTraitor(traitorIdentities[i]);
            if (traitorIdentities[i] == true) {
                for (int j = 1; j < playerCount; j++) {
                    if (j != i && traitorIdentities[j]) allPlayers[i].setOtherTraitor(j);
                }
            }
        }
    }

    @Override
    public boolean isCompatibleWith(Game<ResistancePlayer, ActionEnum<ResistancePlayer>> otherGame, ActionWithRef<ResistancePlayer> actionRef) {
        // incompatible iff actionRef is the end result of a mission, and has more defections than traitors present
        throw new AssertionError("Not yet implemented");
    }

    @Override
    public ResistancePlayer getCurrentPlayer() {
        return allPlayers[getCurrentPlayerRef()];
    }

    @Override
    public List<ResistancePlayer> getAllPlayers() {
        return Arrays.stream(allPlayers).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public int getPlayerNumber(ResistancePlayer player) {
        for (int i = 1; i <= playerCount; i++)
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
                if (traitorIdentities[getCurrentPlayerRef()])
                    retValue.add(new Defect());
                retValue.add(new Cooperate());
                break;
        }
        return retValue;
    }

    @Override
    public boolean gameOver() {
        return failedMissions >= 3 || successfulMissions >= 3 || failedVotes >= 5;
    }

    public boolean spiesHaveWon() {
        return failedMissions >= 3 || failedVotes >= 5;
    }

    public int getMission() {
        return mission;
    }

    public int getSuccessfulMissions() {
        return successfulMissions;
    }

    public int getFailedVotes() {
        return failedVotes;
    }

    private void updateCurrentPlayer() {
        int newPlayer = (getCurrentPlayerRef() + 1) % playerCount;
        if (newPlayer == 0) newPlayer = playerCount;
        changeCurrentPlayer(newPlayer);
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
                    changeCurrentPlayer(firstPlayer);
                    failedVotes = 0;
                    if (debug)
                        log(String.format("Mission Team proposal: %s", HopshackleUtilities.formatList(currentMissionTeam, ", ", null)));
                } else {
                    // continue
                }
                break;
            case VOTE:
                updateCurrentPlayer();
                if (getCurrentPlayerRef() == firstPlayer) {
                    // everyone has now voted
                    applyGameAction(new VoteResult(missionTeamVotes), calendar.getTime());
                    int votesInFavour = 0;
                    for (int i = 1; i < missionTeamVotes.length; i++) {
                        if (missionTeamVotes[i]) votesInFavour++;
                    }
                    if (debug)
                        log(String.format("%d out of %d votes in favour of proposed team", votesInFavour, playerCount));
                    if (votesInFavour > playerCount / 2.0) {
                        currentPhase = Phase.MISSION;
                        changeCurrentPlayer(currentMissionTeam.get(0));
                        defectorsOnMission = 0;
                    } else {
                        updateFirstPlayer();
                        changeCurrentPlayer(firstPlayer);
                        failedVotes++;
                        missionTeamVotes = new boolean[playerCount + 1];
                        currentPhase = Phase.ASSEMBLE;
                        currentMissionTeam = new ArrayList();
                    }
                }
                break;
            case MISSION:
                if (getCurrentPlayerRef() == currentMissionTeam.get(teamMembersByMissionandPlayercount[mission][playerCount] - 1)) {
                    // last member of team to decide...so move on to next phase
                    applyGameAction(new MissionResult(currentMissionTeam, defectorsOnMission), calendar.getTime());
                    if (defectorsOnMission == 0) {
                        successfulMissions++;
                    } else {
                        failedMissions++;
                        currentMissionTeam.add(defectorsOnMission);
                        failedMissionDetails.add(currentMissionTeam);
                    }
                    if (debug) log(String.format("%d total defectors on mission", defectorsOnMission));
                    currentPhase = Phase.ASSEMBLE;
                    mission++;
                    calendar.setTime(mission);
                    updateFirstPlayer();
                    changeCurrentPlayer(firstPlayer);
                    currentMissionTeam = new ArrayList();
                } else {
                    changeCurrentPlayer(currentMissionTeam.get(currentMissionTeam.indexOf(getCurrentPlayerRef()) + 1));
                }
                break;
        }
    }

    // no method for cooperation, as this is the default assumption
    public void defect(int player) {
        if (getCurrentPlayerRef() == player && currentPhase == Phase.MISSION && traitorIdentities[player]) {
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
        return String.format("Resistance Game %d: Phase %s Mission %d, with %d failed missions so far", getRef(), currentPhase, mission, failedMissions);
    }

    @Override
    public String logName() {
        return String.format("Resistance %s", getRef());
    }

    public Phase getPhase() {
        return currentPhase;
    }

    @Override
    public AllPlayerDeterminiser getAPD(int player) {
        throw new AssertionError("Not yet implemented");
    }
}
