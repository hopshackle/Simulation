package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;

import hopshackle.simulation.MCTS.*;
import hopshackle.simulation.games.resistance.*;
import hopshackle.simulation.*;

import java.util.*;
import java.util.stream.*;

import org.junit.*;
import org.javatuples.*;

public class BackPropagationTacticsTest {

    private Resistance game;
    private ResistancePlayer[] players;
    private DeciderProperties localProp;
    private OpenLoopMCTree<ResistancePlayer> openLoopTree;
    private MCTSMasterDecider<ResistancePlayer> masterDecider;
    private SingletonStateFactory<ResistancePlayer> singletonStateFactory = new SingletonStateFactory<>();
    private BaseStateDecider<ResistancePlayer> rolloutDecider = new RandomDecider<>(singletonStateFactory);
    private World world = new World();
    private State<ResistancePlayer> singletonState;
    private List<Triplet<State<ResistancePlayer>, ActionWithRef<ResistancePlayer>, Long>>[] trajectory;
    private MCStatistics<ResistancePlayer>[] onInclude3 = new MCStatistics[5];
    private MCStatistics<ResistancePlayer>[] onSupport = new MCStatistics[5];
    private MCStatistics<ResistancePlayer>[] onInclude2 = new MCStatistics[5];
    private int[] rootVisits = new int[5];
    private int[] onInclude3Visits = new int[5];
    private int[] onSupportVisits = new int[5];
    private int[] onInclude2Visits = new int[5];
    private double[] onInclude2Mean = new double[5];
    private double[] onInclude3Mean = new double[5];
    private double[] onSupportMean = new double[5];
    private double[] scores = new double[]{0.0, 5.0, 0.0, 0.0, 0.0};


    @Before
    public void setup() {
        localProp = SimProperties.getDeciderProperties("GLOBAL");
        localProp.setProperty("MonteCarloTree", "perPlayer");
        localProp.setProperty("MonteCarloReward", "true");
        localProp.setProperty("MonteCarloRL", "false");
        localProp.setProperty("MonteCarloUCTType", "MC");
        localProp.setProperty("MonteCarloUCTC", "5.0");
        localProp.setProperty("Gamma", "1.0");
        localProp.setProperty("TimePeriodForGamma", "1000");
        localProp.setProperty("IncrementalScoreReward", "false");
        localProp.setProperty("MonteCarloRolloutCount", "100");
        localProp.setProperty("MonteCarloTimePerMove", "1000");
        localProp.setProperty("MonteCarloPriorActionWeightingForBestAction", "0");
        localProp.setProperty("MonteCarloActionValueRollout", "false");
        localProp.setProperty("MonteCarloActionValueOpponentModel", "false");
        localProp.setProperty("MonteCarloActionValueDeciderTemperature", "0.0");
        localProp.setProperty("MonteCarloRetainTreeBetweenActions", "false");
        localProp.setProperty("MaxTurnsPerGame", "10000");
        localProp.setProperty("GameOrdinalRewards", "0");
        localProp.setProperty("MonteCarloHeuristicOnExpansion", "false");
        localProp.setProperty("MonteCarloMAST", "false");
        localProp.setProperty("MonteCarloOpenLoop", "true");
        localProp.setProperty("MonteCarloHeuristicOnSelection", "false");
        localProp.setProperty("MonteCarloParentalVisitValidity", "true");
        Dice.setSeed(6l);
        game = new Resistance(5, 2, world);
        masterDecider = new OLMCTSMasterDecider<>(game, singletonStateFactory, rolloutDecider, rolloutDecider);
        masterDecider.injectProperties(localProp);
        players = new ResistancePlayer[5];

        for (int i = 0; i < 5; i++) {
            players[i] = game.getPlayer(i + 1);
            players[i].setDecider(masterDecider);
        }
        singletonState = singletonStateFactory.getCurrentState(players[0]);
        game.oneAction();

        // now we have tree set up, and we extract the nodes of interest in tests
        for (int i = 0; i < 5; i++) {
            MonteCarloTree<ResistancePlayer> tree = masterDecider.getTree(players[i]);
            onInclude2[i] = tree.getRootStatistics();
            onInclude3[i] = onInclude2[i].getSuccessorNode(new ActionWithRef<>(new IncludeInTeam(2), 1));
            onSupport[i] = onInclude3[i].getSuccessorNode(new ActionWithRef<>(new IncludeInTeam(3), 1));
            assertNotNull(onInclude2[i]);
            assertNotNull(onInclude3[i]);
            assertNotNull(onSupport[i]);
            rootVisits[i] = onInclude2[i].getVisits();
            onInclude2Visits[i] = onInclude2[i].getVisits(new IncludeInTeam((2)));
            onInclude3Visits[i] = onInclude3[i].getVisits(new IncludeInTeam(3));
            onSupportVisits[i] = onSupport[i].getVisits(new SupportTeam());
            onInclude2Mean[i] = onInclude2[i].getMean(new IncludeInTeam(2), 1)[i];
            onInclude3Mean[i] = onInclude3[i].getMean(new IncludeInTeam(3), 1)[i];
            onSupportMean[i] = onSupport[i].getMean(new SupportTeam(), i + 1)[i];
        }

        // Now we construct the trajectories that we will use (manually filtering for player visibility)
        trajectory = new List[5];
        List<Integer> missionTeam = new ArrayList<>();
        missionTeam.add(2);
        missionTeam.add(3);
        for (int i = 0; i < 5; i++) {
            trajectory[i] = new ArrayList<>();
            trajectory[i].add(new Triplet<>(singletonState, new ActionWithRef<>(new IncludeInTeam(2), 1), 0l));
            trajectory[i].add(new Triplet<>(singletonState, new ActionWithRef<>(new IncludeInTeam(3), 1), 0l));
            trajectory[i].add(new Triplet<>(singletonState, new ActionWithRef<>(new SupportTeam(), i + 1), 0l));
            trajectory[i].add(new Triplet<>(singletonState, new ActionWithRef<>(new VoteResult(new boolean[]{true, true, true, true, true}), -1), 0l));
            if (i == 1) trajectory[i].add(new Triplet<>(singletonState, new ActionWithRef<>(new Cooperate(), 2), 0l));
            if (i == 2) trajectory[i].add(new Triplet<>(singletonState, new ActionWithRef<>(new Defect(), 3), 0l));
            trajectory[i].add(new Triplet<>(singletonState, new ActionWithRef<>(new MissionResult(missionTeam, 1), -1), 0l));
        }
    }

