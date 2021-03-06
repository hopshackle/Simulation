package hopshackle.simulation.test;

import java.util.*;

import hopshackle.simulation.*;
import hopshackle.simulation.basic.BasicAgent;
import static org.junit.Assert.*;

import org.junit.*;

public class GeneralQDeciderTest {

	private TestLinearQDecider decider;
	private Agent testAgent;
	private List<ActionEnum<Agent>> actions;
	private List<GeneticVariable<Agent>> variables;
	private World w;
	DeciderProperties localProp;

	public static GeneticVariable<Agent> gold = new GeneticVariable<Agent>() {
		@Override
		public double getValue(Agent a1) {
			Agent agent = (Agent) a1;
			return agent.getGold();
		}
		@Override
		public String getDescriptor() {return "TEST";}
		@Override
		public boolean unitaryRange() {return false;}
	};

	public static GeneticVariable<Agent> constantTerm = new GeneticVariable<Agent>() {
		@Override
		public double getValue(Agent a1) {return 1.0;}
		@Override
		public String getDescriptor() {return "TEST";}
		@Override
		public boolean unitaryRange() {return false;}
	};

	@Before
	public void setup() {
		localProp = SimProperties.getDeciderProperties("GLOBAL");
		localProp.setProperty("Gamma", "0.90");
		localProp.setProperty("Alpha", "0.20");
		localProp.setProperty("Lambda", "0.00");
		localProp.setProperty("QTraceLambda", "0.00");
		localProp.setProperty("QTraceMaximum", "2.0");
		localProp.setProperty("TimePeriodForGamma", "1000");
		localProp.setProperty("MonteCarloReward", "false");
		actions = new ArrayList<ActionEnum<Agent>>();
		actions.add(RightLeft.RIGHT);
		actions.add(RightLeft.LEFT);

		variables = new ArrayList<GeneticVariable<Agent>>();
		variables.add(constantTerm);
		variables.add(gold);

		decider = new TestLinearQDecider(actions, variables);
		decider.injectProperties(localProp);
		for (ActionEnum<Agent> a : actions)
			decider.setWeights(a.toString(), new double[2]);
		// so we have two actions, right and left
		// and two inputs, one is just equal to the Agents Gold, and the other is equal to a constant
		w = new World();
		w.setCalendar(new FastCalendar(0l));
		testAgent = new BasicAgent(w);
	}

	@Test
	public void updateWeights() {
		assertEquals(decider.getWeightOf(1, RightLeft.RIGHT), 0.0, 0.001);
		assertEquals(decider.getWeightOf(1, RightLeft.LEFT), 0.0, 0.001);
		assertEquals(decider.getWeightOf(0, RightLeft.RIGHT), 0.0, 0.001);
		assertEquals(decider.getWeightOf(0, RightLeft.LEFT), 0.0, 0.001);
		decider.updateWeight(1, RightLeft.RIGHT, 0.2);
		decider.updateWeight(0, RightLeft.LEFT, -0.05);
		assertEquals(decider.getWeightOf(1, RightLeft.RIGHT), 0.2, 0.001);
		assertEquals(decider.getWeightOf(1, RightLeft.LEFT), 0.0, 0.001);
		assertEquals(decider.getWeightOf(0, RightLeft.RIGHT), 0.0, 0.001);
		assertEquals(decider.getWeightOf(0, RightLeft.LEFT), -0.05, 0.001);
	}

	@Test
	public void valueOption() {
		assertEquals(testAgent.getGold(), 0.0, 0.0001);
		assertEquals(decider.valueOption(RightLeft.RIGHT, testAgent), 0.0, 0.001);
		assertEquals(decider.valueOption(RightLeft.LEFT, testAgent), 0.0, 0.001);

		testAgent.addGold(0.5);
		assertEquals(decider.valueOption(RightLeft.RIGHT, testAgent), 0.0, 0.001);
		assertEquals(decider.valueOption(RightLeft.LEFT, testAgent), 0.0, 0.001);

		decider.updateWeight(1, RightLeft.RIGHT, 1.0);
		assertEquals(decider.valueOption(RightLeft.RIGHT, testAgent), 0.5, 0.001);
		assertEquals(decider.valueOption(RightLeft.LEFT, testAgent), 0.0, 0.001);
	}

