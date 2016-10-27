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
	MonteCarloTree<TestAgent, ActionEnum<TestAgent>> tree = new MonteCarloTree<TestAgent, ActionEnum<TestAgent>>();
	// This is a single agent test, so no opponent model is needed, and the last parameter is safely null
	MCTSMasterDecider<TestAgent> masterDecider = new MCTSMasterDecider<TestAgent>(factory, actionList, rolloutDecider, null);
	
	World world = new World();
	TestAgent agent = new TestAgent(world);

	SimpleMazeGame mazeGame;
	
	@Before
	public void setup()  {
		world.setCalendar(new FastCalendar(0l));
		SimProperties.setProperty("MonteCarloReward", "true");
		agent.setDecider(masterDecider);
		mazeGame = new SimpleMazeGame(2, agent);
		agent.setGame(mazeGame);
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
		
		mazeGame.oneMove();
		MonteCarloTree<TestAgent, ActionEnum<TestAgent>> tree = masterDecider.getTree();
		System.out.println(tree);
		
		fail("Not yet implemented");
	}

}


