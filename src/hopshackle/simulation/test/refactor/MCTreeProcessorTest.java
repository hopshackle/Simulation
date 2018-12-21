package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;

import java.util.*;

import hopshackle.simulation.MCTS.*;
import hopshackle.simulation.*;

import org.encog.neural.data.basic.*;
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
	State<TestAgent> dummyState;
	
	@Before
	public void setUp() throws Exception {
		leftRightOnly.remove(TestActionEnum.TEST);
		localProp = SimProperties.getDeciderProperties("GLOBAL").clone();
		localProp.setProperty("MonteCarloRolloutModel", "neural");
		localProp.setProperty("MonteCarloMinVisitsForRolloutTraining", "1");
		localProp.setProperty("MonteCarloRolloutTarget", "basic");
		localProp.setProperty("NeuralShuffleData", "false");
		localProp.setProperty("NeuralMaxOutput", "5");
		localProp.setProperty("NeuralControlSignal", "false");
		localProp.setProperty("NeuralDeciderArchitecture", "10");
		localProp.setProperty("NeuralNeuronType", "TANH");
		localProp.setProperty("NeuralPropagationType", "resilient");
//		localProp.setProperty("NeuralLearningMomentum", "0.98");
		localProp.setProperty("NeuralLearningIterations", "100");
		localProp.setProperty("MonteCarloOpenLoop", "false");
		localProp.setProperty("MonteCarloTree", "ignoreOthers");
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
		MCStatistics<TestAgent> stats = new MCStatistics<TestAgent>(localProp, 2, stateFactory.getCurrentState(agent1));
		double[] asArray = processor.getOutputValuesAsArray(stats);
		assertEquals(asArray.length, 0);
		asArray = processor.getOutputValuesAsArray(stats);
		assertEquals(asArray.length, 0);
		
		double[] reward = {1.0, 0.2};
		stats.update(TestActionEnum.LEFT, reward, 1);
		stats.update(TestActionEnum.LEFT, reward, 1);
		
		asArray = processor.getOutputValuesAsArray(stats);
		assertEquals(asArray.length, 1);
		assertEquals(asArray[0], 1.00, 0.0001);

		stats = new MCStatistics<TestAgent>(localProp, 2, stateFactory.getCurrentState(agent2));
		stats.update(TestActionEnum.LEFT, reward, 2);
		stats.update(TestActionEnum.LEFT, reward, 2);
		asArray = processor.getOutputValuesAsArray(stats);
		assertEquals(asArray.length, 1);
		assertEquals(asArray[0], 0.20, 0.0001);
	}
	
	@Test
	public void outputValuesWithAdditionalAction() {
		MCStatistics<TestAgent> stats = new MCStatistics<TestAgent>(localProp, 2, stateFactory.getCurrentState(agent1));
		double[] asArray;
		double[] rewardLeft = {1.0, 0.2};
		double[] rewardTest = {-0.5, 0.5};
		stats.update(TestActionEnum.LEFT, rewardLeft, 1);
		stats.update(TestActionEnum.LEFT, rewardLeft, 1);
		stats.update(TestActionEnum.TEST, rewardTest, 1);
		
		asArray = processor.getOutputValuesAsArray(stats);
		assertEquals(asArray.length, 2);
		assertEquals(asArray[0], 1.00, 0.0001);
		assertEquals(asArray[1], -0.50, 0.0001);

		stats = new MCStatistics<TestAgent>(localProp, 2, stateFactory.getCurrentState(agent2));
		stats.update(TestActionEnum.LEFT, rewardLeft, 2);
		stats.update(TestActionEnum.LEFT, rewardLeft, 2);
		stats.update(TestActionEnum.TEST, rewardTest, 2);
		asArray = processor.getOutputValuesAsArray(stats);
		assertEquals(asArray.length, 2);
		assertEquals(asArray[0], 0.20, 0.0001);
		assertEquals(asArray[1], 0.50, 0.0001);
	}
	
	
	@Test
	public void outputValuesWithOneHotEncoding() {
		localProp.setProperty("MonteCarloRolloutTarget", "oneHot");
		processor = new TestMCTreeProcessor(localProp);
		MCStatistics<TestAgent> stats = new MCStatistics<TestAgent>(localProp, 2, null);
		
		double[] rewardLeft = {1.0, 0.2};
		double[] rewardTest = {-0.5, 0.5};
		stats.update(TestActionEnum.LEFT, rewardLeft, 1);
		stats.update(TestActionEnum.LEFT, rewardLeft, 1);
		stats.update(TestActionEnum.TEST, rewardTest, 1);
		
		double[] asArray = processor.getOutputValuesAsArray(stats);
		assertEquals(asArray.length, 2);
		assertEquals(asArray[0], 1.00, 0.0001);
		assertEquals(asArray[1], 0.00, 0.0001);
		asArray = processor.getOutputValuesAsArray(stats);
		assertEquals(asArray.length, 2);		// the refAgent has no impact with oneHot encoding switched on
		assertEquals(asArray[0], 1.00, 0.0001);
		assertEquals(asArray[1], 0.00, 0.0001);

		double[] rewardRight = {2.0, 0.5};
		stats.update(TestActionEnum.RIGHT, rewardRight, 1);
		asArray = processor.getOutputValuesAsArray(stats);
		assertEquals(asArray.length, 3);	// will include LEFT and RIGHT, with RIGHT now 1.0
		assertEquals(asArray[0], 0.00, 0.0001);
		assertEquals(asArray[1], 0.00, 0.0001);
		assertEquals(asArray[2], 1.00, 0.0001);
	}
	
	@Test
	public void basicTreeProcess() {
		processor.processTree(generateTree());
		BasicNeuralDataSet data = processor.finalTrainingData(null);
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
		processor.processTree(generateTree());	// this will put actions in order TEST, LEFT, RIGHT
		MCStatistics<TestAgent> stats = new MCStatistics<TestAgent>(localProp, 2, null);
		MCStatistics<TestAgent> fullStats = new MCStatistics<TestAgent>(localProp, 2, null);
		double[] rewardLeft = {1.0, 0.2};
		double[] rewardRight = {-0.5, 0.5};
		double[] rewardTest = {0.6, 0.6};
		stats.update(TestActionEnum.LEFT, rewardLeft, 1);
		stats.update(TestActionEnum.RIGHT, rewardRight, 1);
		stats.update(TestActionEnum.TEST, rewardTest, 1);
		double[] asArray = processor.getOutputValuesAsArray(stats);
		assertEquals(asArray.length, 3);
		assertEquals(asArray[0], 0.6, 0.0001);
		assertEquals(asArray[1], 1.0, 0.0001);
		assertEquals(asArray[2], -0.5, 0.0001);
		
		fullStats.update(TestActionEnum.LEFT, rewardLeft,1);
		fullStats.update(TestActionEnum.RIGHT, rewardRight, 1);
		fullStats.update(TestActionEnum.TEST, rewardTest, 1);
		asArray = processor.getOutputValuesAsArray(fullStats);
		assertEquals(asArray.length, 3);
		assertEquals(asArray[0], 0.6, 0.0001);
		assertEquals(asArray[1], 1.0, 0.0001);
		assertEquals(asArray[2], -0.5, 0.0001);
		
	}

	@Test
	public void createNeuralDeciderFromData() {
		processor.processTree(generateTree());	// this will put actions in order TEST, LEFT, RIGHT
		NeuralDecider<TestAgent> nd = (NeuralDecider<TestAgent>) processor.generateDecider(stateFactory, 1.0, 1.0, null);
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
	public void createNeuralDeciderFromDataWithShuffle() {
		localProp.setProperty("NeuralShuffleData", "true");
		processor = new TestMCTreeProcessor(localProp);
		processor.processTree(generateTree());	// this will put actions in order TEST, LEFT, RIGHT
		NeuralDecider<TestAgent> nd = (NeuralDecider<TestAgent>) processor.generateDecider(stateFactory, 1.0, 1.0, null);
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
	public void createLogisticDeciderFromData() {
		localProp.setProperty("MonteCarloRolloutModel", "logistic");
		processor = new TestMCTreeProcessor(localProp);
		processor.processTree(generateTree());	// this will put actions in order TEST, LEFT, RIGHT
		LogisticDecider<TestAgent> nd = (LogisticDecider<TestAgent>) processor.generateDecider(stateFactory, 1.0, 1.0, null);
		String result = String.format("LEFT: %.2f, RIGHT: %.2f, TEST: %.2f",
				nd.valueOption(TestActionEnum.LEFT, agent1),
				nd.valueOption(TestActionEnum.RIGHT, agent1),
				nd.valueOption(TestActionEnum.TEST, agent1));
		System.out.println(result);
		// values are rather screwy; but at least in correct rank order
		assertEquals(nd.valueOption(TestActionEnum.RIGHT, agent1), 0.31, 0.01);
		assertEquals(nd.valueOption(TestActionEnum.LEFT, agent1), 0.57, 0.01);
		assertEquals(nd.valueOption(TestActionEnum.TEST, agent1), 0.37, 0.01);
	}
	
	@Test
	public void createNeuralDeciderFromDataWithControlSignal() {
		localProp.setProperty("NeuralControlSignal", "true");
		processor = new TestMCTreeProcessor(localProp);
		processor.processTree(generateTree());	// this will put actions in order TEST, LEFT, RIGHT
		BasicNeuralDataSet trainingData = processor.finalTrainingData(null);
		double[] input = trainingData.get(0).getInputArray();
		double[] output = trainingData.get(0).getIdealArray();
		assertEquals(trainingData.size(), 5);	// 3 for state1, 2 for state2
		assertEquals(input.length, 9);
		assertEquals(output.length, 1);
		// actions should be in order TEST, LEFT, RIGHT in array.
		assertEquals(input[4], 1.0, 0.001);
		assertEquals(input[5], 0.0, 0.001);
		assertEquals(input[6], 0.0, 0.001);
		assertEquals(input[7], 0.0, 0.001);
		assertEquals(input[8], 0.0, 0.001);
		assertEquals(output[0], 0.0, 0.001);
		
		input = trainingData.get(1).getInputArray();
		output = trainingData.get(1).getIdealArray();
		assertEquals(input[4], 0.0, 0.001);
		assertEquals(input[5], 1.0, 0.001);
		assertEquals(input[6], 0.0, 0.001);
		assertEquals(input[7], 0.0, 0.001);
		assertEquals(input[8], 0.0, 0.001);
		assertEquals(output[0], 1.0, 0.001);
		
		input = trainingData.get(2).getInputArray();
		output = trainingData.get(2).getIdealArray();
		assertEquals(input[4], 0.0, 0.001);
		assertEquals(input[5], 0.0, 0.001);
		assertEquals(input[6], 1.0, 0.001);
		assertEquals(input[7], 0.0, 0.001);
		assertEquals(input[8], 0.0, 0.001);
		assertEquals(output[0], -0.5, 0.001);
		
		input = trainingData.get(3).getInputArray();
		output = trainingData.get(3).getIdealArray();
		assertEquals(input[4], 1.0, 0.001);
		assertEquals(input[5], 0.0, 0.001);
		assertEquals(input[6], 0.0, 0.001);
		assertEquals(input[7], 0.0, 0.001);
		assertEquals(input[8], 0.0, 0.001);
		assertEquals(output[0], 0.1, 0.001);
		
		input = trainingData.get(4).getInputArray();
		output = trainingData.get(4).getIdealArray();
		assertEquals(input[4], 0.0, 0.001);
		assertEquals(input[5], 0.0, 0.001);
		assertEquals(input[6], 1.0, 0.001);
		assertEquals(input[7], 0.0, 0.001);
		assertEquals(input[8], 0.0, 0.001);
		assertEquals(output[0], 0.5, 0.001);
		
		NeuralDecider<TestAgent> nd = (NeuralDecider<TestAgent>) processor.generateDecider(stateFactory, 1.0, 1.0, null);
		State<TestAgent> agentState = stateFactory.getCurrentState(agent1);
		String result = String.format("LEFT: %.2f, RIGHT: %.2f, TEST: %.2f",
				nd.valueOption(TestActionEnum.LEFT, agentState),
				nd.valueOption(TestActionEnum.RIGHT, agentState),
				nd.valueOption(TestActionEnum.TEST, agentState));
		System.out.println(result); 
		assertEquals(nd.valueOption(TestActionEnum.RIGHT, agentState), -0.5, 0.01);
		assertEquals(nd.valueOption(TestActionEnum.LEFT, agentState), 0.98, 0.02);
		assertEquals(nd.valueOption(TestActionEnum.TEST, agentState), 0.0, 0.01);
	}
	
	@Test
	public void createNeuralDeciderFromDataWithOneHotEncoding() {
		localProp.setProperty("MonteCarloRolloutTarget", "oneHot");
		processor = new TestMCTreeProcessor(localProp);
		processor.processTree(generateTree());	// this will put actions in order TEST, LEFT, RIGHT
		NeuralDecider<TestAgent> nd = (NeuralDecider<TestAgent>) processor.generateDecider(stateFactory, 1.0, 1.0, null);
		State<TestAgent> agentState = stateFactory.getCurrentState(agent1);
		String result = String.format("LEFT: %.2f, RIGHT: %.2f, TEST: %.2f",
				nd.valueOption(TestActionEnum.LEFT, agentState),
				nd.valueOption(TestActionEnum.RIGHT, agentState),
				nd.valueOption(TestActionEnum.TEST, agentState));
		System.out.println(result);
		assertEquals(nd.valueOption(TestActionEnum.RIGHT, agentState), 0.0, 0.01);
		assertEquals(nd.valueOption(TestActionEnum.LEFT, agentState), 0.98, 0.02);
		assertEquals(nd.valueOption(TestActionEnum.TEST, agentState), 0.0, 0.01);
		
		agentState = stateFactory.getCurrentState(agent2);
		result = String.format("LEFT: %.2f, RIGHT: %.2f, TEST: %.2f",
				nd.valueOption(TestActionEnum.LEFT, agentState),
				nd.valueOption(TestActionEnum.RIGHT, agentState),
				nd.valueOption(TestActionEnum.TEST, agentState));
		System.out.println(result);
		assertEquals(nd.valueOption(TestActionEnum.RIGHT, agentState), 0.98, 0.02);
		assertEquals(nd.valueOption(TestActionEnum.LEFT, agentState), 0.0, 0.01);
		assertEquals(nd.valueOption(TestActionEnum.TEST, agentState), 0.0, 0.01);
	}

	
	@Test
	public void minVisitsControlsWhichTreeNodesAreExtracted() {
		localProp.setProperty("MonteCarloMinVisitsForRolloutTraining", "3");
		processor = new TestMCTreeProcessor(localProp);
		processor.processTree(generateTree());
		BasicNeuralDataSet data = processor.finalTrainingData(null);
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
	
	@Test
	public void treeStateWithOneActionIsNotExtracted() {
        TranspositionTableMCTree<TestAgent> retValue = new TranspositionTableMCTree<TestAgent>(localProp, 2);
		State<TestAgent> agentState2 = stateFactory.getCurrentState(agent2);
		retValue.insertState(agentState2);
		double[] rewardRight = {-0.5, 0.5};
		retValue.updateState(agentState2, TestActionEnum.RIGHT, agentState2, rewardRight, 1);
		
		processor = new TestMCTreeProcessor(localProp);
		assertEquals(processor.finalTrainingData(null).getRecordCount(), 0);
		processor.processTree(retValue);
		assertEquals(processor.finalTrainingData(null).getRecordCount(), 0);
	}
	
	private MonteCarloTree<TestAgent> generateTree() {
        TranspositionTableMCTree<TestAgent> retValue = new TranspositionTableMCTree<TestAgent>(localProp, 2);
		State<TestAgent> agentState1 = stateFactory.getCurrentState(agent1);
		State<TestAgent> agentState2 = stateFactory.getCurrentState(agent2);
		retValue.insertState(agentState1);
		retValue.insertState(agentState2);
		double[] rewardLeft = {1.0, -1.0};
		double[] rewardRight = {-0.5, 0.5};
		double[] rewardTest = {0.0, 0.1};
		retValue.updateState(agentState1, TestActionEnum.TEST, agentState1, rewardTest, 1);
		retValue.updateState(agentState1, TestActionEnum.LEFT, agentState1, rewardLeft, 1);
		retValue.updateState(agentState1, TestActionEnum.RIGHT, agentState1, rewardRight, 1);
		retValue.updateState(agentState1, TestActionEnum.LEFT, agentState1, rewardLeft, 1);
		retValue.updateState(agentState2, TestActionEnum.RIGHT, agentState2, rewardRight, 2);
		retValue.updateState(agentState2, TestActionEnum.TEST, agentState2, rewardTest, 2);
		return retValue;
	}
}

class TestMCTreeProcessor extends MCTreeProcessor<TestAgent> {

	public TestMCTreeProcessor(DeciderProperties prop) {
		super(prop, "");
	}

	@Override
	public double[] convertStateAsStringToArray(String stringRepresentation) {
		return super.convertStateAsStringToArray(stringRepresentation);
	}

	@Override
	public double[] getOutputValuesAsArray(MCStatistics<TestAgent> stats) {
		return super.getOutputValuesAsArray(stats);
	}
	
	@Override
	public BasicNeuralDataSet finalTrainingData(RawDecider<TestAgent> rawDecider) {
		return super.finalTrainingData(rawDecider);
	}
}
