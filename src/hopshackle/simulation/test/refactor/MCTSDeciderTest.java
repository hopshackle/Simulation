package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;

import hopshackle.simulation.MCTS.*;
import hopshackle.simulation.*;

import java.util.*;

import org.junit.*;

public class MCTSDeciderTest {
	
	List<GeneticVariable<TestAgent>> genVar = new ArrayList<GeneticVariable<TestAgent>>(EnumSet.allOf(TestGenVar.class));
	List<ActionEnum<TestAgent>> allActions = new ArrayList<ActionEnum<TestAgent>>(EnumSet.allOf(TestActionEnum.class));
	StateFactory<TestAgent> factory = new LinearStateFactory<TestAgent>(genVar);
	BaseStateDecider<TestAgent> rolloutDecider = new SimpleMazeDecider();
    TranspositionTableMCTree<TestAgent> tree;
	// This is a single agent test, so no opponent model is needed, and the last parameter is safely null
	MCTSMasterDecider<TestAgent> masterDecider;
	DeciderProperties localProp;
	
	World world = new World();
	TestAgent agent = new TestAgent(world);

	SimpleMazeGame mazeGame;
	
	@Before
	public void setup()  {
		world.setCalendar(new FastCalendar(0l));
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
		localProp.setProperty("MonteCarloOpenLoop", "false");
        localProp.setProperty("MonteCarloTree", "ignoreOthers");
		localProp.setProperty("MonteCarloChoice", "default");
		localProp.setProperty("MonteCarloHeuristicOnSelection", "false");
		localProp.setProperty("MonteCarloRetainTreeBetweenActions","false");
		localProp.setProperty("MonteCarloMAST", "false");
		localProp.setProperty("MaxTurnsPerGame", "10000");
		localProp.setProperty("GameOrdinalRewards", "0");
		masterDecider = new MCTSMasterDecider<>(factory, rolloutDecider, rolloutDecider);
		masterDecider.injectProperties(localProp);
		agent.setDecider(masterDecider);
		Dice.setSeed(6l);
	}
	
	@After
	public void tearDown() {
		SimProperties.clear();
	}
	
	@Test
	public void test() {
		/*
		 * TestAgent has a position that starts at 0. We achieve success when it reaches +2. Each LEFT adds 1.
		 * Each RIGHT subtracts 1. Each time step taken has reward of -1.
		 * 
		 * The test plan is to cycle through decisions, and check that the MonteCarloTree is working as expected
		 * 
		 */
		tree = new TranspositionTableMCTree<>(localProp, 1);
		mazeGame = new SimpleMazeGame(2, agent);
		
		State<TestAgent> startState = masterDecider.getCurrentState(agent);
		mazeGame.oneAction();
        TranspositionTableMCTree<TestAgent> tree = (TranspositionTableMCTree) masterDecider.getTree(agent);
	//	System.out.println(tree.toString(true));
		assertEquals(tree.numberOfStates(), 7);
		MCStatistics<TestAgent> startStats = tree.getStatisticsFor(startState);
		assertEquals(startStats.getVisits(), 99);
		assertEquals(startStats.getMean(TestActionEnum.LEFT, 1)[0], 7.499, 0.01);
		assertEquals(startStats.getMean(TestActionEnum.TEST, 1)[0], 1.737, 0.01);
		assertEquals(startStats.getMean(TestActionEnum.RIGHT, 1)[0], 0.0, 0.01);
		assertEquals(startStats.getVisits(TestActionEnum.LEFT), 96);
		assertEquals(startStats.getVisits(TestActionEnum.TEST), 2);
		assertEquals(startStats.getVisits(TestActionEnum.RIGHT), 1);
		
		startState = masterDecider.getCurrentState(agent);
		mazeGame.oneAction();
		tree = (TranspositionTableMCTree) masterDecider.getTree(agent);
		System.out.println(tree.toString(true));
		startStats = tree.getStatisticsFor(startState);
		assertEquals(tree.numberOfStates(), 3);
		assertEquals(startStats.getVisits(), 99);
		assertEquals(startStats.getMean(TestActionEnum.LEFT, 1)[0], 8.00, 0.01);
		assertEquals(startStats.getMean(TestActionEnum.TEST, 1)[0], 4.29, 0.01);
		assertEquals(startStats.getMean(TestActionEnum.RIGHT, 1)[0], 1.47, 0.01);
		assertEquals(startStats.getVisits(TestActionEnum.LEFT), 97);
		assertEquals(startStats.getVisits(TestActionEnum.TEST), 1);
		assertEquals(startStats.getVisits(TestActionEnum.RIGHT), 1);
	}
	
