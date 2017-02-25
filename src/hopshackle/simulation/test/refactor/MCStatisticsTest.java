package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;

import java.util.*;

import hopshackle.simulation.*;

import org.junit.*;

public class MCStatisticsTest {

	List<ActionEnum<TestAgent>> allActions = new ArrayList<ActionEnum<TestAgent>>(EnumSet.allOf(TestActionEnum.class));
	List<ActionEnum<TestAgent>> leftRightOnly = new ArrayList<ActionEnum<TestAgent>>(EnumSet.allOf(TestActionEnum.class));
	MCStatistics<TestAgent> stats;
	DeciderProperties localProp;
	TestState A, B, C, END;

	@Before 
	public void setup() {
		A = new TestState("A");
		B = new TestState("B");
		C = new TestState("C");
		END = new TestState("END");
		leftRightOnly.remove(TestActionEnum.TEST);
		localProp = SimProperties.getDeciderProperties("GLOBAL").clone();
		localProp.setProperty("MonteCarloUCTC", "1");
		localProp.setProperty("MonteCarloPriorActionWeightingForBestAction", "0");
		localProp.setProperty("MonteCarloRL", "false");
		
	}

	@Test
	public void createEmpty() {
		stats = new MCStatistics<TestAgent>(leftRightOnly, localProp);
		assertEquals(stats.getVisits(TestActionEnum.LEFT), 0);
		assertEquals(stats.getVisits(TestActionEnum.RIGHT), 0);
		assertEquals(stats.getVisits(TestActionEnum.TEST), 0);
		assertEquals(stats.getMean(TestActionEnum.LEFT), 0.0, 0.001);
		assertEquals(stats.getMean(TestActionEnum.RIGHT), 0.0, 0.001);
		assertEquals(stats.getMean(TestActionEnum.TEST), 0.0, 0.001);
	}

	@Test
	public void updateWithNewVisit() {
		stats = new MCStatistics<TestAgent>(leftRightOnly, localProp);
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
		stats = new MCStatistics<TestAgent>(leftRightOnly,localProp);
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
		stats = new MCStatistics<TestAgent>(leftRightOnly, localProp);
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
		stats = new MCStatistics<TestAgent>(leftRightOnly, localProp);
		try {
			stats.update(TestActionEnum.LEFT, 5.0);
			fail("Error should be thrown if unseen action used.");
		} catch (AssertionError e) {
			// as expected
		}
	}
	
