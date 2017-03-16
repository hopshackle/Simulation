package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import hopshackle.simulation.*;

import org.encog.neural.data.basic.BasicNeuralDataSet;
import org.junit.*;

public class MCTreeProcessorTest {
	
	List<ActionEnum<TestAgent>> allActions = new ArrayList<ActionEnum<TestAgent>>(EnumSet.allOf(TestActionEnum.class));
	List<ActionEnum<TestAgent>> leftRightOnly = new ArrayList<ActionEnum<TestAgent>>(EnumSet.allOf(TestActionEnum.class));
	List<GeneticVariable<TestAgent>> genVars = new ArrayList<GeneticVariable<TestAgent>>(EnumSet.allOf(TestGenVar.class));
	TestMCTreeProcessor processor;
	DeciderProperties localProp;
	TestAgent agent1, agent2;
	World world;
	SimpleMazeGame game;
	StateFactory<TestAgent> stateFactory;
	
	@Before
	public void setUp() throws Exception {
		leftRightOnly.remove(TestActionEnum.TEST);
		localProp = SimProperties.getDeciderProperties("GLOBAL").clone();
		localProp.setProperty("MonteCarloMinVisitsForRolloutTraining", "1");
		localProp.setProperty("MonteCarloOneHotRolloutTraining", "false");
		localProp.setProperty("NeuralMaxOutput", "5");
		localProp.setProperty("NeuralDeciderArchitecture", "10");
		localProp.setProperty("NeuralNeuronType", "TANH");
		localProp.setProperty("NeuralPropagationType", "resilient");
//		localProp.setProperty("NeuralLearningMomentum", "0.98");
		localProp.setProperty("NeuralLearningIterations", "100");
		processor = new TestMCTreeProcessor(localProp);
		world = new World();
		agent1 = new TestAgent(world);
		agent1.addGold(1.4);
		agent2 = new TestAgent(world);
		TestAgent[] players = {agent1, agent2};
		game = new SimpleMazeGame(3, players);
		stateFactory = new LinearStateFactory<TestAgent>(genVars);
	}

	@Test
	public void arrayToStringToArray() {
		String arrayAsString = stateFactory.getCurrentState(agent1).getAsString();
		double[] originalArray = stateFactory.getCurrentState(agent1).getAsArray();
		// CONSTANT, GOLD, POSITION, AGE
		assertTrue(arrayAsString.equals("100|140|000|000"));
		double[] asArray = processor.convertStateAsStringToArray(arrayAsString);
		assertEquals(asArray.length, 4);
		for (int i = 0; i < 4; i++) {
			assertEquals(asArray[i], originalArray[i], 0.005);
		}
	}
	
	@Test
	public void outputValues() {
		MCStatistics<TestAgent> stats = new MCStatistics<TestAgent>(leftRightOnly, localProp, 2, 1);
		double[] asArray = processor.getOutputValuesAsArray(stats, 1);
		assertEquals(asArray.length, 2);
		assertEquals(asArray[0], 0.00, 0.0001);
		assertEquals(asArray[1], 0.00, 0.0001);
		asArray = processor.getOutputValuesAsArray(stats, 2);
		assertEquals(asArray.length, 2);
		assertEquals(asArray[0], 0.00, 0.0001);
		assertEquals(asArray[1], 0.00, 0.0001);
		
		double[] reward = {1.0, 0.2};
		stats.update(TestActionEnum.LEFT, reward);
		stats.update(TestActionEnum.LEFT, reward);
		
		asArray = processor.getOutputValuesAsArray(stats, 1);
		assertEquals(asArray.length, 2);
		assertEquals(asArray[0], 1.00, 0.0001);
		assertEquals(asArray[1], 0.00, 0.0001);
		asArray = processor.getOutputValuesAsArray(stats, 2);
		assertEquals(asArray.length, 2);
		assertEquals(asArray[0], 0.20, 0.0001);
		assertEquals(asArray[1], 0.00, 0.0001);
	}
	
