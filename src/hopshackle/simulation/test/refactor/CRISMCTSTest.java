package hopshackle.simulation.test.refactor;


import static org.junit.Assert.*;

import hopshackle.simulation.MCTS.*;
import hopshackle.simulation.*;
import hopshackle.simulation.games.*;
import hopshackle.simulation.games.resistance.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.*;

public class CRISMCTSTest {

    Resistance masterGame;
    TestCRISMCTSDecider CRISDecider;
    int firstTraitor, secondTraitor, secondLoyalist;
    SingletonStateFactory<ResistancePlayer> factory = new SingletonStateFactory<>();
    DeciderProperties localProp;

    @Before
    public void setup() {
        localProp = SimProperties.getDeciderProperties("GLOBAL");
        localProp.setProperty("MonteCarloReward", "true");
        localProp.setProperty("MonteCarloRL", "false");
        localProp.setProperty("MonteCarloUCTType", "MC");
        localProp.setProperty("MonteCarloUCTC", "5.0");
        localProp.setProperty("Gamma", "0.95");
        localProp.setProperty("TimePeriodForGamma", "1000");
        localProp.setProperty("IncrementalScoreReward", "false");
        localProp.setProperty("MonteCarloTimePerMove", "1000");
        localProp.setProperty("MonteCarloRolloutCount", "100");
        localProp.setProperty("MonteCarloActionValueRollout", "false");
        localProp.setProperty("MonteCarloActionValueOpponentModel", "false");
        localProp.setProperty("MonteCarloActionValueDeciderTemperature", "0.0");
        localProp.setProperty("MonteCarloRetainTreeBetweenActions", "false");
        localProp.setProperty("MonteCarloOpenLoop", "true");
        localProp.setProperty("MonteCarloTree", "perPlayer");
        localProp.setProperty("MonteCarloChoice", "default");
        localProp.setProperty("MonteCarloHeuristicOnSelection", "false");
        localProp.setProperty("MonteCarloRetainTreeBetweenActions", "false");
        localProp.setProperty("MonteCarloMAST", "false");
        localProp.setProperty("MaxTurnsPerGame", "10000");
        localProp.setProperty("GameOrdinalRewards", "0");
        Dice.setSeed(6l);
        do {
            masterGame = new Resistance(5, 2, new World());
        } while (masterGame.getTraitors().contains(1));
        firstTraitor = masterGame.getTraitors().get(0); // guaranteed not to be the first player
        secondTraitor = masterGame.getTraitors().get(1);
        secondLoyalist = IntStream.rangeClosed(2, 5).filter(l -> masterGame.getLoyalists().contains(l)).findFirst().getAsInt();
        assertNotEquals(secondLoyalist, 1);
        CRISDecider = new TestCRISMCTSDecider(masterGame, factory, new RandomDecider<>(factory), null);
        CRISDecider.injectProperties(localProp);
        masterGame.getAllPlayers().forEach(p -> p.setDecider(CRISDecider));
    }

