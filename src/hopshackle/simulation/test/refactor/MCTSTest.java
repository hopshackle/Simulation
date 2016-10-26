package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;
import hopshackle.simulation.*;

import java.util.*;

import org.junit.*;

public class MCTSTest {
	
	List<TestActionEnum> actionList = new ArrayList<TestActionEnum>(EnumSet.allOf(TestActionEnum.class));
	List<GeneticVariable<TestAgent>> genVar = new ArrayList<GeneticVariable<TestAgent>>(EnumSet.allOf(TestGenVar.class));
	StateFactory<TestAgent> factory = new LinearStateFactory<TestAgent>(genVar);
	SimpleMazeDecider rolloutDecider = new SimpleMazeDecider();
	MonteCarloTree<TestAgent, TestActionEnum> tree = new MonteCarloTree<TestAgent, TestActionEnum>();
	MCTSChildDecider childDecider = new MCTSChildDecider<TestAgent, TestActionEnum>(factory, actionList, tree, rolloutDecider);
	
	World world = new World();
	TestAgent agent = new TestAgent(world);

	@Before
	public void setup()  {
		agent.setDeath(masterDecider);
	}
	
	@Test
	public void test() {
		
		fail("Not yet implemented");
	}

}
