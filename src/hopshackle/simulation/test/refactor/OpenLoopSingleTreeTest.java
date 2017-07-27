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
    Decider<TestAgent> rolloutDecider = new SimpleMazeDecider();
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
        localProp.setProperty("MonteCarloUCTC", "1.0");
        localProp.setProperty("Gamma", "0.95");
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
        game = new SimpleMazeGame(2, players);

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
        // If we do 6 rollouts, then we expect to get 2 states added to tree for each player
        // however we no longer record the player who took the action to get to a state
        // instead we provide the next actor as an argument when deciding on the next action.
        State<TestAgent> initial = factory.getCurrentState(players[0]);
        tree.insertState(initial, possibleActions);
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
            clonedPlayers.get(0).setDecider(childDecider);
            for (int i = 1; i < 3; i++) {
                clonedPlayers.get(i).setDecider(masterDecider.createChildDecider(tree, i + 1, true));
            }
            for (int i = 0; i < 3; i++) {
                assertTrue(factory.getCurrentState(clonedPlayers.get(i)) == initial);
            }
            for (TestAgent player : clonedPlayers)
                erc.registerAgentWithReference(player, clonedPlayers.get(0));

            clonedGame.playGame();
            System.out.println("Loop : " + loop);
            teacher.teach();
            assertEquals(tree.getStatisticsFor(initial).getVisits(), loop + 1);
            List<String> allStates = tree.getAllStatesWithMinVisits(0);
            for (String s : allStates) {
                if (!addedStates.contains(s)) {
                    // new state found
                    newState = s;
                    System.out.println(s);
                    addedStates.add(s);
                }
            }
            MCStatistics<TestAgent> newStats = tree.getStatisticsFor(newState);
            // unfortunately, after all that, we have no way of knowing the agent this was created for
            // we could test that the added OpenLoopState was from the correct perspective
            // if each agent has different available actions. These would then be reflected in newStats.
            // But that is defined within SimpleMazeGame.
            // Alternatively, this could be detected in the scores held. If I *only* decrement the
            // score of a player when they take their turn, then this will clearly indicate whose turn it was
            // when the MCStatistics were created and added to the tree.
        }
    }

    @Test
    public void explorationCorrectInTreeForOLST() {
        fail("Not yet implemented");
    }
}
