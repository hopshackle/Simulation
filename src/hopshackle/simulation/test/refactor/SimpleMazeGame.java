package hopshackle.simulation.test.refactor;

import hopshackle.simulation.*;
import hopshackle.simulation.games.AllPlayerDeterminiser;
import hopshackle.simulation.games.Game;
import hopshackle.simulation.games.GameDeterminisationMemory;

import java.util.*;

public class SimpleMazeGame extends Game<TestAgent, ActionEnum<TestAgent>> {

    int target = 0;
    int numberOfPlayers;
    TestAgent[] players;
    double[] reward;
    List<ActionEnum<TestAgent>> allActions = new ArrayList<ActionEnum<TestAgent>>(EnumSet.allOf(TestActionEnum.class));

    public SimpleMazeGame(int target, TestAgent player) {
        this(target, new TestAgent[]{player});
    }

    @Override
    public Game<TestAgent, ActionEnum<TestAgent>> cloneLocalFields() {
        World clonedWorld = new World();
        long currentTime = getCurrentPlayer().getWorld().getCurrentTime();
        clonedWorld.setCalendar(new FastCalendar(currentTime));

        TestAgent[] clonedPlayers = new TestAgent[numberOfPlayers];
        for (int i = 0; i < numberOfPlayers; i++) {
            TestAgent original = players[i];
            TestAgent clonedPlayer = new TestAgent(clonedWorld);
            clonedPlayer.setAge(original.getAge());
            clonedPlayer.position = original.position;
            clonedPlayer.addGold(original.getGold());
            clonedPlayers[i] = clonedPlayer;
        }

        SimpleMazeGame retValue = new SimpleMazeGame(target, clonedPlayers);
        retValue.reward = new double[numberOfPlayers];
        for (int i = 0; i < numberOfPlayers; i++) {
            retValue.reward[i] = this.reward[i];
        }
        return retValue;
    }

    @Override
    public boolean isCompatibleWith(Game<TestAgent, ActionEnum<TestAgent>> otherState, ActionWithRef<TestAgent> actionRef) {
        return true;
    }

    @Override
    public void redeterminise(int perspectivePlayer, int ISPlayer, Optional<Game> rootGame) {
    }
//    @Override
//    public void undeterminise(GameDeterminisationMemory referenceData) {}

    @Override
    public void redeterminiseKeepingHiddenActions(int perspectivePlayer, int ISPlayer, Optional<Game> rootGame) {
        throw new AssertionError("Not implemented");
    }

    public String getRef() {
        return "SimpleMazeGame";
    }

    public SimpleMazeGame(int target, TestAgent[] players) {
        numberOfPlayers = players.length;
        this.target = target;
        this.players = new TestAgent[numberOfPlayers];
        reward = new double[numberOfPlayers];
        TestActionEnum.defaultMakeNextDecision = false;
        TestActionEnum.waitTime = 0;
        for (int i = 0; i < numberOfPlayers; i++) {
            this.players[i] = players[i];
            this.players[i].setGame(this);
        }
        changeCurrentPlayer(1);
    }

    @Override
    public boolean gameOver() {
        for (int i = 0; i < numberOfPlayers; i++) {
            TestAgent player = players[i];
            if (player.position >= target || player.decisionsTaken > 100) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void endOfGameHouseKeeping() {

    }

    @Override
    public String logName() {
        return "SimpleMazeGame";
    }

    private void nextPlayer() {
        int nextPlayer = getCurrentPlayerRef();
        if (nextPlayer == numberOfPlayers) nextPlayer = 0;
        changeCurrentPlayer(nextPlayer +1);
    }

    @Override
    public TestAgent getCurrentPlayer() {
        return players[getCurrentPlayerRef()-1];
    }

    @Override
    public List<TestAgent> getAllPlayers() {
        List<TestAgent> retValue = new ArrayList<TestAgent>();
        for (int i = 0; i < numberOfPlayers; i++)
            retValue.add(players[i]);
        return retValue;
    }

    @Override
    public int getPlayerNumber(TestAgent player) {
        for (int i = 0; i < numberOfPlayers; i++) {
            if (player == players[i]) return i + 1;
        }
        return 0;
    }

    @Override
    public TestAgent getPlayer(int n) {
        return players[n - 1];
    }

    @Override
    public List<ActionEnum<TestAgent>> getPossibleActions() {
        return allActions;
    }

    @Override
    public void updateGameStatus() {
        calendar.setTime(calendar.getTime() + 1000);
        nextPlayer();
    }

    @Override
    public AllPlayerDeterminiser getAPD(int player) {
        throw new AssertionError("Not yet implemented");
    }
}
