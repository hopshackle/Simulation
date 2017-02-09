package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;

import java.util.*;

import hopshackle.simulation.*;

import org.junit.*;

public class MCStatisticsTest {

	List<ActionEnum<TestAgent>> allActions = new ArrayList<ActionEnum<TestAgent>>(EnumSet.allOf(TestActionEnum.class));
	List<ActionEnum<TestAgent>> leftRightOnly = new ArrayList<ActionEnum<TestAgent>>(EnumSet.allOf(TestActionEnum.class));
	MCStatistics<TestAgent> stats;
	TestState A, B, C, END;

	@Before 
	public void setup() {
		A = new TestState("A");
		B = new TestState("B");
		C = new TestState("C");
		END = new TestState("END");
		leftRightOnly.remove(TestActionEnum.TEST);
		SimProperties.setProperty("MonteCarloUCTC", "1");
		SimProperties.setProperty("MonteCarloPriorActionWeightingForBestAction", "0");
		MCStatistics.refresh();
	}

	@Test
	public void createEmpty() {
		stats = new MCStatistics<TestAgent>(leftRightOnly);
		assertEquals(stats.getVisits(TestActionEnum.LEFT), 0);
		assertEquals(stats.getVisits(TestActionEnum.RIGHT), 0);
		assertEquals(stats.getVisits(TestActionEnum.TEST), 0);
		assertEquals(stats.getMean(TestActionEnum.LEFT), 0.0, 0.001);
		assertEquals(stats.getMean(TestActionEnum.RIGHT), 0.0, 0.001);
		assertEquals(stats.getMean(TestActionEnum.TEST), 0.0, 0.001);
	}

	@Test
	public void updateWithNewVisit() {
		stats = new MCStatistics<TestAgent>(leftRightOnly);
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
		
		assertTrue(stats.getBestAction(leftRightOnly) == TestActionEnum.LEFT);
		assertTrue(stats.getUCTAction(leftRightOnly) == TestActionEnum.LEFT);
	}

