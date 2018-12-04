package hopshackle.simulation.test.refactor;

import java.util.*;

import hopshackle.simulation.MCTS.*;
import hopshackle.simulation.*;

import static org.junit.Assert.*;

import hopshackle.simulation.games.GameEvent;
import org.junit.*;

public class OpenLoopStateFactoryTest {

    Map<Integer, MonteCarloTree<TestAgent>> trees = new HashMap<>();
    DeciderProperties localProp;
    OpenLoopStateFactory<TestAgent> factory;
    SimpleMazeGame masterGame;
    MCTSMasterDecider<TestAgent> masterDecider;
    TestAgent[] masterPlayers;

    @Before
    public void setup() {
        localProp = SimProperties.getDeciderProperties("GLOBAL");
        localProp.setProperty("MonteCarloReward", "true");
        localProp.setProperty("MonteCarloRL", "false");
        localProp.setProperty("MonteCarloUCTType", "MC");
        localProp.setProperty("MonteCarloUCTC", "5.0");
        localProp.setProperty("Gamma", "1.0");
        localProp.setProperty("TimePeriodForGamma", "1000");
        localProp.setProperty("IncrementalScoreReward", "false");
        localProp.setProperty("MonteCarloRolloutCount", "99");
        localProp.setProperty("MonteCarloPriorActionWeightingForBestAction", "0");
        localProp.setProperty("MonteCarloActionValueRollout", "false");
        localProp.setProperty("MonteCarloActionValueOpponentModel", "false");
        localProp.setProperty("MonteCarloActionValueDeciderTemperature", "0.0");
        localProp.setProperty("MonteCarloRetainTreeBetweenActions", "false");
        localProp.setProperty("MaxTurnsPerGame", "10000");
        localProp.setProperty("GameOrdinalRewards", "0");
        localProp.setProperty("MonteCarloHeuristicOnExpansion", "true");
        localProp.setProperty("MonteCarloMAST", "true");
        localProp.setProperty("MonteCarloOpenLoop", "true");
        localProp.setProperty("MonteCarloHeuristicOnSelection", "false");
    }

    private void setUpTrees() {
        masterDecider = new MCTSMasterDecider<>(new SingletonStateFactory<>(), new SimpleMazeDecider(), new SimpleMazeDecider());
        masterDecider.injectProperties(localProp);
        Dice.setSeed(6l);
        masterPlayers = new TestAgent[3];
        World world = new World();

        for (int i = 0; i < 3; i++) {
            masterPlayers[i] = new TestAgent(world);
            masterPlayers[i].setDecider(masterDecider);
        }
        masterGame = new SimpleMazeGame(4, masterPlayers);
        masterGame.oneAction();
        for (int i = 0; i < 3; i++)
            trees.put(i + 1, (OpenLoopMCTree) masterDecider.getTree(masterPlayers[i]));
    }

    @Test
    public void singleTreeEventsTrackedCorrectly() {
        localProp.setProperty("MonteCarloSingleTree", "single");
        setUpTrees();
        // create an OLSF and tree
        // Run a MazeGame move to populate the tree
        // then check that processEvent descends the tree correctly
        TestAgent[] players = new TestAgent[3];
        World world = new World();
        for (int i = 0; i < 3; i++) {
            players[i] = new TestAgent(world);
        }
        SimpleMazeGame game = new SimpleMazeGame(4, players);
        factory = new OpenLoopStateFactory<>("single", trees, game);
        // should now be at root
        MCStatistics<TestAgent> root = trees.get(1).getRootStatistics();
        for (int j = 0; j < 3; j++)
            for (int i = 0; i < 3; i++) {
                OpenLoopState<TestAgent> state = (OpenLoopState) factory.getCurrentState(players[j]);
                assertEquals(state.currentPlayer, 1);
                assertTrue(state.currentNodesByPlayer.get(i + 1) == root);
            }
        MCStatistics<TestAgent> moveLeftNode = root.getSuccessorNode(new ActionWithRef(TestActionEnum.LEFT, 1));
        factory.processGameEvent(new GameEvent<>(new ActionWithRef<>(TestActionEnum.LEFT, 1), game));
        for (int j = 0; j < 3; j++)
            for (int i = 0; i < 3; i++) {
                OpenLoopState<TestAgent> state = (OpenLoopState) factory.getCurrentState(players[j]);
                assertEquals(state.currentPlayer, 2);
                assertTrue(state.currentNodesByPlayer.get(i + 1) == moveLeftNode);
            }
        MCStatistics<TestAgent> testNode = moveLeftNode.getSuccessorNode(new ActionWithRef(TestActionEnum.TEST, 2));
        factory.processGameEvent(new GameEvent<>(new ActionWithRef<>(TestActionEnum.TEST, 2), game));
        for (int j = 0; j < 3; j++)
            for (int i = 0; i < 3; i++) {
                OpenLoopState<TestAgent> state = (OpenLoopState) factory.getCurrentState(players[j]);
                assertEquals(state.currentPlayer, 3);
                assertTrue(state.currentNodesByPlayer.get(i + 1) == testNode);
            }
    }

