package hopshackle.simulation.games;

import hopshackle.simulation.*;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public abstract class Game<P extends Agent, A extends ActionEnum<P>> {

    private static AtomicLong idFountain = new AtomicLong(1);
    protected double[] finalScores;
    private EntityLog log;
    private static SimpleGameScoreCalculator simpleGameScoreCalculator = new SimpleGameScoreCalculator();
    public boolean debug = false;
    protected GameScoreCalculator scoreCalculator;
    protected WorldCalendar calendar = new GameTurnCalendar(1);
    protected List<Pair<ActionWithRef<P>, Long>> trajectory = new ArrayList();
    private List<GameListener<P>> listeners = new ArrayList<>();
    private long refID = idFountain.getAndIncrement();
    private int currentPlayer;

    public abstract Game<P, A> cloneLocalFields();

    @Override
    public final Game<P, A> clone() {
        Game<P, A> retValue = this.cloneLocalFields();
        retValue.cloneCoreFieldsFrom(this);
        return retValue;
    }

    private void cloneCoreFieldsFrom(Game<P, A> original) {
        currentPlayer = original.currentPlayer;
        scoreCalculator = original.scoreCalculator;
        calendar = new GameTurnCalendar(original.calendar.getTime());
        //      trajectory = HopshackleUtilities.cloneList(original.trajectory);
        // Each element of trajectory is immutable, so not cloned
    }

    /*
    This Game is the master for this method, so otherState is compatible as long as the state is
    one that thisPerspective believes that otherPerspective could believe.
    This is false if thisPerspective and otherPerspective have information in common, and this subset is not
    identical between the two states.
     */
    public abstract boolean isCompatibleWith(Game<P, A> otherState, ActionWithRef<P> actionRef);

    /*
    Redeterminises hidden information from that player's perspective (WARNING: mutable state)
    TODO: In the future we may need to apply a distribution over belief states
    */
    public abstract void redeterminise(int perspectivePlayer);

    /*
    Returns an AllPlayerDeterminiser that redeterminises from the perspective of every agent apart from the perspectivePlayer
    This will also clone the game (N-1) times
     */
    public abstract AllPlayerDeterminiser<?, P> getAPD(int perspectivePlayer);

    /*
    Undeterminises by taking the current game as the base, and updating (WARNING: mutable state) it to
    be compatible with referenceData. If the observed game history of this is incompatible with
    referenceData, then this wins out.
     */
 //   public abstract void undeterminise(GameDeterminisationMemory referenceData);

    public String getRef() {
        return String.valueOf(refID);
    }
    public long getID() { return refID;}

    protected void changeCurrentPlayer(int newPlayer) {
        int oldPlayer = currentPlayer;
        currentPlayer = newPlayer;
        if (oldPlayer != newPlayer) sendMessage(new GameEvent(oldPlayer, this));
    }
    public final int getCurrentPlayerRef() {
        return currentPlayer;
    }

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
        }

        return finalScores;
    }

    public long getTime() {
        return calendar.getTime();
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
    and does not stand separate...it is responsible for updating the game state
     */
    public void applyAction(Action action) {
        if (action == null) return;
        int actorRef = action.getActor().getActorRef();
        if (actorRef != getCurrentPlayerRef()) {
            throw new AssertionError("Only the current Player should really be taking actions");
        }

        // we update the trajectory now, as we need to record this from the startState, in which the decision was taken
        trajectory.add(new Pair(new ActionWithRef(action.getType(), actorRef), action.getStartTime()));
        sendMessage(new GameEvent(new ActionWithRef<>(action.getType(), actorRef), this));

        action.addToAllPlans(); // this is for compatibility with Action statuses in a real-time simulation
        // it also means that each agent tracks the actions they execute over a game
        action.start();
        action.run();

        if (debug)
            log(getCurrentPlayer().toString() + "  : " + action.toString());

        updateGameStatus();
    }

    protected void applyGameAction(GameActionEnum<P> action, long startTime) {
        trajectory.add(new Pair(new ActionWithRef(action, -1), startTime));
        sendMessage(new GameEvent(new ActionWithRef<>(action, -1), this));
    }

    public void log(String message) {
  //      System.out.println(message);
        if (log == null) {
            log = new EntityLog(logName() + ".log", calendar);
        }
        log.log(message);
        log.flush();
    }

    public abstract String logName();

    public void setCalendar(WorldCalendar cal) {
        calendar = cal;
    }

    public List<Pair<ActionWithRef<P>, Long>> getTrajectory() {
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