	@Test
	public void cycleThroughActionsIfNotAllTried() {
		stats = new MCStatistics<TestAgent>(leftRightOnly);
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
		stats = new MCStatistics<TestAgent>(leftRightOnly);
		stats.update(TestActionEnum.LEFT, 2.0);
		stats.update(TestActionEnum.RIGHT, 1.0);
		assertFalse(stats.hasUntriedAction(leftRightOnly));
		TestActionEnum nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly);
		assertTrue(nextPick == TestActionEnum.LEFT);
		stats.update(nextPick, 2.0);
		nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly);
		assertTrue(nextPick == TestActionEnum.LEFT);
		stats.update(nextPick, 0.5);
		nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly);
		assertTrue(nextPick == TestActionEnum.LEFT);
		stats.update(nextPick, 1.5);
		nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly);
		assertTrue(nextPick == TestActionEnum.RIGHT);
		stats.update(nextPick, 1.0);
		nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly);
		assertTrue(nextPick == TestActionEnum.LEFT);
		/* Expected results for C, N (total), 
		 * n (for this action) and Q
		C	N	n	Q	UCT
		1	2	1	1	1.833
		1	2	1	2	2.833
		1	3	1	1	2.048
		1	3	2	2	2.741
		1	4	1	1	2.177
		1	4	3	1.5	2.180
		1	5	1	1	2.269
		1	5	4	1.5	2.134
		1	6	2	1	1.947
		1	6	5	1.5	2.099
		 */
	}
	
	@Test
	public void actionAddedAfterInstantiation() {
		updateWithNewVisit();
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
		stats = new MCStatistics<TestAgent>(leftRightOnly);
		try {
			stats.update(TestActionEnum.LEFT, 5.0);
			fail("Error should be thrown if unseen action used.");
		} catch (AssertionError e) {
			// as expected
		}
	}
	
	@Test
	public void qAndvMinVisits() {
		SimProperties.setProperty("MonteCarloMinVisitsOnActionForQType", "1");
		SimProperties.setProperty("MonteCarloMinVisitsOnActionForVType", "2");
		MCStatistics.refresh();
		MonteCarloTree<TestAgent> tree = new MonteCarloTree<TestAgent>();
		tree.insertState(A, leftRightOnly);
		tree.insertState(B, leftRightOnly);
		tree.insertState(C, leftRightOnly);

		// all updates on MC basis
		tree.updateState(A, TestActionEnum.LEFT, C, 10);
		tree.updateState(C, TestActionEnum.RIGHT, END, 10);
		
		assertEquals(tree.getStatisticsFor(A).getV(), 0.0, 0.01);
		assertEquals(tree.getStatisticsFor(A).getQ(), 0.0, 0.01);
		assertEquals(tree.getStatisticsFor(B).getV(), 0.0, 0.01);
		assertEquals(tree.getStatisticsFor(B).getQ(), 0.0, 0.01);
		assertEquals(tree.getStatisticsFor(C).getV(), 0.0, 0.01);
		assertEquals(tree.getStatisticsFor(C).getQ(), 0.0, 0.01);
		
		// all updates still on MC basis
		tree.updateState(A, TestActionEnum.RIGHT, B, 3);
		tree.updateState(B, TestActionEnum.RIGHT, END, 3);
		
		// V on A now at min Visits, and so is Q (1 for each action)
		assertEquals(tree.getStatisticsFor(A).getV(), 6.5, 0.01);
		assertEquals(tree.getStatisticsFor(A).getQ(), 10.0, 0.01);
		assertEquals(tree.getStatisticsFor(B).getV(), 0.0, 0.01);
		assertEquals(tree.getStatisticsFor(B).getQ(), 0.0, 0.01);
		assertEquals(tree.getStatisticsFor(C).getV(), 0.0, 0.01);
		assertEquals(tree.getStatisticsFor(C).getQ(), 0.0, 0.01);
		
		// all updates on MC basis (as A is only state that meets criteria, and nothing transitions to it)
		tree.updateState(A, TestActionEnum.LEFT, B, 4);
		tree.updateState(B, TestActionEnum.RIGHT, END, 4);
			
		// A retains full Q, V calculations (but all updates used MC)
		// B is now clear for V, but not yet for Q
		assertEquals(tree.getStatisticsFor(A).getV(), 5.667, 0.01);
		assertEquals(tree.getStatisticsFor(A).getQ(), 7.0, 0.01);
		assertEquals(tree.getStatisticsFor(B).getV(), 3.5, 0.01);
		assertEquals(tree.getStatisticsFor(B).getQ(), 0.0, 0.01);
		assertEquals(tree.getStatisticsFor(C).getV(), 0.0, 0.01);
		assertEquals(tree.getStatisticsFor(C).getQ(), 0.0, 0.01);
		
		// update for A -> B is now MC for Q, but V for V
		tree.updateState(A, TestActionEnum.LEFT, B, 6);
		tree.updateState(B, TestActionEnum.LEFT, END, 6);
		tree.insertState(END, leftRightOnly);
			
		assertEquals(tree.getStatisticsFor(A).getV(), 5.125, 0.01);
		assertEquals(tree.getStatisticsFor(A).getQ(), 6.667, 0.01);
		assertEquals(tree.getStatisticsFor(B).getV(), 4.333, 0.01);
		assertEquals(tree.getStatisticsFor(B).getQ(), 6.0, 0.01);
		assertEquals(tree.getStatisticsFor(C).getV(), 0.0, 0.01);
		assertEquals(tree.getStatisticsFor(C).getQ(), 0.0, 0.01);
		assertEquals(tree.getStatisticsFor(END).getV(), 0, 0.01);
		assertEquals(tree.getStatisticsFor(END).getQ(), 0, 0.01);
	}
	

	@Test
	public void qAndvUpdated() {
		SimProperties.setProperty("MonteCarloMinVisitsOnActionForQType", "0");
		SimProperties.setProperty("MonteCarloMinVisitsOnActionForVType", "0");
		MCStatistics.refresh();
		MonteCarloTree<TestAgent> tree = new MonteCarloTree<TestAgent>();
		tree.insertState(A, leftRightOnly);
		tree.insertState(B, leftRightOnly);
		tree.insertState(C, leftRightOnly);

		tree.updateState(A, TestActionEnum.LEFT, C, 10);
		tree.updateState(C, TestActionEnum.RIGHT, END, 10);
		
		assertEquals(tree.getStatisticsFor(A).getV(), 10.0, 0.01);
		assertEquals(tree.getStatisticsFor(A).getQ(), 10.0, 0.01);
		assertEquals(tree.getStatisticsFor(B).getV(), 0.0, 0.01);
		assertEquals(tree.getStatisticsFor(B).getQ(), 0.0, 0.01);
		assertEquals(tree.getStatisticsFor(C).getV(), 10, 0.01);
		assertEquals(tree.getStatisticsFor(C).getQ(), 10, 0.01);
		
		tree.updateState(A, TestActionEnum.RIGHT, B, 3);
		tree.updateState(B, TestActionEnum.RIGHT, END, 3);
		
		assertEquals(tree.getStatisticsFor(A).getV(), 6.5, 0.01);
		assertEquals(tree.getStatisticsFor(A).getQ(), 10.0, 0.01);
		assertEquals(tree.getStatisticsFor(B).getV(), 3.0, 0.01);
		assertEquals(tree.getStatisticsFor(B).getQ(), 3.0, 0.01);
		assertEquals(tree.getStatisticsFor(C).getV(), 10, 0.01);
		assertEquals(tree.getStatisticsFor(C).getQ(), 10, 0.01);
		
		tree.updateState(A, TestActionEnum.LEFT, B, 4);
		tree.updateState(B, TestActionEnum.LEFT, END, 4);
			
		assertEquals(tree.getStatisticsFor(A).getV(), 5.333, 0.01);
		assertEquals(tree.getStatisticsFor(A).getQ(), 6.5, 0.01);
		assertEquals(tree.getStatisticsFor(B).getV(), 3.5, 0.01);
		assertEquals(tree.getStatisticsFor(B).getQ(), 4.0, 0.01);
		assertEquals(tree.getStatisticsFor(C).getV(), 10, 0.01);
		assertEquals(tree.getStatisticsFor(C).getQ(), 10, 0.01);
		
		tree.updateState(A, TestActionEnum.LEFT, B, 6);
		tree.updateState(B, TestActionEnum.LEFT, END, 6);
		tree.insertState(END, leftRightOnly);
			
		assertEquals(tree.getStatisticsFor(A).getV(), 4.875, 0.01);
		assertEquals(tree.getStatisticsFor(A).getQ(), 5.667, 0.01);
		assertEquals(tree.getStatisticsFor(B).getV(), 4.333, 0.01);
		assertEquals(tree.getStatisticsFor(B).getQ(), 5.0, 0.01);
		assertEquals(tree.getStatisticsFor(C).getV(), 10, 0.01);
		assertEquals(tree.getStatisticsFor(C).getQ(), 10, 0.01);
		assertEquals(tree.getStatisticsFor(END).getV(), 0, 0.01);
		assertEquals(tree.getStatisticsFor(END).getQ(), 0, 0.01);
	}
}