    @Test
    public void multiTreeEventsTrackedCorrectly() {
        localProp.setProperty("MonteCarloSingleTree", "perPlayer");
        setUpTrees();
        // create an OLSF and tree
        // Run a MazeGame move to populate the tree
        // then check that processEvent descends the tree correctly
        TestAgent[] players = new TestAgent[3];
        World world = new World();
        for (int i = 0; i < 3; i++) {
            players[i] = new TestAgent(world);
        }
        SimpleMazeGame game = new SimpleMazeGame(4, players);
        factory = new OpenLoopStateFactory<>("perPlayer", trees, game);
        // should now be at root
        MCStatistics<TestAgent>[] root = new MCStatistics[3];
        for (int i = 0; i < 3; i++) root[i] = trees.get(i + 1).getRootStatistics();
        for (int j = 0; j < 3; j++) {
            OpenLoopState<TestAgent> state = (OpenLoopState) factory.getCurrentState(players[j]);
            assertTrue(state.currentNodesByPlayer.get(j + 1) == root[j]);
            for (int i = 0; i < 3; i++) {
                OpenLoopState<TestAgent> state2 = (OpenLoopState) factory.getCurrentState(players[i]);
                assertEquals(state2.currentPlayer, 1);
                assertEquals(state2.currentNodesByPlayer.get(i + 1).getVisits(), state.currentNodesByPlayer.get(j + 1).getVisits());
                if (i != j)
                    assertFalse(state2.currentNodesByPlayer.get(i + 1) == state.currentNodesByPlayer.get(j + 1));
            }
        }
        MCStatistics<TestAgent>[] moveLeftNode = new MCStatistics[3];
        for (int i = 0; i < 3; i++) {
            moveLeftNode[i] = root[i].getSuccessorNode(new ActionWithRef(TestActionEnum.LEFT, 1));
            for (int j = 0; j < i; j++) assertFalse(moveLeftNode[j] == moveLeftNode[i]);
        }
        factory.processGameEvent(new GameEvent<>(new ActionWithRef<>(TestActionEnum.LEFT, 1), game));
        for (int j = 0; j < 3; j++) {
            OpenLoopState<TestAgent> state = (OpenLoopState) factory.getCurrentState(players[j]);
            assertTrue(state.currentNodesByPlayer.get(j + 1) == moveLeftNode[j]);
            for (int i = 0; i < 3; i++) {
                OpenLoopState<TestAgent> state2 = (OpenLoopState) factory.getCurrentState(players[i]);
                assertEquals(state2.currentPlayer, 2);
                assertEquals(state2.currentNodesByPlayer.get(i + 1).getVisits(), state.currentNodesByPlayer.get(j + 1).getVisits());
                if (i != j)
                    assertFalse(state2.currentNodesByPlayer.get(i + 1) == state.currentNodesByPlayer.get(j + 1));
            }
        }
        MCStatistics<TestAgent>[] testNode = new MCStatistics[3];
        for (int i = 0; i < 3; i++) {
            testNode[i] = moveLeftNode[i].getSuccessorNode(new ActionWithRef(TestActionEnum.TEST, 2));
            for (int j = 0; j < i; j++) assertFalse(testNode[j] == testNode[i]);
        }
        factory.processGameEvent(new GameEvent<>(new ActionWithRef<>(TestActionEnum.TEST, 2), game));
        for (int j = 0; j < 3; j++) {
            OpenLoopState<TestAgent> state = (OpenLoopState) factory.getCurrentState(players[j]);
            assertTrue(state.currentNodesByPlayer.get(j + 1) == testNode[j]);
            for (int i = 0; i < 3; i++) {
                OpenLoopState<TestAgent> state2 = (OpenLoopState) factory.getCurrentState(players[i]);
                assertEquals(state2.currentPlayer, 3);
                assertEquals(state2.currentNodesByPlayer.get(i + 1).getVisits(), state.currentNodesByPlayer.get(j + 1).getVisits());
                if (i != j)
                    assertFalse(state2.currentNodesByPlayer.get(i + 1) == state.currentNodesByPlayer.get(j + 1));
            }
        }
    }

