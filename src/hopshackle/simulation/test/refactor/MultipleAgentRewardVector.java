package hopshackle.simulation.test.refactor;

import java.util.*;
import hopshackle.simulation.*;

import static org.junit.Assert.*;
import org.junit.*;

public class MultipleAgentRewardVector {

	// TEST: 4 actors, three in group, 1 not
	// provide some experience (1 ER record each, followed by termination should suffice)
	// check that streams are merged / not merged as expected
	// also check that the vector rewards are as expected from ER
	// and that with MCStatistics, the correct vector index is used to update the data

	List<ActionEnum<TestAgent>> possibleActions;
	SimpleMazeGame game;
	TestAgent[] players;
	DeciderProperties localProp;
	List<GeneticVariable<TestAgent>> genVar = new ArrayList<GeneticVariable<TestAgent>>(EnumSet.allOf(TestGenVar.class));
	StateFactory<TestAgent> factory = new LinearStateFactory<TestAgent>(genVar);
	BaseStateDecider<TestAgent> rolloutDecider = new SimpleMazeDecider();
	MonteCarloTree<TestAgent> tree;
	// This is a single agent test, so no opponent model is needed, and the last parameter is safely null
	MCTSMasterDecider<TestAgent> masterDecider;
	ExperienceRecordCollector<TestAgent> erc;

	@Before
	public void setUp() throws Exception {
		possibleActions = new ArrayList<ActionEnum<TestAgent>>();
		possibleActions.add(TestActionEnum.LEFT);
		possibleActions.add(TestActionEnum.RIGHT);
		possibleActions.add(TestActionEnum.TEST);
		World world = new World();
		world.setCalendar(new FastCalendar(0l));
		localProp = SimProperties.getDeciderProperties("GLOBAL");
		localProp.setProperty("MonteCarloReward", "true");
		localProp.setProperty("MonteCarloRL", "false");
		localProp.setProperty("MonteCarloUCTType", "MC");
		localProp.setProperty("MonteCarloUCTC", "1.0");
		localProp.setProperty("Gamma", "0.95");
		localProp.setProperty("TimePeriodForGamma", "1000");
		localProp.setProperty("IncrementalScoreReward", "false");
		localProp.setProperty("MonteCarloRolloutCount", "99");
		localProp.setProperty("MonteCarloPriorActionWeightingForBestAction", "0");
		localProp.setProperty("MonteCarloActionValueRollout", "false");
		localProp.setProperty("MonteCarloActionValueOpponentModel", "false");
		localProp.setProperty("MonteCarloActionValueDeciderTemperature", "0.0");
		localProp.setProperty("MonteCarloRetainTreeBetweenActions", "false");
		localProp.setProperty("MaxTurnsPerGame", "10000");
		localProp.setProperty("GameOrdinalRewards", "0");
		localProp.setProperty("MonteCarloOpenLoop", "false");
		tree = new MonteCarloTree<TestAgent>(localProp, 1);
		masterDecider = new MCTSMasterDecider<TestAgent>(factory, rolloutDecider, rolloutDecider);
		masterDecider.injectProperties(localProp);
		Dice.setSeed(6l);
		players = new TestAgent[4];
		for (int i = 0; i < 4; i++) {
			players[i] = new TestAgent(world);
			players[i].setDecider(masterDecider);
		}
		game = new SimpleMazeGame(2, players);

		erc = new ExperienceRecordCollector<TestAgent>(new StandardERFactory<TestAgent>(localProp));
	}

	@Test
	public void experienceRecordHasFourElements() {
		List<TestAgent> agent = new ArrayList<TestAgent>();
		agent.add(players[0]);
		TestAction action = new TestAction(TestActionEnum.RIGHT, agent, null, 0, 1000, false, false, 0);
		ExperienceRecord<TestAgent> er = new ExperienceRecord<TestAgent>(players[0], factory.getCurrentState(players[0]), 
				action, possibleActions, localProp);
		assertEquals(er.getReward().length, 4);
		assertEquals(er.getEndScore().length, 4);
		assertEquals(er.getStartScore().length, 4);
	}