    @Test
    public void BPStopsWithOLForAllPlayersWhenWeHaveAStartNode() {
        Map<Integer, MCStatistics> startNodeMap = new HashMap<>();
        // we then set a startNode for player 2 a few actions in....we only expect nodes between the root and this node to be
        // updated when trajectories are processed
        IntStream.range(0, 5).forEach(i -> startNodeMap.put(i + 1, onInclude3[i]));
        // We set the startNode to be the OnInclude3 Node. This is the Node at which player 1 takes the Include_3 action
        // we should only back-propagate to nodes above this point (not including the node)

        BackPropagationTactics bpTactics = new BackPropagationTactics(startNodeMap, new HashMap<>(), 5);
        for (int i = 0; i < 5; i++) {
            // we should now update visits and mean for Include3, but not Support
            masterDecider.getTree(players[i]).processTrajectory(trajectory[i], scores,
                    bpTactics.getStartNode(i + 1), bpTactics.getStopNode(i + 1));
            assertEquals(onInclude2[i].getVisits(), rootVisits[i] + 1);
            assertEquals(onInclude2[i].getVisits(new IncludeInTeam(2)), onInclude2Visits[i] + 1);
            assertEquals(onInclude3[i].getVisits(new IncludeInTeam(3)), onInclude3Visits[i]);
            assertEquals(onSupport[i].getVisits(new SupportTeam()), onSupportVisits[i]);

            assertEquals(onInclude2[i].getMean(new IncludeInTeam(2), 1)[i],
                    (onInclude2Mean[i] * onInclude2Visits[i] + scores[i]) / (onInclude2Visits[i] + 1), 0.01);
            assertEquals(onInclude3[i].getMean(new IncludeInTeam(3), 1)[i], onInclude3Mean[i], 0.01);
            assertEquals(onSupport[i].getMean(new SupportTeam(), i + 1)[i], onSupportMean[i], 0.01);
        }
    }

