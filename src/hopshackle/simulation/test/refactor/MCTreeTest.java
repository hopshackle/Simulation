package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;
import hopshackle.simulation.*;

import java.util.*;

import org.junit.*;

public class MCTreeTest {

	State<TestAgent> test, other, yetAnother;
	List<ActionEnum<TestAgent>> allActions = new ArrayList<ActionEnum<TestAgent>>(EnumSet.allOf(TestActionEnum.class));
	List<ActionEnum<TestAgent>> leftRightOnly = new ArrayList<ActionEnum<TestAgent>>(EnumSet.allOf(TestActionEnum.class));
	MonteCarloTree<TestAgent> tree;
	World world;
	TestAgent agent;
	DeciderProperties localProp;

	@Before 
	public void setup() {
		localProp = SimProperties.getDeciderProperties("GLOBAL");
		localProp.setProperty("MonteCarloRL", "false");
		localProp.setProperty("MonteCarloUCTType", "MC");
		localProp.setProperty("MonteCarloUCTC", "1");
		leftRightOnly.remove(TestActionEnum.TEST);
		test = new State<TestAgent>() {
			@Override
			public double[] getAsArray() {
				double[] retValue = {0.0, 0.1, 0.2};
				return retValue;
			}
			@Override
			public String getAsString() {
				return "0.00|0.10|0.20";
			}
			@Override public State<TestAgent> clone() { return this;}
			@Override public State<TestAgent> apply(ActionEnum<TestAgent> action) { return this;}
			@Override public int getActorRef() {return 0;}
			@Override public double[] getScore() {return new double[1];}
		};
		other = new State<TestAgent>() {
			@Override
			public double[] getAsArray() {
				double[] retValue = {1.0, 0.1, 0.2};
				return retValue;
			}
			@Override
			public String getAsString() {
				return "1.00|0.10|0.20|";
			}
			@Override public State<TestAgent> clone() { return this;}
			@Override public State<TestAgent> apply(ActionEnum<TestAgent> action) { return this;}
			@Override public int getActorRef() {return 0;}
			@Override public double[] getScore() {return new double[1];}
		};
		yetAnother = new State<TestAgent>() {
			@Override
			public double[] getAsArray() {
				double[] retValue = {0.0, 0.5, 0.2};
				return retValue;
			}
			@Override
			public String getAsString() {
				return "0.00|0.50|0.20|";
			}
			@Override public State<TestAgent> clone() { return this;}
			@Override public State<TestAgent> apply(ActionEnum<TestAgent> action) { return this;}
			@Override public int getActorRef() {return 0;}
			@Override public double[] getScore() {return new double[1];}
		};
		world = new World();
		agent = new TestAgent(world);
	}
	
	@After
	public void tearDown() {
		SimProperties.clear();
	}

	@Test
	public void settingUpdatesLeft() {
		tree = new MonteCarloTree<TestAgent>(localProp, 1);
		assertEquals(tree.updatesLeft(), 0);
		assertFalse(tree.containsState(test));
		tree.setUpdatesLeft(1);
		assertEquals(tree.updatesLeft(), 1);
	}

	@Test
	public void addANode() {
		tree = new MonteCarloTree<TestAgent>(localProp, 1);
		tree.setUpdatesLeft(1);
		tree.insertState(test, leftRightOnly);
		assertTrue(tree.containsState(test));
		assertFalse(tree.containsState(other));
		assertEquals(tree.updatesLeft(), 0);
	}

