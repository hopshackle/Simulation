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
        localProp.setProperty("MonteCarloHeuristicOnSelection", "false");
        tree = new OpenLoopMCTree<>(localProp, 3);
        masterDecider = new MCTSMasterDecider<TestAgent>(null, rolloutDecider, rolloutDecider);
        masterDecider.injectProperties(localProp);
        Dice.setSeed(6l);
        players = new TestAgent[3];

        for (int i = 0; i < 3; i++) {
            players[i] = new TestAgent(world);
            players[i].setDecider(masterDecider);
        }
        game = new SimpleMazeGame(3, players);
    }

    @Test
    public void openLoopStateMovesWithGameNotPlayers() {
        for (int i = 0; i < 3; i++) {
            players[i].setDecider(new SimpleMazeDecider());
            // for this test we just want to use simple deciders
        }
        assertEquals(tree.numberOfStates(), 1);

        assertTrue(tree.withinTree(null));
        game.oneMove();
        assertFalse(tree.withinTree(null));
        assertEquals(tree.numberOfStates(), 100);

        assertEquals(tree.getRootStatistics().getVisits(), 99);
    }

    @Test
    public void monteCarloTreeUpdatesOncePerMoveWithSingleTree() {
        // This test emulates a few rollouts of a game within MCTSMasterDecider
        // If we do 6 rollouts
        MCTSChildDecider<TestAgent> childDecider = masterDecider.createChildDecider(tree, 1, false);
        // just use one all the time, otherwise we re-register a new decider each time and get multiple tree updates
        for (int loop = 0; loop < 6; loop++) {
            tree.setUpdatesLeft(1);
            SimpleMazeGame clonedGame = (SimpleMazeGame) game.clone(players[0]);
            List<TestAgent> clonedPlayers = clonedGame.getAllPlayers();
            childDecider.setRolloutDecider(new HardCodedDecider<TestAgent>(TestActionEnum.LEFT));
            clonedPlayers.get(0).setDecider(childDecider);
            for (int i = 1; i < 3; i++) {
                MCTSChildDecider d = masterDecider.createChildDecider(tree, i + 1, true);
                if (i == 1) d.setRolloutDecider(new HardCodedDecider<TestAgent>(TestActionEnum.TEST));
                if (i == 2) d.setRolloutDecider(new HardCodedDecider<TestAgent>(TestActionEnum.RIGHT));
                clonedPlayers.get(i).setDecider(d);
            }

            clonedGame.playGame();

            MCStatistics<TestAgent> rootStats = tree.getRootStatistics();
            assertEquals(rootStats.getVisits(), loop + 1);

            MCStatistics<TestAgent> testStats = rootStats.getSuccessorNode(TestActionEnum.TEST);
            MCStatistics<TestAgent> leftStats = rootStats.getSuccessorNode(TestActionEnum.LEFT);
            MCStatistics<TestAgent> leftTest = leftStats == null ? null : leftStats.getSuccessorNode(TestActionEnum.TEST);
            MCStatistics<TestAgent> leftLeft = leftStats == null ? null : leftStats.getSuccessorNode(TestActionEnum.LEFT);
            MCStatistics<TestAgent> leftRight = leftStats == null ? null : leftStats.getSuccessorNode(TestActionEnum.RIGHT);
            MCStatistics<TestAgent> rightStats = rootStats.getSuccessorNode(TestActionEnum.RIGHT);
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
                case 5:
                    assertEquals(leftExplored, 3);
                    break;
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

            double[] newScoreTest = rootStats.getMean(TestActionEnum.TEST, 1);
            double[] newScoreLeft = rootStats.getMean(TestActionEnum.LEFT, 1);
            double[] newScoreRight = rootStats.getMean(TestActionEnum.RIGHT, 1);
            for (int p = 2; p <= 3; p++) {
                double[] newTest = rootStats.getMean(TestActionEnum.TEST, p);
                if (newTest[0] != 0.00 || newTest[1] != 0.00 || newTest[2] != 0.00)
                    newScoreTest = newTest;
                double[] newLeft = rootStats.getMean(TestActionEnum.LEFT, p);
                if (newLeft[0] != 0.00 || newLeft[1] != 0.00 || newLeft[2] != 0.00)
                    newScoreLeft = newLeft;
                double[] newRight = rootStats.getMean(TestActionEnum.RIGHT, p);
                if (newRight[0] != 0.00 || newRight[1] != 0.00 || newRight[2] != 0.00)
                    newScoreRight = newRight;
            }
            // one, and only one of these should be non-zero
            double[] nonZeroScore;
            if (newScoreTest[2] != 0.0) {
                nonZeroScore = newScoreTest;
                assertEquals(newScoreLeft[2], 0.0, 0.01);
                assertEquals(newScoreRight[2], 0.0, 0.01);
            } else if (newScoreLeft[2] != 0.0) {
                nonZeroScore = newScoreLeft;
                assertEquals(newScoreTest[2], 0.0, 0.01);
                assertEquals(newScoreRight[2], 0.0, 0.01);
            } else {
                assertTrue(newScoreRight[2] != 0.0);
                nonZeroScore = newScoreRight;
                assertEquals(newScoreLeft[2], 0.0, 0.01);
                assertEquals(newScoreTest[2], 0.0, 0.01);
            }
            //           System.out.println(tree.toString(true));
            if (loop > 3) {
                // once we are investigating actions for players 2 and 3, their decisions will have no impact
                // on the score for 1 (this assumes that we always go LEFT at root of tree)
                assertEquals(nonZeroScore[0], 3.0, 0.01);
            }
            System.out.println(HopshackleUtilities.formatArray(nonZeroScore, ",", "%.2f"));
        }

        MCStatistics<TestAgent> rootStats = tree.getRootStatistics();
        System.out.println(rootStats);
        // from the root state player 1 will want to go left ... but the other two players have never
        // acted from this state, so will default to expansion policy (which uses MAST)
        assertTrue(rootStats.getBestAction(possibleActions, 1) == TestActionEnum.LEFT);
        assertTrue(rootStats.getBestAction(possibleActions, 2) == TestActionEnum.LEFT);
        assertTrue(rootStats.getBestAction(possibleActions, 3) == TestActionEnum.TEST);

        assertNotEquals(rootStats.getV(1)[0], 0.0, 0.001);
        assertEquals(rootStats.getV(2).length, 0.0, 0.001);
        assertEquals(rootStats.getV(3).length, 0.0, 0.001);
    }

}
