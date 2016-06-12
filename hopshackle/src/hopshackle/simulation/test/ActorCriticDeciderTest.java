package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;

import java.util.ArrayList;

import org.junit.*;
public class ActorCriticDeciderTest {

	private ActorCriticDecider<BasicAgent> decider;
	private BasicAgent agent;
	private World world;
	private ArrayList<ActionEnum<BasicAgent>> actions = new ArrayList<ActionEnum<BasicAgent>>();
	private ArrayList<GeneticVariable> variables = new ArrayList<GeneticVariable>();
	private AgentTeacher<BasicAgent> agentTeacher = new AgentTeacher<BasicAgent>();
	private HexMap<BasicHex> hexMap;
	private BasicHex plainsHex;

	@Before
	public void setup() {
		SimProperties.setProperty("NeuralNoise", "0.0");
		SimProperties.setProperty("NeuralLearningCoefficient", "0.20");
		world = new World();
		agent = new BasicAgent(world);
		actions.add(BasicActions.FORAGE);
		actions.add(BasicActions.REST);
		variables.add(BasicVariables.HEALTH);
		variables.add(BasicVariables.FOOD_LEVEL);

		decider = new ActorCriticDecider<BasicAgent>(actions, variables);
		agent.setDecider(decider);
		decider.setTeacher(agentTeacher);

		hexMap = new HexMap<BasicHex>(5, 5, BasicHex.getHexFactory());
		world.setLocationMap(hexMap);
		plainsHex = hexMap.getHexAt(0, 0);
		agent.setLocation(plainsHex);
	}

	@Test
	public void stateBrainIsTaughtAsWellAsMainBrain() {
		agent.maintenance();	// essential to set lastMaintainedTime for later results
		double startValue = decider.valueState(agent);
		double forageValue = decider.valueOption(BasicActions.FORAGE, agent, agent);
		double restValue = decider.valueOption(BasicActions.REST, agent, agent);

		Action<BasicAgent> actionTaken = decider.decide(agent, agent);
		ActionEnum<BasicAgent> decisionTaken = actionTaken.getType();
		ActionEnum<BasicAgent> decisionNotTaken = BasicActions.REST;
		double decisionTakenValue = forageValue;
		double decisionNotTakenValue = restValue; 
		if (decisionTaken == BasicActions.REST) {
			decisionNotTaken = BasicActions.FORAGE;
			decisionTakenValue = restValue;
			decisionNotTakenValue = forageValue;
		}

		ExperienceRecord<BasicAgent> exp = agentTeacher.getExperienceRecords(agent).get(0);
		exp.updateWithResults(1.0, decider.getCurrentState(agent, agent));
		exp.updateNextActions(actions);
		decider.learnFrom(exp, 1.0);
		
		double laterValue = decider.valueState(agent);
		assertTrue(laterValue > startValue);
		double updatedDValue = decider.valueOption(decisionTaken, agent, agent);
		assertTrue(updatedDValue > decisionTakenValue);
		double updatedNonDValue = decider.valueOption(decisionNotTaken, agent, agent);
		assertEquals(updatedNonDValue, decisionNotTakenValue, 0.00001);
		
		startValue = decider.valueState(agent);
		forageValue = decider.valueOption(BasicActions.FORAGE, agent, agent);
		restValue = decider.valueOption(BasicActions.REST, agent, agent);
		if (decisionTaken == BasicActions.REST) {
			decisionTakenValue = restValue;
			decisionNotTakenValue = forageValue;
		} else {
			decisionTakenValue = forageValue;
			decisionNotTakenValue = restValue;
		}
	//	decisionTaken = decider.decide(agent, agent).getType();
		actionTaken.cancel();	// this will make a follow-up decision - but without changing the state
								// It will also invoke update results on the AgentTeacher ... but there are no 
								// open ExperienceRecords, so this should have no effect.
								// Well...it does call maintenance() via doCleanUp(), which is
								// why we call maintenance() at the start of this test!
								// God this is kludgy.
		exp = agentTeacher.getExperienceRecords(agent).get(0);
		exp.updateWithResults(0.0, decider.getCurrentState(agent, agent));
		exp.updateNextActions(actions);
		decider.learnFrom(exp, 1.0);
		laterValue = decider.valueState(agent);
		updatedNonDValue = decider.valueOption(decisionNotTaken, agent, agent);
		updatedDValue = decider.valueOption(decisionTaken, agent, agent);
		assertTrue(startValue - laterValue > 0.00);
		assertTrue(updatedDValue < decisionTakenValue);
		assertEquals(updatedNonDValue, decisionNotTakenValue, 0.00001);
	}

}