	@Test
	public void seriesOfActionsFromSameStateThroughUCTLogic() {
		// This uses the same numeric test case as in MCStatisticsTest (but one level higher in 
		// the object hierarchy)
		tree = new MonteCarloTree<TestAgent>(localProp);
		tree.insertState(test, leftRightOnly);
		TestActionEnum next = (TestActionEnum) tree.getNextAction(test, leftRightOnly);
		boolean firstLeft = false;
		if (next == TestActionEnum.LEFT) 
			firstLeft = true;
		if (firstLeft) 
			tree.updateState(test, TestActionEnum.LEFT, test, 2.0);
		else
			tree.updateState(test, TestActionEnum.RIGHT, test, 1.0);
		next = (TestActionEnum) tree.getNextAction(test, leftRightOnly);
		if (firstLeft) {
			assertTrue(next == TestActionEnum.RIGHT);
			tree.updateState(test, TestActionEnum.RIGHT, test, 1.0);
		} else {
			assertTrue(next == TestActionEnum.LEFT);
			tree.updateState(test, TestActionEnum.LEFT, test, 2.0);
		}

		next = (TestActionEnum) tree.getNextAction(test, leftRightOnly);
		assertTrue(next == TestActionEnum.LEFT);
		tree.updateState(test, TestActionEnum.LEFT, test, 2.0);
		next = (TestActionEnum) tree.getNextAction(test, leftRightOnly);
		assertTrue(next == TestActionEnum.LEFT);
		tree.updateState(test, TestActionEnum.LEFT, test, 0.5);
		next = (TestActionEnum) tree.getNextAction(test, leftRightOnly);
		assertTrue(next == TestActionEnum.LEFT);
		tree.updateState(test, TestActionEnum.LEFT, test, 1.5);
		next = (TestActionEnum) tree.getNextAction(test, leftRightOnly);
		assertTrue(next == TestActionEnum.RIGHT);
		tree.updateState(test, TestActionEnum.RIGHT, test, 1.0);
		next = (TestActionEnum) tree.getNextAction(test, leftRightOnly);
		assertTrue(next == TestActionEnum.LEFT);

	}

	@Test
	public void getNextActionWithAPreviouslyUnseenActionShouldReturnIt() {
		tree = new MonteCarloTree<TestAgent>(localProp);
		tree.insertState(test, leftRightOnly);
		tree.updateState(test, TestActionEnum.LEFT, test, 2.0);
		tree.updateState(test, TestActionEnum.RIGHT, test, 2.0);
		tree.updateState(test, TestActionEnum.LEFT, test, 1.0);
		TestActionEnum leftRight = (TestActionEnum) tree.getNextAction(test, leftRightOnly);
		TestActionEnum allA = (TestActionEnum) tree.getNextAction(test, allActions);
		assertTrue(leftRight == TestActionEnum.RIGHT);
		assertTrue(allA == TestActionEnum.TEST);
	}

	@Test
	public void getNextActionWithRestrictedListShouldObeyRestrictions() {
		tree = new MonteCarloTree<TestAgent>(localProp);
		tree.insertState(test, allActions);
		tree.updateState(test, TestActionEnum.LEFT, test, 2.0);
		tree.updateState(test, TestActionEnum.RIGHT, test, 2.0);
		tree.updateState(test, TestActionEnum.LEFT, test, 1.0);
		TestActionEnum leftRight = (TestActionEnum) tree.getNextAction(test, leftRightOnly);
		TestActionEnum allA = (TestActionEnum) tree.getNextAction(test, allActions);
		assertTrue(leftRight == TestActionEnum.RIGHT);
		assertTrue(allA == TestActionEnum.TEST);
	}


	@Test
	public void getBestActionWithAPreviouslyUnseenActionShouldNotReturnIt() {
		tree = new MonteCarloTree<TestAgent>(localProp);
		tree.insertState(test, leftRightOnly);
		tree.updateState(test, TestActionEnum.LEFT, test, 2.0);
		tree.updateState(test, TestActionEnum.RIGHT, test, 2.0);
		tree.updateState(test, TestActionEnum.LEFT, test, 1.0);
		TestActionEnum leftRight = (TestActionEnum) tree.getBestAction(test, leftRightOnly);
		TestActionEnum allA = (TestActionEnum) tree.getBestAction(test, allActions);
		assertTrue(leftRight == TestActionEnum.RIGHT);
		assertTrue(allA == TestActionEnum.RIGHT);
	}

