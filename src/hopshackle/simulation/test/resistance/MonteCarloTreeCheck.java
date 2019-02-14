package hopshackle.simulation.test.resistance;

import java.util.*;
import java.util.stream.IntStream;

import hopshackle.simulation.MCTS.*;
import hopshackle.simulation.*;

import static org.junit.Assert.*;

import hopshackle.simulation.games.resistance.*;
import org.junit.*;

public class MonteCarloTreeCheck {

    private Resistance game;
    private ResistancePlayer[] players;
    private DeciderProperties localProp;
    private OpenLoopMCTree<ResistancePlayer> tree;
    private MCTSMasterDecider<ResistancePlayer> masterDecider;
    private SingletonStateFactory<ResistancePlayer> singletonStateFactory = new SingletonStateFactory<>();
    private BaseStateDecider<ResistancePlayer> rolloutDecider = new RandomDecider<>(singletonStateFactory);
    private World world = new World();

    @Before
    public void setUp() {
        localProp = SimProperties.getDeciderProperties("GLOBAL");
        localProp.setProperty("MonteCarloTree", "single");
        localProp.setProperty("MonteCarloReward", "true");
        localProp.setProperty("MonteCarloRL", "false");
        localProp.setProperty("MonteCarloUCTType", "MC");
        localProp.setProperty("MonteCarloUCTC", "5.0");
        localProp.setProperty("Gamma", "1.0");
        localProp.setProperty("TimePeriodForGamma", "1000");
        localProp.setProperty("IncrementalScoreReward", "false");
        localProp.setProperty("MonteCarloRolloutCount", "5000");
        localProp.setProperty("MonteCarloTimePerMove", "10000");
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
        localProp.setProperty("MonteCarloRandomTieBreaks", "true");
        localProp.setProperty("MonteCarloParentalVisitValidity", "true");
        Dice.setSeed(6l);
        game = new Resistance(5, 2, world);
    }

    private void setupGame() {
        masterDecider = new MCTSMasterDecider<>(singletonStateFactory, rolloutDecider, rolloutDecider);
        masterDecider.injectProperties(localProp);
        players = new ResistancePlayer[5];

        for (int i = 0; i < 5; i++) {
            players[i] = game.getPlayer(i + 1);
            players[i].setDecider(masterDecider);
        }
    }