    @Test
    public void BPStartsWithOLForAllPlayersWhenWeHaveAStopNode() {
        Map<Integer, MCStatistics> stopNodeMap = new HashMap<>();
        // we then set a startNode for player 2 a few actions in....we only expect nodes between the root and this node to be
        // updated when trajectories are processed
        IntStream.range(0, 5).forEach(i -> stopNodeMap.put(i + 1, onInclude3[i])); // we should only back-propagate to nodes below this point (not including the node)
        BackPropagationTactics bpTactics = new BackPropagationTactics(new HashMap<>(), stopNodeMap, 5);
        for (int i = 0; i < 5; i++) {
            // we should now update visits and mean for Include3, but not Support
            masterDecider.getTree(players[i]).processTrajectory(trajectory[i], scores,
                    bpTactics.getStartNode(i + 1), bpTactics.getStopNode(i + 1));
            assertEquals(rootVisits[i], rootVisits[i]);
            assertEquals(onInclude2[i].getVisits(new IncludeInTeam(2)), onInclude2Visits[i]);
            assertEquals(onInclude3[i].getVisits(new IncludeInTeam(3)), onInclude3Visits[i] + 1);
            assertEquals(onSupport[i].getVisits(new SupportTeam()), onSupportVisits[i] + 1);

            assertEquals(onInclude2[i].getMean(new IncludeInTeam(2), 1)[i], onInclude2Mean[i], 0.01);
            assertEquals(onInclude3[i].getMean(new IncludeInTeam(3), 1)[i],
                    (onInclude3Mean[i] * onInclude3Visits[i] + scores[i]) / (onInclude3Visits[i] + 1), 0.01);
            assertEquals(onSupport[i].getMean(new SupportTeam(), i + 1)[i],
                    (onSupportMean[i] * onSupportVisits[i] + scores[i]) / (onSupportVisits[i] + 1), 0.01);
        }
    }

    @Test
    public void BPStartsAndStopsWithOLForAllPlayers() {
        Map<Integer, MCStatistics> startNodeMap = new HashMap<>();
        Map<Integer, MCStatistics> stopNodeMap = new HashMap<>();
        IntStream.range(0, 5).forEach(i -> stopNodeMap.put(i + 1, onInclude2[i])); // we should only back-propagate to nodes below this point (including the node)
        IntStream.range(0, 5).forEach(i -> startNodeMap.put(i + 1, onInclude3[i])); // we should only back-propagate to nodes above this point (not including the node)
        // this should mean that we only update the one node...onInclude2
        BackPropagationTactics bpTactics = new BackPropagationTactics(startNodeMap, stopNodeMap, 5);

        for (int i = 0; i < 5; i++) {
            masterDecider.getTree(players[i]).processTrajectory(trajectory[i], scores,
                    bpTactics.getStartNode(i + 1), bpTactics.getStopNode(i + 1));
            assertEquals(rootVisits[i], rootVisits[i]);
            assertEquals(onInclude2[i].getVisits(new IncludeInTeam(2)), onInclude2Visits[i] + 1);
            assertEquals(onInclude3[i].getVisits(new IncludeInTeam(3)), onInclude3Visits[i]);
            assertEquals(onSupport[i].getVisits(new SupportTeam()), onSupportVisits[i]);

            assertEquals(onInclude2[i].getMean(new IncludeInTeam(2), 1)[i],
                    (onInclude2Mean[i] * onInclude2Visits[i] + scores[i]) / (onInclude2Visits[i] + 1), 0.01);
            assertEquals(onInclude3[i].getMean(new IncludeInTeam(3), 1)[i], onInclude3Mean[i], 0.01);
            assertEquals(onSupport[i].getMean(new SupportTeam(), i + 1)[i], onSupportMean[i], 0.01);
        }
    }

    @Test
    public void BPReturnsTheCorrectNodes() {
        Map<Integer, MCStatistics> startNodeMap = new HashMap<>();
        Map<Integer, MCStatistics> stopNodeMap = new HashMap<>();
        IntStream.range(0, 5).forEach(i -> stopNodeMap.put(i + 1, onInclude2[i])); // we should only back-propagate to nodes below this point (including the node)
        IntStream.range(0, 5).forEach(i -> startNodeMap.put(i + 1, onSupport[i])); // we should only back-propagate to nodes above this point (not including the node)
        // this should mean that we only update the one node...onInclude2
        BackPropagationTactics bpTactics = new BackPropagationTactics(startNodeMap, stopNodeMap, 5);

        for (int i = 0; i < 5; i++) {
            assertSame(bpTactics.getStartNode(i+1), onSupport[i]);
            assertSame(bpTactics.getStopNode(i+1), onInclude2[i]);
        }
    }
}