	@Test
	public void outputValuesWithAdditionalAction() {
		MCStatistics<TestAgent> stats = new MCStatistics<TestAgent>(leftRightOnly, localProp, 2, 1);
		double[] asArray = processor.getOutputValuesAsArray(stats, 1);
		assertEquals(asArray.length, 2);
		assertEquals(asArray[0], 0.00, 0.0001);
		assertEquals(asArray[1], 0.00, 0.0001);
		asArray = processor.getOutputValuesAsArray(stats, 2);
		assertEquals(asArray.length, 2);
		assertEquals(asArray[0], 0.00, 0.0001);
		assertEquals(asArray[1], 0.00, 0.0001);
		
		double[] rewardLeft = {1.0, 0.2};
		double[] rewardTest = {-0.5, 0.5};
		stats.update(TestActionEnum.LEFT, rewardLeft);
		stats.update(TestActionEnum.LEFT, rewardLeft);
		stats.update(TestActionEnum.TEST, rewardTest);
		
		asArray = processor.getOutputValuesAsArray(stats, 1);
		assertEquals(asArray.length, 3);
		assertEquals(asArray[0], 1.00, 0.0001);
		assertEquals(asArray[1], 0.00, 0.0001);
		assertEquals(asArray[2], -0.50, 0.0001);
		asArray = processor.getOutputValuesAsArray(stats, 2);
		assertEquals(asArray.length, 3);
		assertEquals(asArray[0], 0.20, 0.0001);
		assertEquals(asArray[1], 0.00, 0.0001);
		assertEquals(asArray[2], 0.50, 0.0001);
	}
	
	
	@Test
	public void outputValuesWithOneHotEncoding() {
		localProp.setProperty("MonteCarloOneHotRolloutTraining", "true");
		processor = new TestMCTreeProcessor(localProp);
		MCStatistics<TestAgent> stats = new MCStatistics<TestAgent>(leftRightOnly, localProp, 2, 1);
		
		double[] rewardLeft = {1.0, 0.2};
		double[] rewardTest = {-0.5, 0.5};
		stats.update(TestActionEnum.LEFT, rewardLeft);
		stats.update(TestActionEnum.LEFT, rewardLeft);
		stats.update(TestActionEnum.TEST, rewardTest);
		
		double[] asArray = processor.getOutputValuesAsArray(stats, 1);
		assertEquals(asArray.length, 1);	// will only include LEFT
		assertEquals(asArray[0], 1.00, 0.0001);
		asArray = processor.getOutputValuesAsArray(stats, 2);
		assertEquals(asArray.length, 1);		// the refAgent has no impact with oneHot encoding switched on
		assertEquals(asArray[0], 1.00, 0.0001);

		double[] rewardRight = {2.0, 0.5};
		stats.update(TestActionEnum.RIGHT, rewardRight);
		asArray = processor.getOutputValuesAsArray(stats, 1);
		assertEquals(asArray.length, 2);	// will include LEFT and RIGHT, with RIGHT now 1.0
		assertEquals(asArray[0], 0.00, 0.0001);
		assertEquals(asArray[1], 1.00, 0.0001);
	}
	
	
	@Test
	public void basicTreeProcess() {
		processor.processTree(generateTree(), 1);
		BasicNeuralDataSet data = processor.finalTrainingData();
		assertEquals(data.size(), 2);
		double[] output1 = data.get(0).getIdealArray();
		double[] input1 = data.get(0).getInputArray();
		assertEquals(output1.length, 5);
		assertEquals(output1[0], 0.0, 0.001);
		assertEquals(output1[1], 1.0, 0.001);
		assertEquals(output1[2],-0.5, 0.001);
		assertEquals(input1.length, 4);
		assertEquals(input1[0], 1.0, 0.001);
		assertEquals(input1[1], 1.4, 0.001);
		assertEquals(input1[2], 0.0, 0.001);
		assertEquals(input1[3], 0.0, 0.001);
		
		double[] output2 = data.get(1).getIdealArray();
		double[] input2 = data.get(1).getInputArray();
		assertEquals(output2.length, 5);
		assertEquals(output1[0], 0.0, 0.001);
		assertEquals(output1[1], 1.0, 0.001);
		assertEquals(output1[2],-0.5, 0.001);
		assertEquals(input2.length, 4);
		assertEquals(input2[0], 1.0, 0.001);
		assertEquals(input2[1], 0.0, 0.001);
		assertEquals(input2[2], 0.0, 0.001);
		assertEquals(input2[3], 0.0, 0.001);
	}
	