    @Test
    public void getNextActionTest() {
        // in which we run through the actions of the first Mission, and check that we do branch at the correct points
        // first action to INCLUDE a person in Team
        List<ActionEnum<ResistancePlayer>> inclusionOptions = new ArrayList<>();
        IntStream.rangeClosed(1, 5).forEach(j -> inclusionOptions.add(new IncludeInTeam(j)));
        for (int i = 0; i < 100; i++)
            assertEquals(applyActionViaCRIS(masterGame, inclusionOptions), 0);
        for (int p = 1; p <= 5; p++) {
            assertEquals(CRISDecider.getTree(masterGame.getPlayer(p)).numberOfStates(), 1);
            assertEquals(CRISDecider.getTree(masterGame.getPlayer(p)).getRootStatistics().getVisits(), 0);
        }
        masterGame.apply(new ActionWithRef<>(new IncludeInTeam(secondLoyalist), 1));
        masterGame.getAllPlayers().forEach(p -> CRISDecider.getTree(p).reset());

        for (int i = 0; i < 100; i++)
            assertEquals(applyActionViaCRIS(masterGame, inclusionOptions), 0);
        masterGame.apply(new ActionWithRef<>(new IncludeInTeam(firstTraitor), 1));
        masterGame.getAllPlayers().forEach(p -> CRISDecider.getTree(p).reset());

        List<ActionEnum<ResistancePlayer>> voteOptions = new ArrayList<>();
        voteOptions.add(new SupportTeam());
        voteOptions.add(new RejectTeam());
        for (int i = 0; i < 100; i++)
            assertEquals(applyActionViaCRIS(masterGame, voteOptions), 0);
        for (int p = 1; p <= 5; p++) {
            assertEquals(CRISDecider.getTree(masterGame.getPlayer(p)).numberOfStates(), 1);
            assertEquals(CRISDecider.getTree(masterGame.getPlayer(p)).getRootStatistics().getVisits(), 0);
        }
        masterGame.apply(new ActionWithRef<>(new SupportTeam(), 1));
        masterGame.getAllPlayers().forEach(p -> CRISDecider.getTree(p).reset());

        IntStream.rangeClosed(2, 5).forEach(j -> masterGame.apply(new ActionWithRef<>(new SupportTeam(), j)));

        List<ActionEnum<ResistancePlayer>> traitorOptions = new ArrayList<>();
        traitorOptions.add(new Cooperate());
        traitorOptions.add(new Defect());
        // we now need the default choice to be DEFECT...
        MCStatistics rootStats = CRISDecider.getTree(masterGame.getCurrentPlayer()).getRootStatistics();
        rootStats.update(new Cooperate(), new double[]{-2.0, -2.0, -2.0, -2.0, -2.0}, secondLoyalist);
        assertEquals(rootStats.getNextAction(traitorOptions, secondLoyalist), new Defect());
        int incrementalSpawnCount = 0;
        assertEquals(rootStats.getVisits(), 1);
        for (int i = 0; i < 10; i++) {
            ResistanceAPD apd = apdFromGame(masterGame, 1); //which means that sometime we should have a D that has secondLoyalist as a traitor from their perspective
            while (apd.getDeterminisationFor(secondLoyalist).getLoyalists().contains(secondLoyalist)) {
                apd = apdFromGame(masterGame, 1);
                // we keep going until we know that secondLoyalist (at the root D) thinks they are a traitor in their own D
            }
            if (i > 0 || apd.isValid(new ActionWithRef<>(new Defect(), secondLoyalist), apd.root)) {
                assertEquals(applyActionViaCRIS(apd, traitorOptions), 0);
            } else {
                int spawnCount = applyActionViaCRIS(apd, traitorOptions);
                assertNotEquals(spawnCount, 0);
                incrementalSpawnCount += spawnCount;
                assertEquals(rootStats.getVisits(), incrementalSpawnCount + 1);
                assertEquals(rootStats.getUCTAction(traitorOptions, secondLoyalist), new Cooperate());
                // as once we choose Cooperate, we'll choose it for all future UCT actions (as we only back-propagate after
                // the spawned defections
            }
        }
        assertEquals(incrementalSpawnCount, 3);
        for (int p = 1; p <= 5; p++) {
            assertEquals(CRISDecider.getTree(masterGame.getPlayer(p)).numberOfStates(), 1 + incrementalSpawnCount);
            // we did one rootStats.update earlier, so secondLoyalist will have had one more visit
            assertEquals(CRISDecider.getTree(masterGame.getPlayer(p)).getRootStatistics().getVisits(), (p == secondLoyalist ? 1 : 0) + incrementalSpawnCount);
        }
        masterGame.apply(new ActionWithRef<>(new Cooperate(), secondLoyalist));
        masterGame.getAllPlayers().forEach(p -> CRISDecider.getTree(p).reset());

        List<ActionEnum<ResistancePlayer>> loyalistOptions = new ArrayList<>();
        loyalistOptions.add(new Cooperate());
        for (int i = 0; i < 100; i++)
            assertEquals(applyActionViaCRIS(masterGame, voteOptions), 0);
        for (int p = 1; p <= 5; p++) {
            assertEquals(CRISDecider.getTree(masterGame.getPlayer(p)).numberOfStates(), 1);
            assertEquals(CRISDecider.getTree(masterGame.getPlayer(p)).getRootStatistics().getVisits(), 0);
        }
    }