	@Test
	public void qAndvMinVisits() {
		localProp.setProperty("MonteCarloMinVisitsOnActionForQType", "1");
		localProp.setProperty("MonteCarloMinVisitsOnActionForVType", "2");
		MonteCarloTree<TestAgent> tree = new MonteCarloTree<TestAgent>(localProp);
		tree.insertState(A, leftRightOnly);

		// all updates on MC basis
		tree.updateState(A, TestActionEnum.LEFT, C, 10);

		tree.insertState(C, leftRightOnly);
		tree.updateState(C, TestActionEnum.RIGHT, END, 10);
		
		assertEquals(tree.getStatisticsFor(A).getV(), Double.NaN, 0.01);
		assertEquals(tree.getStatisticsFor(A).getQ(), Double.NaN, 0.01);
		assertEquals(tree.getStatisticsFor(C).getV(), Double.NaN, 0.01);
		assertEquals(tree.getStatisticsFor(C).getQ(), Double.NaN, 0.01);
		
		// all updates still on MC basis
		tree.updateState(A, TestActionEnum.RIGHT, B, 3);

		tree.insertState(B, leftRightOnly);
		tree.updateState(B, TestActionEnum.RIGHT, END, 3);
		
		// V on A now at min Visits, and so is Q (1 for each action)
		assertEquals(tree.getStatisticsFor(A).getV(), 6.5, 0.01); // 10, 3
		assertEquals(tree.getStatisticsFor(A).getQ(), 10.0, 0.01); // 10 | 3
		assertEquals(tree.getStatisticsFor(B).getV(), Double.NaN, 0.01);
		assertEquals(tree.getStatisticsFor(B).getQ(), Double.NaN, 0.01); 
		assertEquals(tree.getStatisticsFor(C).getV(), Double.NaN, 0.01);
		assertEquals(tree.getStatisticsFor(C).getQ(), Double.NaN, 0.01);
		
		// all updates on MC basis (as A is only state that meets criteria, and nothing transitions to it)
		tree.updateState(A, TestActionEnum.LEFT, B, 4);
		tree.updateState(B, TestActionEnum.RIGHT, END, 4);
			
		// A retains full Q, V calculations (but all updates used MC)
		// B is now clear for V, but not yet for Q
		assertEquals(tree.getStatisticsFor(A).getV(), 17.0/3.0, 0.01); // 10, 3, 4
		assertEquals(tree.getStatisticsFor(A).getQ(), 7.0, 0.01); // 10, 4 | 3
		assertEquals(tree.getStatisticsFor(B).getV(), 3.5, 0.01); 
		assertEquals(tree.getStatisticsFor(B).getQ(), Double.NaN, 0.01); 
		assertEquals(tree.getStatisticsFor(C).getV(), Double.NaN, 0.01);
		assertEquals(tree.getStatisticsFor(C).getQ(), Double.NaN, 0.01);
		
		// update for A -> B is now MC for Q, but V for V
		tree.updateState(A, TestActionEnum.LEFT, B, 6);
		tree.updateState(B, TestActionEnum.LEFT, END, 6);
		tree.insertState(END, leftRightOnly);
			
		assertEquals(tree.getStatisticsFor(A).getV(), (20.5/4.0), 0.01); // 10, 3, 4, 3.5
		assertEquals(tree.getStatisticsFor(A).getQ(), (20.0/3.0), 0.01); // 10, 4, 6 | 3 
		assertEquals(tree.getStatisticsFor(B).getV(), 4.333, 0.01);
		assertEquals(tree.getStatisticsFor(B).getQ(), 6.0, 0.01);
		assertEquals(tree.getStatisticsFor(C).getV(), Double.NaN, 0.01);
		assertEquals(tree.getStatisticsFor(C).getQ(), Double.NaN, 0.01);
		assertEquals(tree.getStatisticsFor(END).getV(), Double.NaN, 0.01);
		assertEquals(tree.getStatisticsFor(END).getQ(), Double.NaN, 0.01);
	}
	
	@Test
	public void qAndvUpdated() {
		localProp.setProperty("MonteCarloMinVisitsOnActionForQType", "0");
		localProp.setProperty("MonteCarloMinVisitsOnActionForVType", "0");
		MonteCarloTree<TestAgent> tree = new MonteCarloTree<TestAgent>(localProp);
		tree.insertState(A, leftRightOnly);
		tree.insertState(B, leftRightOnly);
		tree.insertState(C, leftRightOnly);

		tree.updateState(A, TestActionEnum.LEFT, C, 10);
		tree.updateState(C, TestActionEnum.RIGHT, END, 10);
		
		assertEquals(tree.getStatisticsFor(A).getV(), 10.0, 0.01);
		assertEquals(tree.getStatisticsFor(A).getQ(), 10.0, 0.01);
		assertEquals(tree.getStatisticsFor(B).getV(), Double.NaN, 0.01);
		assertEquals(tree.getStatisticsFor(B).getQ(), Double.NaN, 0.01);
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
			
		assertEquals(tree.getStatisticsFor(A).getV(), 4.875, 0.01);
		assertEquals(tree.getStatisticsFor(A).getQ(), 5.667, 0.01);
		assertEquals(tree.getStatisticsFor(B).getV(), 4.333, 0.01);
		assertEquals(tree.getStatisticsFor(B).getQ(), 5.0, 0.01);
		assertEquals(tree.getStatisticsFor(C).getV(), 10, 0.01);
		assertEquals(tree.getStatisticsFor(C).getQ(), 10, 0.01);
	}
	
