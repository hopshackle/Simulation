package hopshackle.simulation.test.resistance;

import static org.junit.Assert.*;

import hopshackle.simulation.MCTS.SingletonStateFactory;
import hopshackle.simulation.games.Game;
import hopshackle.simulation.games.GameEvent;
import hopshackle.simulation.games.GameListener;
import hopshackle.simulation.games.GameTracker;
import hopshackle.simulation.games.resistance.*;
import hopshackle.simulation.*;

import java.util.*;
import java.util.stream.*;

import org.junit.*;

public class ResistanceGameTest {

    private ResistanceListener rl;
    private Resistance game;
    private SingletonStateFactory<ResistancePlayer> factory = new SingletonStateFactory<>();
    private RandomDecider<ResistancePlayer> decider = new RandomDecider<>(factory);

    @Before
    public void setup() {
        game = new Resistance(5, 2, new World());
        rl = new ResistanceListener(game);
        IntStream.rangeClosed(1, 5).forEach(i -> game.getPlayer(i).setDecider(decider));
    }

    @Test
    public void chooseFirstTeam() {
        chooseTeam(true);
        assertEquals(game.getTrajectory().size(), 2);
        assertSame(game.getCurrentPlayer().getActorRef(), 1);
    }

    @Test
    public void successfulVoteOnFirstTeam() {
        chooseFirstTeam();
        IntStream.rangeClosed(1, 5).forEach(
                i -> {
                    SupportTeam support = new SupportTeam();
                    game.applyAction(support.getAction(game.getCurrentPlayer()));
                    int plusOne = i == 5 ? 1 : 0;   // add one for the VoteResult after everyone has voted
                    IntStream.rangeClosed(1, i).forEach(j -> assertEquals(rl.data.get(j).size(), 3 + plusOne));
                    IntStream.rangeClosed(i + 1, 5).forEach(j -> assertEquals(rl.data.get(j).size(), 2));
                    assertEquals(game.getTrajectory().size(), 2 + i + plusOne);
                }
        );

        IntStream.range(2, 7).forEach(i -> assertTrue(game.getTrajectory().get(i).getValue0().actionTaken instanceof SupportTeam));
        assertTrue(game.getTrajectory().get(7).getValue0().actionTaken instanceof VoteResult);
        VoteResult vr = (VoteResult) game.getTrajectory().get(7).getValue0().actionTaken;
        assertEquals(vr.toString(), "VOTE_RESULT: 11111");
        assertTrue(vr.isPassed());
        IntStream.rangeClosed(1, 5).forEach(i -> assertTrue(vr.voteOfPlayer(i)));
        assertSame(game.getFailedVotes(), 0);
        assertSame(game.getMission(), 1);
        assertSame(game.getSuccessfulMissions(), 0);
        assertSame(game.getPhase(), Resistance.Phase.MISSION);
    }

    @Test
    public void unsuccessfulVoteOnFirstTeam() {
        chooseFirstTeam();
        IntStream.rangeClosed(1, 5).forEach(
                i -> {
                    RejectTeam reject = new RejectTeam();
                    game.applyAction(reject.getAction(game.getCurrentPlayer()));
                    int plusOne = i == 5 ? 1 : 0;   // add one for the VoteResult after everyone has voted
                    IntStream.rangeClosed(1, i).forEach(j -> assertEquals(rl.data.get(j).size(), 3 + plusOne));
                    IntStream.rangeClosed(i + 1, 5).forEach(j -> assertEquals(rl.data.get(j).size(), 2));
                    assertEquals(game.getTrajectory().size(), 2 + i + plusOne);
                }
        );

        IntStream.range(2, 7).forEach(i -> assertTrue(game.getTrajectory().get(i).getValue0().actionTaken instanceof RejectTeam));
        assertTrue(game.getTrajectory().get(7).getValue0().actionTaken instanceof VoteResult);
        VoteResult vr = (VoteResult) game.getTrajectory().get(7).getValue0().actionTaken;
        assertEquals(vr.toString(), "VOTE_RESULT: 00000");
        assertFalse(vr.isPassed());
        IntStream.rangeClosed(1, 5).forEach(i -> assertFalse(vr.voteOfPlayer(i)));
        assertSame(game.getFailedVotes(), 1);
        assertSame(game.getMission(), 1);
        assertSame(game.getSuccessfulMissions(), 0);
        assertSame(game.getPhase(), Resistance.Phase.ASSEMBLE);
    }

