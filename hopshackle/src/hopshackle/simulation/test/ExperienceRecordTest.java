package hopshackle.simulation.test;

import static org.junit.Assert.*;

import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;
import java.util.*;

import org.junit.*;

public class ExperienceRecordTest {

	private List<GeneticVariable> varList = new ArrayList<GeneticVariable>();
	private ExperienceRecord er;

	@Before
	public void setUp() throws Exception {
		for (GeneticVariable gv : BasicVariables.values())
			varList.add(gv);
	}

	@Test
	public void experienceRecordInitialisation() {
		List<ActionEnum> possibleActions = new ArrayList<ActionEnum>();
		possibleActions.add(BasicActions.FARM);
		possibleActions.add(BasicActions.FIND_UNKNOWN);
		double[] startState = new double[varList.size()];
		for (int i = 0; i < startState.length; i++)
			startState[i] = i;

		er = new ExperienceRecord(varList, startState, BasicActions.FARM, possibleActions, null);

		assertFalse(er.isInFinalState());
		assertTrue(er.getEndState() == null);
		assertTrue(er.getActionTaken() == BasicActions.FARM);
		assertEquals(er.getReward(), 0.0, 0.01);
		assertTrue(er.getPossibleActionsFromEndState() == null);
		assertEquals(er.getPossibleActionsFromStartState().size(), 2);

		double[] endState = new double[varList.size()];
		for (int i = 0; i < endState.length; i++)
			endState[i] = Math.sqrt(i);
		possibleActions.add(BasicActions.BREED);
		er.updateWithResults(10.0, endState, possibleActions, true);
		assertTrue(er.isInFinalState());
		assertFalse(er.getEndState() == null);
		assertTrue(er.getActionTaken() == BasicActions.FARM);
		assertEquals(er.getReward(), 10.0, 0.01);
		assertEquals(er.getPossibleActionsFromEndState().size(), 3);
		assertEquals(er.getPossibleActionsFromStartState().size(), 2);
	}

	@Test
	public void experienceRecordGetValuesReturnsJustThoseGenVarValuesRequired() {
		experienceRecordInitialisation();
		List<GeneticVariable> reducedList = new ArrayList<GeneticVariable>();
		reducedList.add(BasicVariables.FOOD_LEVEL);
		reducedList.add(BasicVariables.FOREST);
		reducedList.add(BasicVariables.AGE);

		double[][] values = er.getValues(reducedList);
		int foodIndex = varList.indexOf(BasicVariables.FOOD_LEVEL);
		int forestIndex = varList.indexOf(BasicVariables.FOREST);
		int ageIndex = varList.indexOf(BasicVariables.AGE);
		assertEquals(values.length, 2);
		assertEquals(values[0].length, 3);
		assertEquals(values[0][0], foodIndex, 0.01);
		assertEquals(values[0][1], forestIndex, 0.01);
		assertEquals(values[0][2], ageIndex, 0.01);
		assertEquals(values[1][0], Math.sqrt(foodIndex), 0.01);
		assertEquals(values[1][1], Math.sqrt(forestIndex), 0.01);
		assertEquals(values[1][2], Math.sqrt(ageIndex), 0.01);
	}

}
