package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;
import hopshackle.simulation.*;

import java.util.*;

import org.junit.*;

public class MCTreeTest {

	State<TestAgent> test, other;
	List<ActionEnum<TestAgent>> allActions = new ArrayList<ActionEnum<TestAgent>>(EnumSet.allOf(TestActionEnum.class));
	List<ActionEnum<TestAgent>> leftRightOnly = new ArrayList<ActionEnum<TestAgent>>(EnumSet.allOf(TestActionEnum.class));
	MonteCarloTree<TestAgent> tree;
	World world;
	TestAgent agent;

	@Before 
	public void setup() {
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
		};
		world = new World();
		agent = new TestAgent(world);
		SimProperties.setProperty("MonteCarloUCTC", "1");
	}

	@Test
	public void settingUpdatesLeft() {
		tree = new MonteCarloTree<TestAgent>();
		assertEquals(tree.updatesLeft(), 0);
		assertFalse(tree.containsState(test));
		tree.setUpdatesLeft(1);
		assertEquals(tree.updatesLeft(), 1);
	}

	@Test
	public void addANode() {
		tree = new MonteCarloTree<TestAgent>();
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
		tree = new MonteCarloTree<TestAgent>();
		tree.insertState(test, leftRightOnly);
		TestActionEnum next = (TestActionEnum) tree.getNextAction(test, leftRightOnly);
		boolean firstLeft = false;
		if (next == TestActionEnum.LEFT) 
			firstLeft = true;
		if (firstLeft) 
			tree.updateState(test, TestActionEnum.LEFT, 2.0);
		else
			tree.updateState(test, TestActionEnum.RIGHT, 1.0);
		next = (TestActionEnum) tree.getNextAction(test, leftRightOnly);
		if (firstLeft) {
			assertTrue(next == TestActionEnum.RIGHT);
			tree.updateState(test, TestActionEnum.RIGHT, 1.0);
		} else {
			assertTrue(next == TestActionEnum.LEFT);
			tree.updateState(test, TestActionEnum.LEFT, 2.0);
		}

		next = (TestActionEnum) tree.getNextAction(test, leftRightOnly);
		assertTrue(next == TestActionEnum.LEFT);
		tree.updateState(test, TestActionEnum.LEFT, 2.0);
		next = (TestActionEnum) tree.getNextAction(test, leftRightOnly);
		assertTrue(next == TestActionEnum.LEFT);
		tree.updateState(test, TestActionEnum.LEFT, 0.5);
		next = (TestActionEnum) tree.getNextAction(test, leftRightOnly);
		assertTrue(next == TestActionEnum.LEFT);
		tree.updateState(test, TestActionEnum.LEFT, 1.5);
		next = (TestActionEnum) tree.getNextAction(test, leftRightOnly);
		assertTrue(next == TestActionEnum.RIGHT);
		tree.updateState(test, TestActionEnum.RIGHT, 1.0);
		next = (TestActionEnum) tree.getNextAction(test, leftRightOnly);
		assertTrue(next == TestActionEnum.LEFT);

	}

	@Test
	public void getNextActionWithAPreviouslyUnseenActionShouldReturnIt() {
		tree = new MonteCarloTree<TestAgent>();
		tree.insertState(test, leftRightOnly);
		tree.updateState(test, TestActionEnum.LEFT, 2.0);
		tree.updateState(test, TestActionEnum.RIGHT, 2.0);
		tree.updateState(test, TestActionEnum.LEFT, 1.0);
		TestActionEnum leftRight = (TestActionEnum) tree.getNextAction(test, leftRightOnly);
		TestActionEnum allA = (TestActionEnum) tree.getNextAction(test, allActions);
		assertTrue(leftRight == TestActionEnum.RIGHT);
		assertTrue(allA == TestActionEnum.TEST);
	}

	@Test
	public void getNextActionWithRestrictedListShouldObeyRestrictions() {
		tree = new MonteCarloTree<TestAgent>();
		tree.insertState(test, allActions);
		tree.updateState(test, TestActionEnum.LEFT, 2.0);
		tree.updateState(test, TestActionEnum.RIGHT, 2.0);
		tree.updateState(test, TestActionEnum.LEFT, 1.0);
		TestActionEnum leftRight = (TestActionEnum) tree.getNextAction(test, leftRightOnly);
		TestActionEnum allA = (TestActionEnum) tree.getNextAction(test, allActions);
		assertTrue(leftRight == TestActionEnum.RIGHT);
		assertTrue(allA == TestActionEnum.TEST);
	}


	@Test
	public void getBestActionWithAPreviouslyUnseenActionShouldNotReturnIt() {
		tree = new MonteCarloTree<TestAgent>();
		tree.insertState(test, leftRightOnly);
		tree.updateState(test, TestActionEnum.LEFT, 2.0);
		tree.updateState(test, TestActionEnum.RIGHT, 2.0);
		tree.updateState(test, TestActionEnum.LEFT, 1.0);
		TestActionEnum leftRight = (TestActionEnum) tree.getBestAction(test, leftRightOnly);
		TestActionEnum allA = (TestActionEnum) tree.getBestAction(test, allActions);
		assertTrue(leftRight == TestActionEnum.RIGHT);
		assertTrue(allA == TestActionEnum.RIGHT);
	}

	@Test
	public void getBestActionWithRestrictedListShouldObeyRestrictions() {
		tree = new MonteCarloTree<TestAgent>();
		tree.insertState(test, allActions);
		tree.updateState(test, TestActionEnum.LEFT, 2.0);
		tree.updateState(test, TestActionEnum.RIGHT, 2.0);
		tree.updateState(test, TestActionEnum.TEST, 5.0);
		tree.updateState(test, TestActionEnum.LEFT, 1.0);
		TestActionEnum leftRight = (TestActionEnum) tree.getBestAction(test, leftRightOnly);
		TestActionEnum allA = (TestActionEnum) tree.getBestAction(test, allActions);
		assertTrue(leftRight == TestActionEnum.RIGHT);
		assertTrue(allA == TestActionEnum.TEST);
	}


	@Test
	public void updateStateWithAPreviouslyUnseenActionShouldError() {
		tree = new MonteCarloTree<TestAgent>();
		tree.insertState(test, leftRightOnly);
		try {
			tree.updateState(test, TestActionEnum.TEST, 1.0);
			fail("TEST action is unknown - this should throw error");
		} catch (AssertionError e) {
			// as expected
		}
	}

	@Test
	public void actionValueDeciderInGreedyMode() {
		tree = new MonteCarloTree<TestAgent>();
		SimProperties.setProperty("MonteCarloActionValueDeciderTemperature", "0.00");
		tree.updateState(test, TestActionEnum.RIGHT, 5.0);
		tree.updateState(test, TestActionEnum.LEFT, 4.0);
		assertEquals(tree.getActionValue(TestActionEnum.RIGHT.toString()), 5.0, 0.001);
		assertEquals(tree.getActionValue(TestActionEnum.LEFT.toString()), 4.0, 0.001);
		assertEquals(tree.getActionValue(TestActionEnum.TEST.toString()), 0.0, 0.001);
		MCActionValueDecider<TestAgent> avDecider = new MCActionValueDecider<TestAgent>(tree, null, allActions);
		for (int i = 0; i < 100; i++) {
			assertTrue(avDecider.makeDecision(agent, leftRightOnly) == TestActionEnum.RIGHT);
			assertTrue(avDecider.makeDecision(agent, allActions) == TestActionEnum.RIGHT);
		}
	}

	@Test
	public void actionValueDeciderinBoltzmannMode() {
		tree = new MonteCarloTree<TestAgent>();
		SimProperties.setProperty("MonteCarloActionValueDeciderTemperature", "0.1");
		tree.updateState(test, TestActionEnum.RIGHT, 5.0);
		tree.updateState(test, TestActionEnum.LEFT, 4.0);
		assertEquals(tree.getActionValue(TestActionEnum.RIGHT.toString()), 5.0, 0.001);
		assertEquals(tree.getActionValue(TestActionEnum.LEFT.toString()), 4.0, 0.001);
		assertEquals(tree.getActionValue(TestActionEnum.TEST.toString()), 0.0, 0.001);
		MCActionValueDecider<TestAgent> avDecider = new MCActionValueDecider<TestAgent>(tree, null, allActions);
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
		System.out.println(rightCount + " : " + leftCount + " : " + testCount);
		assertEquals(rightCount+leftCount+testCount, 100);
		assertTrue(rightCount > leftCount);
		assertTrue(leftCount > testCount);
		assertTrue(leftCount > 0);
	}

}