    @Test
    public void successfulFirstMission() {
        successfulVoteOnFirstTeam();
        int traitor = game.getCurrentPlayer().getPlayerNumber();
        assertEquals(traitor, (int) game.getTraitors().get(0));
        game.applyAction(new Cooperate().getAction(game.getPlayer(game.getTraitors().get(0))));
        int loyalist = game.getCurrentPlayer().getPlayerNumber();
        assertNotEquals(loyalist, (int) game.getTraitors().get(0));
        assertEquals(game.getPossibleActions().size(), 1);
        game.oneAction();   // should only be able to Cooperate

        IntStream.rangeClosed(1, 5).forEach(
                i -> {
                    int plusOne = (i == traitor || i == loyalist) ? 1 : 0;
                    assertEquals(rl.data.get(i).size(), 5 + plusOne);
                    assertEquals(game.getTrajectory().size(), 11);
                }
        );

        assertTrue(game.getTrajectory().get(8).getValue0().actionTaken instanceof Cooperate);
        assertTrue(game.getTrajectory().get(9).getValue0().actionTaken instanceof Cooperate);
        assertTrue(game.getTrajectory().get(10).getValue0().actionTaken instanceof MissionResult);

        assertSame(game.getFailedVotes(), 0);
        assertSame(game.getMission(), 2);
        assertSame(game.getSuccessfulMissions(), 1);
        assertSame(game.getPhase(), Resistance.Phase.ASSEMBLE);
    }

    @Test
    public void unsuccessfulFirstMission() {
        successfulVoteOnFirstTeam();
        int traitor = game.getCurrentPlayer().getPlayerNumber();
        assertEquals(traitor, (int) game.getTraitors().get(0));
        assertEquals(game.getPossibleActions().size(), 2);
        game.applyAction(new Defect().getAction(game.getPlayer(game.getTraitors().get(0))));
        int loyalist = game.getCurrentPlayer().getPlayerNumber();
        assertNotEquals(loyalist, (int) game.getTraitors().get(0));
        assertEquals(game.getPossibleActions().size(), 1);
        game.oneAction();   // should only be able to Cooperate

        IntStream.rangeClosed(1, 5).forEach(
                i -> {
                    int plusOne = (i == traitor || i == loyalist) ? 1 : 0;
                    assertEquals(rl.data.get(i).size(), 5 + plusOne);
                    assertEquals(game.getTrajectory().size(), 11);
                }
        );

        assertTrue(game.getTrajectory().get(8).getValue0().actionTaken instanceof Defect);
        assertTrue(game.getTrajectory().get(9).getValue0().actionTaken instanceof Cooperate);
        assertTrue(game.getTrajectory().get(10).getValue0().actionTaken instanceof MissionResult);
        MissionResult mr = (MissionResult) game.getTrajectory().get(10).getValue0().actionTaken;
        assertEquals(mr.getDefections(), 1);
        assertEquals(mr.getTeam().size(), 2);
        assertEquals((int) mr.getTeam().get(0), traitor);
        assertEquals((int) mr.getTeam().get(1), loyalist);

        assertSame(game.getFailedVotes(), 0);
        assertSame(game.getMission(), 2);
        assertSame(game.getSuccessfulMissions(), 0);
        assertSame(game.getPhase(), Resistance.Phase.ASSEMBLE);
    }

    @Test
    public void gameOverAfterFiveFailedVotes() {
        for (int vote = 0; vote < 5; vote++) {
            chooseTeam(false);
            IntStream.rangeClosed(1, 5).forEach(
                    i -> {
                        RejectTeam reject = new RejectTeam();
                        game.applyAction(reject.getAction(game.getCurrentPlayer()));
                    }
            );
            assertSame(game.getFailedVotes(), vote + 1);
            assertSame(game.getMission(), 1);
            assertSame(game.getSuccessfulMissions(), 0);
            assertSame(game.getPhase(), Resistance.Phase.ASSEMBLE);
        }
        assertTrue(game.gameOver());
        assertTrue(game.spiesHaveWon());
    }

    @Test
    public void voteResultEquality() {
        VoteResult vr1 = new VoteResult(new boolean[]{true, false, true});
        VoteResult vr2 = new VoteResult(new boolean[]{true, true, true});
        assertFalse("11".equals("01"));
        assertFalse(vr1.equals(vr2));
    }