	@Test
	public void qAndvWithBaseValue() {
		localProp.setProperty("MonteCarloRL", "true");
		localProp.setProperty("MonteCarloRLBaseValue", "50.0");
		localProp.setProperty("Alpha", "0.05");
		MonteCarloTree<TestAgent> tree = new MonteCarloTree<TestAgent>(localProp);
		tree.insertState(A, leftRightOnly);
		tree.insertState(B, leftRightOnly);
		tree.insertState(C, leftRightOnly);

		tree.updateState(A, TestActionEnum.LEFT, C, 10);
		tree.updateState(C, TestActionEnum.RIGHT, END, 10);
		
		assertEquals(tree.getStatisticsFor(A).getV(), 50.0, 0.01);
		assertEquals(tree.getStatisticsFor(A).getQ(), 50.0, 0.01);
		assertEquals(tree.getStatisticsFor(B).getV(), 50.0, 0.01);
		assertEquals(tree.getStatisticsFor(B).getQ(), 50.0, 0.01);
		assertEquals(tree.getStatisticsFor(C).getV(), 48.0, 0.01);
		assertEquals(tree.getStatisticsFor(C).getQ(), 50.0, 0.01);	// any action not yet taken will use the default value
		
		tree.updateState(A, TestActionEnum.RIGHT, B, 3);
		tree.updateState(B, TestActionEnum.RIGHT, END, 3);
		
		assertEquals(tree.getStatisticsFor(A).getV(), 50.0, 0.01);
		assertEquals(tree.getStatisticsFor(A).getQ(), 50.0, 0.01);
		assertEquals(tree.getStatisticsFor(B).getV(), 47.65, 0.01); // 0 x 50, 1 x 47.65
		assertEquals(tree.getStatisticsFor(B).getQ(), 50.0, 0.01);
		assertEquals(tree.getStatisticsFor(C).getV(), 48.0, 0.01);
		assertEquals(tree.getStatisticsFor(C).getQ(), 50.0, 0.01);
		
		tree.updateState(A, TestActionEnum.LEFT, B, 4);
		tree.updateState(B, TestActionEnum.LEFT, END, 4);
			
		assertEquals(tree.getStatisticsFor(A).getV(), 49.92167, 0.01); // 1 x 50 and 2 x 49.8825
		assertEquals(tree.getStatisticsFor(A).getQ(), 50.0, 0.01);
		assertEquals(tree.getStatisticsFor(B).getV(), 47.675, 0.01); 	// 1 x 47.7, 1 x 47.65
		assertEquals(tree.getStatisticsFor(B).getQ(), 47.7, 0.01);		// max of above
		assertEquals(tree.getStatisticsFor(C).getV(), 48.0, 0.01);
		assertEquals(tree.getStatisticsFor(C).getQ(), 50.0, 0.01);
		
		tree.updateState(A, TestActionEnum.LEFT, B, 6);
		tree.updateState(B, TestActionEnum.LEFT, END, 6);
			
		assertEquals(tree.getStatisticsFor(A).getV(), 49.829, 0.01);	// 1 x 50, 3 x 49.772
		assertEquals(tree.getStatisticsFor(A).getQ(), 50.0, 0.01);		// max
		assertEquals(tree.getStatisticsFor(B).getV(), 46.2933, 0.01);		// 2 x 45.615, 1 x 47.65
		assertEquals(tree.getStatisticsFor(B).getQ(), 47.65, 0.01);			// max
		assertEquals(tree.getStatisticsFor(C).getV(), 48.0, 0.01);
		assertEquals(tree.getStatisticsFor(C).getQ(), 50.0, 0.01);
	}
	
	@Test
	public void updateTowardsAStateWithZeroValue() {
		localProp.setProperty("MonteCarloRL", "true");
		localProp.setProperty("MonteCarloRLBaseValue", "0.0");
		localProp.setProperty("Alpha", "0.1");
		MonteCarloTree<TestAgent> tree = new MonteCarloTree<TestAgent>(localProp);
		tree.insertState(A, leftRightOnly);
		tree.insertState(B, leftRightOnly);
		
		tree.updateState(A, TestActionEnum.LEFT, B, 50.0);
		assertEquals(tree.getStatisticsFor(A).getV(), 0.0, 0.01);
		assertEquals(tree.getStatisticsFor(A).getQ(), 0.0, 0.01);

		tree.updateState(B, TestActionEnum.LEFT, C, 50.0);
		tree.updateState(A, TestActionEnum.LEFT, B, 50.0);
		assertEquals(tree.getStatisticsFor(A).getV(), 0.5, 0.01);
		assertEquals(tree.getStatisticsFor(A).getQ(), 0.5, 0.01);
	}
	
}

