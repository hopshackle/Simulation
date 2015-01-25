package hopshackle.simulation;

import static org.junit.Assert.*;
import hopshackle.simulation.basic.*;

import java.util.*;

import org.junit.*;

public class DeciderTest {

	ArrayList<ActionEnum> reducedActionSet;
	ArrayList<ActionEnum> actionSet = new ArrayList<ActionEnum>(EnumSet.allOf(BasicActions.class));
	ArrayList<GeneticVariable> variableSet = new ArrayList<GeneticVariable>(EnumSet.allOf(BasicVariables.class));
	TestDecider decider = new TestDecider(actionSet, variableSet);
	World w = new World();
	BasicAgent agent = new BasicAgent(w);
	private HexMap<BasicHex> hexMap;
	
	@Before
	public void setup() {
		w.initialiseTemperature(new Temperature(1.0, 1.0));
		reducedActionSet = new ArrayList<ActionEnum>();
		reducedActionSet.add(BasicActions.FARM);				// 1.00 (option value before Boltzmann)
		reducedActionSet.add(BasicActions.FIND_WATER);			// -.50
		reducedActionSet.add(BasicActions.FIND_CIVILISATION);	// 0.00
		reducedActionSet.add(BasicActions.REST);				// 0.01
		hexMap = new HexMap<BasicHex>(5, 5, BasicHex.getHexFactory());
		w.setLocationMap(hexMap);

		agent.setLocation(hexMap.getHexAt(0, 0));
		new Hut(agent);
	}


	@Test
	public void optimalDecisionIs() {
		ActionEnum optimalAction = decider.getOptimalDecision(agent, agent);
		ActionEnum action = decider.makeDecision(agent, agent, 0.0);
		assertTrue(action.equals(optimalAction));
		assertTrue(action.equals(BasicActions.FARM));
	}

	@Test
	public void valuesPerOptionAreCorrect() {
		List<Double> values = decider.getValuesPerOption(actionSet, agent, agent);
		assertEquals(values.size(), 13);
		assertEquals(values.get(0), 0.01, 0.0001);
		assertEquals(values.get(3), 1.00, 0.0001);
		assertEquals(values.get(9), -0.50, 0.0001);
		assertEquals(values.get(12), 0.00, 0.0001);
	}

	@Test
	public void boltzmannValuesPerOptionAreCorrect() {
		List<Double> values = decider.getNormalisedBoltzmannValuesPerOption(reducedActionSet, agent, agent);
		assertEquals(values.size(), 4);
		assertEquals(values.get(0), 0.510, 0.001);
		assertEquals(values.get(1), 0.114, 0.001);
		assertEquals(values.get(2), 0.187, 0.001);
		assertEquals(values.get(3), 0.189, 0.001);
	}
	
	@Test
	public void boltzmannValuesPerOptionChangeWithTemperature() {
		SimProperties.setProperty("Temperature", "0.50");
		List<Double> values = decider.getNormalisedBoltzmannValuesPerOption(reducedActionSet, agent, agent);
		assertEquals(values.size(), 4);
		assertEquals(values.get(0), 0.756, 0.001);
		assertEquals(values.get(1), 0.038, 0.001);
		assertEquals(values.get(2), 0.102, 0.001);
		assertEquals(values.get(3), 0.104, 0.001);
	}
	
	@Test
	public void boltzmannValuesPerOptionChangeWithHighestOption() {
		reducedActionSet.remove(BasicActions.FARM);
		List<Double> values = decider.getNormalisedBoltzmannValuesPerOption(reducedActionSet, agent, agent);
		assertEquals(values.size(), 3);
		assertEquals(values.get(0), 0.000, 0.001);
		assertEquals(values.get(1), 0.269, 0.001);
		assertEquals(values.get(2), 0.731, 0.001);
	}
	
	@After
	public void tearDown() {
		SimProperties.setProperty("Temperature", "1.00");
	}
}


class TestDecider extends QDecider {

	public TestDecider(ArrayList<ActionEnum> actions, ArrayList<GeneticVariable> variables) {
		super(actions, variables);
	}
	
	@Override
	public double valueOption(ActionEnum option, double[] state) {
		return valueOption(option, null, null);
	}

	@Override
	public double valueOption(ActionEnum option, Agent decidingAgent, Agent contextAgent) {
		BasicActions ba = (BasicActions) option;

		// order when enumerated is:
		// [REST, FORAGE, BUILD, FARM, BREED, MARRY, OBEY_SPOUSE, FIND_PLAINS, FIND_FOREST, FIND_WATER, FIND_UNKNOWN, FIND_HUT, FIND_CIVILISATION]
		
		switch(ba) {
		case BREED:
			return 0.01;
		case BUILD:
			return 0.01;
		case FARM:
			return 1.00;
		case FIND_CIVILISATION:
			return 0.00;
		case FIND_FOREST:
			return 0.5;
		case FIND_HUT:
			return 0.01;
		case FIND_PLAINS:
			return 0.01;
		case FIND_UNKNOWN:
			return 0.01;
		case FIND_WATER:
			return -0.5;
		case FORAGE:
			return 0.01;
		case MARRY:
			return 0.01;
		case OBEY_SPOUSE:
			return 0.01;
		case REST:
			return 0.01;
		}
		return 0.0;
	}
}