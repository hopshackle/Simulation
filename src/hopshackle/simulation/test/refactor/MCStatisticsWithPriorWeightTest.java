package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;

import java.util.*;

import hopshackle.simulation.*;

import org.junit.*;

public class MCStatisticsWithPriorWeightTest {

	List<ActionEnum<TestAgent>> allActions = new ArrayList<ActionEnum<TestAgent>>(EnumSet.allOf(TestActionEnum.class));
	List<ActionEnum<TestAgent>> leftRightOnly = new ArrayList<ActionEnum<TestAgent>>(EnumSet.allOf(TestActionEnum.class));
	MCStatistics<TestAgent> stats;
	MonteCarloTree<TestAgent> startingTree;
	
	
	@Before 
	public void setup() {
		leftRightOnly.remove(TestActionEnum.TEST);
		SimProperties.setProperty("MonteCarloUCTC", "1");
		SimProperties.setProperty("MonteCarloPriorActionWeightingForBestAction", "5");
		MCStatistics.refresh();
		State<TestAgent> dummyState = new State<TestAgent>() {
			@Override
			public String getAsString() {return "DUMMY";}
			@Override
			public double[] getAsArray() {return new double[1];}
			@Override
			public State<TestAgent> apply(ActionEnum<TestAgent> proposedAction) {return this;}
			@Override
			public State<TestAgent> clone() {return this;}
		};
		startingTree = new MonteCarloTree<TestAgent>();
		startingTree.insertState(dummyState, leftRightOnly);
		startingTree.updateState(dummyState, TestActionEnum.RIGHT, dummyState, 2.0);
	}

	@Test
	public void priorWeightWillShiftDecisionOnUCT() {
		stats = new MCStatistics<TestAgent>(leftRightOnly, startingTree);
		stats.update(TestActionEnum.LEFT, 2.0);
		stats.update(TestActionEnum.LEFT, 3.5);
		stats.update(TestActionEnum.LEFT, -1.0);
		stats.update(TestActionEnum.RIGHT, 1.0);

		assertEquals(stats.getVisits(TestActionEnum.LEFT), 3);
		assertEquals(stats.getVisits(TestActionEnum.RIGHT), 1);
		assertEquals(stats.getVisits(TestActionEnum.TEST), 0);
		assertEquals(stats.getMean(TestActionEnum.LEFT), 1.5, 0.001);
		assertEquals(stats.getMean(TestActionEnum.RIGHT), 1.0, 0.001);
		assertEquals(stats.getMean(TestActionEnum.TEST), 0.0, 0.001);
		
		assertTrue(stats.getBestAction(leftRightOnly) == TestActionEnum.RIGHT);
		assertTrue(stats.getUCTAction(leftRightOnly) == TestActionEnum.RIGHT);
	}

	@Test
	public void cycleThroughActionsIfNotAllTried() {
		stats = new MCStatistics<TestAgent>(leftRightOnly, startingTree);
		assertTrue(stats.hasUntriedAction(leftRightOnly));
		TestActionEnum newAction = (TestActionEnum) stats.getRandomUntriedAction(leftRightOnly);
		stats.update(newAction, 1.0);
		assertTrue(stats.hasUntriedAction(leftRightOnly));
		TestActionEnum newAction2 = (TestActionEnum) stats.getRandomUntriedAction(leftRightOnly);
		assertTrue(newAction != newAction2);
		stats.update(newAction2, 1.0);
		assertFalse(stats.hasUntriedAction(leftRightOnly));
	}

