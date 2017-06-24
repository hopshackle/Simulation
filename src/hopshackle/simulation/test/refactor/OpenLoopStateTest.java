package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;

import hopshackle.simulation.*;

import java.util.*;

import org.junit.*;

/**
 * Created by james on 22/06/2017.
 */
public class OpenLoopStateTest {

    List<ActionEnum<TestAgent>> allActions = new ArrayList<ActionEnum<TestAgent>>(EnumSet.allOf(TestActionEnum.class));
    List<ActionEnum<TestAgent>> leftRightOnly = new ArrayList<ActionEnum<TestAgent>>(EnumSet.allOf(TestActionEnum.class));
    List<ActionEnum<TestAgent>> testOnly = new ArrayList<ActionEnum<TestAgent>>(EnumSet.allOf(TestActionEnum.class));
    World world;
    TestAgent agent1, agent2, agent3;
    DeciderProperties localProp;
    OpenLoopStateFactory<TestAgent> olsf = new OpenLoopStateFactory<TestAgent>();
    Decider<TestAgent> decider;
    OpenLoopState<TestAgent> initialState;

    @Before
    public void setup() {
        localProp = SimProperties.getDeciderProperties("GLOBAL");
        localProp.setProperty("OpenLoopUpdateLimit", "2");
        leftRightOnly.remove(TestActionEnum.TEST);
        testOnly.remove((TestActionEnum.LEFT));
        testOnly.remove((TestActionEnum.RIGHT));
        world = new World();
        agent1 = new TestAgent(world);
        agent2 = new TestAgent(world);
        agent3 = new TestAgent(world);
        decider = agent1.getDecider();
    }

    @Test
    public void eachAgentHasDifferentStartingState() {
        assertFalse(olsf.getCurrentState(agent1).equals(olsf.getCurrentState(agent2)));
        assertFalse(olsf.getCurrentState(agent2).equals(olsf.getCurrentState(agent3)));
        assertFalse(olsf.getCurrentState(agent1).equals(olsf.getCurrentState(agent3)));
        assertFalse(olsf.getCurrentState(agent1) == null);
        assertFalse(olsf.getCurrentState(agent2) == null);
        assertFalse(olsf.getCurrentState(agent3) == null);
    }

    @Test
    public void asAgentTakesActionsTheStateMovesOn() {
        State<TestAgent> start = olsf.getCurrentState(agent1);
        Action<?> action1 = decider.decide(agent1, testOnly);
        assertTrue(action1.getType() == TestActionEnum.TEST);
        State<TestAgent> middle = olsf.getCurrentState(agent1);
        assertTrue(start.equals(middle));       // nothing has yet changed

        action1.start();
        action1.run();
        middle = olsf.getCurrentState(agent1);
        assertFalse(start.equals(middle));      // now it has

        Action<?> action2 = decider.decide(agent1, testOnly);
        assertTrue(action2.getType() == TestActionEnum.TEST);
        State<TestAgent> end = olsf.getCurrentState(agent1);
        assertTrue(middle.equals(end));  // nothing has yet changed

        action2.start();
        action2.run();
        end = olsf.getCurrentState(agent1);
        assertFalse(end.equals(middle));      // now it has
        assertFalse(end.equals(start));
    }

    @Test
    public void updateLimitIsFollowed() {
        OpenLoopState<TestAgent> state = (OpenLoopState<TestAgent>) olsf.getCurrentState(agent1);
        initialState = state;
        assertTrue(olsf.containsState(state));
        Action<?> action1 = decider.decide(agent1, testOnly);
        action1.start();
        action1.run();   //#1
        OpenLoopState<TestAgent> oldState = state;
        state = (OpenLoopState<TestAgent>) olsf.getCurrentState(agent1);
        assertTrue(olsf.containsState(state));
        assertFalse(oldState.equals(state));

        action1 = decider.decide(agent1, testOnly);
        action1.start();
        action1.run();   //#2
        oldState = state;
        state = (OpenLoopState<TestAgent>) olsf.getCurrentState(agent1);
        assertTrue(olsf.containsState(state));
        assertFalse(oldState.equals(state));
        action1 = decider.decide(agent1, testOnly);

        action1.start();
        action1.run();   //#3 and we have now run out of updates
        oldState = state;
        state = (OpenLoopState<TestAgent>) olsf.getCurrentState(agent1);
        assertTrue(olsf.containsState(state));
        assertTrue(oldState.equals(state));
    }