	@Test
	public void multiplePlayersWithMultipleTrees() {
		localProp.setProperty("MonteCarloTree", "ignoreOthers");
		masterDecider.injectProperties(localProp);
		TestAgent[] players = new TestAgent[3];
		players[0] = agent;
		players[1] = new TestAgent(world);
		players[1].addGold(0.1);
		players[2] = new TestAgent(world);
		players[2].addGold(0.2);
		mazeGame = new SimpleMazeGame(2, players);
		for (TestAgent a : players) {
			a.setDecider(masterDecider);
		}
		
		State<TestAgent> startState = masterDecider.getCurrentState(agent);
		assertEquals(mazeGame.getCurrentPlayerRef(), 1);
		mazeGame.oneAction();
        TranspositionTableMCTree<TestAgent> tree = (TranspositionTableMCTree)masterDecider.getTree(agent);
		System.out.println(tree.toString(true));
		assertEquals(tree.numberOfStates(), 4);
		MCStatistics<TestAgent> startStats = tree.getStatisticsFor(startState);
		assertEquals(startStats.getVisits(), 99);
		assertEquals(startStats.getMean(TestActionEnum.LEFT, 1)[0], 4.91, 0.01);    // was 5.45
		assertEquals(startStats.getMean(TestActionEnum.TEST, 1)[0], -6.001, 0.01); // was -6.3175
		assertEquals(startStats.getMean(TestActionEnum.RIGHT, 1)[0], -6.859, 0.01);
		assertEquals(startStats.getVisits(TestActionEnum.LEFT), 95);
		assertEquals(startStats.getVisits(TestActionEnum.TEST), 3);
		assertEquals(startStats.getVisits(TestActionEnum.RIGHT), 1);
		
		assertEquals(mazeGame.getCurrentPlayerRef(), 2);
		State<TestAgent> secondPlayerState = masterDecider.getCurrentState(players[1]);
		assertFalse(tree.containsState(secondPlayerState));
		
		mazeGame.oneAction();
		assertEquals(mazeGame.getCurrentPlayerRef(), 3);
		State<TestAgent> thirdPlayerState = masterDecider.getCurrentState(players[2]);
		assertFalse(tree.containsState(thirdPlayerState));
	}

    @Test
    public void perPlayerTree() {
        localProp.setProperty("MonteCarloTree", "perPlayer");
        masterDecider.injectProperties(localProp);
        TestAgent[] players = new TestAgent[3];
        players[0] = agent;
        players[1] = new TestAgent(world);
        players[1].addGold(0.1);
        players[2] = new TestAgent(world);
        players[2].addGold(0.2);
        mazeGame = new SimpleMazeGame(2, players);
        for (TestAgent a : players) {
            a.setDecider(masterDecider);
        }

        State<TestAgent> startState = masterDecider.getCurrentState(agent);
        State<TestAgent> secondPlayerState = masterDecider.getCurrentState(players[1]);
        State<TestAgent> thirdPlayerState = masterDecider.getCurrentState(players[2]);
        assertEquals(mazeGame.getCurrentPlayerRef(), 1);

        mazeGame.oneAction();
        tree = (TranspositionTableMCTree)masterDecider.getTree(agent);
        TranspositionTableMCTree tree2 = (TranspositionTableMCTree)masterDecider.getTree(players[1]);
        System.out.println(tree.toString(true));
        MCStatistics<TestAgent> startStats;
        assertEquals(tree.numberOfStates(), 10);	// more states visited as we use state of other players
        startStats = tree.getStatisticsFor(startState);
        assertEquals(startStats.getVisits(), 99);
        assertEquals(startStats.getMean(TestActionEnum.LEFT, 1)[0], 4.812, 0.01);
        assertEquals(startStats.getMean(TestActionEnum.TEST, 1)[0], -5.22, 0.01);
        assertEquals(startStats.getMean(TestActionEnum.RIGHT, 1)[0], -6.19, 0.01);
        assertEquals(startStats.getVisits(TestActionEnum.LEFT), 94);
        assertEquals(startStats.getVisits(TestActionEnum.TEST), 4);
        assertEquals(startStats.getVisits(TestActionEnum.RIGHT), 1);

        assertEquals(mazeGame.getCurrentPlayerRef(), 2);
        assertFalse(tree.containsState(secondPlayerState));
        assertTrue(tree2.containsState(secondPlayerState)); // this is the secondPlayerState before p1 moved
        assertEquals(tree2.getStatisticsFor(secondPlayerState).getVisits(), 99);
        assertFalse(tree.containsState(thirdPlayerState));
        secondPlayerState = masterDecider.getCurrentState(players[1]); // this is the secondPlayerState after p1 moved
        assertFalse(tree.containsState(secondPlayerState));
        assertTrue(tree2.containsState(secondPlayerState));
        assertEquals(tree2.getStatisticsFor(secondPlayerState).getVisits(), 98); // and all bar one of the rollouts must have visited it

        startState = masterDecider.getCurrentState(players[0]);
        mazeGame.oneAction();
        tree = (TranspositionTableMCTree)masterDecider.getTree(agent);
        assertFalse(tree.containsState(secondPlayerState));
        assertEquals(mazeGame.getCurrentPlayerRef(), 3);
        assertEquals(tree.getStatisticsFor(startState).getVisits(), 99);
        startState = masterDecider.getCurrentState(players[0]);
        assertEquals(tree.getStatisticsFor(startState).getVisits(), 98);
    }
	
	@Test
	public void RAVEChildDeciderUpdates() {
		localProp.setProperty("MonteCarloRAVE", "true");
		localProp.setProperty("MonteCarloRAVEExploreConstant", "2");
		tree = new TranspositionTableMCTree<TestAgent>(localProp, 1);
		mazeGame = new SimpleMazeGame(2, agent);
		
		State<TestAgent> startState = masterDecider.getCurrentState(agent);
		mazeGame.oneAction();
        TranspositionTableMCTree<TestAgent> tree = (TranspositionTableMCTree) masterDecider.getTree(agent);
		assertEquals(tree.getStatisticsFor(startState).getRAVEValue(TestActionEnum.LEFT, 0.0, 1), 7.10, 0.01); // was 6.77
		assertEquals(tree.getStatisticsFor(startState).getRAVEValue(TestActionEnum.RIGHT, 0.0, 1), 0.84, 0.01); // was 0.91
		assertEquals(tree.getStatisticsFor(startState).getRAVEValue(TestActionEnum.TEST, 0.0, 1), 2.59, 0.01); // was 4.59
	}
}