	@Test
	public void getBestActionWithRestrictedListShouldObeyRestrictions() {
		tree = new MonteCarloTree<TestAgent>(localProp);
		tree.insertState(test, allActions);
		tree.updateState(test, TestActionEnum.LEFT, test, 2.0);
		tree.updateState(test, TestActionEnum.RIGHT, test, 2.0);
		tree.updateState(test, TestActionEnum.TEST, test, 5.0);
		tree.updateState(test, TestActionEnum.LEFT, test, 1.0);
		TestActionEnum leftRight = (TestActionEnum) tree.getBestAction(test, leftRightOnly);
		TestActionEnum allA = (TestActionEnum) tree.getBestAction(test, allActions);
		assertTrue(leftRight == TestActionEnum.RIGHT);
		assertTrue(allA == TestActionEnum.TEST);
	}


	@Test
	public void updateStateWithAPreviouslyUnseenActionShouldError() {
		tree = new MonteCarloTree<TestAgent>(localProp);
		tree.insertState(test, leftRightOnly);
		try {
			tree.updateState(test, TestActionEnum.TEST, test, 1.0);
			fail("TEST action is unknown - this should throw error");
		} catch (AssertionError e) {
			// as expected
		}
	}

	@Test
	public void actionValueDeciderInGreedyMode() {
		localProp.setProperty("Temperature", "0.00");
		localProp.setProperty("RandomDeciderMaxChance", "0.0");
		localProp.setProperty("RandomDeciderMinChance", "0.0");
		tree = new MonteCarloTree<TestAgent>(localProp);
		tree.updateState(test, TestActionEnum.RIGHT, test, 5.0);
		tree.updateState(test, TestActionEnum.LEFT, test, 4.0);
		assertEquals(tree.getActionValue(TestActionEnum.RIGHT.toString(), 1), 5.0, 0.001);
		assertEquals(tree.getActionValue(TestActionEnum.LEFT.toString(), 1), 4.0, 0.001);
		assertEquals(tree.getActionValue(TestActionEnum.TEST.toString(), 1), 0.0, 0.001);
		MCActionValueDecider<TestAgent> avDecider = new MCActionValueDecider<TestAgent>(tree, null, 1);
		for (int i = 0; i < 100; i++) {
			assertTrue(avDecider.makeDecision(agent, leftRightOnly) == TestActionEnum.RIGHT);
			assertTrue(avDecider.makeDecision(agent, allActions) == TestActionEnum.RIGHT);
		}
	}

	@Test
	public void actionValueDeciderinBoltzmannMode() {
		localProp.setProperty("Temperature", "0.1");
		localProp.setProperty("RandomDeciderMaxChance", "1.0");
		localProp.setProperty("RandomDeciderMinChance", "1.0");
		tree = new MonteCarloTree<TestAgent>(localProp);
		tree.updateState(test, TestActionEnum.RIGHT, test, 5.0);
		tree.updateState(test, TestActionEnum.LEFT, test, 4.0);
		assertEquals(tree.getActionValue(TestActionEnum.RIGHT.toString(), 1), 5.0, 0.001);
		assertEquals(tree.getActionValue(TestActionEnum.LEFT.toString(), 1), 4.0, 0.001);
		assertEquals(tree.getActionValue(TestActionEnum.TEST.toString(), 1), 0.0, 0.001);
		MCActionValueDecider<TestAgent> avDecider = new MCActionValueDecider<TestAgent>(tree, null, 1);
		int rightCount = 0, leftCount = 0, testCount = 0;
		for (int i = 0; i < 100; i++) {
			ActionEnum<TestAgent> d = avDecider.makeDecision(agent, allActions);
			switch (d.toString()) {
			case "RIGHT":
				rightCount++;
				break;
			case "LEFT":
				leftCount++;
				break;
			case "TEST":
				testCount++;
				break;
			}
		}	
		assertEquals(rightCount+leftCount+testCount, 100);
		assertTrue(rightCount > leftCount);
		assertTrue(leftCount > testCount);
		assertTrue(leftCount > 0);
	}
	
