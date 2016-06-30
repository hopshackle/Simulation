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
	private List<GeneticVariable> variables;

	public static GeneticVariable gold = GeneralQDeciderTest.gold;
	public static GeneticVariable constantTerm = GeneralQDeciderTest.constantTerm;
	
	@Before
	public void setup() {
		SimProperties.setProperty("Gamma", "0.90");
		SimProperties.setProperty("Alpha", "0.20");
		SimProperties.setProperty("QTraceLambda", "0.50");
		SimProperties.setProperty("QTraceMaximum", "2.0");
		ExperienceRecord.refreshProperties();
		actions = new ArrayList<ActionEnum<Agent>>();
		actions.add(RightLeft.RIGHT);
		actions.add(RightLeft.LEFT);

		variables = new ArrayList<GeneticVariable>();
		variables.add(constantTerm);
		variables.add(gold);

		decider = new TestLinearQDecider(actions, variables);
		decider.setWeights(new double[3][2]);
		// so we have two actions, right and left
		// and two inputs, one is just equal to the Agents Gold, and the other is equal to a constant
		World w = new World();
		testAgent = new BasicAgent(w);
	}

	@Test
	public void teachingDecisionUpdatesWeights() {
		ExperienceRecord<Agent> exp = new ExperienceRecord<Agent>(testAgent, variables, decider.getCurrentState(testAgent, testAgent, null), RightLeft.RIGHT.getAction(testAgent), 
				decider.getChooseableOptions(testAgent, testAgent));
		exp.updateWithResults(2.0, decider.getCurrentState(testAgent, testAgent, null));
		ExperienceRecord<Agent> exp2 = new ExperienceRecord<Agent>(testAgent, variables, decider.getCurrentState(testAgent, testAgent, null), RightLeft.LEFT.getAction(testAgent), 
				decider.getChooseableOptions(testAgent, testAgent));
		exp.updateNextActions(exp2);
		decider.learnFrom(exp, 10.0);
		// reward of 2.0 versus an expected 0.0
		// we have no information, so both actions give an expectation of 0.0
		// so actual difference is 2.0 + gamma * 0.0 - 0.0 = 2.0
		assertEquals(decider.getWeightOf(gold, RightLeft.RIGHT), 0.0, 0.001);
		assertEquals(decider.getWeightOf(gold, RightLeft.LEFT), 0.0, 0.001);
		assertEquals(decider.getWeightOf(constantTerm, RightLeft.RIGHT), 0.4, 0.001);
		assertEquals(decider.getWeightOf(constantTerm, RightLeft.LEFT), 0.0, 0.001);
		
		testAgent.addGold(-2.0);
		exp2.updateWithResults(-2.0, decider.getCurrentState(testAgent, testAgent, null));
		ExperienceRecord<Agent> exp3 = new ExperienceRecord<Agent>(testAgent, variables, decider.getCurrentState(testAgent, testAgent, null), RightLeft.RIGHT.getAction(testAgent), 
				decider.getChooseableOptions(testAgent, testAgent));
		exp2.updateNextActions(exp3);
		decider.learnFrom(exp2, 10.0);
		// prediction would be 0.0 from starting State
		// reward of -2.0. Value of left is 0.0, and value of right is 0.6. So max is 0.4.
		// so difference = -2.0 + gamma * 0.40 - 0.00 = -1.64
		// For weight update, we have 1.45 (1.0 * gamma and lambda) for Constant term, and 0.0 for gold term
		assertEquals(decider.getWeightOf(gold, RightLeft.RIGHT), 0.0, 0.001);
		assertEquals(decider.getWeightOf(gold, RightLeft.LEFT), 0.0, 0.001);
		assertEquals(decider.getWeightOf(constantTerm, RightLeft.RIGHT), 0.4, 0.001);
		assertEquals(decider.getWeightOf(constantTerm, RightLeft.LEFT), -0.328 * 1.45, 0.001);
		
		testAgent.addGold(1.0);
		exp3.updateWithResults(1.0, decider.getCurrentState(testAgent, testAgent, null));
		ExperienceRecord<Agent> exp4 = new ExperienceRecord<Agent>(testAgent, variables, decider.getCurrentState(testAgent, testAgent, null), RightLeft.RIGHT.getAction(testAgent), 
				decider.getChooseableOptions(testAgent, testAgent));
		exp3.updateNextActions(exp4);
		decider.learnFrom(exp3, 10.0);
		// prediction would be 0.4 from starting State
		// reward of 1.0. Value of left is -0.328, and value of right is 0.4. So max is 0.4
		// so difference = 1.0 + gamma * 0.4 - 0.40 = 0.96
		// For weight update, we have 1.6525 (1.00 + 1.45 * gamma and lambda) for Constant term, and -2.0 for gold term
		assertEquals(decider.getWeightOf(gold, RightLeft.RIGHT), -0.96 * 0.2 * 2.0, 0.001);
		assertEquals(decider.getWeightOf(gold, RightLeft.LEFT), 0.0, 0.001);
		assertEquals(decider.getWeightOf(constantTerm, RightLeft.RIGHT), 0.4 + 0.2 * 0.96 * 1.6525, 0.001);
		assertEquals(decider.getWeightOf(constantTerm, RightLeft.LEFT), -0.328 * 1.45, 0.001);
	}

}
