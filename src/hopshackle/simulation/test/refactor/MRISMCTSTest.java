package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;

import hopshackle.simulation.MCTS.*;
import hopshackle.simulation.*;

import java.util.*;
import java.util.stream.IntStream;

import hopshackle.simulation.games.*;
import org.junit.*;

public class MRISMCTSTest {

    List<GeneticVariable<MRISAgent>> genVar = new ArrayList<>(EnumSet.allOf(MRISGenVar.class));
    LinearStateFactory<MRISAgent> factory = new LinearStateFactory<>(genVar);
    MRISTestDecider MRISDecider;
    DeciderProperties localProp;

    @Before
    public void setup() {
        localProp = SimProperties.getDeciderProperties("GLOBAL");
        localProp.setProperty("MonteCarloReward", "true");
        localProp.setProperty("MonteCarloRL", "false");
        localProp.setProperty("MonteCarloUCTType", "MC");
        localProp.setProperty("MonteCarloUCTC", "1.0");
        localProp.setProperty("Gamma", "0.95");
        localProp.setProperty("TimePeriodForGamma", "1000");
        localProp.setProperty("IncrementalScoreReward", "false");
        localProp.setProperty("MonteCarloTimePerMove", "1000");
        localProp.setProperty("MonteCarloRolloutCount", "99");
        localProp.setProperty("MonteCarloActionValueRollout", "false");
        localProp.setProperty("MonteCarloActionValueOpponentModel", "false");
        localProp.setProperty("MonteCarloActionValueDeciderTemperature", "0.0");
        localProp.setProperty("MonteCarloRetainTreeBetweenActions", "false");
        localProp.setProperty("MonteCarloOpenLoop", "true");
        localProp.setProperty("MonteCarloTree", "ignoreOthers");
        localProp.setProperty("MonteCarloChoice", "default");
        localProp.setProperty("MonteCarloHeuristicOnSelection", "false");
        localProp.setProperty("MonteCarloRetainTreeBetweenActions", "false");
        localProp.setProperty("MonteCarloMAST", "false");
        localProp.setProperty("MaxTurnsPerGame", "10000");
        localProp.setProperty("GameOrdinalRewards", "0");
    }

    @Test
    public void confirmThatWeOnlyDeterminiseOnceWithAnEmptyTree() {
        MRISTestGame game = new MRISTestGame(10, true);
        MRISDecider = new MRISTestDecider(game, factory, new RandomDecider<>(factory), new RandomDecider<>(factory));
        MRISDecider.injectProperties(localProp);
        // We never return to the first player, which means that we will never exit the tree, and
        // hence redeterminise at each move

        MonteCarloTree<MRISAgent> tree = MRISDecider.getTree(game.getCurrentPlayer());
        MRISDecider.executeSearch(game);

        assertEquals(game.playerChange, 9, 2);
        int updates = game.redeterminisationCounts.values().stream().mapToInt(i -> i).sum();
        assertEquals(updates, 1);
        // just the once on initialisation. The first action we take will expand a node, so we then stop determinising
    }

    @Test
    public void confirmThatCallsToRedeterminiseCoincideWithPlayerChange() {
        MRISTestGame game = new MRISTestGame(10, true);
        MRISDecider = new MRISTestDecider(game, factory, new RandomDecider<>(factory), new RandomDecider<>(factory));
        MRISDecider.injectProperties(localProp);
        // We never return to the first player, which means that we will never exit the tree, and
        // hence redeterminise at each move

        MonteCarloTree<MRISAgent> tree = MRISDecider.getTree(game.getCurrentPlayer());
        // we now set up two transitions in the tree, so that our first action should not stop redeterminisation
        tree.setUpdatesLeft(2);
        tree.getRootStatistics().update(MRISAction.Type.MRIS_ACTION_1, new double[10], 1);
        tree.getRootStatistics().update(MRISAction.Type.MRIS_ACTION_2, new double[10], 1);
        assertEquals(tree.updatesLeft(), 0);
        MRISDecider.executeSearch(game);

        assertEquals(game.playerChange, 10);
        int updates = game.redeterminisationCounts.values().stream().mapToInt(i -> i).sum();
        assertEquals(updates, 11); // playerChanges + 1
    }

    @Test
    public void weStopRedeterminisingOnceOutsideTree() {
        MRISTestGame game = new MRISTestGame(100, false);
        MRISDecider = new MRISTestDecider(game, factory, new RandomDecider<>(factory), new RandomDecider<>(factory));
        MRISDecider.injectProperties(localProp);
        // this time, we will stop redeterminising once we return to the first player

        MonteCarloTree<MRISAgent> tree = MRISDecider.getTree(game.getCurrentPlayer());
        tree.setUpdatesLeft(2);
        tree.getRootStatistics().update(MRISAction.Type.MRIS_ACTION_1, new double[10], 1);
        tree.getRootStatistics().update(MRISAction.Type.MRIS_ACTION_2, new double[10], 1);
        assertEquals(tree.updatesLeft(), 0);

        MRISDecider.executeSearch(game);

        assertEquals(game.playerChange, 90, 10);
        int updates = game.redeterminisationCounts.values().stream().mapToInt(i -> i).sum();
        assertTrue(updates < 40);
        assertTrue(updates > 1);
    }

    @Test
    public void weStopDeterminisingOnceOutsidePerPLayerTree() {
        localProp.setProperty("MonteCarloTree", "perPlayer");
        weStopRedeterminisingOnceOutsideTree();
    }
}

