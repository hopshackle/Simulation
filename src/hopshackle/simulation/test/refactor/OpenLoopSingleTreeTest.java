package hopshackle.simulation.test.refactor;

import java.util.*;

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
    OpenLoopStateFactory<TestAgent> factory = OpenLoopStateFactory.newInstanceGameLevelStates();
    BaseStateDecider<TestAgent> rolloutDecider = new SimpleMazeDecider();
    MonteCarloTree<TestAgent> tree;
    MCTSMasterDecider<TestAgent> masterDecider;
    ExperienceRecordCollector<TestAgent> erc;
    OnInstructionTeacher<TestAgent> teacher;

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
        localProp.setProperty("MonteCarloHeuristicOnExpansion","true");
        localProp.setProperty("MonteCarloMAST", "true");
        localProp.setProperty("MonteCarloHeuristicOnSelection","false");
        tree = new MonteCarloTree<TestAgent>(localProp, 3);
        masterDecider = new MCTSMasterDecider<TestAgent>(factory, rolloutDecider, rolloutDecider);
        masterDecider.injectProperties(localProp);
        Dice.setSeed(6l);
        players = new TestAgent[3];
        erc = new ExperienceRecordCollector<TestAgent>(new StandardERFactory<TestAgent>(localProp));

        for (int i = 0; i < 3; i++) {
            players[i] = new TestAgent(world);
            players[i].setDecider(masterDecider);
            erc.registerAgentWithReference(players[i], players[0]);
        }
        game = new SimpleMazeGame(3, players);

        teacher = new OnInstructionTeacher<TestAgent>();
        teacher.registerToERStream(erc);
    }

    @Test
    public void openLoopStateMovesWithGameNotPlayers() {
        for (int i = 0; i < 3; i++) {
            players[i].setDecider(new SimpleMazeDecider());
            // for this test we just want to use simple deciders
        }

        game.oneMove();
        State<TestAgent> initial = factory.getCurrentState(players[0]);
        for (int i = 0; i < 3; i++) {
            assertTrue(factory.getCurrentState(players[i]) == initial);
        }
        game.oneMove();
        State<TestAgent> next = factory.getCurrentState(players[0]);
        assertFalse(initial == next);
        for (int i = 0; i < 3; i++) {
            assertTrue(factory.getCurrentState(players[i]) == next);
        }
    }

    @Test
    public void monteCarloTreeUpdatesOncePerMoveWithSingleTree() {
        // This test emulates a few rollouts of a game within MCTSMasterDecider
        // If we do 6 rollouts
        State<TestAgent> initial = factory.getCurrentState(players[0]);
        tree.insertState(initial);
        Set<String> addedStates = new HashSet<>();
        addedStates.add(initial.toString());
        String newState = "";
        MCTSChildDecider<TestAgent> childDecider = masterDecider.createChildDecider(tree, 1, false);
        teacher.registerDecider(childDecider);
        // just use one all the time, otherwise we re-register a new decider each time and get multiple tree updates
        for (int loop = 0; loop < 6; loop++) {
            tree.setUpdatesLeft(1);
            SimpleMazeGame clonedGame = (SimpleMazeGame) game.clone(players[0]);
            factory.cloneGame(game, clonedGame);
            List<TestAgent> clonedPlayers = clonedGame.getAllPlayers();
            childDecider.setRolloutDecider(new HardCodedDecider<TestAgent>(TestActionEnum.LEFT));
            clonedPlayers.get(0).setDecider(childDecider);
            for (int i = 1; i < 3; i++) {
                MCTSChildDecider d = masterDecider.createChildDecider(tree, i + 1, true);
                if (i == 1) d.setRolloutDecider(new HardCodedDecider<TestAgent>(TestActionEnum.TEST));
                if (i == 2) d.setRolloutDecider(new HardCodedDecider<TestAgent>(TestActionEnum.RIGHT));
                clonedPlayers.get(i).setDecider(d);
            }
            for (int i = 0; i < 3; i++) {
                assertTrue(factory.getCurrentState(clonedPlayers.get(i)) == initial);
            }
            for (TestAgent player : clonedPlayers)
                erc.registerAgentWithReference(player, clonedPlayers.get(0));

            clonedGame.playGame();
            teacher.teach();
            assertEquals(tree.getStatisticsFor(initial).getVisits(), loop + 1);
            List<String> allStates = tree.getAllStatesWithMinVisits(0);
            for (String s : allStates) {
                if (!addedStates.contains(s)) {
                    // new state found
                    newState = s;
                    addedStates.add(s);
                }
            }
            MCStatistics<TestAgent> newStats = tree.getStatisticsFor(newState);
            // unfortunately, after all that, we have no way of knowing the agent this was created for
            // we could test that the added OpenLoopState was from the correct perspective
            // if each agent has different available actions. These would then be reflected in newStats.
            // But that is defined within SimpleMazeGame.
            // However, we could inject different rollout deciders for each agent.
            // LEFT / TEST / RIGHT as defaults will mean that player 1 always wins

            // Then in terms of tree structure, we expect there to be three states descended from the initial state
            // with the net two exploring the other two options from player 1 going LEFT
            if (loop < 3) {
                assertTrue(tree.getStatisticsFor(initial.toString()).getSuccessorStates().contains(newState.toString()));
            } else {
                assertFalse(tree.getStatisticsFor(initial.toString()).getSuccessorStates().contains(newState.toString()));
            }

            double[] newScoreTest = newStats.getMean(TestActionEnum.TEST, 1);
            double[] newScoreLeft = newStats.getMean(TestActionEnum.LEFT,1 );
            double[] newScoreRight = newStats.getMean(TestActionEnum.RIGHT, 1);
            for (int p = 2; p <= 3; p++) {
                double[] newTest = newStats.getMean(TestActionEnum.TEST, p);
                if (newTest[0] != 0.00 || newTest[1] != 0.00 || newTest[2] != 0.00)
                    newScoreTest = newTest;
                double[] newLeft = newStats.getMean(TestActionEnum.LEFT, p);
                if (newLeft[0] != 0.00 || newLeft[1] != 0.00 || newLeft[2] != 0.00)
                    newScoreLeft = newLeft;
                double[] newRight = newStats.getMean(TestActionEnum.RIGHT, p);
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

        MCStatistics<TestAgent> rootStats = tree.getStatisticsFor(initial.toString());
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
