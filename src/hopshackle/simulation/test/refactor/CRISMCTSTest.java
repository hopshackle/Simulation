package hopshackle.simulation.test.refactor;


import static org.junit.Assert.*;

import hopshackle.simulation.MCTS.*;
import hopshackle.simulation.*;
import hopshackle.simulation.games.*;
import hopshackle.simulation.games.resistance.*;

import java.util.*;
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
        List<ActionEnum<ResistancePlayer>> loyalistOptions = new ArrayList<>();
        loyalistOptions.add(new Cooperate());

        // we now need the default choice to be DEFECT...
        MCStatistics rootStats = CRISDecider.getTree(masterGame.getCurrentPlayer()).getRootStatistics();
        rootStats.update(new Cooperate(), new double[]{-2.0, -2.0, -2.0, -2.0, -2.0}, secondLoyalist);
        assertEquals(rootStats.getNextAction(traitorOptions, secondLoyalist), new Defect());
        boolean uctChoiceHasFlippedToCooperate = false;
        int incrementalSpawnCount = 0;
        for (int i = 0; i < 100; i++) {
            ResistanceAPD apd = apdFromGame(masterGame, 1); //which means that sometime we should have a D that has secondLoyalist as a traitor from their perspective
            while (apd.getDeterminisationFor(secondLoyalist).getLoyalists().contains(secondLoyalist)) {
                apd = apdFromGame(masterGame, 1);
                // we keep going until we know that secondLoyalist (at the root D) thinks they are a traitor in their own D
            }
            if (uctChoiceHasFlippedToCooperate || apd.isValid(new ActionWithRef<>(new Defect(), secondLoyalist), apd.root)) {
                assertEquals(applyActionViaCRIS(apd, traitorOptions), 0);
            } else {
                int spawnCount = applyActionViaCRIS(apd, traitorOptions);
                assertNotEquals(spawnCount, 0);
                incrementalSpawnCount += spawnCount;
                assertEquals(rootStats.getVisits(), incrementalSpawnCount);
                if (rootStats.getUCTAction(traitorOptions, secondLoyalist).equals(new Defect()))
                    uctChoiceHasFlippedToCooperate = true;
            }
        }
        assertTrue(incrementalSpawnCount > 0);
        for (int p = 1; p <= 5; p++) {
            assertEquals(CRISDecider.getTree(masterGame.getPlayer(p)).numberOfStates(), 1 + incrementalSpawnCount);
            // we did one rootStats.update earlier, so player 2 will have had one more visit
            assertEquals(CRISDecider.getTree(masterGame.getPlayer(p)).getRootStatistics().getVisits(), (p == 2 ? 1 : 0) + incrementalSpawnCount);
        }
    }

    private ResistanceAPD apdFromGame(Resistance game, int rootPlayer) {
        return (ResistanceAPD) AllPlayerDeterminiser.getAPD(game.clone(), rootPlayer);
    }

    private int applyActionViaCRIS(Resistance masterGame, List<ActionEnum<ResistancePlayer>> chooseableOptions) {
        ResistanceAPD apd = apdFromGame(masterGame, 1);
        return applyActionViaCRIS(apd, chooseableOptions);
    }

    private int applyActionViaCRIS(ResistanceAPD apd, List<ActionEnum<ResistancePlayer>> chooseableOptions) {
        TestCRISChildDecider childDecider = CRISDecider.createChildDecider(apd, CRISDecider.getTree(apd.getCurrentPlayer()), apd.getCurrentPlayerRef(), false);
        ActionEnum<ResistancePlayer> choice = childDecider.getNextTreeAction(factory.getCurrentState(apd.getCurrentPlayer()), chooseableOptions, apd.getCurrentPlayerRef());
        return childDecider.spawnCount;
    }

}

class TestCRISMCTSDecider extends CRISMCTSDecider<ResistancePlayer> {

    public TestCRISMCTSDecider(Resistance game, StateFactory<ResistancePlayer> stateFactory, BaseStateDecider<ResistancePlayer> rolloutDecider, Decider<ResistancePlayer> opponentModel) {
        super(game, stateFactory, rolloutDecider, opponentModel);
    }

    @Override
    public TestCRISChildDecider createChildDecider(Game clonedGame, MonteCarloTree<ResistancePlayer> tree, int currentPlayer, boolean opponent) {
        TestCRISChildDecider retValue;
        // new for CRIS is that we generate an APD around the cloned game
        if (!(clonedGame instanceof AllPlayerDeterminiser)) {
            throw new AssertionError("This should be an APD");
        }
        AllPlayerDeterminiser apd = (AllPlayerDeterminiser) clonedGame;

        OpenLoopTreeTracker<ResistancePlayer> treeTracker = new OpenLoopTreeTracker<>(treeSetting, treeMap, clonedGame);

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
}