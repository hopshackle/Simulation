package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;
import java.util.*;
import hopshackle.simulation.*;
import org.junit.*;

public class MCStatisticsTest {

	List<TestActionEnum> allActions = new ArrayList<TestActionEnum>(EnumSet.allOf(TestActionEnum.class));
	List<TestActionEnum> leftRightOnly = new ArrayList<TestActionEnum>(EnumSet.allOf(TestActionEnum.class));
	MCStatistics<TestAgent, TestActionEnum> stats;

	@Before 
	public void setup() {
		leftRightOnly.remove(TestActionEnum.TEST);
		SimProperties.setProperty("MonteCarloUCTC", "1");
	}

	@Test
	public void createEmpty() {
		stats = new MCStatistics<TestAgent, TestActionEnum>(leftRightOnly);
		assertEquals(stats.getVisits(TestActionEnum.LEFT), 0);
		assertEquals(stats.getVisits(TestActionEnum.RIGHT), 0);
		assertEquals(stats.getVisits(TestActionEnum.TEST), 0);
		assertEquals(stats.getMean(TestActionEnum.LEFT), 0.0, 0.001);
		assertEquals(stats.getMean(TestActionEnum.RIGHT), 0.0, 0.001);
		assertEquals(stats.getMean(TestActionEnum.TEST), 0.0, 0.001);
	}

	@Test
	public void updateWithNewVisit() {
		stats = new MCStatistics<TestAgent, TestActionEnum>(leftRightOnly);
		stats.update(TestActionEnum.LEFT, 2.0);
		stats.update(TestActionEnum.LEFT, 5.0);
		stats.update(TestActionEnum.LEFT, -1.0);
		stats.update(TestActionEnum.RIGHT, 1.0);

		assertEquals(stats.getVisits(TestActionEnum.LEFT), 3);
		assertEquals(stats.getVisits(TestActionEnum.RIGHT), 1);
		assertEquals(stats.getVisits(TestActionEnum.TEST), 0);
		assertEquals(stats.getMean(TestActionEnum.LEFT), 2.0, 0.001);
		assertEquals(stats.getMean(TestActionEnum.RIGHT), 1.0, 0.001);
		assertEquals(stats.getMean(TestActionEnum.TEST), 0.0, 0.001);
	}

	@Test
	public void cycleThroughActionsIfNotAllTried() {
		stats = new MCStatistics<TestAgent, TestActionEnum>(leftRightOnly);
		assertTrue(stats.hasUntriedAction());
		TestActionEnum newAction = stats.getRandomUntriedAction();
		stats.update(newAction, 1.0);
		assertTrue(stats.hasUntriedAction());
		TestActionEnum newAction2 = stats.getRandomUntriedAction();
		assertTrue(newAction != newAction2);
		stats.update(newAction2, 1.0);
		assertFalse(stats.hasUntriedAction());
	}

	@Test
	public void uctActionReturnsBestBound() {
		stats = new MCStatistics<TestAgent, TestActionEnum>(leftRightOnly);
		stats.update(TestActionEnum.LEFT, 2.0);
		stats.update(TestActionEnum.RIGHT, 1.0);
		assertFalse(stats.hasUntriedAction());
		TestActionEnum nextPick = stats.getUCTAction();
		assertTrue(nextPick == TestActionEnum.LEFT);
		stats.update(nextPick, 2.0);
		nextPick = stats.getUCTAction();
		assertTrue(nextPick == TestActionEnum.LEFT);
		stats.update(nextPick, 0.5);
		nextPick = stats.getUCTAction();
		assertTrue(nextPick == TestActionEnum.LEFT);
		stats.update(nextPick, 1.5);
		nextPick = stats.getUCTAction();
		assertTrue(nextPick == TestActionEnum.RIGHT);
		stats.update(nextPick, 1.0);
		nextPick = stats.getUCTAction();
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
}