    @Test
    public void checkNodesInTreeForOneTreePerPlayer() {
        localProp.setProperty("MonteCarloTree", "perPlayer");
        setupGame();
        do {
            if (players[0].isTraitor()) {
                int nonTraitor = players[1].isTraitor() ? 3 : 2;
                game.redeterminise(nonTraitor, nonTraitor, Optional.empty());
                setupGame();
            }
        } while (players[0].isTraitor());

        List<Integer> traitors = game.getTraitors();
        // include ourselves in team as first action
        game.applyAction(new IncludeInTeam(1).getAction(game.getCurrentPlayer()));
        OpenLoopMCTree[] allTrees = new OpenLoopMCTree[6];
        IntStream.rangeClosed(1, 5).forEach(p -> allTrees[p] = (OpenLoopMCTree) masterDecider.getTree(players[p - 1]));

        assertEquals(game.getPossibleActions().size(), 4);  // Include one of remaining 4 players
        game.oneAction();
        assertEquals(game.getPossibleActions().size(), 2);  // Vote for or against

        // We now check each of the five trees in turn
        // Player 1 (not Traitor)
        // 4 options to include the next team member, then Support/Reject in favour
        // then on each of those, a number of possible VoteResults (but all with player 1 voting the right way)
        // Vote results that pass lead to Cooperate, then MissionResult
        // Vote results that fail lead to IncludeInTeam by player 2
        // Player 2-5 (traitor)
        // 4 options to include the next team member (by p1), then Support/Reject in favour (by p2-5)
        // then on each of those, a number of possible VoteResults (but all with p2-5 voting the right way)
        // Vote results that pass lead to :
        // Cooperate/Defect if on Mission Team, then MissionResult [Defect only if Traitor]
        // or else just MissionResult
        // MissionResult or failed vote result always successed by IncludeInTeam for p2

        for (int player = 1; player <= 5; player++) {
            MCStatistics<ResistancePlayer> rootStats = allTrees[player].getRootStatistics();
            assertEquals(rootStats.getPossibleActions(1).size(), 4);
            assertEquals(rootStats.getPossibleActions().size(), 4);

            MCStatistics<ResistancePlayer>[] includeNext = new MCStatistics[6];
            for (int otherTeamMember = 2; otherTeamMember <= 5; otherTeamMember++) {
                includeNext[otherTeamMember] = rootStats.getSuccessorNode(new ActionWithRef<>(new IncludeInTeam(otherTeamMember), 1));
                assertEquals(includeNext[otherTeamMember].getPossibleActions().size(), 2);
                if (!includeNext[otherTeamMember].getPossibleActions(player).contains(new SupportTeam())) {
                    throw new AssertionError("HMM");
                }
                assertTrue(includeNext[otherTeamMember].getPossibleActions(player).contains(new SupportTeam()));
                assertTrue(includeNext[otherTeamMember].getPossibleActions(player).contains(new RejectTeam()));
                MCStatistics<ResistancePlayer>[] vote = new MCStatistics[2];
                vote[0] = includeNext[otherTeamMember].getSuccessorNode(new ActionWithRef<>(new SupportTeam(), player));
                vote[1] = includeNext[otherTeamMember].getSuccessorNode(new ActionWithRef<>(new RejectTeam(), player));
                List<Integer> team = new ArrayList<>();
                team.add(1);
                team.add(otherTeamMember);

                for (int i = 0; i < 2; i++) {
                    // for each way of voting...if we have voted one way, there are 2^4 = 16 possible vote results
                    assertEquals(vote[i].getPossibleActions().size(), 16);
                    for (ActionWithRef action : vote[i].getPossibleActions()) {
                        assertTrue(action.actionTaken instanceof VoteResult);
                        assertEquals(action.agentRef, -1);
                        VoteResult vr = (VoteResult) action.actionTaken;
                        if (i == 0) {
                            assertTrue(vr.voteOfPlayer(player));
                        } else {
                            assertFalse(vr.voteOfPlayer(player));
                        }
                        MCStatistics<ResistancePlayer> voteResultStats = vote[i].getSuccessorNode(action);
                        MCStatistics<ResistancePlayer> missionResultStats = null;
                        if (vr.isPassed()) {
                            if (player == 1 || player == otherTeamMember) {
                                // were on the mission. Dur to randomisation, 1 is the only player who will not be a traitor
                                if (player != 1) {
                                    assertEquals(voteResultStats.getPossibleActions().size(), 2);
                                    assertTrue(voteResultStats.getPossibleActions().contains(new ActionWithRef<>(new Cooperate(), player)));
                                    assertTrue(voteResultStats.getPossibleActions().contains(new ActionWithRef<>(new Defect(), player)));
                                    missionResultStats = voteResultStats.getSuccessorNode(new ActionWithRef<>(new Defect(), player));
                                    missionResultStats = missionResultStats.getSuccessorNode(new ActionWithRef<>(new MissionResult(team, 1), -1));
                                } else {
                                    assertEquals(voteResultStats.getPossibleActions().size(), 1);
                                    assertTrue(voteResultStats.getPossibleActions().get(0).actionTaken instanceof Cooperate);
                                    assertEquals(voteResultStats.getPossibleActions().get(0).agentRef, player);
                                    missionResultStats = voteResultStats.getSuccessorNode(new ActionWithRef<>(new Cooperate(), player));
                                    missionResultStats = missionResultStats.getSuccessorNode(new ActionWithRef<>(new MissionResult(team, 0), -1));
                                }
                            } else {
                                missionResultStats = voteResultStats.getSuccessorNode(new ActionWithRef<>(new MissionResult(team, 0), -1));
                            }
                            if (missionResultStats.getVisits() > 10) {
                                assertEquals(missionResultStats.getPossibleActions().size(), 5);
                                final MCStatistics<ResistancePlayer> temp = missionResultStats;
                                IntStream.rangeClosed(1, 5).
                                        forEach(k -> {
                                            ActionEnum<ResistancePlayer> includeInTeam = new IncludeInTeam(k);
                                            assertTrue(temp.getPossibleActions(2).contains(includeInTeam));
                                            if (temp.getVisits(includeInTeam) > 1)
                                                assertNotNull(temp.getSuccessorNode(new ActionWithRef<>(includeInTeam, 2)));
                                        });
                            }
                        } else {
                            // Vote was rejected
                            final MCStatistics<ResistancePlayer> temp = voteResultStats;
                            IntStream.rangeClosed(1, 5).
                                    forEach(k -> {
                                        assertTrue(temp.getPossibleActions(2).contains(new IncludeInTeam(k)));
                                        assertNotNull(temp.getSuccessorNode(new ActionWithRef<>(new IncludeInTeam(k), 2)));
                                    });
                        }
                    }
                }
            }
        }
    }

