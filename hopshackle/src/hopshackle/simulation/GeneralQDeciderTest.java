package hopshackle.simulation;

import java.util.*;

import hopshackle.simulation.basic.BasicAgent;
import static org.junit.Assert.*;
import org.junit.*;

public class GeneralQDeciderTest {

	private GeneralLinearQDecider decider;
	private Agent testAgent;
	private List<ActionEnum> actions;
	
	public static ActionEnum right = new ActionEnum() {
		@Override
		public boolean isChooseable(Agent a) {return true;}
		@Override
		public Enum getEnum() {return null;}
		@Override
		public String getChromosomeDesc() {return "T";}		
		@Override
		public Action getAction(Agent a1, Agent a2) {return getAction(a1);}
		@Override
		public Action getAction(Agent a) {return null;}
	};
	public static ActionEnum left = new ActionEnum() {
		@Override
		public boolean isChooseable(Agent a) {return true;}
		@Override
		public Enum getEnum() {return null;}
		@Override
		public String getChromosomeDesc() {return "T";}		
		@Override
		public Action getAction(Agent a1, Agent a2) {return getAction(a1);}
		@Override
		public Action getAction(Agent a) {return null;}
	};

	public static GeneticVariable gold = new GeneticVariable() {
		@Override
		public double getValue(Object a, double var) {return getValue(a, a);}
		@Override
		public double getValue(Object a1, Object a2) {
			Agent agent = (Agent) a1;
			return agent.getGold();
		}
		@Override
		public String getDescriptor() {return null;}
		@Override
		public boolean unitaryRange() {return false;}
	};

	public static GeneticVariable constantTerm = new GeneticVariable() {
		@Override
		public double getValue(Object a, double var) {return getValue(a, a);}
		@Override
		public double getValue(Object a1, Object a2) {return 1.0;}
		@Override
		public String getDescriptor() {return null;}
		@Override
		public boolean unitaryRange() {return false;}
	};
	
	@Before
	public void setup() {
		SimProperties.setProperty("Gamma", "0.90");
		SimProperties.setProperty("Alpha", "0.20");
		actions = new ArrayList<ActionEnum>();
		actions.add(right);
		actions.add(left);

		List<GeneticVariable> variables = new ArrayList<GeneticVariable>();
		variables.add(constantTerm);
		variables.add(gold);

		decider = new GeneralLinearQDecider(actions, variables);
		decider.setWeights(new double[3][2]);
		// so we have two actions, right and left
		// and two inputs, one is just equal to the Agents Gold, and the other is equal to a constant
		World w = new World();
		testAgent = new BasicAgent(w);
	}

	@Test
	public void updateWeights() {
		assertEquals(decider.getWeightOf(gold, right), 0.0, 0.001);
		assertEquals(decider.getWeightOf(gold, left), 0.0, 0.001);
		assertEquals(decider.getWeightOf(constantTerm, right), 0.0, 0.001);
		assertEquals(decider.getWeightOf(constantTerm, left), 0.0, 0.001);
		decider.updateWeight(gold, right, 0.2);
		decider.updateWeight(constantTerm, left, -0.05);
		assertEquals(decider.getWeightOf(gold, right), 0.2, 0.001);
		assertEquals(decider.getWeightOf(gold, left), 0.0, 0.001);
		assertEquals(decider.getWeightOf(constantTerm, right), 0.0, 0.001);
		assertEquals(decider.getWeightOf(constantTerm, left), -0.05, 0.001);
	}

	@Test
	public void valueOption() {
		assertEquals(testAgent.getGold(), 0.0, 0.0001);
		assertEquals(decider.valueOption(right, testAgent, testAgent), 0.0, 0.001);
		assertEquals(decider.valueOption(left, testAgent, testAgent), 0.0, 0.001);
		
		testAgent.addGold(0.5);
		assertEquals(decider.valueOption(right, testAgent, testAgent), 0.0, 0.001);
		assertEquals(decider.valueOption(left, testAgent, testAgent), 0.0, 0.001);
		
		decider.updateWeight(gold, right, 1.0);
		assertEquals(decider.valueOption(right, testAgent, testAgent), 1.0, 0.001);
		assertEquals(decider.valueOption(left, testAgent, testAgent), 0.5, 0.001);
	}

