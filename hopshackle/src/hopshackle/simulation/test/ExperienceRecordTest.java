package hopshackle.simulation.test;

import static org.junit.Assert.*;

import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;
import java.util.*;

import org.junit.*;

public class ExperienceRecordTest {

	private List<GeneticVariable<BasicAgent>> varList = new ArrayList<GeneticVariable<BasicAgent>>();
	private ExperienceRecord<BasicAgent> er;
	private BasicAgent agent;
	private World w;

	@Before
	public void setUp() throws Exception {
		for (GeneticVariable<BasicAgent> gv : BasicVariables.values())
			varList.add(gv);
		w = new World();
		agent = new BasicAgent(w);
	}

	@Test
	public void experienceRecordInitialisation() {
		List<ActionEnum<BasicAgent>> possibleActions = new ArrayList<ActionEnum<BasicAgent>>();
		possibleActions.add(BasicActions.FARM);
		possibleActions.add(BasicActions.FIND_UNKNOWN);
		double[] startState = new double[varList.size()];
		for (int i = 0; i < startState.length; i++)
			startState[i] = i;

		er = new ExperienceRecord<BasicAgent>(agent, varList, startState, BasicActions.FARM.getAction(agent), possibleActions);

		assertFalse(er.isInFinalState());
		assertTrue(er.getEndState() == null);
		assertTrue(er.getActionTaken().getType() == BasicActions.FARM);
		assertEquals(er.getReward(), 0.0, 0.01);
		assertTrue(er.getPossibleActionsFromEndState() == null);
		assertEquals(er.getPossibleActionsFromStartState().size(), 2);

		double[] endState = new double[varList.size()];
		for (int i = 0; i < endState.length; i++)
			endState[i] = Math.sqrt(i);
		possibleActions.add(BasicActions.BREED);
		er.updateWithResults(30.0, endState);
		er.setIsFinal();
		assertTrue(er.isInFinalState());
		assertFalse(er.getEndState() == null);
		assertTrue(er.getActionTaken().getType() == BasicActions.FARM);
		assertEquals(er.getReward(), 10.0, 0.01);	// -20 in change of score from full health to dead, plus 30 reward
		assertEquals(er.getPossibleActionsFromStartState().size(), 2);
	}

}
