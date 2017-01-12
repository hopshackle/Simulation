package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;
import hopshackle.simulation.*;

import java.util.*;

import org.junit.*;

public class MCTSTest {
	
	List<ActionEnum<TestAgent>> actionList = new ArrayList<ActionEnum<TestAgent>>(EnumSet.allOf(TestActionEnum.class));
	List<GeneticVariable<TestAgent>> genVar = new ArrayList<GeneticVariable<TestAgent>>(EnumSet.allOf(TestGenVar.class));
	StateFactory<TestAgent> factory = new LinearStateFactory<TestAgent>(genVar);
	Decider<TestAgent> rolloutDecider = new SimpleMazeDecider();
	MonteCarloTree<TestAgent> tree = new MonteCarloTree<TestAgent>();
	// This is a single agent test, so no opponent model is needed, and the last parameter is safely null
	MCTSMasterDecider<TestAgent> masterDecider;
	
	World world = new World();
	TestAgent agent = new TestAgent(world);

	SimpleMazeGame mazeGame;
	
	@Before
	public void setup()  {
		world.setCalendar(new FastCalendar(0l));
		SimProperties.setProperty("MonteCarloReward", "true");
		SimProperties.setProperty("MonteCarloUCTC", "1.0");
		SimProperties.setProperty("Gamma", "0.95");
		SimProperties.setProperty("IncrementalScoreReward", "false");
		SimProperties.setProperty("MonteCarloRolloutCount", "99");
		SimProperties.setProperty("MonteCarloEffectiveVisitsForPriorActionInformation", "0");
		MCStatistics.refresh();
		ExperienceRecord.refreshProperties();
		mazeGame = new SimpleMazeGame(2, agent);
		masterDecider = new MCTSMasterDecider<TestAgent>(factory, actionList, rolloutDecider, null);
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
		
		State<TestAgent> startState = masterDecider.getCurrentState(agent);
		mazeGame.oneMove();
		MonteCarloTree<TestAgent> tree = masterDecider.getTree(agent);
		System.out.println(tree.toString(true));
		assertEquals(tree.numberOfStates(), 7);
		MCStatistics<TestAgent> startStats = tree.getStatisticsFor(startState);
		assertEquals(startStats.getVisits(), 99);
		assertEquals(startStats.getMean(TestActionEnum.LEFT), -1.96, 0.01);
		assertEquals(startStats.getMean(TestActionEnum.TEST), -5.33, 0.01);
		assertEquals(startStats.getMean(TestActionEnum.RIGHT), -6.3, 0.01);
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
		assertEquals(startStats.getMean(TestActionEnum.LEFT), -2.00, 0.01);
		assertEquals(startStats.getMean(TestActionEnum.TEST), -4.29, 0.01);
		assertEquals(startStats.getMean(TestActionEnum.RIGHT), -5.88, 0.01);
		assertEquals(startStats.getVisits(TestActionEnum.LEFT), 97);
		assertEquals(startStats.getVisits(TestActionEnum.TEST), 1);
		assertEquals(startStats.getVisits(TestActionEnum.RIGHT), 1);
	}

}