	@Test
	public void changingOrderOfActionsInMCStatisticsReflectedInOutput() {
		processor.processTree(generateTree(), 1);	// this will put actions in order TEST, LEFT, RIGHT
		MCStatistics<TestAgent> stats = new MCStatistics<TestAgent>(leftRightOnly, localProp, 2, 1);
		MCStatistics<TestAgent> fullStats = new MCStatistics<TestAgent>(allActions, localProp, 2, 1);
		double[] rewardLeft = {1.0, 0.2};
		double[] rewardRight = {-0.5, 0.5};
		double[] rewardTest = {0.6, 0.6};
		stats.update(TestActionEnum.LEFT, rewardLeft);
		stats.update(TestActionEnum.RIGHT, rewardRight);
		stats.update(TestActionEnum.TEST, rewardTest);
		double[] asArray = processor.getOutputValuesAsArray(stats, 1);
		assertEquals(asArray.length, 3);
		assertEquals(asArray[0], 0.6, 0.0001);
		assertEquals(asArray[1], 1.0, 0.0001);
		assertEquals(asArray[2], -0.5, 0.0001);
		
		fullStats.update(TestActionEnum.LEFT, rewardLeft);
		fullStats.update(TestActionEnum.RIGHT, rewardRight);
		fullStats.update(TestActionEnum.TEST, rewardTest);
		asArray = processor.getOutputValuesAsArray(fullStats, 1);
		assertEquals(asArray.length, 3);
		assertEquals(asArray[0], 0.6, 0.0001);
		assertEquals(asArray[1], 1.0, 0.0001);
		assertEquals(asArray[2], -0.5, 0.0001);
		
	}

	@Test
	public void createNeuralDeciderFromData() {
		processor.processTree(generateTree(), 1);	// this will put actions in order TEST, LEFT, RIGHT
		NeuralDecider<TestAgent> nd = processor.generateDecider(stateFactory, 1.0);
		State<TestAgent> agentState = stateFactory.getCurrentState(agent1);
/*		String result = String.format("LEFT: %.2f, RIGHT: %.2f, TEST: %.2f",
				nd.valueOption(TestActionEnum.LEFT, agentState),
				nd.valueOption(TestActionEnum.RIGHT, agentState),
				nd.valueOption(TestActionEnum.TEST, agentState));
		System.out.println(result); */ 
		assertEquals(nd.valueOption(TestActionEnum.RIGHT, agentState), -0.5, 0.01);
		assertEquals(nd.valueOption(TestActionEnum.LEFT, agentState), 0.99, 0.01);
		assertEquals(nd.valueOption(TestActionEnum.TEST, agentState), 0.0, 0.01);
	}
	
