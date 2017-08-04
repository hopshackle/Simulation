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
	DeciderProperties localProp;
	State<TestAgent> dummyState = new State<TestAgent>() {
		@Override
		public String getAsString() {return "DUMMY";}
		@Override
		public double[] getAsArray() {return new double[1];}
		@Override
		public State<TestAgent> apply(ActionEnum<TestAgent> proposedAction) {return this;}
		@Override
		public State<TestAgent> clone() {return this;}
		@Override
		public int getActorRef() {return 0;	}
		@Override
		public double[] getScore() {return new double[1];}
	};
	
	@Before 
	public void setup() {
		localProp = SimProperties.getDeciderProperties("GLOBAL");
		leftRightOnly.remove(TestActionEnum.TEST);
		localProp.setProperty("MonteCarloUCTC", "1");
		localProp.setProperty("MonteCarloHeuristicOnSelection", "true");
		localProp.setProperty("MonteCarloMAST", "true");
		localProp.setProperty("MonteCarloHeuristicWeighting", "5");
		localProp.setProperty("MonteCarloChoice", "default");
		
		startingTree = new MonteCarloTree<TestAgent>(localProp, 1);
	//	startingTree.setOfflineHeuristic(new MASTHeuristic<TestAgent>(startingTree));
		startingTree.insertState(dummyState, leftRightOnly);
		startingTree.updateState(dummyState, TestActionEnum.RIGHT, dummyState, toArray(2.0), 0);
	}

	@Test
	public void priorWeightWillShiftDecisionOnUCT() {
		stats = new MCStatistics<TestAgent>(leftRightOnly, startingTree, 1, dummyState);
		stats.update(TestActionEnum.LEFT, toArray(2.0));
		stats.update(TestActionEnum.LEFT, toArray(3.5));
		stats.update(TestActionEnum.LEFT, toArray(-1.0));
		stats.update(TestActionEnum.RIGHT, toArray(1.0));

		assertEquals(stats.getVisits(TestActionEnum.LEFT), 3);
		assertEquals(stats.getVisits(TestActionEnum.RIGHT), 1);
		assertEquals(stats.getVisits(TestActionEnum.TEST), 0);
		assertEquals(stats.getMean(TestActionEnum.LEFT)[0], 1.5, 0.001);
		assertEquals(stats.getMean(TestActionEnum.RIGHT)[0], 1.0, 0.001);
		assertEquals(stats.getMean(TestActionEnum.TEST)[0], 0.0, 0.001);
		
		assertTrue(stats.getBestAction(leftRightOnly, 0) == TestActionEnum.RIGHT);
		assertTrue(stats.getUCTAction(leftRightOnly, 0) == TestActionEnum.RIGHT);
	}

	@Test
	public void cycleThroughActionsIfNotAllTried() {
		stats = new MCStatistics<TestAgent>(leftRightOnly, startingTree, 1, dummyState);
		assertTrue(stats.hasUntriedAction(leftRightOnly));
		TestActionEnum newAction = (TestActionEnum) stats.getRandomUntriedAction(leftRightOnly, 0);
		stats.update(newAction, toArray(1.0));
		assertTrue(stats.hasUntriedAction(leftRightOnly));
		TestActionEnum newAction2 = (TestActionEnum) stats.getRandomUntriedAction(leftRightOnly, 0);
		assertTrue(newAction != newAction2);
		stats.update(newAction2, toArray(1.0));
		assertFalse(stats.hasUntriedAction(leftRightOnly));
	}

	@Test
	public void uctActionReturnsBestBound() {
		stats = new MCStatistics<TestAgent>(leftRightOnly, startingTree, 1, dummyState);
		stats.update(TestActionEnum.LEFT, toArray(2.0));
		stats.update(TestActionEnum.RIGHT, toArray(1.0));
		assertFalse(stats.hasUntriedAction(leftRightOnly));
		TestActionEnum nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly, 0);
		assertTrue(nextPick == TestActionEnum.RIGHT);
		assertTrue(stats.getBestAction(leftRightOnly, 0) == TestActionEnum.RIGHT);
		stats.update(nextPick, toArray(-2.0));
		nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly, 0);
		assertTrue(nextPick == TestActionEnum.RIGHT);
		assertTrue(stats.getBestAction(leftRightOnly, 0) == TestActionEnum.RIGHT);
		stats.update(nextPick, toArray(0.0));
		nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly, 0);
		assertTrue(nextPick == TestActionEnum.RIGHT);
		assertTrue(stats.getBestAction(leftRightOnly, 0) == TestActionEnum.RIGHT);
		stats.update(nextPick, toArray(-1.0));
		nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly, 0);
		assertTrue(nextPick == TestActionEnum.LEFT);
		assertTrue(stats.getBestAction(leftRightOnly, 0) == TestActionEnum.RIGHT);
		stats.update(nextPick, toArray(1.0));
		nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly, 0);
		assertTrue(nextPick == TestActionEnum.RIGHT);
		assertTrue(stats.getBestAction(leftRightOnly, 0) == TestActionEnum.RIGHT);
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
		startingTree.insertState(dummyState, leftRightOnly);
		startingTree.updateState(dummyState, TestActionEnum.RIGHT, dummyState, toArray(2.0), 0);
		stats = new MCStatistics<TestAgent>(leftRightOnly, startingTree, 1, dummyState);
		stats.update(TestActionEnum.LEFT, toArray(2.0));
		stats.update(TestActionEnum.RIGHT, toArray(1.0));
		assertFalse(stats.hasUntriedAction(leftRightOnly));
		TestActionEnum nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly, 0);
		assertTrue(nextPick == TestActionEnum.RIGHT);
		assertTrue(stats.getBestAction(leftRightOnly, 0) == TestActionEnum.RIGHT);
		stats.update(nextPick, toArray(-2.0));
		nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly, 0);
		assertTrue(nextPick == TestActionEnum.RIGHT);
		assertTrue(stats.getBestAction(leftRightOnly, 0) == TestActionEnum.RIGHT);
		stats.update(nextPick, toArray(0.0));
		nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly, 0);
		assertTrue(nextPick == TestActionEnum.RIGHT);
		assertTrue(stats.getBestAction(leftRightOnly, 0) == TestActionEnum.RIGHT);
		stats.update(nextPick, toArray(-1.0));
		nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly, 0);
		assertTrue(nextPick == TestActionEnum.LEFT);
		assertTrue(stats.getBestAction(leftRightOnly, 0) == TestActionEnum.RIGHT);
		stats.update(nextPick, toArray(1.0));
		nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly, 0);
		assertTrue(nextPick == TestActionEnum.RIGHT);
		assertTrue(stats.getBestAction(leftRightOnly, 0) == TestActionEnum.RIGHT);
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
			stats.getRandomUntriedAction(leftRightOnly, 0);
			fail("Random action returned when there should not be any.");
		} catch (AssertionError e) {
			// as expected
		}
		assertTrue(stats.hasUntriedAction(allActions));
		assertEquals(stats.getPossibleActions().size(),3);
		assertTrue(stats.getRandomUntriedAction(allActions, 0) == TestActionEnum.TEST);
	}
	
	@Test
	public void updateWithPreviouslyUnknownActionShouldError() {
		stats = new MCStatistics<TestAgent>(leftRightOnly, startingTree, 1, dummyState);
		try {
			stats.update(TestActionEnum.LEFT, toArray(5.0));
			fail("Error should be thrown if unseen action used.");
		} catch (AssertionError e) {
			// as expected
		}
	}
	
	private double[] toArray(double single) {
		double[] retValue = new double[1];
		retValue[0] = single;
		return retValue;
	}
}