    @Test
    public void soloTreeEventsTrackedCorrectly() {
        localProp.setProperty("MonteCarloSingleTree", "ignoreOthers");
        setUpTrees();
        // create an OLSF and tree
        // Run a MazeGame move to populate the tree
        // then check that processEvent descends the tree correctly
        TestAgent[] players = new TestAgent[3];
        World world = new World();
        for (int i = 0; i < 3; i++) {
            players[i] = new TestAgent(world);
        }
        SimpleMazeGame game = new SimpleMazeGame(4, players);
        factory = new OpenLoopStateFactory<>("ignoreOthers", trees, game);
        // should now be at root
        MCStatistics<TestAgent>[] root = new MCStatistics[3];
        for (int i = 0; i < 3; i++) {
            root[i] = trees.get(i + 1).getRootStatistics();
            if (i == 0) {
                assertEquals(root[i].getVisits(), 99);
            } else {
                assertEquals(root[i].getVisits(), 0);
            }
        }
        for (int j = 0; j < 3; j++) {
            OpenLoopState<TestAgent> state = (OpenLoopState) factory.getCurrentState(players[j]);
            if (j == 0) {
                assertTrue(state.currentNodesByPlayer.get(j + 1) == root[j]);
                assertEquals(state.currentPlayer, j + 1);
            } else {
                assertEquals(state.currentPlayer, j + 1);
                assertEquals(state.currentNodesByPlayer.get(j + 1), root[j]);
            }
        }
        MCStatistics<TestAgent> moveLeftNode = root[0].getSuccessorNode(new ActionWithRef(TestActionEnum.LEFT, 1));
        factory.processGameEvent(new GameEvent<>(new ActionWithRef<>(TestActionEnum.LEFT, 1), game));
        for (int j = 0; j < 3; j++) {
            OpenLoopState<TestAgent> state = (OpenLoopState) factory.getCurrentState(players[j]);
            if (j == 0) {
                assertTrue(state.currentNodesByPlayer.get(j + 1) == moveLeftNode);
                assertEquals(state.currentPlayer, 1);
            } else {
                assertEquals(state.currentPlayer, j + 1);
                assertEquals(state.currentNodesByPlayer.get(j + 1), root[j]);
            }
        }
        MCStatistics<TestAgent> testNode = moveLeftNode.getSuccessorNode(new ActionWithRef(TestActionEnum.TEST, 2));
        assertNull(testNode);
        testNode = moveLeftNode.getSuccessorNode(new ActionWithRef(TestActionEnum.TEST, 1));
        factory.processGameEvent(new GameEvent<>(new ActionWithRef<>(TestActionEnum.TEST, 1), game));
        for (int j = 0; j < 3; j++) {
            OpenLoopState<TestAgent> state = (OpenLoopState) factory.getCurrentState(players[j]);
            if (j == 0) {
                assertTrue(state.currentNodesByPlayer.get(j + 1) == testNode);
                assertEquals(state.currentPlayer, 1);
            } else {
                assertEquals(state.currentPlayer, j + 1);
                assertEquals(state.currentNodesByPlayer.get(j + 1), root[j]);
            }
        }
    }

    @Test
    public void secondMoveInSoloTreeGameSetsUpSecondTreeOnly() {
        soloTreeEventsTrackedCorrectly();
        masterGame.oneAction();
        for (int i = 0; i < 3; i++)
            trees.put(i + 1, (OpenLoopMCTree) masterDecider.getTree(masterPlayers[i]));

        TestAgent[] players = new TestAgent[3];
        World world = new World();
        for (int i = 0; i < 3; i++) {
            players[i] = new TestAgent(world);
        }
        SimpleMazeGame game = new SimpleMazeGame(4, players);
        factory = new OpenLoopStateFactory<>("ignoreOthers", trees, game);
        // should now be at root
        MCStatistics<TestAgent>[] root = new MCStatistics[3];
        for (int i = 0; i < 3; i++) {
            root[i] = trees.get(i + 1).getRootStatistics();
            if (i != 2) {
                assertEquals(root[i].getVisits(), 99);
            } else {
                assertEquals(root[i].getVisits(), 0);
            }
        }
        for (int j = 0; j < 3; j++) {
            OpenLoopState<TestAgent> state = (OpenLoopState) factory.getCurrentState(players[j]);
            if (j != 2) {
                assertTrue(state.currentNodesByPlayer.get(j + 1) == root[j]);
                assertEquals(state.currentPlayer, j + 1);
            } else {
                assertEquals(state.currentPlayer, j + 1);
                assertEquals(state.currentNodesByPlayer.get(j + 1), root[j]);
            }
        }
        MCStatistics<TestAgent> moveLeftNode = root[0].getSuccessorNode(new ActionWithRef(TestActionEnum.LEFT, 1));
        factory.processGameEvent(new GameEvent<>(new ActionWithRef<>(TestActionEnum.LEFT, 1), game));
        for (int j = 0; j < 3; j++) {
            OpenLoopState<TestAgent> state = (OpenLoopState) factory.getCurrentState(players[j]);
            if (j == 0) {
                assertTrue(state.currentNodesByPlayer.get(j + 1) == moveLeftNode);
                assertEquals(state.currentPlayer, 1);
            } else {
                assertEquals(state.currentPlayer, j + 1);
                assertEquals(state.currentNodesByPlayer.get(j + 1), root[j]);
            }
        }
    }

    @Test
    public void processingAnEventOutOfTreeReturnsNull() {
        fail("Not yet implemented");
    }

    @Test
    public void reuseOfOpenLoopTree() {
        fail("Not yet implemented");
    }

    @Test
    public void factoryListensToGameAndUpdates() {
        fail("Not yet implemented");
    }
}