	@Test
	public void successorStatesInTree() {
		tree = new MonteCarloTree<TestAgent>(localProp);
		tree.insertState(test, allActions);
		tree.updateState(test, TestActionEnum.LEFT, other, 0.0);
		assertEquals(tree.getStatisticsFor(test).getSuccessorStates().size(), 0);

		assertEquals(tree.numberOfStates(), 1);
		tree.updateState(test, TestActionEnum.RIGHT, yetAnother, 0.0);
		tree.updateState(test, TestActionEnum.RIGHT, other, 0.0);
		assertEquals(tree.getStatisticsFor(test).getSuccessorStates().size(), 0);
		assertEquals(tree.numberOfStates(), 1);
		
		tree.insertState(other, allActions);
		tree.insertState(yetAnother, allActions);
		tree.updateState(test, TestActionEnum.RIGHT, yetAnother, 0.0);
		tree.updateState(test, TestActionEnum.RIGHT, other, 0.0);
		tree.updateState(other, TestActionEnum.RIGHT, yetAnother, 0.0);
		tree.updateState(yetAnother, TestActionEnum.RIGHT, yetAnother, 0.0);
		assertEquals(tree.getStatisticsFor(test).getSuccessorStates().size(), 2);
		assertEquals(tree.numberOfStates(), 3);
		
		tree.pruneTree(test.getAsString());	// should have no impact
		assertEquals(tree.getStatisticsFor(test).getSuccessorStates().size(), 2);
		assertEquals(tree.numberOfStates(), 3);
		
		tree.pruneTree(other.getAsString());
		assertTrue(tree.getStatisticsFor(test) == null);
		assertEquals(tree.getStatisticsFor(other).getSuccessorStates().size(), 1);
		assertEquals(tree.numberOfStates(), 2);
	}
	
	@Test
	public void RAVEvalues() {
		localProp.setProperty("MonteCarloRAVE", "true");
		localProp.setProperty("MonteCarloRAVEWeight", "2");
		tree = new MonteCarloTree<TestAgent>(localProp);
		tree.insertState(test, allActions);
		tree.updateState(test, TestActionEnum.LEFT, other, 3.0);
		MCStatistics<TestAgent> stats = tree.getStatisticsFor(test);
		assertEquals(stats.getRAVEValue(TestActionEnum.LEFT), 0.0, 0.001);
		assertEquals(stats.getRAVEValue(TestActionEnum.RIGHT), 0.0, 0.001);
		assertEquals(stats.getRAVEPlus(TestActionEnum.RIGHT), 0.0, 0.001);
		tree.updateState(test, TestActionEnum.RIGHT, other, 0.0);
		tree.updateState(test, TestActionEnum.TEST, other, 0.0);
		stats = tree.getStatisticsFor(test);
		assertEquals(stats.getRAVEValue(TestActionEnum.LEFT), 0.0, 0.001);
		assertEquals(stats.getRAVEValue(TestActionEnum.RIGHT), 0.0, 0.001);
		assertEquals(stats.getRAVEPlus(TestActionEnum.RIGHT), 0.0, 0.001);
		
		tree.updateRAVE(test, TestActionEnum.RIGHT, new double[] {3.0});
		tree.updateRAVE(test, TestActionEnum.RIGHT, new double[] {5.0});
		tree.updateRAVE(test, TestActionEnum.TEST, new double[] {0.0});
		stats = tree.getStatisticsFor(test);
		assertEquals(stats.getRAVEValue(TestActionEnum.LEFT), 0.0, 0.001);
		assertEquals(stats.getRAVEValue(TestActionEnum.TEST), 0.0, 0.001);
		assertEquals(stats.getRAVEValue(TestActionEnum.RIGHT), 4.0, 0.001);
		assertEquals(stats.getRAVEPlus(TestActionEnum.RIGHT), 4.0 + Math.sqrt(Math.log(3.0) / 2.0), 0.001);
		assertEquals(stats.getRAVEPlus(TestActionEnum.TEST), 0.0 + Math.sqrt(Math.log(3.0) / 1.0), 0.001);
		assertEquals(stats.getRAVEPlus(TestActionEnum.LEFT), 0.0, 0.001);
		
		assertTrue(stats.getUCTAction(allActions).equals(TestActionEnum.LEFT));
		tree.updateRAVE(test, TestActionEnum.RIGHT, new double[] {6.0});
		tree.updateRAVE(test, TestActionEnum.RIGHT, new double[] {7.0});
		assertTrue(stats.getUCTAction(allActions).equals(TestActionEnum.RIGHT));
	}

}