    @Test
    public void redeterminiseISAndPPBothTraitor() {
        // firstly we set up a game that meets criteria
        // then we loop redeterminise and check invariants
        // we want to have a vanilla game, and a game that has had some water under the bridge
        int secondTraitor = game.getTraitors().get(1);
        int firstTraitor = game.getTraitors().get(0);
        for (int loop = 0; loop < 100; loop++) {
            game.redeterminise(firstTraitor, secondTraitor, Optional.empty());
            assertTrue(game.getTraitors().contains(firstTraitor));
            assertTrue(game.getTraitors().contains(secondTraitor));
            assertEquals(game.getTraitors().size(), 2);
        }

        game.applyAction((new IncludeInTeam(1)).getAction(game.getCurrentPlayer()));
        game.applyAction((new IncludeInTeam(2)).getAction(game.getCurrentPlayer()));
        game.applyAction((new SupportTeam()).getAction(game.getCurrentPlayer()));
        for (int loop = 0; loop < 100; loop++) {
            game.redeterminise(firstTraitor, secondTraitor, Optional.empty());
            assertTrue(game.getTraitors().contains(firstTraitor));
            assertTrue(game.getTraitors().contains(secondTraitor));
            assertEquals(game.getTraitors().size(), 2);
        }
    }

    @Test
    public void redeterminiseISLoyalIsNeverCoTraitor() {
        for (int loop = 0; loop < 100; loop++) {
            int firstLoyalist = game.getLoyalists().get(0);
            int perspective = Dice.roll(1, 5);
            game.redeterminise(perspective, firstLoyalist, Optional.empty());
            assertFalse(game.getTraitors().contains(firstLoyalist) && game.getTraitors().contains(perspective) && firstLoyalist != perspective);
        }
    }

    @Test
    public void redeterminiseISOnlyTraitor() {
        List<Integer> loyalists = IntStream.rangeClosed(1, 5).filter(i -> !game.getTraitors().contains(i)).mapToObj(i -> i).collect(Collectors.toList());
        int firstTraitor = game.getTraitors().get(0);
        Set<Integer> traitorsAfterRedeterminisation = new HashSet<>();
        for (int loop = 0; loop < 100; loop++) {
            Resistance clonedGame = (Resistance) game.clone();
            clonedGame.redeterminise(loyalists.get(1), firstTraitor, Optional.empty());
            traitorsAfterRedeterminisation.addAll(clonedGame.getTraitors());
            assertEquals(clonedGame.getTraitors().size(), 2);
        }
        assertEquals(traitorsAfterRedeterminisation.size(), 4);
        assertFalse(traitorsAfterRedeterminisation.contains(loyalists.get(1)));
    }

    @Test
    public void redeterminiseISOnlyTraitorWithReferenceGame() {
        List<Integer> loyalists = IntStream.rangeClosed(1, 5).filter(i -> !game.getTraitors().contains(i)).mapToObj(i -> i).collect(Collectors.toList());
        int firstTraitor = game.getTraitors().get(0);
        Resistance rootGame = (Resistance) game.clone();
        Set<Integer> traitorsAfterRedeterminisation = new HashSet<>();
        for (int loop = 0; loop < 100; loop++) {
            game.redeterminise(loyalists.get(1), firstTraitor, Optional.of(rootGame));
            traitorsAfterRedeterminisation.addAll(game.getTraitors());
            assertEquals(game.getTraitors().size(), 2);
        }
        assertEquals(traitorsAfterRedeterminisation.size(), 4);
        assertFalse(traitorsAfterRedeterminisation.contains(loyalists.get(1)));
    }

    @Test
    public void redeterminiseISAndPPLoyalAndDifferent() {
        List<Integer> loyalists = IntStream.rangeClosed(1, 5).filter(i -> !game.getTraitors().contains(i)).mapToObj(i -> i).collect(Collectors.toList());
        Resistance rootGame = (Resistance) game.clone();
        Set<Integer> traitorsAfterRedeterminisation = new HashSet<>();
        for (int loop = 0; loop < 100; loop++) {
            game.redeterminise(loyalists.get(1), loyalists.get(2), Optional.of(rootGame));
            traitorsAfterRedeterminisation.addAll(game.getTraitors());
            assertEquals(game.getTraitors().size(), 2);
        }
        assertEquals(traitorsAfterRedeterminisation.size(), 5);
    }

