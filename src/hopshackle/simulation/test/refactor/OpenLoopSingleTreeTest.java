package hopshackle.simulation.test.refactor;

import java.util.*;

import hopshackle.simulation.MCTS.*;
import hopshackle.simulation.*;

import static org.junit.Assert.*;

import org.junit.*;

/**
 * Created by james on 26/07/2017.
 */
public class OpenLoopSingleTreeTest {

    /*
    We have a SimpleMazeGame with 3 players, and using a single tree with OpenLoop
    The rollout deciders (SimpleMazeDecider) will default to LEFT, LEFT, RIGHT in deterministic order
     */

    List<ActionEnum<TestAgent>> possibleActions;
    SimpleMazeGame game;
    TestAgent[] players;
    DeciderProperties localProp;
    BaseStateDecider<TestAgent> rolloutDecider = new SimpleMazeDecider();
    OpenLoopMCTree<TestAgent> tree;
    MCTSMasterDecider<TestAgent> masterDecider;
    SingletonStateFactory<TestAgent> singletonStateFactory = new SingletonStateFactory<>();
    World world;

    @Before
    public void setUp() {
        possibleActions = new ArrayList<>();
        possibleActions.add(TestActionEnum.LEFT);
        possibleActions.add(TestActionEnum.RIGHT);
        possibleActions.add(TestActionEnum.TEST);
        world = new World();
        world.setCalendar(new FastCalendar(0l));
        localProp = SimProperties.getDeciderProperties("GLOBAL");
        localProp.setProperty("MonteCarloSingleTree", "single");
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

    private void setupGame() {
        masterDecider = new MCTSMasterDecider<>(singletonStateFactory, rolloutDecider, rolloutDecider);
        masterDecider.injectProperties(localProp);
        Dice.setSeed(6l);
        players = new TestAgent[3];

        for (int i = 0; i < 3; i++) {
            players[i] = new TestAgent(world);
            players[i].setDecider(masterDecider);
        }
        game = new SimpleMazeGame(4, players);
    }

    @Test
    public void openLoopSingleTreeHasSameTreeForAllPlayers() {
        setupGame();
        MonteCarloTree<TestAgent>[] trees = new MonteCarloTree[3];
        for (int i = 0; i < 3; i++) trees[i] = masterDecider.getTree(players[i]);
        for (int i = 0; i < 2; i++)
            for (int j = i + 1; j < 3; j++)
                assertTrue(trees[i] == trees[j]);

        game.oneAction();

        for (int i = 0; i < 3; i++) trees[i] = masterDecider.getTree(players[i]);
        for (int i = 0; i < 2; i++)
            for (int j = i + 1; j < 3; j++)
                assertTrue(trees[i] == trees[j]);
    }

    @Test
    public void openLoopIgnoreOthersHasDifferentTreesPerPlayer() {
        localProp.setProperty("MonteCarloSingleTree", "ignoreOthers");
        setupGame();

        MonteCarloTree<TestAgent>[] trees = new MonteCarloTree[3];
        for (int i = 0; i < 3; i++) trees[i] = masterDecider.getTree(players[i]);
        for (int i = 0; i < 2; i++)
            for (int j = i + 1; j < 3; j++)
                assertTrue(trees[i] != trees[j]);

        game.oneAction();

        for (int i = 0; i < 3; i++) trees[i] = masterDecider.getTree(players[i]);
        for (int i = 0; i < 2; i++)
            for (int j = i + 1; j < 3; j++)
                assertTrue(trees[i] != trees[j]);
    }

    @Test
    public void openLoopMultiTreeHasDifferentTreesPerPlayer() {
        localProp.setProperty("MonteCarloSingleTree", "perPlayer");
        setupGame();

        MonteCarloTree<TestAgent>[] trees = new MonteCarloTree[3];
        for (int i = 0; i < 3; i++) trees[i] = masterDecider.getTree(players[i]);
        for (int i = 0; i < 2; i++)
            for (int j = i + 1; j < 3; j++)
                assertTrue(trees[i] != trees[j]);

        game.oneAction();

        for (int i = 0; i < 3; i++) trees[i] = masterDecider.getTree(players[i]);
        for (int i = 0; i < 2; i++)
            for (int j = i + 1; j < 3; j++)
                assertTrue(trees[i] != trees[j]);
    }

    @Test
    public void openLoopSingleTreeNodeOnlyIncludesActionsForActingPlayers() {
        setupGame();
        tree = (OpenLoopMCTree) masterDecider.getTree(players[0]);
        assertEquals(tree.numberOfStates(), 1);

        assertTrue(tree.withinTree(null));

        assertEquals(game.getPossibleActions().size(), 3);
        game.oneAction();
        assertEquals(game.getPossibleActions().size(), 3);

        MCStatistics<TestAgent> rootStats = tree.getRootStatistics();
        assertEquals(rootStats.getPossibleActions(1).size(), 3);
        assertEquals(rootStats.getPossibleActions().size(), 3);

        assertEquals(tree.numberOfStates(), 96);
        assertEquals(tree.getRootStatistics().getVisits(), 99);
        MCStatistics left = tree.getRootStatistics().getSuccessorNode(new ActionWithRef(TestActionEnum.LEFT, 1));
        assertNotNull(left);
        MCStatistics left2 = tree.getRootStatistics().getSuccessorNode(new ActionWithRef(TestActionEnum.LEFT, 2));
        assertNull(left2);

        MCStatistics leftleft = left.getSuccessorNode(new ActionWithRef(TestActionEnum.LEFT, 2));
        assertNotNull(leftleft);
        MCStatistics leftleft2 = left.getSuccessorNode(new ActionWithRef(TestActionEnum.LEFT, 1));
        assertNull(leftleft2);

        MCStatistics leftleftright = leftleft.getSuccessorNode(new ActionWithRef(TestActionEnum.RIGHT, 3));
        assertNotNull(leftleftright);
        MCStatistics leftleftright2 = leftleft.getSuccessorNode(new ActionWithRef(TestActionEnum.LEFT, 1));
        assertNull(leftleftright2);
    }

    @Test
    public void openLoopIgnoreOthersTreeNodeOnlyHasNodesForActingPlayer() {
        localProp.setProperty("MonteCarloSingleTree", "ignoreOthers");
        setupGame();
        tree = (OpenLoopMCTree) masterDecider.getTree(players[0]);
        MonteCarloTree<TestAgent> tree2 = masterDecider.getTree(players[1]);
        MonteCarloTree<TestAgent> tree3 = masterDecider.getTree(players[2]);

        assertEquals(tree.numberOfStates(), 1);
        assertEquals(tree2.numberOfStates(), 1);
        assertTrue(tree.withinTree(null));
        assertTrue(tree2.withinTree(null));

        assertEquals(game.getPossibleActions().size(), 3);
        game.oneAction();
        assertEquals(game.getPossibleActions().size(), 3);

        MCStatistics<TestAgent> rootStats = tree.getRootStatistics();
        assertEquals(rootStats.getPossibleActions(1).size(), 3);
        assertEquals(rootStats.getPossibleActions().size(), 3);

        assertEquals(tree.numberOfStates(), 18);
        assertEquals(tree2.numberOfStates(), 1);
        assertEquals(tree3.numberOfStates(), 1);
        assertEquals(tree.getRootStatistics().getVisits(), 99);
        assertEquals(tree2.getRootStatistics().getVisits(), 0);
        MCStatistics left = tree.getRootStatistics().getSuccessorNode(new ActionWithRef(TestActionEnum.LEFT, 1));
        assertNotNull(left);
        MCStatistics left2 = tree.getRootStatistics().getSuccessorNode(new ActionWithRef(TestActionEnum.LEFT, 2));
        assertNull(left2);

        MCStatistics leftleft = left.getSuccessorNode(new ActionWithRef(TestActionEnum.LEFT, 2));
        assertNull(leftleft);
        MCStatistics leftleft2 = left.getSuccessorNode(new ActionWithRef(TestActionEnum.LEFT, 1));
        assertNotNull(leftleft2);

        MCStatistics leftleftright = leftleft2.getSuccessorNode(new ActionWithRef(TestActionEnum.RIGHT, 3));
        assertNull(leftleftright);
        MCStatistics leftleftright2 = leftleft2.getSuccessorNode(new ActionWithRef(TestActionEnum.LEFT, 1));
        assertNotNull(leftleftright2);
    }

    @Test
    public void openLoopPerPlayerTreeHasNodesForAllPlayers() {
        localProp.setProperty("MonteCarloSingleTree", "perPlayer");
        setupGame();
        fail("Not yet implemented");
    }

    @Test
    public void openLoopBranchesCorrectlyForAllPlayers() {
        setupGame();
        tree = (OpenLoopMCTree) masterDecider.getTree(players[0]);
        game.oneAction();

        MCStatistics<TestAgent> stats = tree.getRootStatistics();
        assertEquals(stats.getPossibleActions().size(), 3);

        // we now take LEFT action each time, and check there are three actions for each other player
        stats = stats.getSuccessorNode(new ActionWithRef<>(TestActionEnum.LEFT, 1));
        assertEquals(stats.getPossibleActions().size(), 3);
        stats = stats.getSuccessorNode(new ActionWithRef<>(TestActionEnum.LEFT, 2));
        assertEquals(stats.getPossibleActions().size(), 3);
        stats = stats.getSuccessorNode(new ActionWithRef<>(TestActionEnum.LEFT, 3));
        assertEquals(stats.getPossibleActions().size(), 3);
    }

    @Test
    public void openLoopIgnoreOthersBranchesCorrectly() {
        localProp.setProperty("MonteCarloSingleTree", "ignoreOthers");
        setupGame();
        tree = (OpenLoopMCTree) masterDecider.getTree(players[0]);
        game.oneAction();

        MCStatistics<TestAgent> stats = tree.getRootStatistics();
        assertEquals(stats.getPossibleActions().size(), 3);

        // we now take LEFT action each time, and check there are three actions for each other player
        stats = stats.getSuccessorNode(new ActionWithRef<>(TestActionEnum.LEFT, 1));
        assertEquals(stats.getPossibleActions().size(), 3);
        stats = stats.getSuccessorNode(new ActionWithRef<>(TestActionEnum.LEFT, 1));
        assertEquals(stats.getPossibleActions().size(), 3);
        stats = stats.getSuccessorNode(new ActionWithRef<>(TestActionEnum.LEFT, 1));
        assertEquals(stats.getPossibleActions().size(), 3);
    }

    @Test
    public void openLoopPerPlayerBranchesCorrectly() {
        localProp.setProperty("MonteCarloSingleTree", "perPlayer");
        setupGame();
        fail("Not yet implemented");
    }

    @Test
    public void openLoopSingleTreeUpdatesOncePerMove() {
        setupGame();
        tree = (OpenLoopMCTree) masterDecider.getTree(players[0]);
        // This test emulates a few rollouts of a game within MCTSMasterDecider
        // If we do 6 rollouts
        MCTSChildDecider<TestAgent>[] childDecider = new MCTSChildDecider[3];
        for (int i = 0; i < 3; i++) {
            childDecider[i] = masterDecider.createChildDecider(singletonStateFactory, tree, i + 1, false);
        }
        childDecider[0].setRolloutDecider(new HardCodedDecider<>(TestActionEnum.LEFT));
        childDecider[1].setRolloutDecider(new HardCodedDecider<>(TestActionEnum.TEST));
        childDecider[2].setRolloutDecider(new HardCodedDecider<>(TestActionEnum.RIGHT));

        // just use one all the time, otherwise we re-register a new decider each time and get multiple tree updates
        for (int loop = 0; loop < 8; loop++) {
            tree.setUpdatesLeft(1);
            SimpleMazeGame clonedGame = (SimpleMazeGame) game.clone(players[0]);
            List<TestAgent> clonedPlayers = clonedGame.getAllPlayers();
            for (int i = 0; i < 3; i++) clonedPlayers.get(i).setDecider(childDecider[i]);

            clonedGame.playGame();
            tree.processTrajectory(tree.filterTrajectory(clonedGame.getTrajectory(), 1), clonedGame.getFinalScores());

            MCStatistics<TestAgent> rootStats = tree.getRootStatistics();
            assertEquals(rootStats.getVisits(), loop + 1);

            MCStatistics<TestAgent> testStats = rootStats.getSuccessorNode(new ActionWithRef<>(TestActionEnum.TEST, 1));
            MCStatistics<TestAgent> leftStats = rootStats.getSuccessorNode(new ActionWithRef<>(TestActionEnum.LEFT, 1));
            MCStatistics<TestAgent> leftTest = leftStats == null ? null : leftStats.getSuccessorNode(new ActionWithRef<>(TestActionEnum.TEST, 2));
            MCStatistics<TestAgent> leftLeft = leftStats == null ? null : leftStats.getSuccessorNode(new ActionWithRef<>(TestActionEnum.LEFT, 2));
            MCStatistics<TestAgent> leftRight = leftStats == null ? null : leftStats.getSuccessorNode(new ActionWithRef<>(TestActionEnum.RIGHT, 2));
            MCStatistics<TestAgent> rightStats = rootStats.getSuccessorNode(new ActionWithRef<>(TestActionEnum.RIGHT, 1));
            int explored = 0, leftExplored = 0;
            if (testStats != null) explored++;
            if (leftStats != null) explored++;
            if (rightStats != null) explored++;
            if (leftTest != null) leftExplored++;
            if (leftLeft != null) leftExplored++;
            if (leftRight != null) leftExplored++;

            // Then in terms of tree structure, we expect there to be three states descended from the initial state
            // with the next two exploring the other two options from player 1 going LEFT

            switch (loop) {
                case 7:
                    assertEquals(leftExplored, 3);
                    break;
                case 6:
                case 5:
                case 4:
                    assertEquals(leftExplored, 2);
                    assertEquals(explored, 3);
                    break;
                case 3:
                    assertEquals(leftExplored, 1);
                    assertEquals(explored, 3);
                    break;
                case 2:
                    assertEquals(explored, 3);
                    break;
                case 1:
                    assertEquals(explored, 2);
                    break;
                case 0:
                    assertEquals(explored, 1);
                    break;
            }
        }

        MCStatistics<TestAgent> rootStats = tree.getRootStatistics();
        System.out.println(rootStats);
        // from the root state player 1 will want to go left ... but the other two players have never
        // acted from this state, so will default to expansion policy (which uses MAST)
        assertTrue(rootStats.getBestAction(possibleActions, 1) == TestActionEnum.LEFT);
        assertTrue(rootStats.getBestAction(possibleActions, 2) == TestActionEnum.LEFT);
        assertTrue(rootStats.getBestAction(possibleActions, 3) == TestActionEnum.TEST);
    }

    @Test
    public void openLoopIgnoreOthersUpdatesOncePerMove() {
        localProp.setProperty("MonteCarloSingleTree", "ignoreOthers");
        setupGame();
        MonteCarloTree<TestAgent>[] tree = new MonteCarloTree[3];
        for (int i = 0; i < 3; i++) {
            tree[i] = masterDecider.getTree(players[i]);
        }
        MCTSChildDecider<TestAgent> childDecider = masterDecider.createChildDecider(singletonStateFactory, tree[0], 1, false);
        childDecider.setRolloutDecider(new HardCodedDecider<>(TestActionEnum.LEFT));

        // This test emulates a few rollouts of a game within MCTSMasterDecider
        for (int loop = 0; loop < 8; loop++) {
            for (int i = 0; i < 3; i++) tree[i].setUpdatesLeft(1);
            SimpleMazeGame clonedGame = (SimpleMazeGame) game.clone(players[0]);
            List<TestAgent> clonedPlayers = clonedGame.getAllPlayers();
            clonedPlayers.get(0).setDecider(childDecider);
            clonedPlayers.get(1).setDecider(new HardCodedDecider<>(TestActionEnum.TEST));
            clonedPlayers.get(1).setDecider(new HardCodedDecider<>(TestActionEnum.RIGHT));

            clonedGame.playGame();
            tree[0].processTrajectory(tree[0].filterTrajectory(clonedGame.getTrajectory(), 1), clonedGame.getFinalScores());

            MCStatistics<TestAgent> rootStats = tree[0].getRootStatistics();
            assertEquals(rootStats.getVisits(), loop + 1);

            MCStatistics<TestAgent> testStats = rootStats.getSuccessorNode(new ActionWithRef<>(TestActionEnum.TEST, 1));
            MCStatistics<TestAgent> leftStats = rootStats.getSuccessorNode(new ActionWithRef<>(TestActionEnum.LEFT, 1));
            MCStatistics<TestAgent> leftTest = leftStats == null ? null : leftStats.getSuccessorNode(new ActionWithRef<>(TestActionEnum.TEST, 1));
            MCStatistics<TestAgent> leftLeft = leftStats == null ? null : leftStats.getSuccessorNode(new ActionWithRef<>(TestActionEnum.LEFT, 1));
            MCStatistics<TestAgent> leftRight = leftStats == null ? null : leftStats.getSuccessorNode(new ActionWithRef<>(TestActionEnum.RIGHT, 1));
            MCStatistics<TestAgent> rightStats = rootStats.getSuccessorNode(new ActionWithRef<>(TestActionEnum.RIGHT, 1));
            int explored = 0, leftExplored = 0;
            if (testStats != null) explored++;
            if (leftStats != null) explored++;
            if (rightStats != null) explored++;
            if (leftTest != null) leftExplored++;
            if (leftLeft != null) leftExplored++;
            if (leftRight != null) leftExplored++;

            switch (loop) {
                case 7:
                case 6:
                    assertEquals(leftExplored, 3);
                    break;
                case 5:
                    assertEquals(leftExplored, 2);
                    assertEquals(explored, 3);
                    break;
                case 4: // check out TEST at root
                case 3:
                    assertEquals(leftExplored, 1);
                    assertEquals(explored, 3);
                    break;
                case 2:
                    assertEquals(explored, 3);
                    break;
                case 1:
                    assertEquals(explored, 2);
                    break;
                case 0:
                    assertEquals(explored, 1);
                    break;
            }
        }

        MCStatistics<TestAgent> rootStats = tree[0].getRootStatistics();
        // from the root state player 1 will want to go left ... but the other two players have never
        assertTrue(rootStats.getBestAction(possibleActions, 1) == TestActionEnum.LEFT);
    }
}