	@Test
	public void rewardVectorHasFourElementsWithOneWinner() {
		double[] result = game.playGame();
		assertEquals(result.length, 4);
		double max = -10;
		int n = 0;
		for (int i = 0; i < 4; i++) {
			if (result[i] > max) {
				max = result[i];
				n = 0;
			}
			if (result[i] == max) n++;
		}
		assertEquals(n, 1);
		// score is 10.0 for reaching target, -1.0 per second elapsed. Each agent moves consecutively
		// and takes 1 second. So we have five moves in total to the end.
		assertEquals(max, 5, 0.001);
		assertEquals(result[1], -6.0, 0.001);
		assertEquals(result[2], -6.0, 0.001);
		assertEquals(result[3], -6.0, 0.001);
	}

	@Test
	public void experienceRecordStreamIsMungedForLinkedAgents() {
		erc.registerAgent(players[0]);
		erc.registerAgent(players[1]);
		erc.registerAgentWithReference(players[2], players[0]);		
		erc.registerAgentWithReference(players[3], players[0]);

		double[] result = game.playGame();
		assertEquals(result[0], 5.0, 0.001);
		assertEquals(result[1], -6.0, 0.001);
		assertEquals(result[2], -6.0, 0.001);
		assertEquals(result[3], -6.0, 0.001);

		assertEquals(erc.getAllExperienceRecords().size(), 5);
		for (int i = 0; i < 4; i++) {
			List<ExperienceRecord<TestAgent>> erList = erc.getExperienceRecords(players[i]);
			switch (i) {
			case 2:	// player 3 has no content, as it is registered under player 1
			case 3: // player 4 has no content, as it is registered under player 1
				assertEquals(erList.size(), 0);
				break;
			case 0:	// player 1 is reference for 3 and 4 as well
				assertEquals(erList.size(), 4);
				for (int record = 0; record < 4; record++) {
					for (int player = 0; player < 4; player++) {
	//					System.out.println("Player " + (player+1) + " : " + (record+1));
						double expectation = -6.0;
						double gamma = Math.pow(0.95, (3 - record));
						if (player == 0) expectation = 5.0;

						expectation = expectation * gamma;
						assertEquals(erList.get(record).getMonteCarloReward()[player], expectation, 0.001);
					}
				}
				break;
			case 1:	// player 2 is self only
				assertEquals(erList.size(), 1);
				assertEquals(erList.get(0).getMonteCarloReward()[0], 5.0, 0.001);
				assertEquals(erList.get(0).getMonteCarloReward()[1], -6.0, 0.001);
				assertEquals(erList.get(0).getMonteCarloReward()[2], -6.0, 0.001);
				assertEquals(erList.get(0).getMonteCarloReward()[3], -6.0, 0.001);
				break;
			}
		}
	}

	@Test
	public void experienceRecordStreamIsNotMungedForUnlinkedAgents() {	
		for (TestAgent p : game.getAllPlayers()) {
			erc.registerAgent(p);
		}

		double[] result = game.playGame();
		assertEquals(result[0], 5.0, 0.001);
		assertEquals(result[1], -6.0, 0.001);
		assertEquals(result[2], -6.0, 0.001);
		assertEquals(result[3], -6.0, 0.001);
		result = game.getFinalScores();
		assertEquals(result[0], 5.0, 0.001);
		assertEquals(result[1], -6.0, 0.001);
		assertEquals(result[2], -6.0, 0.001);
		assertEquals(result[3], -6.0, 0.001);
		assertEquals(erc.getAllExperienceRecords().size(), 5);
		for (int i = 0; i < 4; i++) {
			List<ExperienceRecord<TestAgent>> erList = erc.getExperienceRecords(players[i]);
			for (int j = 0; j < 4; j++) {
				// System.out.println(i + " " + j);
				double expectation = -6.0;
				double gamma = 1.0;
				if (j==0) expectation = 5.0;
				if (i==0) gamma = 0.95;		// only player 1 has two ER in stream

				expectation = expectation * gamma;
				assertEquals(erList.get(0).getMonteCarloReward()[j], expectation, 0.001);
				if (i == 0) assertEquals(erList.get(1).getMonteCarloReward()[j], expectation/gamma, 0.001);
			}
		}

	}

}