    @Test
    public void updateLimitsWorksWithExistingStatesInTree() {
        updateLimitIsFollowed();
        olsf.setState(agent1, initialState);

        OpenLoopState<TestAgent> state = (OpenLoopState<TestAgent>) olsf.getCurrentState(agent1);
        assertTrue(olsf.containsState(state));
        for (int i = 0; i < 6; i++) {
            Action<?> action1 = decider.decide(agent1, testOnly);
            action1.start();
            action1.run();   //#1
            OpenLoopState<TestAgent> oldState = state;
            state = (OpenLoopState<TestAgent>) olsf.getCurrentState(agent1);
            assertTrue(olsf.containsState(state));
            if (i < 4)
                assertFalse(oldState.equals(state));
            else
                assertTrue(oldState.equals(state));
        }
    }

    @Test
    public void differentAgentsKeepDifferentStatesAsActionsTaken() {
        State<TestAgent> start1 = olsf.getCurrentState(agent1);
        Action<?> action1 = decider.decide(agent1, testOnly);
        assertTrue(action1.getType() == TestActionEnum.TEST);
        action1.start();
        action1.run();
        State<TestAgent> end1 = olsf.getCurrentState(agent1);
        assertFalse(start1.equals(end1));

        State<TestAgent> start2 = olsf.getCurrentState(agent2);
        Action<?> action2 = decider.decide(agent2, testOnly);
        assertTrue(action2.getType() == TestActionEnum.TEST);
        action2.start();
        action2.run();
        State<TestAgent> end2 = olsf.getCurrentState(agent2);
        assertFalse(start2.equals(end2));

        assertFalse(olsf.getCurrentState(agent1).equals(olsf.getCurrentState(agent2)));
    }

    @Test
    public void cloneGameCopiesOverCurrentStatesCorrectly() {
        TestAgent[] players = new TestAgent[3];
        players[0] = agent1;
        players[1] = agent2;
        players[2] = agent3;
        SimpleMazeGame game = new SimpleMazeGame(2, players);
        State<TestAgent> s1 = olsf.getCurrentState(agent1);
        State<TestAgent> s2 = olsf.getCurrentState(agent2);
        SimpleMazeGame newGame = (SimpleMazeGame) game.clone(agent1);
        olsf.cloneGame(game, newGame);
        assertTrue(olsf.getCurrentState(agent1).equals(olsf.getCurrentState(newGame.getPlayer(1))));
        assertTrue(olsf.getCurrentState(agent2).equals(olsf.getCurrentState(newGame.getPlayer(2))));
        // and since p3 was not tracked, the two should not be present, and have different states created
        assertFalse(olsf.getCurrentState(agent3) == null);
        assertFalse(olsf.getCurrentState(newGame.getPlayer(3)) == null);
        assertFalse(olsf.getCurrentState(agent3).equals(olsf.getCurrentState(newGame.getPlayer(3))));
        assertTrue(olsf.getCurrentState(agent1).equals(s1));
        assertTrue(olsf.getCurrentState(agent2).equals(s2));
    }

