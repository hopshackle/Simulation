package hopshackle.simulation.test;

import java.util.*;

import hopshackle.simulation.*;
import hopshackle.simulation.basic.BasicAgent;
import static org.junit.Assert.*;

import org.junit.*;

public class GeneralQLambdaTest {

	private TestLinearQDecider decider;
	private Agent testAgent;
	private List<ActionEnum<Agent>> actions;
	private List<GeneticVariable<Agent>> variables;
	private World w;
	DeciderProperties localProp;

	public static GeneticVariable<Agent> gold = GeneralQDeciderTest.gold;
	public static GeneticVariable<Agent> constantTerm = GeneralQDeciderTest.constantTerm;

	@Before
	public void setup() {
		localProp = SimProperties.getDeciderProperties("GLOBAL");
		localProp.setProperty("Gamma", "0.90");
		localProp.setProperty("Alpha", "0.20");
		localProp.setProperty("Lambda", "0.00");
		localProp.setProperty("QTraceLambda", "0.50");
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
			decider.setWeights(a, new double[2]);
		// so we have two actions, right and left
		// and two inputs, one is just equal to the Agents Gold, and the other is equal to a constant
		w = new World();
		w.setCalendar(new FastCalendar(0l));
		testAgent = new BasicAgent(w);
	}

	@Test
	public void teachingDecisionUpdatesWeights() {
		ExperienceRecord<Agent> exp = new ExperienceRecord<Agent>(testAgent, decider.getCurrentState(testAgent), RightLeft.RIGHT.getAction(testAgent), 
				actions, localProp);
		exp.updateWithResults(2.0);
		// Feature traces of exp are 1.0 for Constant, and 0.0 for Gold
		assertEquals(exp.getFeatureTrace()[0], 1.00, 0.001);
		w.setCurrentTime(1000l); // move forward before taking next action, so that discounting works
		ExperienceRecord<Agent> exp2 = new ExperienceRecord<Agent>(testAgent, decider.getCurrentState(testAgent), RightLeft.LEFT.getAction(testAgent), 
				actions, localProp);
		exp.updateNextActions(exp2);
		decider.learnFrom(exp, 10.0);
		// reward of 2.0 versus an expected 0.0
		// we have no information, so both actions give an expectation of 0.0
		// so actual difference is 2.0 + gamma * 0.0 - 0.0 = 2.0
		// FeatureTraces for exp2 are 1.45 (1.0 + 1.0 * gamma * lambda) for constant, and 0.0 for gold
		assertEquals(exp2.getFeatureTrace()[0], 1.45, 0.001);
		assertEquals(decider.getWeightOf(1, RightLeft.RIGHT), 0.0, 0.001);
		assertEquals(decider.getWeightOf(1, RightLeft.LEFT), 0.0, 0.001);
		assertEquals(decider.getWeightOf(0, RightLeft.RIGHT), 0.4, 0.001);
		assertEquals(decider.getWeightOf(0, RightLeft.LEFT), 0.0, 0.001);

		testAgent.addGold(-2.0);
		exp2.updateWithResults(-2.0);
		w.setCurrentTime(3000l); // move forward before taking next action, so that discounting works
		ExperienceRecord<Agent> exp3 = new ExperienceRecord<Agent>(testAgent, decider.getCurrentState(testAgent), RightLeft.RIGHT.getAction(testAgent), 
				actions, localProp);
		exp2.updateNextActions(exp3);
		// New feature traces for constant is 1.293625 (1.0 + 1.45 * gamma * gamma * lambda * lambda)
		assertEquals(exp3.getFeatureTrace()[0], 1.293625, 0.001);
		assertEquals(exp3.getFeatureTrace()[1], -2.0, 0.001);
		decider.learnFrom(exp2, 10.0);
		// Note that we moved the time forward by 2000 before creating exp3, so we discount over two periods
		// prediction would be 0.0 from starting State
		// reward of -2.0. Value of left is 0.0, and value of right is 0.6. So max is 0.4.
		// so difference = -2.0 + gamma * gamma * 0.40 - 0.00 = -1.676
		// Hence alpha * difference = -0.3352
		assertEquals(decider.getWeightOf(1, RightLeft.RIGHT), 0.0, 0.001);
		assertEquals(decider.getWeightOf(1, RightLeft.LEFT), 0.0, 0.001);
		assertEquals(decider.getWeightOf(0, RightLeft.RIGHT), 0.4, 0.001);
		assertEquals(decider.getWeightOf(0, RightLeft.LEFT), -0.3352 * 1.45, 0.001);

		testAgent.addGold(1.0);
		exp3.updateWithResults(1.0);
		w.setCurrentTime(4500l); // move forward before taking next action, so that discounting works
		ExperienceRecord<Agent> exp4 = new ExperienceRecord<Agent>(testAgent, decider.getCurrentState(testAgent), RightLeft.RIGHT.getAction(testAgent), 
				actions, localProp);
		exp3.updateNextActions(exp4);
		// Constant = 1.0 + 1.293625 * (lambda * gamma) ^ 1.5 = 1.3905055
		// Gold = -1.0 + (-2.0) * (lambda * gamma) ^ 1.5 = -1.603738354
		assertEquals(exp4.getFeatureTrace()[0], 1.3905055, 0.001);
		assertEquals(exp4.getFeatureTrace()[1], -1.603738354, 0.001);
		decider.learnFrom(exp3, 10.0);
		// prediction would be 0.4 from starting State
		// reward of 1.0. Value of left is -0.48604, and value of right is 0.4. So max is 0.4
		// so difference = 1.0 + gamma^1.5 * 0.4 - 0.40 = 0.941526
		assertEquals(decider.getWeightOf(1, RightLeft.RIGHT), -0.941526 * 0.2 * 2.0, 0.001);
		assertEquals(decider.getWeightOf(1, RightLeft.LEFT), 0.0, 0.001);
		assertEquals(decider.getWeightOf(0, RightLeft.RIGHT), 0.4 + 0.2 * 0.941526 * 1.293625, 0.001);
		assertEquals(decider.getWeightOf(0, RightLeft.LEFT), -0.3352 * 1.45, 0.001);
	}

}