/*
This is a stub game with 10 players, with a single Action (MRIS_ACTION).
On each turn the current player takes a turn, and play passes on to a random player (possibly the same)
This continues until we have reached the maximum turn count
 */
class MRISTestGame extends Game<MRISAgent, ActionEnum<MRISAgent>> {

    private static World world = new World();
    Map<Integer, Integer> redeterminisationCounts = new HashMap<>();
    MRISAgent[] players = new MRISAgent[10];
    private boolean ignoreFirstPlayer;
    private int maxTurns, currentTurn;
    public int playerChange;

    public MRISTestGame(int turns, boolean ignoreFirstPlayer) {
        IntStream.range(0, 10).forEach(i -> {
            players[i] = new MRISAgent(world, this);
        });
        currentTurn = 0;
        maxTurns = turns;
        this.ignoreFirstPlayer = ignoreFirstPlayer;
        changeCurrentPlayer(1);
    }

    @Override
    public Game cloneLocalFields() {
        MRISTestGame retValue = new MRISTestGame(maxTurns, ignoreFirstPlayer);
        retValue.currentTurn = currentTurn;
        retValue.playerChange = playerChange;
        redeterminisationCounts.entrySet().forEach(entry -> retValue.redeterminisationCounts.put(entry.getKey(), entry.getValue()));
        return retValue;
    }

    @Override
    public boolean isCompatibleWith(Game otherState, ActionWithRef actionRef) {
        return true;
    }

    @Override
    public void redeterminise(int perspectivePlayer, int ISPlayer, Optional<Game> rootGame) {
        int newCount = redeterminisationCounts.getOrDefault(perspectivePlayer, 0) + 1;
        redeterminisationCounts.put(perspectivePlayer, newCount);
    }

    @Override
    public void redeterminiseKeepingHiddenActions(int perspectivePlayer, int ISPlayer, Optional<Game> rootGame) {
        throw new AssertionError("Not implemented");
    }

    @Override
    public AllPlayerDeterminiser getAPD(int perspectivePlayer) {
        throw new AssertionError("Not implemented");
    }

    @Override
    public MRISAgent getCurrentPlayer() {
        return players[getCurrentPlayerRef() - 1];
    }

    @Override
    public List<MRISAgent> getAllPlayers() {
        return HopshackleUtilities.convertArrayToList(players);
    }

    @Override
    public int getPlayerNumber(MRISAgent player) {
        for (int i = 0; i < players.length; i++) {
            if (players[i] == player) return i + 1;
        }
        return 0;
    }

    @Override
    public MRISAgent getPlayer(int n) {
        return players[n - 1];
    }

    @Override
    public List<ActionEnum<MRISAgent>> getPossibleActions() {
        List<ActionEnum<MRISAgent>> retValue = new ArrayList<>();
        retValue.add(MRISAction.Type.MRIS_ACTION_1);
        retValue.add(MRISAction.Type.MRIS_ACTION_2);
        return retValue;
    }

    @Override
    public boolean gameOver() {
        return currentTurn >= maxTurns;
    }

    @Override
    public void updateGameStatus() {
        currentTurn++;
        calendar.setTime(currentTurn);
        int oldPlayer = getCurrentPlayerRef();
        int newPlayer = oldPlayer;
        do {
            newPlayer = ignoreFirstPlayer ? Dice.roll(1, 9) + 1 : Dice.roll(1, 10);
        } while (newPlayer == oldPlayer);
        changeCurrentPlayer(newPlayer);
        if (getCurrentPlayerRef() != oldPlayer) playerChange++;
    }

    @Override
    public String logName() {
        return "MRIS";
    }
}

class MRISAgent extends Agent {

    private MRISTestGame game;

    public MRISAgent(World w, MRISTestGame g) {
        super(w);
        game = g;
    }

    @Override
    public MRISTestGame getGame() {
        return game;
    }
}

class MRISAction extends GameAction<MRISAgent> {

    public enum Type implements GameActionEnum<MRISAgent> {
        MRIS_ACTION_1,
        MRIS_ACTION_2;

        @Override
        public Action<MRISAgent> getAction(MRISAgent mrisAgent) {
            return new MRISAction(this, mrisAgent);
        }
    }

    public MRISAction(Type t, MRISAgent a) {
        super(t, a);
    }
}


enum MRISGenVar implements GeneticVariable<MRISAgent> {
    CONSTANT,
    PLAYER,
    AGE;

    @Override
    public double getValue(MRISAgent a1) {
        switch (this) {
            case CONSTANT:
                return 1.0;
            case PLAYER:
                return a1.getActorRef();
            case AGE:
                return a1.getGame().getTime();
        }
        return 1.0;
    }

    @Override
    public String getDescriptor() {
        return "MRIS_GV";
    }

    @Override
    public boolean unitaryRange() {
        return true;
    }

}

class MRISTestDecider extends MRISMCTSDecider<MRISAgent> {

    public MRISTestDecider(Game<MRISAgent, ActionEnum<MRISAgent>> game, StateFactory<MRISAgent> stateFactory, BaseStateDecider<MRISAgent> rolloutDecider, Decider<MRISAgent> opponentModel) {
        super(game, stateFactory, rolloutDecider, opponentModel);
    }

    @Override
    public void executeSearch(Game<MRISAgent, ActionEnum<MRISAgent>> clonedGame) {
        preIterationProcessing(clonedGame, clonedGame);
        super.executeSearch(clonedGame);
        postIterationProcessing(clonedGame);
    }
}