    @Test
    public void redeterminiseISAndPPLoyalAndSame() {
        List<Integer> loyalists = IntStream.rangeClosed(1, 5).filter(i -> !game.getTraitors().contains(i)).mapToObj(i -> i).collect(Collectors.toList());
        Resistance rootGame = (Resistance) game.clone();
        Set<Integer> traitorsAfterRedeterminisation = new HashSet<>();
        for (int loop = 0; loop < 100; loop++) {
            game.redeterminise(loyalists.get(1), loyalists.get(1), Optional.of(rootGame));
            traitorsAfterRedeterminisation.addAll(game.getTraitors());
            assertEquals(game.getTraitors().size(), 2);
        }
        assertEquals(traitorsAfterRedeterminisation.size(), 4);
        assertFalse(traitorsAfterRedeterminisation.contains(loyalists.get(1)));
    }

    @Test
    public void redeterminiseTakesNoAccountOfMissionAboutToFail() {
        do {
            game.redeterminise(Dice.roll(1, 5), Dice.roll(1, 5), Optional.empty());
        } while (!game.getTraitors().contains(1));
        int secondTraitor = game.getTraitors().get(1);
        int firstTraitor = game.getTraitors().get(0);
        int loyalist = 5;
        if (firstTraitor == 5 || secondTraitor == 5)
            loyalist = 4;
        if (firstTraitor == 4 || secondTraitor == 4)
            loyalist = 3;

        game.applyAction((new IncludeInTeam(1)).getAction(game.getCurrentPlayer()));
        game.applyAction((new IncludeInTeam(2)).getAction(game.getCurrentPlayer()));
        for (int i = 0; i < 5; i++) game.applyAction((new SupportTeam()).getAction(game.getCurrentPlayer()));
        game.applyAction((new Defect()).getAction(game.getCurrentPlayer()));

        Set<Integer> traitorsAfterRedeterminisation = new HashSet<>();
        Set<Integer> loyalistsAfterRedeterminisation = new HashSet<>();
        for (int loop = 0; loop < 100; loop++) {
            // technically anyone could be a traitor...but we will insist that player 1 is a traitor given the (as yet unrevealed DEFECT)
            game.redeterminise(loyalist, loyalist, Optional.empty());
            traitorsAfterRedeterminisation.addAll(game.getTraitors());
            loyalistsAfterRedeterminisation.addAll(game.getLoyalists());
            assertEquals(game.getTraitors().size(), 2);
        }
        assertEquals(traitorsAfterRedeterminisation.size(), 4);
        assertEquals(loyalistsAfterRedeterminisation.size(), 5);
        assertFalse(traitorsAfterRedeterminisation.contains(loyalist));
    }


    @Test
    public void redeterminiseKeepingHiddenActionsTakesAccountOfMissionAboutToFail() {
        do {
            game.redeterminise(Dice.roll(1, 5), Dice.roll(1, 5), Optional.empty());
        } while (!game.getTraitors().contains(1));
        int secondTraitor = game.getTraitors().get(1);
        int firstTraitor = game.getTraitors().get(0);
        int loyalist = 5;
        if (firstTraitor == 5 || secondTraitor == 5)
            loyalist = 4;
        if (firstTraitor == 4 || secondTraitor == 4)
            loyalist = 3;

        game.applyAction((new IncludeInTeam(1)).getAction(game.getCurrentPlayer()));
        game.applyAction((new IncludeInTeam(2)).getAction(game.getCurrentPlayer()));
        for (int i = 0; i < 5; i++) game.applyAction((new SupportTeam()).getAction(game.getCurrentPlayer()));
        game.applyAction((new Defect()).getAction(game.getCurrentPlayer()));

        for (int loop = 0; loop < 100; loop++) {
            // player 1 must now be a traitor
            game.redeterminiseKeepingHiddenActions(loyalist, loyalist, Optional.empty());
            assertTrue(game.getTraitors().contains(1));
            assertEquals(game.getTraitors().size(), 2);
        }
    }

    @Test
    public void redeterminisationReinitialisesToStartOfVotePhase() {
        game.applyAction((new IncludeInTeam(3)).getAction(game.getCurrentPlayer()));
        game.applyAction((new IncludeInTeam(4)).getAction(game.getCurrentPlayer()));

        for (int i = 1; i <= 4; i++) {
            game.applyAction((new SupportTeam()).getAction(game.getCurrentPlayer()));
        }
        // redeterminise from perspective of player about to act
        assertEquals(game.getCurrentPlayerRef(), 5);
        game.redeterminise(5, 4, Optional.empty());
        assertEquals(game.getCurrentPlayerRef(), 1);
        assertSame(game.getPhase(), Resistance.Phase.VOTE);
    }