    @Test
    public void checkNodesInTreeForSingleTreeNotTraitor() {
        localProp.setProperty("MonteCarloTree", "single");
        setupGame();
        do {
            if (players[0].isTraitor()) {
                int nonTraitor = players[1].isTraitor() ? 3 : 2;
                game.redeterminise(nonTraitor, nonTraitor, Optional.empty());
                setupGame();
            }
        } while (players[0].isTraitor());
        // include ourselves in team as first action
        game.applyAction(new IncludeInTeam(1).getAction(game.getCurrentPlayer()));
        tree = (OpenLoopMCTree) masterDecider.getTree(players[0]);
        assertEquals(tree.numberOfStates(), 1);
        assertEquals(game.getPossibleActions().size(), 4);  // Include one of remaining 4 players
        game.oneAction();
        assertEquals(game.getPossibleActions().size(), 2);  // Vote for or against

        MCStatistics<ResistancePlayer> rootStats = tree.getRootStatistics();
        assertEquals(rootStats.getPossibleActions(1).size(), 4);
        assertEquals(rootStats.getPossibleActions().size(), 4);
        assertEquals(rootStats.getVisits(), 5000);

        // Now we check we have the following structure
        // 4 options to include the next one
        // then Support/Reject for each of the five players in turn
        // then one VoteResult per leaf..leading to either an Include if rejected..or a Cooperate or Cooperate/Defect if supported

        MCStatistics<ResistancePlayer>[] includeNext = new MCStatistics[6];
        for (int i = 2; i <= 5; i++) {
            includeNext[i] = rootStats.getSuccessorNode(new ActionWithRef<>(new IncludeInTeam(i), 1));
            assertEquals(includeNext[i].getPossibleActions().size(), 2);
            assertTrue(includeNext[i].getPossibleActions(1).contains(new SupportTeam()));
            assertTrue(includeNext[i].getPossibleActions(1).contains(new RejectTeam()));
            MCStatistics<ResistancePlayer>[] vote = new MCStatistics[2];
            vote[0] = includeNext[i].getSuccessorNode(new ActionWithRef<>(new SupportTeam(), 1));
            vote[1] = includeNext[i].getSuccessorNode(new ActionWithRef<>(new RejectTeam(), 1));
            for (int voter = 2; voter <= 5; voter++) { // All Support chain
                assertEquals(vote[0].getPossibleActions().size(), 2);
                assertTrue(vote[0].getPossibleActions(voter).contains(new SupportTeam()));
                assertTrue(vote[0].getPossibleActions(voter).contains(new RejectTeam()));
                vote[0] = vote[0].getSuccessorNode(new ActionWithRef<>(new SupportTeam(), voter));
                if (voter == 5) {
                    assertEquals(vote[0].getPossibleActions().size(), 1);
                    assertTrue(vote[0].getPossibleActions(-1).get(0) instanceof VoteResult);
                    VoteResult vr = (VoteResult) vote[0].getPossibleActions(-1).get(0);
                    assertTrue(vr.isPassed());
                    MCStatistics<ResistancePlayer> voteResultStats = vote[0].getSuccessorNode(new ActionWithRef<>(vr, -1));
                    assertEquals(voteResultStats.getPossibleActions().size(), 1);
                    assertTrue(voteResultStats.getPossibleActions().get(0).actionTaken instanceof Cooperate);
                    assertEquals(voteResultStats.getPossibleActions().get(0).agentRef, 1);
                }
            }

            for (int voter = 2; voter <= 5; voter++) { // All Reject chain
                assertEquals(vote[1].getPossibleActions().size(), 2);
                assertTrue(vote[1].getPossibleActions(voter).contains(new SupportTeam()));
                assertTrue(vote[1].getPossibleActions(voter).contains(new RejectTeam()));
                vote[1] = vote[1].getSuccessorNode(new ActionWithRef<>(new RejectTeam(), voter));
                if (voter == 5) {
                    assertEquals(vote[1].getPossibleActions().size(), 1);
                    assertTrue(vote[1].getPossibleActions(-1).get(0) instanceof VoteResult);
                    VoteResult vr = (VoteResult) vote[1].getPossibleActions(-1).get(0);
                    assertFalse(vr.isPassed());
                    MCStatistics<ResistancePlayer> voteResultStats = vote[1].getSuccessorNode(new ActionWithRef<>(vr, -1));
                    assertEquals(voteResultStats.getPossibleActions().size(), 5);
                    assertTrue(voteResultStats.getPossibleActions().get(0).actionTaken instanceof IncludeInTeam);
                    assertEquals(voteResultStats.getPossibleActions().get(0).agentRef, 2);
                }
            }
        }
    }

