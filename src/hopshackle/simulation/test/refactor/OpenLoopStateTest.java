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

    @Before
    public void setup() {
        localProp = SimProperties.getDeciderProperties("GLOBAL");
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
        assertFalse(start.equals(middle));

        action1.start();
        action1.run();
        assertTrue(olsf.getCurrentState(agent1).equals(middle));

        Action<?> action2 = decider.decide(agent1, testOnly);
        assertTrue(action2.getType() == TestActionEnum.TEST);
        State<TestAgent> end = olsf.getCurrentState(agent1);
        assertFalse(middle.equals(end));
        assertFalse(start.equals(end));
    }

    @Test
    public void differentAgentsKeepDifferentStatesAsActionsTaken() {
        State<TestAgent> start1 = olsf.getCurrentState(agent1);
        Action<?> action1 = decider.decide(agent1, testOnly);
        assertTrue(action1.getType() == TestActionEnum.TEST);
        State<TestAgent> end1 = olsf.getCurrentState(agent1);
        assertFalse(start1.equals(end1));

        State<TestAgent> start2 = olsf.getCurrentState(agent2);
        Action<?> action2 = decider.decide(agent2, testOnly);
        assertTrue(action2.getType() == TestActionEnum.TEST);
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
        State<TestAgent> s3 = olsf.getCurrentState(agent3);
        SimpleMazeGame newGame = (SimpleMazeGame) game.clone(agent1);
        olsf.cloneGame(game, newGame);
        assertTrue(olsf.getCurrentState(agent1).equals(olsf.getCurrentState(newGame.getPlayer(1))));
        assertTrue(olsf.getCurrentState(agent2).equals(olsf.getCurrentState(newGame.getPlayer(2))));
        assertTrue(olsf.getCurrentState(agent3).equals(olsf.getCurrentState(newGame.getPlayer(3))));
        assertTrue(olsf.getCurrentState(agent1).equals(s1));
        assertTrue(olsf.getCurrentState(agent2).equals(s2));
        assertTrue(olsf.getCurrentState(agent3).equals(s3));
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
        State<TestAgent> end1 = olsf.getCurrentState(agent1);
        assertFalse(s1.equals(end1));

        Action<?> action2 = decider.decide(newGame.getPlayer(1), testOnly);
        assertTrue(action2.getType() == TestActionEnum.TEST);
        State<TestAgent> end2 = olsf.getCurrentState(newGame.getPlayer(1));
        assertFalse(s2.equals(end2));

        assertTrue(end1.equals(end2));
    }

    @Test
    public void deathRemovesAgentFromTracking() {
        State<TestAgent> start1 = olsf.getCurrentState(agent1);
        Action<?> action1 = decider.decide(agent1, testOnly);
        assertTrue(action1.getType() == TestActionEnum.TEST);
        State<TestAgent> end1 = olsf.getCurrentState(agent1);
        assertFalse(start1.equals(end1));

        assertTrue(olsf.getCurrentState(agent1).equals(end1));
        olsf.processEvent(new AgentEvent(agent1, AgentEvent.Type.DEATH));
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
