package hopshackle.simulation;

import java.util.*;

public abstract class Game<P extends Agent, A extends ActionEnum<P>> implements WorldLogic<P>, Persistent {

    protected Stack<Action<P>> actionStack = new Stack<Action<P>>();
    protected double[] finalScores;
    private EntityLog log;
    private static SimpleGameScoreCalculator simpleGameScoreCalculator = new SimpleGameScoreCalculator();
    public static final int MAX_TURNS = SimProperties.getPropertyAsInteger("MaxTurnsPerGame", "50");
    public boolean debug = false;
    protected GameScoreCalculator scoreCalculator;
    private World world = new World();
    private FastCalendar calendar;
    protected List<P> players = new ArrayList();
    protected List<P> masters = new ArrayList();
    protected Map<P, P> masterAgents = new HashMap();

    {
        calendar = new FastCalendar(1l);
        world.setCalendar(calendar);
        world.registerWorldLogic(this, "AGENT");
    }

    public abstract Game<P, A> clone(P perspectivePlayer);

    public abstract String getRef();

    public abstract P getCurrentPlayer();

    public List<P> getAllPlayers() {
        return HopshackleUtilities.cloneList(players);
    }

    public int getPlayerNumber(P player) {
        int actorNumber = players.indexOf(player);
        return actorNumber + 1;
    }

    public P getPlayer(int n) {
        return players.get(n - 1);
    }

    public P getMasterOf(P agent) {
        return masterAgents.getOrDefault(agent, agent);
    }

    public int getMasterNumber(P master) {
        int actorNumber = masters.indexOf(master);
        return actorNumber + 1;
    }

    public List<P> getAllPlayersWithMaster(P master) {
        List<P> retValue = new ArrayList();
        for (P p : players) {
            if (getMasterOf(p).equals(master))
                retValue.add(p);
        }
        return retValue;
    }

    public List<P> getMasters() {
        return HopshackleUtilities.cloneList(masters);
    }

    public abstract int getCurrentPlayerNumber();

    public abstract List<ActionEnum<P>> getPossibleActions(P player);

    public abstract boolean gameOver();

    /*
     * Called after each move processed, and the stack is empty.
     * Implement game specific housekeeping to move game status forward (such as incrementing turn number, changing the active player)
     */
    public abstract void updateGameStatus();

    public final double[] playGame() {
        return playGame(0);
    }

    public final double[] playGame(int maxMoves) {

        if (maxMoves == 0) maxMoves = Integer.MAX_VALUE;
        int moves = 0;
        while (!gameOver() && moves < maxMoves) {
            oneAction();
            moves++;
        }

        forceGameEnd();

        if (debug) {
            log(String.format("Finished Game after %d moves, GameOver: %s, and scores %s", moves, gameOver(),
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
        int numberOfPlayers = masters.size();
        int retValue = numberOfPlayers + 1;
        double[] score = new double[numberOfPlayers];
        for (int i = 0; i < numberOfPlayers; i++) {
            score[i] = masters.get(i).getScore();
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
        for (P actor : players)
            actor.die("Game Over");
        for (P master : masters) {
            master.die("Game Over");
        }
        if (log != null) log.close();
    }

    protected void endOfGameHouseKeeping() {
        // may be overridden
        if (log != null) log.close();
    }

    public final void oneAction() {
        oneAction(false, false);
    }

    public final void oneAction(boolean noUpdate, boolean singleAction) {
        P currentPlayer = null;
        List<ActionEnum<P>> options = null;
        Decider<P> decider = null;
        Action<P> action = null;

        do {
            if (action != null) {
                // this occurs if we popped an action off the stack on the last iteration
                // so we already have the action we wish to execute
            } else {
                // we have completed the last one, so pick a new action
                if (!actionStack.isEmpty()) { // actionStack first, to complete interrupts and consequences
                    options = actionStack.peek().getNextOptions();
                    if (options.isEmpty()) {
                        // then we take the followOnAction instead
                        action = nextFollowOnActionFromStack();
                    } else {
                        currentPlayer = actionStack.peek().getNextActor();
                        decider = currentPlayer.getDecider();
                        action = decider.decide(currentPlayer, options);
                    }
                }
                if (action == null) {    // otherwise we get a new action
                    currentPlayer = getCurrentPlayer();
                    options = getPossibleActions(currentPlayer);
                    decider = currentPlayer.getDecider();
                    action = decider.decide(currentPlayer, options);
                }
            }

            action.addToAllPlans(); // this is for compatibility with Action statuses in a real-time simulation
            action.start();
            action.run();
            if (debug) log(currentPlayer.toString() + "  : " + action.toString());
            options = action.getNextOptions();
            if (options.isEmpty()) {
                if (actionStack.isEmpty()) {
                    action = null;
                } else {
                    action = nextFollowOnActionFromStack();
                }
            } else {
                actionStack.push(action);
                action = null;
            }

            if (action == null && actionStack.isEmpty() && !noUpdate)
                updateGameStatus(); // finished the last cycle, so move game state forward
            // otherwise, we still have interrupts/consequences to deal with

            if (singleAction && action == null) break;

        } while (action != null || !actionStack.isEmpty());
    }

    private Action<P> nextFollowOnActionFromStack() {
        Action<P> retValue = null;
        do {
            retValue = actionStack.pop().getFollowOnAction();
        } while (retValue == null && !actionStack.isEmpty());
        return retValue;
    }

    public void log(String message) {
        if (log == null) {
            log = new EntityLog(toString(), world);
        }
        log.log(message);
        log.flush();
    }

    @Override
    public World getWorld() {
        return world;
    }

    public void setWorld(World w) {
        world = w;
    }

    public long getTime() {
        return calendar.getTime();
    }

    public void setTime(long t) {
        calendar.setTime(t);
    }

    public void setDatabaseAccessUtility(DatabaseAccessUtility databaseUtility) {
        world.setDatabaseAccessUtility(databaseUtility);
    }

    public double[] currentScore() {
          // we only have a score for each master player
        double[] retValue = new double[masters.size()];
        for (int i = 0; i < masters.size(); i++) {
            retValue[i] = masters.get(i).getScore();
        }
        return retValue;
    }
}

