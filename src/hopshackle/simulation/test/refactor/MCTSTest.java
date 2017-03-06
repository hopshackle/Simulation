package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;
import hopshackle.simulation.*;

import java.util.*;

import org.junit.*;

public class MCTSTest {
	
	List<GeneticVariable<TestAgent>> genVar = new ArrayList<GeneticVariable<TestAgent>>(EnumSet.allOf(TestGenVar.class));
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
		localProp.setProperty("IncrementalScoreReward", "false");
		localProp.setProperty("MonteCarloRolloutCount", "99");
		localProp.setProperty("MonteCarloPriorActionWeightingForBestAction", "0");
		localProp.setProperty("MonteCarloActionValueRollout", "false");
		localProp.setProperty("MonteCarloActionValueOpponentModel", "false");
		localProp.setProperty("MonteCarloActionValueDeciderTemperature", "0.0");
		localProp.setProperty("MonteCarloRetainTreeBetweenActions", "false");
		masterDecider = new MCTSMasterDecider<TestAgent>(factory, rolloutDecider, rolloutDecider);
		masterDecider.injectProperties(localProp);
		agent.setDecider(masterDecider);
		Dice.setSeed(6l);
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
	//	System.out.println(tree.toString(true));
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
		players[2] = new TestAgent(world);
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
		assertEquals(startStats.getMean(TestActionEnum.TEST)[0], -4.75, 0.01);
		assertEquals(startStats.getMean(TestActionEnum.RIGHT)[0], -5.7, 0.01);
		assertEquals(startStats.getVisits(TestActionEnum.LEFT), 96);
		assertEquals(startStats.getVisits(TestActionEnum.TEST), 2);
		assertEquals(startStats.getVisits(TestActionEnum.RIGHT), 1);
		
//		startState = masterDecider.getCurrentState(players[1]);
		assertEquals(mazeGame.playerToMove, 1);
		mazeGame.oneMove();
		tree = masterDecider.getTree(agent);	// should be unchanged from other players turn
		assertEquals(tree.numberOfStates(), 4);
		startStats = tree.getStatisticsFor(startState);
		assertEquals(startStats.getVisits(), 99);
		assertEquals(startStats.getMean(TestActionEnum.LEFT)[0], 5.472, 0.01);
		assertEquals(startStats.getMean(TestActionEnum.TEST)[0], -4.75, 0.01);
		assertEquals(startStats.getMean(TestActionEnum.RIGHT)[0], -5.7, 0.01);
		assertEquals(startStats.getVisits(TestActionEnum.LEFT), 96);
		assertEquals(startStats.getVisits(TestActionEnum.TEST), 2);
		assertEquals(startStats.getVisits(TestActionEnum.RIGHT), 1);
	}
	
	@Test
	public void singleTree() {
		localProp.setProperty("MonteCarloSingleTree", "true");
		masterDecider.injectProperties(localProp);
		tree = new MonteCarloTree<TestAgent>(localProp, 1);
		TestAgent[] players = new TestAgent[3];
		players[0] = agent;
		players[1] = new TestAgent(world);
		players[2] = new TestAgent(world);
		mazeGame = new SimpleMazeGame(2, players);
		for (TestAgent a : players) {
			a.setDecider(masterDecider);
		}
		
		State<TestAgent> startState = masterDecider.getCurrentState(agent);
		mazeGame.oneMove();
		MonteCarloTree<TestAgent> tree = masterDecider.getTree(agent);
		System.out.println(tree.toString(true));
		MCStatistics<TestAgent> startStats = tree.getStatisticsFor(startState);
		assertEquals(tree.numberOfStates(), 4);
		startStats = tree.getStatisticsFor(startState);
		assertEquals(startStats.getVisits(), 99);
		assertEquals(startStats.getMean(TestActionEnum.LEFT)[0], 5.472, 0.01);
		assertEquals(startStats.getMean(TestActionEnum.TEST)[0], -4.75, 0.01);
		assertEquals(startStats.getMean(TestActionEnum.RIGHT)[0], -5.7, 0.01);
		assertEquals(startStats.getVisits(TestActionEnum.LEFT), 96);
		assertEquals(startStats.getVisits(TestActionEnum.TEST), 2);
		assertEquals(startStats.getVisits(TestActionEnum.RIGHT), 1);
		
//		startState = masterDecider.getCurrentState(players[1]);
		assertEquals(mazeGame.playerToMove, 1);
		mazeGame.oneMove();
		tree = masterDecider.getTree(agent);	// should be changed from other players turn
		assertEquals(tree.numberOfStates(), 7);
		startStats = tree.getStatisticsFor(startState);
		assertEquals(startStats.getVisits(), 99);
		assertEquals(startStats.getMean(TestActionEnum.LEFT)[0], 7.499, 0.01);
		assertEquals(startStats.getMean(TestActionEnum.TEST)[0], 1.737, 0.01);
		assertEquals(startStats.getMean(TestActionEnum.RIGHT)[0], 0.0, 0.01);
		assertEquals(startStats.getVisits(TestActionEnum.LEFT), 96);
		assertEquals(startStats.getVisits(TestActionEnum.TEST), 2);
		assertEquals(startStats.getVisits(TestActionEnum.RIGHT), 1);
	}

}