	@Test
	public void createNeuralDeciderFromDataWithOneHotEncoding() {
		localProp.setProperty("MonteCarloOneHotRolloutTraining", "true");
		processor = new TestMCTreeProcessor(localProp);
		processor.processTree(generateTree(), 1);	// this will put actions in order TEST, LEFT, RIGHT
		NeuralDecider<TestAgent> nd = processor.generateDecider(stateFactory, 1.0);
		State<TestAgent> agentState = stateFactory.getCurrentState(agent1);
/*		String result = String.format("LEFT: %.2f, RIGHT: %.2f, TEST: %.2f",
				nd.valueOption(TestActionEnum.LEFT, agentState),
				nd.valueOption(TestActionEnum.RIGHT, agentState),
				nd.valueOption(TestActionEnum.TEST, agentState));
		System.out.println(result);  */
		assertEquals(nd.valueOption(TestActionEnum.RIGHT, agentState), 0.0, 0.01);
		assertEquals(nd.valueOption(TestActionEnum.LEFT, agentState), 0.99, 0.01);
		assertEquals(nd.valueOption(TestActionEnum.TEST, agentState), 0.0, 0.01);
		
		agentState = stateFactory.getCurrentState(agent2);
		assertEquals(nd.valueOption(TestActionEnum.RIGHT, agentState), 0.99, 0.01);
		assertEquals(nd.valueOption(TestActionEnum.LEFT, agentState), 0.0, 0.01);
		assertEquals(nd.valueOption(TestActionEnum.TEST, agentState), 0.0, 0.01);
	}

	
	@Test
	public void minVisitsControlsWhichTreeNodesAreExtracted() {
		localProp.setProperty("MonteCarloMinVisitsForRolloutTraining", "3");
		processor = new TestMCTreeProcessor(localProp);
		processor.processTree(generateTree(), 1);
		BasicNeuralDataSet data = processor.finalTrainingData();
		assertEquals(data.size(), 1);
		double[] output1 = data.get(0).getIdealArray();
		double[] input1 = data.get(0).getInputArray();
		assertEquals(output1.length, 5);
		assertEquals(output1[0], 0.0, 0.001);
		assertEquals(output1[1], 1.0, 0.001);
		assertEquals(output1[2],-0.5, 0.001);
		assertEquals(input1.length, 4);
		assertEquals(input1[0], 1.0, 0.001);
		assertEquals(input1[1], 1.4, 0.001);
		assertEquals(input1[2], 0.0, 0.001);
		assertEquals(input1[3], 0.0, 0.001);
	}
	
	private MonteCarloTree<TestAgent> generateTree() {
		MonteCarloTree<TestAgent> retValue = new MonteCarloTree<TestAgent>(localProp, 2);
		State<TestAgent> agentState1 = stateFactory.getCurrentState(agent1);
		State<TestAgent> agentState2 = stateFactory.getCurrentState(agent2);
		retValue.insertState(agentState1, allActions);
		retValue.insertState(agentState2, allActions);
		double[] rewardLeft = {1.0, -1.0};
		double[] rewardRight = {-0.5, 0.5};
		double[] rewardTest = {0.0, 0.1};
		retValue.updateState(agentState1, TestActionEnum.RIGHT, agentState1, rewardRight);
		retValue.updateState(agentState1, TestActionEnum.TEST, agentState1, rewardTest);
		retValue.updateState(agentState1, TestActionEnum.LEFT, agentState1, rewardLeft);
		retValue.updateState(agentState1, TestActionEnum.LEFT, agentState1, rewardLeft);
		retValue.updateState(agentState2, TestActionEnum.RIGHT, agentState2, rewardRight);
		retValue.updateState(agentState2, TestActionEnum.TEST, agentState2, rewardTest);
		return retValue;
	}
}

class TestMCTreeProcessor extends MCTreeProcessor<TestAgent> {

	public TestMCTreeProcessor(DeciderProperties prop) {
		super(prop);
	}

	@Override
	public double[] convertStateAsStringToArray(String stringRepresentation) {
		return super.convertStateAsStringToArray(stringRepresentation);
	}

	@Override
	public double[] getOutputValuesAsArray(MCStatistics<TestAgent> stats, int refAgent) {
		return super.getOutputValuesAsArray(stats, refAgent);
	}
	
	@Override
	public BasicNeuralDataSet finalTrainingData() {
		return super.finalTrainingData();
	}
}