    @Test
    public void checkNodesInTreeForIgnoreOthersNotTraitor() {
        localProp.setProperty("MonteCarloTree", "ignoreOthers");
        setupGame();
        do {
            if (players[0].isTraitor()) {
                int nonTraitor = players[1].isTraitor() ? 3 : 2;
                game.redeterminise(nonTraitor, nonTraitor, Optional.empty());
                setupGame();
            }
        } while (players[0].isTraitor());
        // include ourselves in team as first action
        game.applyAction(new IncludeInTeam(1).getAction(game.getCurrentPlayer()));
        tree = (OpenLoopMCTree) masterDecider.getTree(players[0]);
        assertEquals(tree.numberOfStates(), 1);
        assertEquals(game.getPossibleActions().size(), 4);  // Include one of remaining 4 players
        game.oneAction();
        assertEquals(game.getPossibleActions().size(), 2);  // Vote for or against

        MCStatistics<ResistancePlayer> rootStats = tree.getRootStatistics();
        assertEquals(rootStats.getPossibleActions(1).size(), 4);
        assertEquals(rootStats.getPossibleActions().size(), 4);
        assertEquals(rootStats.getVisits(), 5000);

        // Now we check we have the following structure
        // 4 options to include the next one
        // then a Cooperate/Support/Reject branch...as from our perspective any of these could be next
        // depending on whether the vote was successful or not

        MCStatistics<ResistancePlayer>[] includeNext = new MCStatistics[6];
        for (int i = 2; i <= 5; i++) {
            includeNext[i] = rootStats.getSuccessorNode(new ActionWithRef<>(new IncludeInTeam(i), 1));
            assertEquals(includeNext[i].getPossibleActions(1).size(), 2);
            assertTrue(includeNext[i].getPossibleActions(1).contains(new SupportTeam()));
            assertTrue(includeNext[i].getPossibleActions(1).contains(new RejectTeam()));
            MCStatistics<ResistancePlayer>[] vote = new MCStatistics[2];
            vote[0] = includeNext[i].getSuccessorNode(new ActionWithRef<>(new SupportTeam(), 1));
            vote[1] = includeNext[i].getSuccessorNode(new ActionWithRef<>(new RejectTeam(), 1));
            for (int j = 0; j < 2; j++) {
                assertEquals(vote[j].getPossibleActions(1).size(), 3);
                assertTrue(vote[j].getPossibleActions().contains(new ActionWithRef<>(new SupportTeam(), 1)));
                assertTrue(vote[j].getPossibleActions().contains(new ActionWithRef<>(new RejectTeam(), 1)));
                assertTrue(vote[j].getPossibleActions().contains(new ActionWithRef<>(new Cooperate(), 1)));
            }
        }
    }


