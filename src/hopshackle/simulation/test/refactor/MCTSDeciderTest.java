package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;
import hopshackle.simulation.*;

import java.util.*;

import org.junit.*;

public class MCTSDeciderTest {
	
	List<GeneticVariable<TestAgent>> genVar = new ArrayList<GeneticVariable<TestAgent>>(EnumSet.allOf(TestGenVar.class));
	List<ActionEnum<TestAgent>> allActions = new ArrayList<ActionEnum<TestAgent>>(EnumSet.allOf(TestActionEnum.class));
	StateFactory<TestAgent> factory = new LinearStateFactory<TestAgent>(genVar);
	Decider<TestAgent> rolloutDecider = new SimpleMazeDecider();
	MonteCarloTree<TestAgent> tree;
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
		localProp.setProperty("MonteCarloRolloutCount", "99");
		localProp.setProperty("MonteCarloPriorActionWeightingForBestAction", "0");
		localProp.setProperty("MonteCarloActionValueRollout", "false");
		localProp.setProperty("MonteCarloActionValueOpponentModel", "false");
		localProp.setProperty("MonteCarloActionValueDeciderTemperature", "0.0");
		localProp.setProperty("MonteCarloRetainTreeBetweenActions", "false");
		localProp.setProperty("MonteCarloOpenLoop", "false");
		masterDecider = new MCTSMasterDecider<TestAgent>(factory, rolloutDecider, rolloutDecider);
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
		tree = new MonteCarloTree<TestAgent>(localProp, 1);
		mazeGame = new SimpleMazeGame(2, agent);
		
		State<TestAgent> startState = masterDecider.getCurrentState(agent);
		mazeGame.oneMove();
		MonteCarloTree<TestAgent> tree = masterDecider.getTree(agent);
		System.out.println(tree.toString(true));
		assertEquals(tree.numberOfStates(), 7);
		MCStatistics<TestAgent> startStats = tree.getStatisticsFor(startState);
		assertEquals(startStats.getVisits(), 99);
		assertEquals(startStats.getMean(TestActionEnum.LEFT)[0], 7.499, 0.01);
		assertEquals(startStats.getMean(TestActionEnum.TEST)[0], 1.737, 0.01);
		assertEquals(startStats.getMean(TestActionEnum.RIGHT)[0], 0.0, 0.01);
		assertEquals(startStats.getVisits(TestActionEnum.LEFT), 96);
		assertEquals(startStats.getVisits(TestActionEnum.TEST), 2);
		assertEquals(startStats.getVisits(TestActionEnum.RIGHT), 1);
		
