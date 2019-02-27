package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;

import hopshackle.simulation.MCTS.*;
import hopshackle.simulation.*;
import hopshackle.simulation.games.resistance.*;

import java.util.*;

import org.junit.*;


public class MCTSUtilitiesTest {

    OLMCTSMasterDecider<ResistancePlayer> ISMCTSDecider;
    Map<Integer, MonteCarloTree<ResistancePlayer>> treeMap = new HashMap<>();
    SingletonStateFactory<ResistancePlayer> factory = new SingletonStateFactory<>();
    Resistance game;
    BackPropagationTactics bpTactics = new BackPropagationTactics(new HashMap<>(), new HashMap<>(), 5);
    DeciderProperties localProp;
    int firstTraitor, firstLoyalist, secondTraitor;

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
        // get the master game to the right state
        game = new Resistance(5, 2, new World());
        firstTraitor = game.getTraitors().get(0);
        secondTraitor = game.getTraitors().get(1);
        game.applyAction((new IncludeInTeam(firstTraitor)).getAction(game.getCurrentPlayer()));
        assertSame(game.getPhase(), Resistance.Phase.ASSEMBLE);

        for (int i = 1; i <= game.getPlayerCount(); i++) {
            if (game.getTraitors().contains(i)) continue;
            firstLoyalist = i;
            game.applyAction((new IncludeInTeam(i)).getAction(game.getCurrentPlayer()));
            break;
        }
        // we should now have a full team, including one traitor, and one loyalist
        assertSame(game.getPhase(), Resistance.Phase.VOTE);
        for (int i = 1; i <= game.getPlayerCount(); i++) {
            game.applyAction(new SupportTeam().getAction(game.getCurrentPlayer()));
        }
        assertSame(game.getPhase(), Resistance.Phase.MISSION);
        ISMCTSDecider = new OLMCTSMasterDecider<>(game, factory, null, null);
        ISMCTSDecider.injectProperties(localProp);
        for (int i = 0; i < 5; i++) {
            treeMap.put(i + 1, ISMCTSDecider.getTree(game.getPlayer(i + 1)));
        }
    }

    @Test
    public void testLaunchGameWithInitialActions() {
        List<ActionWithRef<ResistancePlayer>> initialActions = new ArrayList<>();
        initialActions.add(new ActionWithRef(new Defect(), firstTraitor));
        List<Integer> missionTeam = new ArrayList<>();
        missionTeam.add(firstTraitor);
        missionTeam.add(firstLoyalist);
        for (int i = 0; i < 100; i++) {
            Resistance clonedGame = (Resistance) game.clone();
            clonedGame.getAllPlayers().forEach(p -> p.setDecider(ISMCTSDecider));
            int currentPlayer = clonedGame.getCurrentPlayerRef();
            MCTSChildDecider<ResistancePlayer> ISMCTSChildDecider = ISMCTSDecider.createChildDecider(clonedGame, treeMap.get(currentPlayer), currentPlayer, false);
            MCTSUtilities.launchGame(treeMap, clonedGame, ISMCTSChildDecider, null, localProp, bpTactics, initialActions);
        }
        // Now we check that in the trees generated, player 1 always Defects, and the others all have MissionResult with 1 Defection
        for (int i = 1; i <= 5; i++) {
            MonteCarloTree<ResistancePlayer> tree = treeMap.get(i);
            MCStatistics<ResistancePlayer> rootNode = tree.getRootStatistics();
            if (i == firstTraitor) {
                assertEquals(rootNode.getVisits(), 100);
                assertEquals(rootNode.getSuccessorNode(new ActionWithRef<>(new Defect(), firstTraitor)).getVisits(), 100);
                assertNull(rootNode.getSuccessorNode(new ActionWithRef<>(new Cooperate(), firstTraitor)));
            } else if (i == firstLoyalist) {
                assertEquals(rootNode.getVisits(), 100);
                assertEquals(rootNode.getSuccessorNode(new ActionWithRef<>(new Cooperate(), firstLoyalist)).getVisits(), 100);
                assertNull(rootNode.getSuccessorNode(new ActionWithRef<>(new Defect(), firstLoyalist)));
            } else {
                assertEquals(rootNode.getVisits(), 100);
                assertEquals(rootNode.getSuccessorNode(new ActionWithRef<>(new MissionResult(missionTeam, 1), -1)).getVisits(), 100);
                assertNull(rootNode.getSuccessorNode(new ActionWithRef<>(new MissionResult(missionTeam, 0), -1)));
            }
        }
    }

    @Test
    public void testLaunchGameWithInitialActionsAndAPD() {
        game.applyAction(new Defect().getAction(game.getCurrentPlayer()));
        while (!game.getTraitors().contains(firstLoyalist)) {
            int perspective = Dice.roll(1, 5);
            game.redeterminiseKeepingHiddenActions(perspective, perspective, Optional.empty());
        }
        assertTrue(game.getTraitors().contains(firstTraitor));
        assertTrue(game.getTraitors().contains(firstLoyalist));
        game.applyAction(new Defect().getAction(game.getCurrentPlayer()));
        // this is to force all games in the APD to have the same traitors (as we are not using a CRIS Decider in this test)

        List<ActionWithRef<ResistancePlayer>> initialActions = new ArrayList<>();
        initialActions.add(new ActionWithRef(new IncludeInTeam(2), 2));
        for (int i = 0; i < 100; i++) {
            Resistance clonedGame = (Resistance) game.clone();
            clonedGame.getAllPlayers().forEach(p -> p.setDecider(ISMCTSDecider));
            int currentPlayer = clonedGame.getCurrentPlayerRef();
            ResistanceAPD apd = new ResistanceAPD(clonedGame, currentPlayer);
            MCTSChildDecider<ResistancePlayer> ISMCTSChildDecider = ISMCTSDecider.createChildDecider(apd, treeMap.get(currentPlayer), currentPlayer, false);
            MCTSUtilities.launchGame(treeMap, apd, ISMCTSChildDecider, null, localProp, bpTactics, initialActions);
        }
        // Now we check that in the trees generated, player 1 always Defects, and the others all have MissionResult with 1 Defection
        for (int i = 1; i <= 5; i++) {
            MonteCarloTree<ResistancePlayer> tree = treeMap.get(i);
            MCStatistics<ResistancePlayer> rootNode = tree.getRootStatistics();
            assertEquals(rootNode.getVisits(), 100);
            MCStatistics<ResistancePlayer> include2Node = rootNode.getSuccessorNode(new ActionWithRef<>(new IncludeInTeam(2), 2));
            assertEquals(include2Node.getVisits(), 100);
            assertNull(rootNode.getSuccessorNode(new ActionWithRef<>(new IncludeInTeam(1), 2)));
            assertNull(rootNode.getSuccessorNode(new ActionWithRef<>(new IncludeInTeam(3), 2)));
            assertNull(rootNode.getSuccessorNode(new ActionWithRef<>(new IncludeInTeam(4), 2)));
            assertNull(rootNode.getSuccessorNode(new ActionWithRef<>(new IncludeInTeam(5), 2)));

            // then check that we look normal beyond that
            assertNotNull(include2Node.getSuccessorNode(new ActionWithRef<>(new IncludeInTeam(1), 2)));
            assertNull(include2Node.getSuccessorNode(new ActionWithRef<>(new IncludeInTeam(2), 2)));
            assertNotNull(include2Node.getSuccessorNode(new ActionWithRef<>(new IncludeInTeam(3), 2)));
            assertNotNull(include2Node.getSuccessorNode(new ActionWithRef<>(new IncludeInTeam(4), 2)));
            assertNotNull(include2Node.getSuccessorNode(new ActionWithRef<>(new IncludeInTeam(5), 2)));
            assertEquals(include2Node.getSuccessorNode(new ActionWithRef<>(new IncludeInTeam(1), 2)).getVisits(), 25, 10);
            assertEquals(include2Node.getSuccessorNode(new ActionWithRef<>(new IncludeInTeam(3), 2)).getVisits(), 25, 10);
            assertEquals(include2Node.getSuccessorNode(new ActionWithRef<>(new IncludeInTeam(4), 2)).getVisits(), 25, 10);
            assertEquals(include2Node.getSuccessorNode(new ActionWithRef<>(new IncludeInTeam(5), 2)).getVisits(), 25, 10);
        }
    }
}
