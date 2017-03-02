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
	private List<ActionEnum<BasicAgent>> possibleActions;
	StateFactory<BasicAgent> stateFactory = new LinearStateFactory<BasicAgent>(new ArrayList<GeneticVariable<BasicAgent>>());
	DeciderProperties localProp;
	
	@Before
	public void setUp() throws Exception {
		localProp = SimProperties.getDeciderProperties("GLOBAL");
		localProp.setProperty("IncrementalScoreReward", "true");
		localProp.setProperty("TimePeriodForGamma", "1000");
		for (GeneticVariable<BasicAgent> gv : BasicVariables.values())
			varList.add(gv);
		w = new World();
		w.setCalendar(new FastCalendar(0l));
		agent = new BasicAgent(w);
		BasicHex loc = new BasicHex(0, 0);
		loc.setParentLocation(w);
		agent.setLocation(loc);
		
		possibleActions = new ArrayList<ActionEnum<BasicAgent>>();
		possibleActions.add(BasicActions.FARM);
		possibleActions.add(BasicActions.FIND_UNKNOWN);
	}

	@Test
	public void experienceRecordInitialisation() {
		er = new ExperienceRecord<BasicAgent>(agent, new LinearState<BasicAgent>(agent, varList), BasicActions.FARM.getAction(agent), possibleActions, localProp);

		assertFalse(er.isInFinalState());
		assertTrue(er.getEndState() == null);
		assertTrue(er.getActionTaken().getType() == BasicActions.FARM);
		assertEquals(er.getReward()[0], 0.0, 0.01);
		assertTrue(er.getPossibleActionsFromEndState() == null);
		assertEquals(er.getPossibleActionsFromStartState().size(), 2);

		possibleActions.add(BasicActions.BREED);
		er.updateWithResults(30.0, new LinearState<BasicAgent>(agent, varList));
		agent.addHealth(-15.0);
		assertEquals(er.getReward()[0], 30.0, 0.01);
		er.updateWithFinalScores(new double[] {agent.getScore()});
		assertTrue(er.isInFinalState());
		assertFalse(er.getEndState() == null);
		assertTrue(er.getActionTaken().getType() == BasicActions.FARM);
		assertEquals(er.getReward()[0], 15.0, 0.01);	// -15 in change of score from full health, plus 30 reward
		assertEquals(er.getPossibleActionsFromStartState().size(), 2);
	}
	
	@Test
	public void monteCarloRewardsWithIncremental() {
		localProp.setProperty("MonteCarloReward", "true");
		localProp.setProperty("Gamma", "0.9");
		
		er = new ExperienceRecord<BasicAgent>(agent, new LinearState<BasicAgent>(agent, varList), BasicActions.FARM.getAction(agent), possibleActions ,localProp);
		er.updateWithResults(30.0, new LinearState<BasicAgent>(agent, varList));
		ExperienceRecord<BasicAgent> nextER = new ExperienceRecord<BasicAgent>(agent, new LinearState<BasicAgent>(agent, varList), BasicActions.FARM.getAction(agent), possibleActions, localProp);
		er.updateNextActions(nextER);
		nextER.updateWithResults(0.0, new LinearState<BasicAgent>(agent, varList));
		assertEquals(er.getReward()[0], 30.0, 0.001);
		assertEquals(nextER.getReward()[0], 0.0, 0.001);
		agent.addHealth(-2.0);
		nextER.updateWithFinalScores(new double[] {agent.getScore()});
		assertEquals(nextER.getReward()[0], -2.0, 0.001);
		assertEquals(er.getReward()[0], -2.0 * 0.9 + 30.0, 0.001);
	}
	
	@Test
	public void monteCarloRewardsWithoutIncremental() {
		localProp.setProperty("MonteCarloReward", "true");
		localProp.setProperty("IncrementalScoreReward", "false");
		localProp.setProperty("Gamma", "0.9");

		er = new ExperienceRecord<BasicAgent>(agent, new LinearState<BasicAgent>(agent, varList), BasicActions.FARM.getAction(agent), possibleActions, localProp);
		er.updateWithResults(3.0, new LinearState<BasicAgent>(agent, varList));
		ExperienceRecord<BasicAgent> nextER = new ExperienceRecord<BasicAgent>(agent, new LinearState<BasicAgent>(agent, varList), BasicActions.FARM.getAction(agent), possibleActions, localProp);
		er.updateNextActions(nextER);
		nextER.updateWithResults(0.0, new LinearState<BasicAgent>(agent, varList));
		assertEquals(er.getReward()[0], 3.0, 0.001);
		assertEquals(nextER.getReward()[0], 0.0, 0.001);
		agent.addHealth(-2.0);
		nextER.updateWithFinalScores(new double[] {agent.getScore()});
		assertEquals(nextER.getReward()[0], 18.0, 0.001);
		assertEquals(er.getReward()[0], 18.0 * 0.9 + 3.0, 0.001);
	}
	
	@Test
	public void discountFactorDefault() {
		// First set World time
		w.setCurrentTime(500l);
		er = new ExperienceRecord<BasicAgent>(agent, new LinearState<BasicAgent>(agent, varList), BasicActions.FARM.getAction(agent), possibleActions, localProp);
		assertEquals(er.getDiscountPeriod(), 0.0, 0.001);
		w.setCurrentTime(700l);
		er.updateWithResults(3.0, new LinearState<BasicAgent>(agent, varList));
		assertEquals(er.getDiscountPeriod(), 200.0 / 1000.0, 0.001);
		w.setCurrentTime(1200l);
		ExperienceRecord<BasicAgent> nextER = new ExperienceRecord<BasicAgent>(agent, new LinearState<BasicAgent>(agent, varList), BasicActions.FARM.getAction(agent), possibleActions, localProp);
		er.updateNextActions(nextER);
		
		assertEquals(er.getDiscountPeriod(), 700.0 / 1000.0, 0.001);
	}
	
	@Test
	public void discountFactorWithLookahead() {
		localProp.setProperty("TimePeriodForGamma", "800");
		// First set World time
		w.setCurrentTime(500l);
		LinearStateTestLookahead lookaheadState = new LinearStateTestLookahead(new LinearState<BasicAgent>(agent, varList));
		ExperienceRecord<BasicAgent> er = new ExperienceRecord<BasicAgent>(agent, lookaheadState, 
				BasicActions.FARM.getAction(agent), possibleActions, localProp);
		assertEquals(er.getDiscountPeriod(), 0.0, 0.001);
		w.setCurrentTime(700l);
		er.updateWithResults(3.0, new LinearStateTestLookahead(new LinearState<BasicAgent>(agent, varList)));
		assertEquals(er.getDiscountPeriod(), 200.0 / 800.0, 0.001);
		w.setCurrentTime(1200l);
		ExperienceRecord<BasicAgent> nextER = new ExperienceRecord<BasicAgent>(agent, 
				new LinearStateTestLookahead(new LinearState<BasicAgent>(agent, varList)), 
				BasicActions.FARM.getAction(agent), 
				possibleActions, localProp);
		er.updateNextActions(nextER);
		
		assertEquals(er.getDiscountPeriod(), 700.0 / 800.0, 0.001);
		
		w.setCurrentTime(3000l);

		assertEquals(er.getDiscountPeriod(), 700.0 / 800.0, 0.001);
	}
	
}