		startState = masterDecider.getCurrentState(agent);
		mazeGame.oneMove();
		tree = masterDecider.getTree(agent);
		System.out.println(tree.toString(true));
		startStats = tree.getStatisticsFor(startState);
		assertEquals(tree.numberOfStates(), 3);
		assertEquals(startStats.getVisits(), 99);
		assertEquals(startStats.getMean(TestActionEnum.LEFT)[0], 8.00, 0.01);
		assertEquals(startStats.getMean(TestActionEnum.TEST)[0], 4.29, 0.01);
		assertEquals(startStats.getMean(TestActionEnum.RIGHT)[0], 1.47, 0.01);
		assertEquals(startStats.getVisits(TestActionEnum.LEFT), 97);
		assertEquals(startStats.getVisits(TestActionEnum.TEST), 1);
		assertEquals(startStats.getVisits(TestActionEnum.RIGHT), 1);
	}
	
	@Test
	public void multiplePlayersWithMultipleTrees() {
		localProp.setProperty("MonteCarloSingleTree", "false");
		masterDecider.injectProperties(localProp);
		tree = new MonteCarloTree<TestAgent>(localProp, 1);
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
		assertEquals(mazeGame.playerToMove, 0);
		mazeGame.oneMove();
		MonteCarloTree<TestAgent> tree = masterDecider.getTree(agent);
		System.out.println(tree.toString(true));
		assertEquals(tree.numberOfStates(), 4);
		MCStatistics<TestAgent> startStats = tree.getStatisticsFor(startState);
		assertEquals(startStats.getVisits(), 99);
		assertEquals(startStats.getMean(TestActionEnum.LEFT)[0], 5.472, 0.01);
		assertEquals(startStats.getMean(TestActionEnum.TEST)[0], -4.5125, 0.01);
		assertEquals(startStats.getMean(TestActionEnum.RIGHT)[0], -5.14, 0.01);
		assertEquals(startStats.getVisits(TestActionEnum.LEFT), 96);
		assertEquals(startStats.getVisits(TestActionEnum.TEST), 2);
		assertEquals(startStats.getVisits(TestActionEnum.RIGHT), 1);
		
		assertEquals(mazeGame.playerToMove, 1);
		State<TestAgent> secondPlayerState = masterDecider.getCurrentState(players[1]);
		assertFalse(tree.containsState(secondPlayerState));
		
		mazeGame.oneMove();
		assertEquals(mazeGame.playerToMove, 2);
		State<TestAgent> thirdPlayerState = masterDecider.getCurrentState(players[2]);
		assertFalse(tree.containsState(thirdPlayerState));
	}
	
	@Test
	public void singleTree() {
		localProp.setProperty("MonteCarloSingleTree", "true");
		masterDecider.injectProperties(localProp);
		tree = new MonteCarloTree<TestAgent>(localProp, 1);
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
		assertEquals(mazeGame.playerToMove, 0);

		mazeGame.oneMove();
		MonteCarloTree<TestAgent> tree = masterDecider.getTree(agent);
		System.out.println(tree.toString(true));
		MCStatistics<TestAgent> startStats = tree.getStatisticsFor(startState);
		assertEquals(tree.numberOfStates(), 7);	// more states visited as we use state of other players
		startStats = tree.getStatisticsFor(startState);
		assertEquals(startStats.getVisits(), 99);
		assertEquals(startStats.getMean(TestActionEnum.LEFT)[0], 4.8533, 0.01);
		assertEquals(startStats.getMean(TestActionEnum.TEST)[0], -4.643, 0.01);
		assertEquals(startStats.getMean(TestActionEnum.RIGHT)[0], -5.587, 0.01);
		assertEquals(startStats.getVisits(TestActionEnum.LEFT), 97);
		assertEquals(startStats.getVisits(TestActionEnum.TEST), 1);
		assertEquals(startStats.getVisits(TestActionEnum.RIGHT), 1);
		
		assertEquals(mazeGame.playerToMove, 1);
		State<TestAgent> secondPlayerState = masterDecider.getCurrentState(players[1]);
		assertTrue(tree.containsState(secondPlayerState));
		assertEquals(tree.getStatisticsFor(secondPlayerState).getVisits(), 99);
		
		mazeGame.oneMove();
		assertEquals(mazeGame.playerToMove, 2);
		State<TestAgent> thirdPlayerState = masterDecider.getCurrentState(players[2]);
		assertTrue(tree.containsState(thirdPlayerState));
		assertEquals(tree.getStatisticsFor(thirdPlayerState).getVisits(), 98);
		
	}
	
	@Test
	public void RAVEChildDeciderUpdates() {
		localProp.setProperty("MonteCarloRAVE", "GellySilver");
		localProp.setProperty("MonteCarloRAVEWeight", "2");
		tree = new MonteCarloTree<TestAgent>(localProp, 1);
		mazeGame = new SimpleMazeGame(2, agent);
		
		State<TestAgent> startState = masterDecider.getCurrentState(agent);
		mazeGame.oneMove();
		MonteCarloTree<TestAgent> tree = masterDecider.getTree(agent);
		assertEquals(tree.getStatisticsFor(startState).getRAVEValue(TestActionEnum.LEFT), 6.77, 0.01);
		assertEquals(tree.getStatisticsFor(startState).getRAVEValue(TestActionEnum.RIGHT), 0.91, 0.01);
		assertEquals(tree.getStatisticsFor(startState).getRAVEValue(TestActionEnum.TEST), 4.29, 0.01);
	}
}


