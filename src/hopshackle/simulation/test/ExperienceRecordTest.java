package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;

import java.util.*;

import org.junit.*;

public class ExperienceRecordTest {

	private List<GeneticVariable<BasicAgent>> varList = new ArrayList<GeneticVariable<BasicAgent>>();
	private ExperienceRecord<BasicAgent> er, er2;
	private BasicAgent agent;
	private World w;
	private BasicHex loc;
	private List<ActionEnum<BasicAgent>> possibleActions;
	StateFactory<BasicAgent> stateFactory;
	DeciderProperties localProp;
	Decider <BasicAgent> basicDecider;

	@Before
	public void setUp() throws Exception {
		localProp = SimProperties.getDeciderProperties("GLOBAL");
		localProp.setProperty("IncrementalScoreReward", "true");
		localProp.setProperty("TimePeriodForGamma", "1000");
		for (GeneticVariable<BasicAgent> gv : BasicVariables.values())
			varList.add(gv);
		stateFactory = new LinearStateFactory<BasicAgent>(varList);
		w = new World();
		w.setCalendar(new FastCalendar(0l));
		agent = new BasicAgent(w);
		loc = new BasicHex(0, 0);
		loc.setParentLocation(w);
		agent.setLocation(loc);

		possibleActions = new ArrayList<ActionEnum<BasicAgent>>();
		possibleActions.add(BasicActions.FARM);
		possibleActions.add(BasicActions.FIND_UNKNOWN);

		basicDecider = new GeneralLinearQDecider<BasicAgent>(stateFactory);
		basicDecider.injectProperties(localProp);
		agent.setDecider(basicDecider);
	}

	@Test
	public void experienceRecordInitialisation() {
		er = new ExperienceRecord<BasicAgent>(agent, new LinearState<BasicAgent>(agent, varList), BasicActions.FARM.getAction(agent), possibleActions ,localProp);
		assertFalse(er.isInFinalState());
		assertTrue(er.getEndState() == null);
		assertTrue(er.getActionTaken().getType() == BasicActions.FARM);
		assertEquals(er.getReward()[0], 0.0, 0.01);
		assertTrue(er.getPossibleActionsFromEndState().isEmpty());
		assertEquals(er.getPossibleActionsFromStartState().size(), 2);

		possibleActions.add(BasicActions.BREED);		

		er.updateWithResults(30.0);
		assertEquals(agent.getScore(), 20.0, 0.001);
		agent.addHealth(-15.0);
		assertEquals(er.getReward()[0], 30.0, 0.01);
		assertEquals(agent.getScore(), 5.0, 0.001);
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
		w.setCurrentTime(0l);
		er = new ExperienceRecord<BasicAgent>(agent, new LinearState<BasicAgent>(agent, varList), BasicActions.FARM.getAction(agent), possibleActions ,localProp);
		w.setCurrentTime(500l);
		er.updateWithResults(30.0);
		ExperienceRecord<BasicAgent> nextER = new ExperienceRecord<BasicAgent>(agent, new LinearState<BasicAgent>(agent, varList), BasicActions.FARM.getAction(agent), possibleActions, localProp);
		w.setCurrentTime(2000l);
		er.updateNextActions(nextER);
		nextER.updateWithResults(0.0);
		w.setCurrentTime(5000l);
		assertEquals(er.getMonteCarloReward()[0], 30.0, 0.001);
		assertEquals(nextER.getMonteCarloReward()[0], 0.0, 0.001);
		agent.addHealth(-2.0);
		w.setCurrentTime(7500l);
		nextER.updateWithFinalScores(new double[] {agent.getScore()});
		assertEquals(nextER.getMonteCarloReward()[0], -2.0, 0.001);
		assertEquals(nextER.getDiscountPeriod(), 7.0, 0.01);
		assertEquals(er.getMonteCarloReward()[0], -2.0 * Math.pow(0.9, 7) + 30.0, 0.001);
	}