    @Test
    public void redeterminisationReinitialisesToStartOfMissionPhase() {
        game.applyAction((new IncludeInTeam(3)).getAction(game.getCurrentPlayer()));
        game.applyAction((new IncludeInTeam(4)).getAction(game.getCurrentPlayer()));

        for (int i = 1; i <= 5; i++) {
            game.applyAction((new SupportTeam()).getAction(game.getCurrentPlayer()));
        }
        game.applyAction((new Cooperate()).getAction(game.getCurrentPlayer()));
        assertEquals(game.getCurrentPlayerRef(), 4);
        game.redeterminise(5, 4, Optional.empty());
        assertEquals(game.getCurrentPlayerRef(), 3);
        assertSame(game.getPhase(), Resistance.Phase.MISSION);
        assertEquals(game.getMission(), 1);
        assertEquals(game.getFailedVotes(), 0);
        assertEquals(game.getSuccessfulMissions(), 0);
    }

    @Test
    public void playerChangeEventNotPublishedInMiddleOfSimultaneousPlay() {
        ResistanceEventTracker tracker = new ResistanceEventTracker();
        game.registerListener(tracker);
        game.applyAction((new IncludeInTeam(3)).getAction(game.getCurrentPlayer()));
        game.applyAction((new IncludeInTeam(4)).getAction(game.getCurrentPlayer()));

        assertEquals((int) tracker.count.getOrDefault(GameEvent.Type.PLAYER_CHANGE, 0), 0);
        assertEquals((int) tracker.count.getOrDefault(GameEvent.Type.GAME_OVER, 0), 0);
        assertEquals((int) tracker.count.getOrDefault(GameEvent.Type.MOVE, 0), 2);
        for (int i = 1; i <= 5; i++) {
            game.applyAction((new SupportTeam()).getAction(game.getCurrentPlayer()));
            assertEquals((int) tracker.count.getOrDefault(GameEvent.Type.PLAYER_CHANGE, 0), i == 5 ? 1 : 0);
            assertEquals((int) tracker.count.getOrDefault(GameEvent.Type.GAME_OVER, 0), 0);
            assertEquals((int) tracker.count.getOrDefault(GameEvent.Type.MOVE, 0), i == 5 ? 8 : 2 + i);
            // after the fifth Vote we get a MOVE detailing the vote result for +1, and the Player will change
        }
        for (int i = 1; i <= 2; i++) {
            assertEquals(game.getCurrentPlayerRef(), 2 + i);
            game.applyAction((new Cooperate()).getAction(game.getCurrentPlayer()));
            assertEquals((int) tracker.count.getOrDefault(GameEvent.Type.PLAYER_CHANGE, 0), i == 2 ? 2 : 1);
            assertEquals((int) tracker.count.getOrDefault(GameEvent.Type.GAME_OVER, 0), 0);
            assertEquals((int) tracker.count.getOrDefault(GameEvent.Type.MOVE, 0), i == 2 ? 11 : 8 + i);
        }
    }


    private void chooseTeam(boolean firstTeam) {
        int firstTraitor = game.getTraitors().get(0);
        game.applyAction((new IncludeInTeam(firstTraitor)).getAction(game.getCurrentPlayer()));
        assertSame(game.getPhase(), Resistance.Phase.ASSEMBLE);
        if (firstTeam)
            IntStream.rangeClosed(1, 5).forEach(i -> assertEquals(rl.data.get(i).size(), 1));

        for (int i = 1; i <= game.getPlayerCount(); i++) {
            if (game.getTraitors().contains(i)) continue;
            game.applyAction((new IncludeInTeam(i)).getAction(game.getCurrentPlayer()));
            break;
        }
        // we should now have a full team, including one traitor, and one loyalist
        assertSame(game.getPhase(), Resistance.Phase.VOTE);
        if (firstTeam)
            IntStream.rangeClosed(1, 5).forEach(i -> assertEquals(rl.data.get(i).size(), 2));
    }
}


class ResistanceEventTracker implements GameListener<ResistancePlayer> {

    public Map<GameEvent.Type, Integer> count = new HashMap<>();

    @Override
    public void processGameEvent(GameEvent<ResistancePlayer> event) {
        //      System.out.println(String.format("Received event %s %s %s", event.type, event.actionTaken != null ? event.actionTaken : "null", HopshackleUtilities.formatList(event.visibleTo(), "|", null)));
        count.put(event.type, count.getOrDefault(event.type, 0) + 1);
    }

}

