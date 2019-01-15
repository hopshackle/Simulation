package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;

import java.util.*;

import hopshackle.simulation.MCTS.MCStatistics;
import hopshackle.simulation.MCTS.MonteCarloTree;
import hopshackle.simulation.*;

import hopshackle.simulation.MCTS.TranspositionTableMCTree;
import org.junit.*;

public class MCStatisticsTest {

    List<ActionEnum<TestAgent>> allActions = new ArrayList<>(EnumSet.allOf(TestActionEnum.class));
    List<ActionEnum<TestAgent>> leftRightOnly = new ArrayList<>(EnumSet.allOf(TestActionEnum.class));
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
        localProp.setProperty("MonteCarloHeuristicWeighting", "0.0");
        localProp.setProperty("MonteCarloRL", "false");
        localProp.setProperty("MonteCarloHeuristicOnExpansion", "false");
        localProp.setProperty("MonteCarloParentalVisitValidity", "false");
    }

    @Test
    public void createEmpty() {
        stats = new MCStatistics<>(localProp, 1, A);
        assertEquals(stats.getVisits(TestActionEnum.LEFT), 0);
        assertEquals(stats.getVisits(TestActionEnum.RIGHT), 0);
        assertEquals(stats.getVisits(TestActionEnum.TEST), 0);
        assertEquals(stats.getMean(TestActionEnum.LEFT, 1)[0], 0.0, 0.001);
        assertEquals(stats.getMean(TestActionEnum.RIGHT, 1)[0], 0.0, 0.001);
        assertEquals(stats.getMean(TestActionEnum.TEST, 1)[0], 0.0, 0.001);
    }

    @Test
    public void updateWithNewVisit() {
        stats = new MCStatistics<>(localProp, 1, A);
        stats.update(TestActionEnum.LEFT, toArray(2.0), 1);
        stats.update(TestActionEnum.LEFT, toArray(3.5), 1);
        stats.update(TestActionEnum.LEFT, toArray(-1.0), 1);
        stats.update(TestActionEnum.RIGHT, toArray(1.0), 1);

        assertEquals(stats.getVisits(TestActionEnum.LEFT), 3);
        assertEquals(stats.getVisits(TestActionEnum.RIGHT), 1);
        assertEquals(stats.getVisits(TestActionEnum.TEST), 0);
        assertEquals(stats.getMean(TestActionEnum.LEFT, 1)[0], 1.5, 0.001);
        assertEquals(stats.getMean(TestActionEnum.RIGHT, 1)[0], 1.0, 0.001);
        assertEquals(stats.getMean(TestActionEnum.TEST, 1)[0], 0.0, 0.001);

        assertTrue(stats.getBestAction(leftRightOnly, 1) == TestActionEnum.LEFT);
        assertTrue(stats.getUCTAction(leftRightOnly, 1) == TestActionEnum.LEFT);
    }

    @Test
    public void cycleThroughActionsIfNotAllTried() {
        stats = new MCStatistics<>(localProp, 1, A);
        assertTrue(stats.hasUntriedAction(leftRightOnly, 1));
        TestActionEnum newAction = (TestActionEnum) stats.getRandomUntriedAction(leftRightOnly, 1);
        stats.update(newAction, toArray(1.0), 1);
        assertTrue(stats.hasUntriedAction(leftRightOnly, 1));
        TestActionEnum newAction2 = (TestActionEnum) stats.getRandomUntriedAction(leftRightOnly, 1);
        assertTrue(newAction != newAction2);
        stats.update(newAction2, toArray(1.0), 1);
        assertFalse(stats.hasUntriedAction(leftRightOnly, 1));
    }

    @Test
    public void uctActionReturnsBestBound() {
        stats = new MCStatistics<>(localProp, 1, A);
        stats.update(TestActionEnum.LEFT, toArray(2.0), 1);
        stats.update(TestActionEnum.RIGHT, toArray(1.0), 1);
        assertFalse(stats.hasUntriedAction(leftRightOnly, 1));
        TestActionEnum nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly, 1);
        assertTrue(nextPick == TestActionEnum.LEFT);
        stats.update(nextPick, toArray(2.0), 1);
        nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly, 1);
        assertTrue(nextPick == TestActionEnum.LEFT);
        stats.update(nextPick, toArray(0.5), 1);
        nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly, 1);
        assertTrue(nextPick == TestActionEnum.LEFT);
        stats.update(nextPick, toArray(1.5), 1);
        nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly, 1);
        assertTrue(nextPick == TestActionEnum.RIGHT);
        stats.update(nextPick, toArray(1.0), 1);
        nextPick = (TestActionEnum) stats.getUCTAction(leftRightOnly, 1);
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
        assertFalse(stats.hasUntriedAction(leftRightOnly, 1));
        assertEquals(stats.getPossibleActions().size(), 2);
        try {
            stats.getRandomUntriedAction(leftRightOnly, 1);
            fail("Random action returned when there should not be any.");
        } catch (AssertionError e) {
            // as expected
        }
        assertTrue(stats.hasUntriedAction(allActions, 1));
        assertEquals(stats.getPossibleActions().size(), 3);
        assertTrue(stats.getRandomUntriedAction(allActions, 1) == TestActionEnum.TEST);
    }

    @Test
    public void expansionInHeuristicOrder() {
        localProp.setProperty("MonteCarloHeuristicOnExpansion", "true");
        createEmpty();
        Map<ActionEnum<TestAgent>, Double> map = new HashMap<>();
        map.put(TestActionEnum.LEFT, 1.0);
        map.put(TestActionEnum.RIGHT, -1.0);
        SimpleHeuristic<TestAgent> simpleHeuristic = new SimpleHeuristic<>(map);
        assertTrue(stats.getRandomUntriedAction(allActions, simpleHeuristic, 1) == TestActionEnum.LEFT);
        stats.update(TestActionEnum.LEFT, new double[1], 1);
        assertTrue(stats.getRandomUntriedAction(allActions, simpleHeuristic, 1) == TestActionEnum.TEST);
        stats.update(TestActionEnum.TEST, new double[1], 1);
        assertTrue(stats.getRandomUntriedAction(allActions, simpleHeuristic, 1) == TestActionEnum.RIGHT);
    }

    @Test
    public void updateWithPreviouslyUnknownActionShouldError() {
        stats = new MCStatistics<>(localProp, 1, A);
        try {
            stats.update(TestActionEnum.LEFT, toArray(5.0), 1);
            fail("Error should be thrown if unseen action used.");
        } catch (AssertionError e) {
            // as expected
        }
    }

    @Test
    public void qAndvMinVisits() {
        localProp.setProperty("MonteCarloMinVisitsOnActionForQType", "1");
        localProp.setProperty("MonteCarloMinVisitsOnActionForVType", "2");
        TranspositionTableMCTree<TestAgent> tree = new TranspositionTableMCTree<TestAgent>(localProp, 1);
        tree.insertState(A);

        tree.updateState(A, TestActionEnum.LEFT, C, 10);
        assertEquals(tree.getStatisticsFor(A).getV(1).length, 0); // less than 2 visits
        assertEquals(tree.getStatisticsFor(A).getQ(1).length, 1); // 1 visit, so hits requirement

        tree.insertState(C);
        tree.updateState(C, TestActionEnum.RIGHT, END, 10);

        assertEquals(tree.getStatisticsFor(A).getV(1).length, 0);
        assertEquals(tree.getStatisticsFor(A).getQ(1).length, 1);
        assertEquals(tree.getStatisticsFor(C).getV(1).length, 0);
        assertEquals(tree.getStatisticsFor(C).getQ(1).length, 1);

        // all updates still on MC basis
        tree.updateState(A, TestActionEnum.RIGHT, B, 3);

        tree.insertState(B);
        tree.updateState(B, TestActionEnum.RIGHT, END, 3);

        // V on A now at min Visits, and so is Q (1 for each action)
        assertEquals(tree.getStatisticsFor(A).getV(1)[0], 6.5, 0.01); // 10, 3
        assertEquals(tree.getStatisticsFor(A).getQ(1)[0], 10.0, 0.01); // 10 | 3
        assertEquals(tree.getStatisticsFor(B).getV(1).length, 0);
        assertEquals(tree.getStatisticsFor(B).getQ(1).length, 1);
        assertEquals(tree.getStatisticsFor(C).getV(1).length, 0);
        assertEquals(tree.getStatisticsFor(C).getQ(1).length, 1);

        // all updates on MC basis (as A is only state that meets criteria, and nothing transitions to it)
        tree.updateState(A, TestActionEnum.LEFT, B, 4);
        tree.updateState(B, TestActionEnum.RIGHT, END, 4);

        // A retains full Q, V calculations (but all updates used MC)
        // B is now clear for V, but not yet for Q
        assertEquals(tree.getStatisticsFor(A).getV(1)[0], 17.0 / 3.0, 0.01); // 10, 3, 4
        assertEquals(tree.getStatisticsFor(A).getQ(1)[0], 6.5, 0.01); // 10, 3 | 3
        // as the Q value from B is 3.0
        assertEquals(tree.getStatisticsFor(B).getV(1)[0], 3.5, 0.01);
        assertEquals(tree.getStatisticsFor(B).getQ(1).length, 1);
        assertEquals(tree.getStatisticsFor(C).getV(1).length, 0);
        assertEquals(tree.getStatisticsFor(C).getQ(1).length, 1);

        // update for A -> B is now MC for Q, but V for V
        tree.updateState(A, TestActionEnum.LEFT, B, 6);
        tree.updateState(B, TestActionEnum.LEFT, END, 6);
        tree.insertState(END);

        assertEquals(tree.getStatisticsFor(A).getV(1)[0], (20.5 / 4.0), 0.01); // 10, 3, 4, 3.5
        assertEquals(tree.getStatisticsFor(A).getQ(1)[0], (16.5 / 3.0), 0.01); // 10, 3, 3.5 | 3
        assertEquals(tree.getStatisticsFor(B).getV(1)[0], 4.333, 0.01);
        assertEquals(tree.getStatisticsFor(B).getQ(1)[0], 6.0, 0.01);
        assertEquals(tree.getStatisticsFor(C).getV(1).length, 0);
        assertEquals(tree.getStatisticsFor(C).getQ(1).length, 1);
        assertEquals(tree.getStatisticsFor(END).getV(1).length, 0);
        assertEquals(tree.getStatisticsFor(END).getQ(1).length, 0);
    }

    @Test
    public void qAndvUpdated() {
        localProp.setProperty("MonteCarloMinVisitsOnActionForQType", "0");
        localProp.setProperty("MonteCarloMinVisitsOnActionForVType", "0");
        TranspositionTableMCTree<TestAgent> tree = new TranspositionTableMCTree<TestAgent>(localProp, 1);
        tree.insertState(A);
        tree.insertState(B);
        tree.insertState(C);

        tree.updateState(A, TestActionEnum.LEFT, C, (10));
        tree.updateState(C, TestActionEnum.RIGHT, END, (10));

        assertEquals(tree.getStatisticsFor(A).getV(1)[0], 10.0, 0.01);
        assertEquals(tree.getStatisticsFor(A).getQ(1)[0], 10.0, 0.01);
        assertEquals(tree.getStatisticsFor(B).getV(1).length, 0);
        assertEquals(tree.getStatisticsFor(B).getQ(1).length, 0);
        assertEquals(tree.getStatisticsFor(C).getV(1)[0], 10, 0.01);
        assertEquals(tree.getStatisticsFor(C).getQ(1)[0], 10, 0.01);

        tree.updateState(A, TestActionEnum.RIGHT, B, (3));
        tree.updateState(B, TestActionEnum.RIGHT, END, (3));

        assertEquals(tree.getStatisticsFor(A).getV(1)[0], 6.5, 0.01);
        assertEquals(tree.getStatisticsFor(A).getQ(1)[0], 10.0, 0.01);
        assertEquals(tree.getStatisticsFor(B).getV(1)[0], 3.0, 0.01);
        assertEquals(tree.getStatisticsFor(B).getQ(1)[0], 3.0, 0.01);
        assertEquals(tree.getStatisticsFor(C).getV(1)[0], 10, 0.01);
        assertEquals(tree.getStatisticsFor(C).getQ(1)[0], 10, 0.01);

        tree.updateState(A, TestActionEnum.LEFT, B, (4));
        tree.updateState(B, TestActionEnum.LEFT, END, (4));

        assertEquals(tree.getStatisticsFor(A).getV(1)[0], 5.333, 0.01);
        assertEquals(tree.getStatisticsFor(A).getQ(1)[0], 6.5, 0.01);
        assertEquals(tree.getStatisticsFor(B).getV(1)[0], 3.5, 0.01);
        assertEquals(tree.getStatisticsFor(B).getQ(1)[0], 4.0, 0.01);
        assertEquals(tree.getStatisticsFor(C).getV(1)[0], 10, 0.01);
        assertEquals(tree.getStatisticsFor(C).getQ(1)[0], 10, 0.01);

        tree.updateState(A, TestActionEnum.LEFT, B, (6));
        tree.updateState(B, TestActionEnum.LEFT, END, (6));

        assertEquals(tree.getStatisticsFor(A).getV(1)[0], 4.875, 0.01);
        assertEquals(tree.getStatisticsFor(A).getQ(1)[0], 5.667, 0.01);
        assertEquals(tree.getStatisticsFor(B).getV(1)[0], 4.333, 0.01);
        assertEquals(tree.getStatisticsFor(B).getQ(1)[0], 5.0, 0.01);
        assertEquals(tree.getStatisticsFor(C).getV(1)[0], 10, 0.01);
        assertEquals(tree.getStatisticsFor(C).getQ(1)[0], 10, 0.01);
    }

    @Test
    public void qAndvWithBaseValue() {
        localProp.setProperty("MonteCarloRL", "true");
        localProp.setProperty("MonteCarloRLBaseValue", "50.0");
        localProp.setProperty("Alpha", "0.05");
        TranspositionTableMCTree<TestAgent> tree = new TranspositionTableMCTree<TestAgent>(localProp, 1);
        tree.insertState(A);
        tree.insertState(B);
        tree.insertState(C);

        assertEquals(tree.getStatisticsFor(A).getV(1)[0], 50.0, 0.01);
        assertEquals(tree.getStatisticsFor(A).getQ(1)[0], 50.0, 0.01);

        tree.updateState(A, TestActionEnum.LEFT, C, (10));
        tree.updateState(C, TestActionEnum.RIGHT, END, (10));

        assertEquals(tree.getStatisticsFor(A).getV(1)[0], 50.0, 0.01);
        assertEquals(tree.getStatisticsFor(A).getQ(1)[0], 50.0, 0.01);
        assertEquals(tree.getStatisticsFor(B).getV(1)[0], 50.0, 0.01);
        assertEquals(tree.getStatisticsFor(B).getQ(1)[0], 50.0, 0.01);
        assertEquals(tree.getStatisticsFor(C).getV(1)[0], 48.0, 0.01);
        assertEquals(tree.getStatisticsFor(C).getQ(1)[0], 48.0, 0.01);

        tree.updateState(A, TestActionEnum.RIGHT, B, (3));
        tree.updateState(B, TestActionEnum.RIGHT, END, (3));

        assertEquals(tree.getStatisticsFor(A).getV(1)[0], 50.0, 0.01);
        assertEquals(tree.getStatisticsFor(A).getQ(1)[0], 50.0, 0.01);
        assertEquals(tree.getStatisticsFor(B).getV(1)[0], 47.65, 0.01); // 0 x 50, 1 x 47.65
        assertEquals(tree.getStatisticsFor(B).getQ(1)[0], 47.65, 0.01); // only RIGHT from B has been tested so far
        assertEquals(tree.getStatisticsFor(C).getV(1)[0], 48.0, 0.01);
        assertEquals(tree.getStatisticsFor(C).getQ(1)[0], 48.0, 0.01);

        tree.updateState(A, TestActionEnum.LEFT, B, (4));
        tree.updateState(B, TestActionEnum.LEFT, END, (4));

        assertEquals(tree.getStatisticsFor(A).getV(1)[0], 49.92167, 0.01); // 1 x 50 and 2 x 49.8825
        assertEquals(tree.getStatisticsFor(A).getQ(1)[0], 50.0, 0.01);
        assertEquals(tree.getStatisticsFor(B).getV(1)[0], 47.675, 0.01);    // 1 x 47.7, 1 x 47.65
        assertEquals(tree.getStatisticsFor(B).getQ(1)[0], 47.7, 0.01);        // max of above
        assertEquals(tree.getStatisticsFor(C).getV(1)[0], 48.0, 0.01);
        assertEquals(tree.getStatisticsFor(C).getQ(1)[0], 48.0, 0.01);

        tree.updateState(A, TestActionEnum.LEFT, B, (6));
        tree.updateState(B, TestActionEnum.LEFT, END, (6));

        assertEquals(tree.getStatisticsFor(A).getV(1)[0], 49.829, 0.01);    // 1 x 50, 3 x 49.772
        assertEquals(tree.getStatisticsFor(A).getQ(1)[0], 50.0, 0.01);        // max
        assertEquals(tree.getStatisticsFor(B).getV(1)[0], 46.2933, 0.01);        // 2 x 45.615, 1 x 47.65
        assertEquals(tree.getStatisticsFor(B).getQ(1)[0], 47.65, 0.01);            // max
        assertEquals(tree.getStatisticsFor(C).getV(1)[0], 48.0, 0.01);
        assertEquals(tree.getStatisticsFor(C).getQ(1)[0], 48.0, 0.01);
    }

    @Test
    public void updateTowardsAStateWithZeroValue() {
        localProp.setProperty("MonteCarloRL", "true");
        localProp.setProperty("MonteCarloRLBaseValue", "0.0");
        localProp.setProperty("Alpha", "0.1");
        TranspositionTableMCTree<TestAgent> tree = new TranspositionTableMCTree<TestAgent>(localProp, 1);
        tree.insertState(A);
        tree.insertState(B);

        tree.updateState(A, TestActionEnum.LEFT, B, (50.0));
        assertEquals(tree.getStatisticsFor(A).getV(1)[0], 0.0, 0.01);
        assertEquals(tree.getStatisticsFor(A).getQ(1)[0], 0.0, 0.01);

        tree.updateState(B, TestActionEnum.LEFT, C, (50.0));
        tree.updateState(A, TestActionEnum.LEFT, B, (50.0));
        assertEquals(tree.getStatisticsFor(A).getV(1)[0], 0.5, 0.01);
        assertEquals(tree.getStatisticsFor(A).getQ(1)[0], 0.5, 0.01);
    }

    @Test
    public void updatingForSeveralDifferentActorsDoNotClash() {
        TranspositionTableMCTree<TestAgent> tree = new TranspositionTableMCTree<>(localProp, 3);
        tree.insertState(A);
        tree.insertState(B);

        MCStatistics statsA = tree.getStatisticsFor(A);
        statsA.update(TestActionEnum.LEFT, B, new double[]{0.0, 1.0, 0.01}, 1);
        statsA.update(TestActionEnum.LEFT, B, new double[]{1.0, 2.0, 0.2}, 1);
        assertEquals(statsA.getMean(TestActionEnum.LEFT, 1)[0], 0.5, 0.001);
        assertEquals(statsA.getMean(TestActionEnum.LEFT, 1)[1], 1.5, 0.001);
        assertEquals(statsA.getMean(TestActionEnum.LEFT, 1)[2], 0.105, 0.001);

        assertEquals(statsA.getMean(TestActionEnum.LEFT, 2)[0], 0.0, 0.001);
        assertEquals(statsA.getMean(TestActionEnum.LEFT, 2)[1], 0.0, 0.001);
        assertEquals(statsA.getMean(TestActionEnum.LEFT, 2)[2], 0.0, 0.001);

        statsA.update(TestActionEnum.LEFT, B, new double[]{1.0, 2.0, 1.01}, 2);
        statsA.update(TestActionEnum.LEFT, B, new double[]{2.0, 3.0, 1.2}, 2);

        assertEquals(statsA.getMean(TestActionEnum.LEFT, 1)[0], 0.5, 0.001);
        assertEquals(statsA.getMean(TestActionEnum.LEFT, 1)[1], 1.5, 0.001);
        assertEquals(statsA.getMean(TestActionEnum.LEFT, 1)[2], 0.105, 0.001);

        assertEquals(statsA.getMean(TestActionEnum.LEFT, 2)[0], 1.5, 0.001);
        assertEquals(statsA.getMean(TestActionEnum.LEFT, 2)[1], 2.5, 0.001);
        assertEquals(statsA.getMean(TestActionEnum.LEFT, 2)[2], 1.105, 0.001);
    }

    @Test
    public void updatingViaTreeDoesNotCreateClashes() {
        TranspositionTableMCTree<TestAgent> tree = new TranspositionTableMCTree<TestAgent>(localProp, 3);
        tree.insertState(A);
        tree.insertState(B);

        tree.updateState(A, TestActionEnum.LEFT, B, new double[]{0.0, 1.0, 0.01}, 1);
        tree.updateState(A, TestActionEnum.LEFT, C, new double[]{1.0, 2.0, 0.2}, 1);
        MCStatistics statsA = tree.getStatisticsFor(A);
        assertEquals(statsA.getMean(TestActionEnum.LEFT, 1)[0], 0.5, 0.001);
        assertEquals(statsA.getMean(TestActionEnum.LEFT, 1)[1], 1.5, 0.001);
        assertEquals(statsA.getMean(TestActionEnum.LEFT, 1)[2], 0.105, 0.001);

        assertEquals(statsA.getMean(TestActionEnum.LEFT, 2)[0], 0.0, 0.001);
        assertEquals(statsA.getMean(TestActionEnum.LEFT, 2)[1], 0.0, 0.001);
        assertEquals(statsA.getMean(TestActionEnum.LEFT, 2)[2], 0.0, 0.001);

        tree.updateState(A, TestActionEnum.LEFT, B, new double[]{1.0, 2.0, 1.01}, 2);
        tree.updateState(A, TestActionEnum.LEFT, B, new double[]{2.0, 3.0, 1.2}, 2);

        assertEquals(statsA.getMean(TestActionEnum.LEFT, 1)[0], 0.5, 0.001);
        assertEquals(statsA.getMean(TestActionEnum.LEFT, 1)[1], 1.5, 0.001);
        assertEquals(statsA.getMean(TestActionEnum.LEFT, 1)[2], 0.105, 0.001);

        assertEquals(statsA.getMean(TestActionEnum.LEFT, 2)[0], 1.5, 0.001);
        assertEquals(statsA.getMean(TestActionEnum.LEFT, 2)[1], 2.5, 0.001);
        assertEquals(statsA.getMean(TestActionEnum.LEFT, 2)[2], 1.105, 0.001);
    }

    @Test
    public void useTotalVisitsOnlyWithVisitValiditySwitchedOff() {
        /*
        we set up MCStatistics, and visit it, 5 times with leftRightOnly
        then we visit it with allActions
        // at each point, we check that the output of getUCTValues is as we expect for each of LEFT, RIGHT, TEST
         */
        stats = new MCStatistics<>(localProp, 1, A);
        int leftChosen = 0, testChosen = 0;
        for (int i = 0; i < 5; i++) {
            double expectedActionScore = leftChosen == 0 ? 0.0 : 1.0;
            assertEquals(stats.getUCTValue(TestActionEnum.LEFT, 1)[0], expectedActionScore, 0.01);  // actionScore
            double expectedExplorationTerm = leftChosen == 0 ? Double.MAX_VALUE : Math.sqrt(Math.log(i) / leftChosen);
            assertEquals(stats.getUCTValue(TestActionEnum.LEFT, 1)[1], expectedExplorationTerm, 0.01);  // explorationTerm
            assertEquals(stats.getUCTValue(TestActionEnum.LEFT, 1)[2], leftChosen, 0.01);  // n
            assertEquals(stats.getUCTValue(TestActionEnum.LEFT, 1)[3], i, 0.01);  // N

            assertEquals(stats.getUCTValue(TestActionEnum.TEST, 1)[0], 0.0, 0.01);  // actionScore
            assertEquals(stats.getUCTValue(TestActionEnum.TEST, 1)[1], Double.MAX_VALUE, 0.01);  // explorationTerm
            assertEquals(stats.getUCTValue(TestActionEnum.TEST, 1)[2], 0, 0.01);  // n
            assertEquals(stats.getUCTValue(TestActionEnum.TEST, 1)[3], i, 0.01);  // N

            ActionEnum action = stats.getNextAction(leftRightOnly, 1);
            stats.update(action, new double[]{1.0}, 1);
            if (action.equals(TestActionEnum.LEFT)) leftChosen++;
        }
        for (int i = 0; i < 5; i++) {
            assertEquals(stats.getUCTValue(TestActionEnum.LEFT, 1)[0], 1.0, 0.01);  // actionScore
            double expectedExplorationTerm = leftChosen == 0 ? Double.MAX_VALUE : Math.sqrt(Math.log(i+5) / leftChosen);
            assertEquals(stats.getUCTValue(TestActionEnum.LEFT, 1)[1], expectedExplorationTerm, 0.01);  // explorationTerm
            assertEquals(stats.getUCTValue(TestActionEnum.LEFT, 1)[2], leftChosen, 0.01);  // n
            assertEquals(stats.getUCTValue(TestActionEnum.LEFT, 1)[3], i+5, 0.01);  // N

            double expectedActionScore = testChosen == 0 ? 0.0 : 1.0;
            assertEquals(stats.getUCTValue(TestActionEnum.TEST, 1)[0], expectedActionScore, 0.01);  // actionScore
            expectedExplorationTerm = testChosen == 0 ? Double.MAX_VALUE : Math.sqrt(Math.log(i+5) / testChosen);
            assertEquals(stats.getUCTValue(TestActionEnum.TEST, 1)[1], expectedExplorationTerm, 0.01);  // explorationTerm
            assertEquals(stats.getUCTValue(TestActionEnum.TEST, 1)[2], testChosen, 0.01);  // n
            assertEquals(stats.getUCTValue(TestActionEnum.TEST, 1)[3], i+5, 0.01);  // N

            ActionEnum action = stats.getNextAction(allActions, 1);
            stats.update(action, new double[]{1.0}, 1);
            if (action.equals(TestActionEnum.LEFT)) leftChosen++;
            if (action.equals(TestActionEnum.TEST)) testChosen++;
            assertNotEquals(testChosen, 0.0, 0.01); // first one should be Test
        }
    }

    @Test
    public void useParentalValidVistsWhenSwitchedOn() {
        // repeat of previous test case, with different setting
        localProp.setProperty("MonteCarloParentalVisitValidity", "true");
        stats = new MCStatistics<>(localProp, 1, A);
        int leftChosen = 0, testChosen = 0;
        for (int i = 0; i < 5; i++) {
            double expectedActionScore = leftChosen == 0 ? 0.0 : 1.0;
            assertEquals(stats.getUCTValue(TestActionEnum.LEFT, 1)[0], expectedActionScore, 0.01);  // actionScore
            double expectedExplorationTerm = leftChosen == 0 ? Double.MAX_VALUE : Math.sqrt(Math.log(i) / leftChosen);
            assertEquals(stats.getUCTValue(TestActionEnum.LEFT, 1)[1], expectedExplorationTerm, 0.01);  // explorationTerm
            assertEquals(stats.getUCTValue(TestActionEnum.LEFT, 1)[2], leftChosen, 0.01);  // n
            assertEquals(stats.getUCTValue(TestActionEnum.LEFT, 1)[3], i, 0.01);  // N

            assertEquals(stats.getUCTValue(TestActionEnum.TEST, 1)[0], 0.0, 0.01);  // actionScore
            assertEquals(stats.getUCTValue(TestActionEnum.TEST, 1)[1], Double.MAX_VALUE, 0.01);  // explorationTerm
            assertEquals(stats.getUCTValue(TestActionEnum.TEST, 1)[2], 0, 0.01);  // n
            assertEquals(stats.getUCTValue(TestActionEnum.TEST, 1)[3], 0.0, 0.01);  // N

            ActionEnum action = stats.getNextAction(leftRightOnly, 1);
            stats.update(action, new double[]{1.0}, 1);
            if (action.equals(TestActionEnum.LEFT)) leftChosen++;
        }
        for (int i = 0; i < 5; i++) {
            assertEquals(stats.getUCTValue(TestActionEnum.LEFT, 1)[0], 1.0, 0.01);  // actionScore
            double expectedExplorationTerm = leftChosen == 0 ? Double.MAX_VALUE : Math.sqrt(Math.log(i+5) / leftChosen);
            assertEquals(stats.getUCTValue(TestActionEnum.LEFT, 1)[1], expectedExplorationTerm, 0.01);  // explorationTerm
            assertEquals(stats.getUCTValue(TestActionEnum.LEFT, 1)[2], leftChosen, 0.01);  // n
            assertEquals(stats.getUCTValue(TestActionEnum.LEFT, 1)[3], i+5, 0.01);  // N

            double expectedActionScore = testChosen == 0 ? 0.0 : 1.0;
            assertEquals(stats.getUCTValue(TestActionEnum.TEST, 1)[0], expectedActionScore, 0.01);  // actionScore
            expectedExplorationTerm = testChosen == 0 ? Double.MAX_VALUE : Math.sqrt(Math.log(i) / testChosen);
            assertEquals(stats.getUCTValue(TestActionEnum.TEST, 1)[1], expectedExplorationTerm, 0.01);  // explorationTerm
            assertEquals(stats.getUCTValue(TestActionEnum.TEST, 1)[2], testChosen, 0.01);  // n
            assertEquals(stats.getUCTValue(TestActionEnum.TEST, 1)[3], i, 0.01);  // N

            ActionEnum action = stats.getNextAction(allActions, 1);
            stats.update(action, new double[]{1.0}, 1);
            if (action.equals(TestActionEnum.LEFT)) leftChosen++;
            if (action.equals(TestActionEnum.TEST)) testChosen++;
            assertNotEquals(testChosen, 0.0, 0.01); // first one should be Test
        }
    }

    private double[] toArray(double single) {
        double[] retValue = new double[1];
        retValue[0] = single;
        return retValue;
    }
}


class SimpleHeuristic<P extends Agent> extends BaseStateDecider<P> {

    private Map<ActionEnum<P>, Double> values;

    public SimpleHeuristic(Map<ActionEnum<P>, Double> map) {
        super(null);
        values = map;
    }

    @Override
    public double valueOption(ActionEnum<P> option, State<P> state) {
        return values.getOrDefault(option, 0.0);
    }

    @Override
    public List<Double> valueOptions(List<ActionEnum<P>> options, State<P> state) {
        List<Double> retValue = new ArrayList<Double>(options.size());
        for (int i = 0; i < options.size(); i++)
            retValue.add(values.getOrDefault(options.get(i), 0.0));
        return retValue;
    }

    @Override
    public void learnFrom(ExperienceRecord<P> exp, double maxResult) {
    }

}