    @Test
    public void theSameActionFromTheSameStartingStateGivesTheSameState() {
        TestAgent[] players = new TestAgent[2];
        players[0] = agent1;
        players[1] = agent2;
        SimpleMazeGame game = new SimpleMazeGame(2, players);
        State<TestAgent> s1 = olsf.getCurrentState(agent1);
        SimpleMazeGame newGame = (SimpleMazeGame) game.clone(agent1);
        olsf.cloneGame(game, newGame);
        State<TestAgent> s2 = olsf.getCurrentState(newGame.getPlayer(1));
        assertTrue(s1.equals(s2));

        Action<?> action1 = decider.decide(agent1, testOnly);
        assertTrue(action1.getType() == TestActionEnum.TEST);
        action1.start();
        action1.run();
        State<TestAgent> end1 = olsf.getCurrentState(agent1);
        assertFalse(s1.equals(end1));

        Action<?> action2 = decider.decide(newGame.getPlayer(1), testOnly);
        assertTrue(action2.getType() == TestActionEnum.TEST);
        action2.start();
        action2.run();
        State<TestAgent> end2 = olsf.getCurrentState(newGame.getPlayer(1));
        assertFalse(s2.equals(end2));

        assertTrue(end1.equals(end2));
    }

    @Test
    public void deathRemovesAgentFromTracking() {
        State<TestAgent> start1 = olsf.getCurrentState(agent1);
        Action<?> action1 = decider.decide(agent1, testOnly);
        assertTrue(action1.getType() == TestActionEnum.TEST);
        action1.start();
        action1.run();
        State<TestAgent> end1 = olsf.getCurrentState(agent1);
        assertFalse(start1.equals(end1));

        assertTrue(olsf.getCurrentState(agent1).equals(end1));
        assertFalse(olsf.getCurrentState(agent1) == null);
        agent1.die("oops");
        assertFalse(olsf.getCurrentState(agent1) == null);
        assertFalse(olsf.getCurrentState(agent1).equals(end1));
    }


    @Test
    public void pruningTreeRemovesOnlyIrrelevantStates() {
        OpenLoopState<TestAgent> s1 = (OpenLoopState<TestAgent>) olsf.getCurrentState(agent1);
        OpenLoopState<TestAgent> s2 = (OpenLoopState<TestAgent>) olsf.getCurrentState(agent2);
        asAgentTakesActionsTheStateMovesOn();
        OpenLoopState<TestAgent> e1 = (OpenLoopState<TestAgent>) olsf.getCurrentState(agent1);
        assertTrue(olsf.containsState(s1));
        assertTrue(olsf.containsState(s2));
        assertTrue(olsf.containsState(e1));

        olsf.prune();
        assertFalse(olsf.containsState(s1));
        assertTrue(olsf.containsState(s2));
        assertTrue(olsf.containsState(e1));

        olsf.processEvent(new AgentEvent(agent2, AgentEvent.Type.DEATH));
        olsf.prune();
        assertFalse(olsf.containsState(s1));
        assertFalse(olsf.containsState(s2));
        assertTrue(olsf.containsState(e1));
    }

    @Test
    public void applyOnStateGivesNextStateIfItExists() {
        State<TestAgent> start1 = olsf.getCurrentState(agent1);
        Action<?> action1 = decider.decide(agent1, leftRightOnly);
        action1.start();
        action1.run();
        State<TestAgent> end1 = olsf.getCurrentState(agent1);
        if (action1.getType() == TestActionEnum.LEFT) {
            assertTrue(start1.apply(TestActionEnum.LEFT).equals(end1));
            OpenLoopState<TestAgent> projection = (OpenLoopState<TestAgent>) start1.apply(TestActionEnum.RIGHT);
            assertFalse(projection.equals(end1));
            assertTrue(olsf.containsState(projection));
        } else {
            assertTrue(action1.getType() == TestActionEnum.RIGHT);
            assertTrue(start1.apply(TestActionEnum.RIGHT).equals(end1));
            OpenLoopState<TestAgent> projection = (OpenLoopState<TestAgent>) start1.apply(TestActionEnum.LEFT);
            assertFalse(projection.equals(end1));
            assertTrue(olsf.containsState(projection));
        }
    }

}
