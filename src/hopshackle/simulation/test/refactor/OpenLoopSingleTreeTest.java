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

    @Before
    public void setUp() throws Exception {
        possibleActions = new ArrayList<ActionEnum<TestAgent>>();
        possibleActions.add(TestActionEnum.LEFT);
        possibleActions.add(TestActionEnum.RIGHT);
        possibleActions.add(TestActionEnum.TEST);
        World world = new World();
        world.setCalendar(new FastCalendar(0l));
        localProp = SimProperties.getDeciderProperties("GLOBAL");
        localProp.setProperty("MonteCarloSingleTree", "true");
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
        tree = new OpenLoopMCTree<>(localProp, 3);
        masterDecider = new MCTSMasterDecider<TestAgent>(new SingletonStateFactory(), rolloutDecider, rolloutDecider);
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
    public void openLoopStateOnlyIncludesActionsForActingPlayers() {
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
    public void openLoopBranchesCorrectlyForAllPlayers() {
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
    public void monteCarloTreeUpdatesOncePerMoveWithSingleTree() {
        // This test emulates a few rollouts of a game within MCTSMasterDecider
        // If we do 6 rollouts
        MCTSChildDecider<TestAgent> childDecider = masterDecider.createChildDecider(tree, 1, false);
        // just use one all the time, otherwise we re-register a new decider each time and get multiple tree updates
        for (int loop = 0; loop < 8; loop++) {
            tree.setUpdatesLeft(1);
            SimpleMazeGame clonedGame = (SimpleMazeGame) game.clone(players[0]);
            List<TestAgent> clonedPlayers = clonedGame.getAllPlayers();
            childDecider.setRolloutDecider(new HardCodedDecider<>(TestActionEnum.LEFT));
            clonedPlayers.get(0).setDecider(childDecider);
            for (int i = 1; i < 3; i++) {
                MCTSChildDecider d = masterDecider.createChildDecider(tree, i + 1, true);
                if (i == 1) d.setRolloutDecider(new HardCodedDecider<>(TestActionEnum.TEST));
                if (i == 2) d.setRolloutDecider(new HardCodedDecider<>(TestActionEnum.RIGHT));
                clonedPlayers.get(i).setDecider(d);
            }

            clonedGame.playGame();
            tree.processTrajectory(clonedGame.getTrajectory(), clonedGame.getFinalScores());

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

            double[] newScoreTest = rootStats.getMean(TestActionEnum.TEST, 1);
            double[] newScoreLeft = rootStats.getMean(TestActionEnum.LEFT, 1);
            double[] newScoreRight = rootStats.getMean(TestActionEnum.RIGHT, 1);
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

}
