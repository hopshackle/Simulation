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
		SimProperties.setProperty("IncrementalScoreReward", "true");
		ExperienceRecord.refreshProperties();
		for (GeneticVariable<BasicAgent> gv : BasicVariables.values())
			varList.add(gv);
		w = new World();
		agent = new BasicAgent(w);
		BasicHex loc = new BasicHex(0, 0);
		loc.setParentLocation(w);
		agent.setLocation(loc);
	}

	@Test
	public void experienceRecordInitialisation() {
		List<ActionEnum<BasicAgent>> possibleActions = new ArrayList<ActionEnum<BasicAgent>>();
		possibleActions.add(BasicActions.FARM);
		possibleActions.add(BasicActions.FIND_UNKNOWN);

		er = new ExperienceRecord<BasicAgent>(agent, new LinearState<BasicAgent>(agent, varList), BasicActions.FARM.getAction(agent), possibleActions);

		assertFalse(er.isInFinalState());
		assertTrue(er.getEndState() == null);
		assertTrue(er.getActionTaken().getType() == BasicActions.FARM);
		assertEquals(er.getReward(), 0.0, 0.01);
		assertTrue(er.getPossibleActionsFromEndState() == null);
		assertEquals(er.getPossibleActionsFromStartState().size(), 2);

		possibleActions.add(BasicActions.BREED);
		er.updateWithResults(30.0, new LinearState<BasicAgent>(agent, varList));
		agent.addHealth(-15.0);
		assertEquals(er.getReward(), 30.0, 0.01);
		er.setIsFinal();
		assertTrue(er.isInFinalState());
		assertFalse(er.getEndState() == null);
		assertTrue(er.getActionTaken().getType() == BasicActions.FARM);
		assertEquals(er.getReward(), 15.0, 0.01);	// -15 in change of score from full health, plus 30 reward
		assertEquals(er.getPossibleActionsFromStartState().size(), 2);
	}
	
	@Test
	public void monteCarloRewardsWithIncremental() {
		String oldGamma = SimProperties.getProperty("Gamma", "0.9");
		SimProperties.setProperty("MonteCarloReward", "true");
		SimProperties.setProperty("Gamma", "0.9");
		ExperienceRecord.refreshProperties();
		
		List<ActionEnum<BasicAgent>> possibleActions = new ArrayList<ActionEnum<BasicAgent>>();
		possibleActions.add(BasicActions.FARM);
		possibleActions.add(BasicActions.FIND_UNKNOWN);

		er = new ExperienceRecord<BasicAgent>(agent, new LinearState<BasicAgent>(agent, varList), BasicActions.FARM.getAction(agent), possibleActions);
		er.updateWithResults(30.0, new LinearState<BasicAgent>(agent, varList));
		ExperienceRecord<BasicAgent> nextER = new ExperienceRecord<BasicAgent>(agent, new LinearState<BasicAgent>(agent, varList), BasicActions.FARM.getAction(agent), possibleActions);
		er.updateNextActions(nextER);
		nextER.updateWithResults(0.0, new LinearState<BasicAgent>(agent, varList));
		assertEquals(er.getReward(), 30.0, 0.001);
		assertEquals(nextER.getReward(), 0.0, 0.001);
		agent.addHealth(-2.0);
		nextER.setIsFinal();
		assertEquals(nextER.getReward(), -2.0, 0.001);
		assertEquals(er.getReward(), -2.0 * 0.9 + 30.0, 0.001);

		
		SimProperties.setProperty("MonteCarloReward", "false");
		SimProperties.setProperty("Gamma", oldGamma);
		ExperienceRecord.refreshProperties();
	}
	
	@Test
	public void monteCarloRewardsWithoutIncremental() {
		String oldGamma = SimProperties.getProperty("Gamma", "0.9");
		SimProperties.setProperty("MonteCarloReward", "true");
		SimProperties.setProperty("IncrementalScoreReward", "false");
		SimProperties.setProperty("Gamma", "0.9");
		ExperienceRecord.refreshProperties();
		
		List<ActionEnum<BasicAgent>> possibleActions = new ArrayList<ActionEnum<BasicAgent>>();
		possibleActions.add(BasicActions.FARM);
		possibleActions.add(BasicActions.FIND_UNKNOWN);

		er = new ExperienceRecord<BasicAgent>(agent, new LinearState<BasicAgent>(agent, varList), BasicActions.FARM.getAction(agent), possibleActions);
		er.updateWithResults(3.0, new LinearState<BasicAgent>(agent, varList));
		ExperienceRecord<BasicAgent> nextER = new ExperienceRecord<BasicAgent>(agent, new LinearState<BasicAgent>(agent, varList), BasicActions.FARM.getAction(agent), possibleActions);
		er.updateNextActions(nextER);
		nextER.updateWithResults(0.0, new LinearState<BasicAgent>(agent, varList));
		assertEquals(er.getReward(), 3.0, 0.001);
		assertEquals(nextER.getReward(), 0.0, 0.001);
		agent.addHealth(-2.0);
		nextER.setIsFinal();
		assertEquals(nextER.getReward(), 18.0, 0.001);
		assertEquals(er.getReward(), 18.0 * 0.9 + 3.0, 0.001);

		
		SimProperties.setProperty("MonteCarloReward", "false");
		SimProperties.setProperty("Gamma", oldGamma);
		ExperienceRecord.refreshProperties();
	}

}