	@Test
	public void teachingDecisionUpdatesWeights() {
		ExperienceRecord exp = decider.getExperienceRecord(testAgent, testAgent, right.getAction(testAgent));
		exp.updateWithResults(2.0, decider.getCurrentState(testAgent, testAgent));
		exp.updateNextActions(actions);
		decider.learnFrom(exp, 10.0);
		// reward of 2.0 versus an expected 0.0
		// we have no information, so both actions give an expectation of 0.0
		// so actual difference is 2.0 + gamma * 0.0 - 0.0 = 2.0
		assertEquals(decider.getWeightOf(gold, right), 0.0, 0.001);
		assertEquals(decider.getWeightOf(gold, left), 0.0, 0.001);
		assertEquals(decider.getWeightOf(constantTerm, right), 0.4, 0.001);
		assertEquals(decider.getWeightOf(constantTerm, left), 0.0, 0.001);
		
		exp = decider.getExperienceRecord(testAgent, testAgent, left.getAction(testAgent));
		testAgent.addGold(-2.0);
		exp.updateWithResults(-2.0, decider.getCurrentState(testAgent, testAgent));
		exp.updateNextActions(actions);
		decider.learnFrom(exp, 10.0);
		// prediction would be 0.4
		// reward of -2.0. Value of left is 0.4, and value of right is 0.8. So max is 0.8.
		// so difference = -2.0 + gamma * 0.80 - 0.40 = -1.68
		assertEquals(decider.getWeightOf(gold, right), 0.0, 0.001);
		assertEquals(decider.getWeightOf(gold, left), 0.0, 0.001);
		assertEquals(decider.getWeightOf(constantTerm, right), 0.4, 0.001);
		assertEquals(decider.getWeightOf(constantTerm, left), -0.336, 0.001);
		
		exp = decider.getExperienceRecord(testAgent, testAgent, right.getAction(testAgent));
		testAgent.addGold(1.0);
		exp.updateWithResults(1.0, decider.getCurrentState(testAgent, testAgent));
		exp.updateNextActions(actions);
		decider.learnFrom(exp, 10.0);
		// prediction would be 0.464
		// reward of 1.0. Value of left is -0.272, and value of right is 0.464. So max is 0.464
		// so difference = 1.0 + gamma * 0.464 - 0.464 = 0.9536
		assertEquals(decider.getWeightOf(gold, right), -0.38144, 0.001);
		assertEquals(decider.getWeightOf(gold, left), 0.0, 0.001);
		assertEquals(decider.getWeightOf(constantTerm, right), 0.59072, 0.001);
		assertEquals(decider.getWeightOf(constantTerm, left), -0.336, 0.001);
	}
	
	@Test
	public void teachingDecisionWithNoDifferenceDoesNotUpdateWeights() {
		ExperienceRecord exp = decider.getExperienceRecord(testAgent, testAgent, right.getAction(testAgent));
		exp.updateWithResults(0.0, decider.getCurrentState(testAgent, testAgent));
		exp.updateNextActions(actions);
		decider.learnFrom(exp, 10.0);
		// reward of 0.0, with value of best action = 0.0, so difference = 0.0 + gamma * 0.0 - 0.0 = 0.00
		assertEquals(decider.getWeightOf(gold, right), 0.0, 0.001);
		assertEquals(decider.getWeightOf(gold, left), 0.0, 0.001);
		assertEquals(decider.getWeightOf(constantTerm, right), 0.0, 0.001);
		assertEquals(decider.getWeightOf(constantTerm, left), 0.0, 0.001);
	}
}