    @Test
    public void branchesOccurCorrectlyDuringTreeSearch() {
        // firstly we have to construct a tree. The easiest way to do this is to run oneAction()
        // using MCTSMasterDecider
        OLMCTSMasterDecider<ResistancePlayer> ISMCTSDecider = new OLMCTSMasterDecider<>(masterGame, factory, new RandomDecider<>(factory), null);
        localProp.setProperty("MonteCarloRolloutCount", "500");
        localProp.setProperty("MonteCarloTimePerMove", "3000");
        ISMCTSDecider.injectProperties(localProp);
        masterGame.apply(new ActionWithRef<>(new IncludeInTeam(secondLoyalist), 1));
        masterGame.apply(new ActionWithRef<>(new IncludeInTeam(firstTraitor), 1));
        masterGame.getAllPlayers().forEach(p -> p.setDecider(ISMCTSDecider));
        Map<Integer, MonteCarloTree<ResistancePlayer>> treeMap = new HashMap<>();
        IntStream.rangeClosed(1, 5).forEach(i -> treeMap.put(i, ISMCTSDecider.getTree(masterGame.getPlayer(i))));

        Resistance gameToTest = (Resistance) masterGame.clone();
        masterGame.oneAction();
        assertEquals(treeMap.get(1).getRootStatistics().getVisits(), 500);
        // this reaches to the Cooperate/Defect level on the first mission, after voting has taken place

        // now we switch over the Decider, while keeping the trees
        CRISDecider.setTreeMap(treeMap);
        ResistanceAPD apd = new ResistanceAPD(gameToTest, gameToTest.getCurrentPlayerRef(), "perPlayer", treeMap);
        while (apd.getDeterminisationFor(secondLoyalist).getLoyalists().contains(secondLoyalist)) {
            apd = new ResistanceAPD(gameToTest, gameToTest.getCurrentPlayerRef(), "perPlayer", treeMap);
            // we keep going until we know that secondLoyalist (at the root D) thinks they are a traitor in their own D
        }
        for (ResistancePlayer p : apd.getAllPlayers()) {
            p.setDecider(CRISDecider.createChildDecider(apd, treeMap, 1, false));
        }
        // we should now be good to go with player 1 to vote (and therefore a known Loyalist as root)
        assertEquals(apd.getCurrentPlayerRef(), 1);
        /*
        We need to check the actual spawned games to make sure they update the tree properly

        we treat masterGame as the rollout game from CRIS. We can then apply actions to it, and check when we get to the
        DEFECT/COOPERATE stage
         */
        List<ActionEnum<ResistancePlayer>> voteOptions = new ArrayList<>();
        voteOptions.add(new SupportTeam());
        voteOptions.add(new RejectTeam());
        OpenLoopTreeTracker<ResistancePlayer> treeTracker = new OpenLoopTreeTracker<>("perPlayer", treeMap, apd);
        int startingStates = treeMap.get(1).numberOfStates();
        List<Integer> startingVisits = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> treeTracker.getCurrentNode(i).getVisits())
                .collect(Collectors.toList());
        List<MCStatistics<ResistancePlayer>> initialNodes = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> treeTracker.getCurrentNode(i))
                .collect(Collectors.toList());
        for (int i = 0; i < 100; i++) {
            Decider temp = apd.getCurrentPlayer().getDecider();
            TestCRISChildDecider childDecider = (TestCRISChildDecider) temp;
            ActionEnum<ResistancePlayer> choice = childDecider.getNextTreeAction(factory.getCurrentState(apd.getCurrentPlayer()), voteOptions, apd.getCurrentPlayerRef());
            assertEquals(childDecider.spawnCount, 0);
        }
        for (int p = 1; p <= 5; p++) { // no branching
            assertEquals(CRISDecider.getTree(apd.getPlayer(p)).numberOfStates(), startingStates);
            assertEquals(treeTracker.getCurrentNode(p).getVisits(), startingVisits.get(p - 1), 0);
        }
        // then all vote in favour
        for (int j = 1; j <= 5; j++) apd.apply(new ActionWithRef<>(new SupportTeam(), j));

        List<MCStatistics> currentNodes = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Decider temp = apd.getCurrentPlayer().getDecider();
            TestCRISChildDecider childDecider = (TestCRISChildDecider) temp;
            currentNodes.add(childDecider.getCurrentNode(i + 1));
            assertNotSame(initialNodes.get(i), currentNodes.get(i));
            if (i == 1 || i == 2)
                assertTrue(currentNodes.get(i).getPossibleActions().stream().allMatch(a -> {
                    ActionWithRef actionWithRef = (ActionWithRef) a;
                    return actionWithRef.actionTaken instanceof Defect || actionWithRef.actionTaken instanceof Cooperate;
                }));
            else
                assertTrue(currentNodes.get(i).getPossibleActions().stream().allMatch(a -> {
                    ActionWithRef actionWithRef = (ActionWithRef) a;
                    return actionWithRef.actionTaken instanceof MissionResult;
                }));

        }
        // Now we need to make sure that our local treeTracker has kept up (with lower visit counts)
        // How do we check that the launched Game is also using the correct tree...?
        // ...we can use the new getCurrentNode that is public in TestCRISChildDecider
        // the issue to now test is whether the spawned games start from the correct point and increment the visits
        // to the correct node
        List<ActionEnum<ResistancePlayer>> traitorOptions = new ArrayList<>();
        traitorOptions.add(new Cooperate());
        traitorOptions.add(new Defect());
        // we now need the default choice to be DEFECT...
        while (!currentNodes.get(secondLoyalist - 1).getNextAction(traitorOptions, secondLoyalist).equals(new Defect())) {
            currentNodes.get(secondLoyalist - 1).update(new Cooperate(), new double[]{-2.0, -2.0, -2.0, -2.0, -2.0}, secondLoyalist);
        }
        assertEquals(currentNodes.get(secondLoyalist - 1).getNextAction(traitorOptions, secondLoyalist), new Defect());
        int incrementalSpawnCount = 0;
        startingVisits = IntStream.range(0, 5)
                .mapToObj(i -> currentNodes.get(i).getVisits())
                .collect(Collectors.toList());
        int firstIterationSpawnCount = 0;
        for (int i = 0; i < 10; i++) {
            Decider temp = apd.getCurrentPlayer().getDecider();
            TestCRISChildDecider childDecider = (TestCRISChildDecider) temp;
            ActionEnum<ResistancePlayer> choice = childDecider.getNextTreeAction(factory.getCurrentState(apd.getCurrentPlayer()), traitorOptions, apd.getCurrentPlayerRef());
            assertNotEquals(childDecider.spawnCount, 0);
            if (i == 0) firstIterationSpawnCount = childDecider.spawnCount;
            assertEquals(childDecider.spawnCount, firstIterationSpawnCount);
            assertEquals(choice, new Cooperate()); // will always be true as the ultimate choice
            // and then we check that visits have been add to the currentNodes
            for (int j = 0; j < 5; j++) {
                assertEquals(currentNodes.get(j).getVisits(), startingVisits.get(j) + childDecider.spawnCount);
                assertEquals(CRISDecider.getTree(apd.getPlayer(j + 1)).numberOfStates(), 501);
                assertEquals(CRISDecider.getTree(apd.getPlayer(j + 1)).getRootStatistics().getVisits(), 500);
            }
        }
    }

    private ResistanceAPD apdFromGame(Resistance game, int rootPlayer) {
        Map<Integer, MonteCarloTree> treeMap = game.getAllPlayers().stream()
                .collect(Collectors.toMap(ResistancePlayer::getPlayerNumber, CRISDecider::getTree));
        return (ResistanceAPD) AllPlayerDeterminiser.getAPD(game.clone(), rootPlayer, "perPlayer", treeMap);
    }

    private int applyActionViaCRIS(Resistance masterGame, List<ActionEnum<ResistancePlayer>> chooseableOptions) {
        ResistanceAPD apd = apdFromGame(masterGame, 1);
        return applyActionViaCRIS(apd, chooseableOptions);
    }

    private int applyActionViaCRIS(ResistanceAPD apd, List<ActionEnum<ResistancePlayer>> chooseableOptions) {
        TestCRISChildDecider childDecider = CRISDecider.createChildDecider(apd, CRISDecider.getTreeMap(), apd.getCurrentPlayerRef(), false);
        ActionEnum<ResistancePlayer> choice = childDecider.getNextTreeAction(factory.getCurrentState(apd.getCurrentPlayer()), chooseableOptions, apd.getCurrentPlayerRef());
        return childDecider.spawnCount;
    }

}