    @Test
    public void checkNodesInTreeForIgnoreOthersTraitor() {
        localProp.setProperty("MonteCarloTree", "ignoreOthers");
        setupGame();
        do {
            if (!players[0].isTraitor()) {
                int nonTraitor = game.getAllPlayers().stream()
                        .filter(p -> !p.isTraitor() && p != players[0])
                        .findFirst().get().getPlayerNumber();
                game.redeterminise(nonTraitor, nonTraitor, Optional.empty());
                setupGame();
            }
        } while (!players[0].isTraitor());
        // include ourselves in team as first action
        game.applyAction(new IncludeInTeam(1).getAction(game.getCurrentPlayer()));
        tree = (OpenLoopMCTree) masterDecider.getTree(players[0]);
        assertEquals(tree.numberOfStates(), 1);
        assertEquals(game.getPossibleActions().size(), 4);  // Include one of remaining 4 players
        game.oneAction();
        assertEquals(game.getPossibleActions().size(), 2);  // Vote for or against

        MCStatistics<ResistancePlayer> rootStats = tree.getRootStatistics();
        assertEquals(rootStats.getPossibleActions(1).size(), 4);
        assertEquals(rootStats.getPossibleActions().size(), 4);

        // Now we check we have the following structure
        // 4 options to include the next one
        // Then a Support/Reject Branch
        // then a Cooperate/Defect/Support/Reject branch...as from our perspective any of these could be next

        MCStatistics<ResistancePlayer>[] includeNext = new MCStatistics[6];
        for (int i = 2; i <= 5; i++) {
            includeNext[i] = rootStats.getSuccessorNode(new ActionWithRef<>(new IncludeInTeam(i), 1));
            assertEquals(includeNext[i].getPossibleActions(1).size(), 2);
            assertTrue(includeNext[i].getPossibleActions(1).contains(new SupportTeam()));
            assertTrue(includeNext[i].getPossibleActions(1).contains(new RejectTeam()));
            MCStatistics<ResistancePlayer>[] vote = new MCStatistics[2];
            vote[0] = includeNext[i].getSuccessorNode(new ActionWithRef<>(new SupportTeam(), 1));
            vote[1] = includeNext[i].getSuccessorNode(new ActionWithRef<>(new RejectTeam(), 1));
            for (int j = 0; j < 2; j++) {
                assertEquals(vote[j].getPossibleActions(1).size(), 4);
                assertTrue(vote[j].getPossibleActions().contains(new ActionWithRef<>(new SupportTeam(), 1)));
                assertTrue(vote[j].getPossibleActions().contains(new ActionWithRef<>(new RejectTeam(), 1)));
                assertTrue(vote[j].getPossibleActions().contains(new ActionWithRef<>(new Cooperate(), 1)));
                assertTrue(vote[j].getPossibleActions().contains(new ActionWithRef<>(new Defect(), 1)));
            }
        }
    }
}