	@Test
	public void teachingDecisionUpdatesWeights() {
		ExperienceRecord<Agent> exp = new ExperienceRecord<Agent>(testAgent, decider.getCurrentState(testAgent), RightLeft.RIGHT.getAction(testAgent), 
				actions, localProp);
		exp.updateWithResults(2.0);
		w.setCurrentTime(1000l); // move forward before taking next action, so that discounting works
		ExperienceRecord<Agent> exp2 = new ExperienceRecord<Agent>(testAgent, decider.getCurrentState(testAgent), RightLeft.LEFT.getAction(testAgent), 
				actions, localProp);
		exp.updateNextActions(exp2);
		decider.learnFrom(exp, 10.0);
		// reward of 2.0 versus an expected 0.0
		// we have no information, so both actions give an expectation of 0.0
		// so actual difference is 2.0 + gamma * 0.0 - 0.0 = 2.0
		assertEquals(decider.getWeightOf(1, RightLeft.RIGHT), 0.0, 0.001);
		assertEquals(decider.getWeightOf(1, RightLeft.LEFT), 0.0, 0.001);
		assertEquals(decider.getWeightOf(0, RightLeft.RIGHT), 0.4, 0.001);
		assertEquals(decider.getWeightOf(0, RightLeft.LEFT), 0.0, 0.001);

		testAgent.addGold(-2.0);
		exp2.updateWithResults(-2.0);
		ExperienceRecord<Agent> exp3 = new ExperienceRecord<Agent>(testAgent, decider.getCurrentState(testAgent), RightLeft.RIGHT.getAction(testAgent), 
				actions, localProp);
		w.setCurrentTime(2000l); // move forward before taking next action, so that discounting works
		exp2.updateNextActions(exp3);
		decider.learnFrom(exp2, 10.0);
		// prediction would be 0.0 from starting State
		// reward of -2.0. Value of left is 0.0, and value of right is 0.4. So max is 0.4.
		// so difference = -2.0 + gamma * 0.40 - 0.00 = -1.64
		assertEquals(exp2.getReward()[0], -2.0, 0.001);
		assertEquals(decider.getWeightOf(1, RightLeft.RIGHT), 0.0, 0.001);
		assertEquals(decider.getWeightOf(1, RightLeft.LEFT), 0.0, 0.001);
		assertEquals(decider.getWeightOf(0, RightLeft.RIGHT), 0.4, 0.001);
		assertEquals(decider.getWeightOf(0, RightLeft.LEFT), -0.328, 0.001);

		testAgent.addGold(1.0);
		exp3.updateWithResults(1.0);
		ExperienceRecord<Agent> exp4 = new ExperienceRecord<Agent>(testAgent, decider.getCurrentState(testAgent), RightLeft.RIGHT.getAction(testAgent), 
				actions, localProp);
		exp3.updateNextActions(exp4);
		decider.learnFrom(exp3, 10.0);
		// prediction would be 0.4 from starting State
		// reward of 1.0. Value of left is -0.328, and value of right is 0.4. So max is 0.4
		// so difference = 1.0 + gamma * 0.4 - 0.40 = 0.96
		assertEquals(decider.getWeightOf(1, RightLeft.RIGHT), -0.96 * 0.2 * 2.0, 0.001);
		assertEquals(decider.getWeightOf(1, RightLeft.LEFT), 0.0, 0.001);
		assertEquals(decider.getWeightOf(0, RightLeft.RIGHT), 0.4 + 0.2 * 0.96, 0.001);
		assertEquals(decider.getWeightOf(0, RightLeft.LEFT), -0.328, 0.001);
	}

	@Test
	public void teachingDecisionWithNoDifferenceDoesNotUpdateWeights() {
		ExperienceRecord<Agent> exp = new ExperienceRecord<Agent>(testAgent, decider.getCurrentState(testAgent), RightLeft.RIGHT.getAction(testAgent), 
				actions, localProp);
		ExperienceRecord<Agent> exp2 = new ExperienceRecord<Agent>(testAgent, decider.getCurrentState(testAgent), RightLeft.RIGHT.getAction(testAgent), 
				actions, localProp);
		exp.updateWithResults(0.0);
		exp.updateNextActions(exp2);
		decider.learnFrom(exp, 10.0);
		// reward of 0.0, with value of best action = 0.0, so difference = 0.0 + gamma * 0.0 - 0.0 = 0.00
		assertEquals(decider.getWeightOf(1, RightLeft.RIGHT), 0.0, 0.001);
		assertEquals(decider.getWeightOf(1, RightLeft.LEFT), 0.0, 0.001);
		assertEquals(decider.getWeightOf(0, RightLeft.RIGHT), 0.0, 0.001);
		assertEquals(decider.getWeightOf(0, RightLeft.LEFT), 0.0, 0.001);
	}

	@Test
	public void valuingAnUnknownOptionWorks() {
		assertEquals(decider.valueOption(RightLeft.DITHER, testAgent), 1.0, 0.001);
		assertEquals(decider.valueOption(RightLeft.RIGHT, testAgent), 0.0, 0.001);
		testAgent.addGold(1.5);
		assertEquals(decider.valueOption(RightLeft.DITHER, testAgent), 2.5, 0.001);
		assertEquals(decider.valueOption(RightLeft.RIGHT, testAgent), 0.0, 0.001);
	}
}



class TestLinearQDecider extends GeneralLinearQDecider<Agent> {

	public TestLinearQDecider(List<ActionEnum<Agent>> actions, List<GeneticVariable<Agent>> variables) {
		super(new LinearStateFactory<Agent>(variables));
	}

	@Override
	public void setWeights(String a, double[] w) {
		super.setWeights(a, w);
	}

}

class TestAction extends Action<Agent> {
	public TestAction(ActionEnum<Agent> type, Agent a, boolean recordAction) {
		super(type, a, recordAction);

	}
}

enum RightLeft implements ActionEnum<Agent> {

	RIGHT,
	LEFT,
	DITHER;

	@Override
	public Action<Agent> getAction(Agent a) {
		return new TestAction(this, a, false);
	}

	@Override
	public boolean isChooseable(Agent a) {
		return true;
	}

	@Override
	public Enum<RightLeft> getEnum() {
		return this;
	}
}