	@Test
	public void uctActionReturnsBestBound() {
		stats = new MCStatistics<TestAgent>(leftRightOnly, startingTree);
		stats.update(TestActionEnum.LEFT, 2.0);
		stats.update(TestActionEnum.RIGHT, 1.0);
		assertFalse(stats.hasUntriedAction(leftRightOnly));
		TestActionEnum nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly);
		assertTrue(nextPick == TestActionEnum.RIGHT);
		assertTrue(stats.getBestAction(leftRightOnly) == TestActionEnum.RIGHT);
		stats.update(nextPick, -2.0);
		nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly);
		assertTrue(nextPick == TestActionEnum.RIGHT);
		assertTrue(stats.getBestAction(leftRightOnly) == TestActionEnum.RIGHT);
		stats.update(nextPick, 0.0);
		nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly);
		assertTrue(nextPick == TestActionEnum.RIGHT);
		assertTrue(stats.getBestAction(leftRightOnly) == TestActionEnum.RIGHT);
		stats.update(nextPick, -1.0);
		nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly);
		assertTrue(nextPick == TestActionEnum.LEFT);
		assertTrue(stats.getBestAction(leftRightOnly) == TestActionEnum.RIGHT);
		stats.update(nextPick, 1.0);
		nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly);
		assertTrue(nextPick == TestActionEnum.RIGHT);
		assertTrue(stats.getBestAction(leftRightOnly) == TestActionEnum.RIGHT);
		// in this test, the actionvalues in tree are never updated
		/* Expected results for C, N (total), 
		 * n (for this action) and Q
		C	N	n	Q	A	Q'		UCT			Action
		1	2	1	1	2	1.833	2.666		RIGHT
		1	2	1	2	0	0.333	1.166		LEFT
		1	3	2 -0.5	2	1.5		2.241		RIGHT
		1	3	1	2	0	0.333	1.381		LEFT
		1	4	3 -0.33	2	1.125	1.805		RIGHT
		1	4	1	2	0	0.333	1.510		LEFT
		1	5	4 -0.5	2	0.889	1.523		RIGHT
		1	5	1	2	0	0.333	1.602		LEFT
		1	6	4 -0.5	2	0.889	1.558		RIGHT
		1	6	2  1.5	0	0.429	1.376		LEFT
		 */
	}
	
	@Test
	public void uctReturnsBestActionWithActionValueWeighting() {
		SimProperties.setProperty("MonteCarloPriorActionWeightingForBestAction", "5");
		MCStatistics.refresh();
		stats = new MCStatistics<TestAgent>(leftRightOnly, startingTree);
		stats.update(TestActionEnum.LEFT, 2.0);
		stats.update(TestActionEnum.RIGHT, 1.0);
		assertFalse(stats.hasUntriedAction(leftRightOnly));
		TestActionEnum nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly);
		assertTrue(nextPick == TestActionEnum.RIGHT);
		assertTrue(stats.getBestAction(leftRightOnly) == TestActionEnum.RIGHT);
		stats.update(nextPick, -2.0);
		nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly);
		assertTrue(nextPick == TestActionEnum.RIGHT);
		assertTrue(stats.getBestAction(leftRightOnly) == TestActionEnum.RIGHT);
		stats.update(nextPick, 0.0);
		nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly);
		assertTrue(nextPick == TestActionEnum.RIGHT);
		assertTrue(stats.getBestAction(leftRightOnly) == TestActionEnum.RIGHT);
		stats.update(nextPick, -1.0);
		nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly);
		assertTrue(nextPick == TestActionEnum.LEFT);
		assertTrue(stats.getBestAction(leftRightOnly) == TestActionEnum.RIGHT);
		stats.update(nextPick, 1.0);
		nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly);
		assertTrue(nextPick == TestActionEnum.RIGHT);
		assertTrue(stats.getBestAction(leftRightOnly) == TestActionEnum.RIGHT);
		// in this test, the actionvalues in tree are never updated
		/* Expected results for C, N (total), 
		 * n (for this action) and Q
		C	N	n	Q	A	Q'		UCT			Action
		1	2	1	1	2	1.833	2.666		RIGHT
		1	2	1	2	0	0.333	1.166		LEFT
		1	3	2 -0.5	2	1.5		2.241		RIGHT
		1	3	1	2	0	0.333	1.381		LEFT
		1	4	3 -0.33	2	1.125	1.805		RIGHT
		1	4	1	2	0	0.333	1.510		LEFT
		1	5	4 -0.5	2	0.889	1.523		RIGHT
		1	5	1	2	0	0.333	1.602		LEFT
		1	6	4 -0.5	2	0.889	1.558		RIGHT
		1	6	2  1.5	0	0.429	1.376		LEFT
		 */
	}
	
	@Test
	public void actionAddedAfterInstantiation() {
		priorWeightWillShiftDecisionOnUCT();
		assertFalse(stats.hasUntriedAction(leftRightOnly));
		assertEquals(stats.getPossibleActions().size(),2);
		try {
			stats.getRandomUntriedAction(leftRightOnly);
			fail("Random action returned when there should not be any.");
		} catch (AssertionError e) {
			// as expected
		}
		assertTrue(stats.hasUntriedAction(allActions));
		assertEquals(stats.getPossibleActions().size(),3);
		assertTrue(stats.getRandomUntriedAction(allActions) == TestActionEnum.TEST);
	}
	
	@Test
	public void updateWithPreviouslyUnknownActionShouldError() {
		stats = new MCStatistics<TestAgent>(leftRightOnly, startingTree);
		try {
			stats.update(TestActionEnum.LEFT, 5.0);
			fail("Error should be thrown if unseen action used.");
		} catch (AssertionError e) {
			// as expected
		}
	}
}