class TestCRISMCTSDecider extends CRISMCTSDecider<ResistancePlayer> {

    public TestCRISMCTSDecider(Resistance game, StateFactory<ResistancePlayer> stateFactory, BaseStateDecider<ResistancePlayer> rolloutDecider, Decider<ResistancePlayer> opponentModel) {
        super(game, stateFactory, rolloutDecider, opponentModel);
    }

    public void setTreeMap(Map<Integer, MonteCarloTree<ResistancePlayer>> newMap) {
        treeMap = newMap;
    }
    public Map<Integer, MonteCarloTree<ResistancePlayer>> getTreeMap() {return treeMap;}

    @Override
    public TestCRISChildDecider createChildDecider(Game clonedGame, Map<Integer, MonteCarloTree<ResistancePlayer>> localTreeMap, int currentPlayer, boolean opponent) {
        TestCRISChildDecider retValue;
        // new for CRIS is that we generate an APD around the cloned game
        if (!(clonedGame instanceof AllPlayerDeterminiser)) {
            throw new AssertionError("This should be an APD");
        }
        AllPlayerDeterminiser apd = (AllPlayerDeterminiser) clonedGame;

        OpenLoopTreeTracker<ResistancePlayer> treeTracker = new OpenLoopTreeTracker<>(treeSetting, localTreeMap, clonedGame);

        retValue = new TestCRISChildDecider(apd, this, stateFactory, treeTracker, rolloutDecider, decProp);

        retValue.setName("Child_TestCRISMCTS");
        return retValue;
    }
}

class TestCRISChildDecider extends CRISChildDecider<ResistancePlayer> {

    public int spawnCount;

    public TestCRISChildDecider(AllPlayerDeterminiser<Game<ResistancePlayer, ActionEnum<ResistancePlayer>>, ResistancePlayer> apd,
                                TestCRISMCTSDecider master, StateFactory<ResistancePlayer> stateFactory,
                                OpenLoopTreeTracker<ResistancePlayer> treeTracker, Decider<ResistancePlayer> rolloutDecider, DeciderProperties prop) {
        super(apd, master, stateFactory, treeTracker, rolloutDecider, prop);
    }

    @Override
    public ActionEnum<ResistancePlayer> getNextTreeAction(State<ResistancePlayer> state, List<ActionEnum<ResistancePlayer>> chooseableOptions, int decidingAgentRef) {
        return super.getNextTreeAction(state, chooseableOptions, decidingAgentRef);
    }

    @Override
    public void spawnBranch(ActionEnum<ResistancePlayer> initialChoice, int decidingAgentRef) {
        spawnCount++;
        super.spawnBranch(initialChoice, decidingAgentRef);
    }

    public MCStatistics<ResistancePlayer> getCurrentNode(int player) {
        return treeTracker.getCurrentNode(player);
    }
}