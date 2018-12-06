package hopshackle.simulation.games;

import hopshackle.simulation.*;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.util.*;

public abstract class Game<P extends Agent, A extends ActionEnum<P>> {

    protected double[] finalScores;
    private EntityLog log;
    private static SimpleGameScoreCalculator simpleGameScoreCalculator = new SimpleGameScoreCalculator();
    public static final int MAX_TURNS = SimProperties.getPropertyAsInteger("MaxTurnsPerGame", "50000");
    public boolean debug = false;
    protected GameScoreCalculator scoreCalculator;
    protected WorldCalendar calendar;
    protected List<Triplet<State<P>, ActionWithRef<P>, Long>> trajectory = new ArrayList();
    private List<GameListener<P>> listeners = new ArrayList<>();

    public abstract Game<P, A> clone(P perspectivePlayer);

    public void cloneCoreFieldsFrom(Game original) {
        scoreCalculator = original.scoreCalculator;
  //      trajectory = HopshackleUtilities.cloneList(original.trajectory);
        // Each element of trajectory is immutable, so not cloned
    }

    public abstract String getRef();

    public abstract P getCurrentPlayer();

    public abstract List<P> getAllPlayers();

    public abstract int getPlayerNumber(P player);

    public abstract P getPlayer(int n);

    public abstract List<ActionEnum<P>> getPossibleActions();

    public abstract boolean gameOver();

    /*
     * Called after each move processed
     * Implement game specific housekeeping to move game status forward (such as incrementing turn number, changing the active player)
     */
    public abstract void updateGameStatus();

    public final double[] playGame() {
        return playGame(0);
    }

    public final double[] playGame(int maxMoves) {

        if (maxMoves == 0) maxMoves = Integer.MAX_VALUE;
        int actions = 0;
        while (!gameOver() && actions < maxMoves) {
            oneAction();
            actions++;
        }

        forceGameEnd();

        if (debug) {
            log(String.format("Finished Game after %d actions, GameOver: %s, and scores %s", actions, gameOver(),
                    HopshackleUtilities.formatArray(finalScores, ", ", "%.2f")));
            log.flush();
        }

        return finalScores;
    }

    public double[] getFinalScores() {
        if (finalScores == null)
            throw new AssertionError("Game is not yet over");
        return finalScores;
    }

    public int getOrdinalPosition(int p) {
        // we count how many scores are less than or equal to the players score
        int numberOfPlayers = getAllPlayers().size();
        int retValue = numberOfPlayers + 1;
        double[] score = new double[numberOfPlayers];
        for (int i = 0; i < numberOfPlayers; i++) {
            score[i] = getPlayer(i + 1).getScore();
        }
        for (double s : score) {
            if (s <= score[p - 1]) retValue--;
        }
        return retValue;
    }

    public void forceGameEnd() {
        if (scoreCalculator != null) {
            forceGameEnd(scoreCalculator);
        } else {
            forceGameEnd(simpleGameScoreCalculator);
        }
    }

    public void forceGameEnd(GameScoreCalculator calc) {
        endOfGameHouseKeeping();
        finalScores = calc.finalScores(this);
        if (debug) log("Final Scores: " + HopshackleUtilities.formatArray(finalScores, ", ", "%.2f"));
        for (int i = 1; i <= finalScores.length; i++)
            getPlayer(i).die("Game Over");
        if (log != null) log.close();
    }

    protected void endOfGameHouseKeeping() {
        // may be overridden
        if (log != null) log.close();
        sendMessage(new GameEvent(GameEvent.Type.GAME_OVER, this));
    }

    public void oneAction() {
        P currentPlayer = getCurrentPlayer();
        List<ActionEnum<P>> options = getPossibleActions();
        Decider<P> decider = currentPlayer.getDecider();
        Action<P> action = decider.decide(currentPlayer, options);
        applyAction(action);
    }

    /* Note that the action is assumed to have a link to the Game
    and does not stand separate...it is responsible for updatign the game state
     */
    public void applyAction(Action action) {
        if (action == null) return;
        P currentPlayer = getCurrentPlayer();
        Decider<P> decider = currentPlayer.getDecider();
        trajectory.add(new Triplet(decider.getCurrentState(currentPlayer), new ActionWithRef(action.getType(), getPlayerNumber(getCurrentPlayer())), action.getStartTime()));

        action.addToAllPlans(); // this is for compatibility with Action statuses in a real-time simulation
        // it also means that each agent tracks the actions they execute over a game
        action.start();
        action.run();

        sendMessage(new GameEvent(new ActionWithRef<>(action.getType(), currentPlayer.getActorRef()),this));
        if (debug)
            log(getCurrentPlayer().toString() + "  : " + action.toString());

        updateGameStatus();
    }

    public void log(String message) {
        System.out.println(message);
        if (log == null) {
            log = new EntityLog(toString(), calendar);
        }
        log.log(message);
        log.flush();
    }

    public void setCalendar(WorldCalendar cal) {
        calendar = cal;
    }

    public List<Triplet<State<P>, ActionWithRef<P>, Long>> getTrajectory() {
        return trajectory;
    }

    public void registerListener(GameListener<P> listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }
    protected void sendMessage(GameEvent event) {
        listeners.stream().forEach(l -> l.processGameEvent(event));
    }
}