	@Test
	public void monteCarloRewardsWithoutIncremental() {
		localProp.setProperty("MonteCarloReward", "true");
		localProp.setProperty("IncrementalScoreReward", "false");
		localProp.setProperty("Gamma", "0.9");

		w.setCurrentTime(0l);

		er = new ExperienceRecord<BasicAgent>(agent, new LinearState<BasicAgent>(agent, varList), BasicActions.FARM.getAction(agent), possibleActions, localProp);
		er.updateWithResults(3.0);

		w.setCurrentTime(1000l);
		ExperienceRecord<BasicAgent> nextER = new ExperienceRecord<BasicAgent>(agent, new LinearState<BasicAgent>(agent, varList), BasicActions.FARM.getAction(agent), possibleActions, localProp);
		er.updateNextActions(nextER);
		nextER.updateWithResults(0.0);
		assertEquals(er.getMonteCarloReward()[0], 3.0, 0.001);
		assertEquals(nextER.getMonteCarloReward()[0], 0.0, 0.001);
		agent.addHealth(-2.0);
		w.setCurrentTime(4000l);
		nextER.updateWithFinalScores(new double[] {agent.getScore()});
		assertEquals(nextER.getMonteCarloReward()[0], 18.0, 0.001);
		assertEquals(nextER.getDiscountPeriod(), 3.0, 0.01);
		assertEquals(er.getMonteCarloReward()[0], 18.0 * Math.pow(0.9, 3) + 3.0, 0.001);
	}

	@Test
	public void discountFactorDefault() {
		// First set World time
		w.setCurrentTime(500l);
		er = new ExperienceRecord<BasicAgent>(agent, new LinearState<BasicAgent>(agent, varList), BasicActions.FARM.getAction(agent), possibleActions, localProp);
		assertEquals(er.getDiscountPeriod(), 0.0, 0.001);
		w.setCurrentTime(700l);
		er.updateWithResults(3.0);
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
		er.updateWithResults(3.0);
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

	@Test public void endStateIsAlwaysStartStateOfNextRecordUnlessFinalState() {
		er = new ExperienceRecord<BasicAgent>(agent, new LinearState<BasicAgent>(agent, varList), BasicActions.FARM.getAction(agent), possibleActions, localProp);
		State<BasicAgent> state1 = stateFactory.getCurrentState(agent);
		er2 = new ExperienceRecord<BasicAgent>(agent, state1, BasicActions.FORAGE.getAction(agent), possibleActions, localProp);

		agent.addHealth(-5.0);
		State<BasicAgent> state2 = stateFactory.getCurrentState(agent);
		er.updateWithResults(2.0);
		er.updateNextActions(er2);

		assertFalse(state1.toString().equals(state2.toString()));
		assertTrue(er.getEndState().toString().equals(state1.toString()));

		er.updateWithFinalScores(new double[1]);
		assertTrue(er.getEndState().toString().equals(state2.toString()));
	}

	@Test
	public void endStateCorrectWithDifferentAgent() {
		er = new ExperienceRecord<BasicAgent>(agent, new LinearState<BasicAgent>(agent, varList), BasicActions.FARM.getAction(agent), possibleActions, localProp);
		BasicAgent otherAgent = new BasicAgent(w);
		BasicHex otherLoc = new BasicHex(1, 1);
		otherLoc.setTerrain(TerrainType.FOREST);
		otherLoc.setParentLocation(w);
		otherAgent.setLocation(otherLoc);
		State<BasicAgent> state1 = stateFactory.getCurrentState(otherAgent);
		er2 = new ExperienceRecord<BasicAgent>(otherAgent, state1, BasicActions.FORAGE.getAction(agent), possibleActions, localProp);

		agent.addHealth(-5.0);
		State<BasicAgent> state2 = stateFactory.getCurrentState(agent);
		er.updateWithResults(2.0);
		er.updateNextActions(er2);

		assertFalse(state1.toString().equals(state2.toString()));
		assertTrue(er.getEndState().toString().equals(state1.toString()));

		er.updateWithFinalScores(new double[1]);
		assertTrue(er.getEndState().toString().equals(state2.toString()));
	}

}
