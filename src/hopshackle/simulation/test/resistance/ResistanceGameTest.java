package hopshackle.simulation.test.resistance;

import static org.junit.Assert.*;

import com.sun.deploy.security.MozillaSSLRootCertStore;
import hopshackle.simulation.MCTS.SingletonStateFactory;
import hopshackle.simulation.games.*;
import hopshackle.simulation.games.resistance.*;
import hopshackle.simulation.*;

import java.util.*;
import java.util.stream.IntStream;

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
        int firstTraitor = game.getTraitors().get(0);
        game.applyAction((new IncludeInTeam(firstTraitor)).getAction(game.getCurrentPlayer()));
        assertSame(game.getPhase(), Resistance.Phase.ASSEMBLE);
        IntStream.rangeClosed(1, 5).forEach(i -> assertEquals(rl.data.get(i).size(), 1));

        for (int i = 1; i <= game.getAllPlayers().size(); i++) {
            if (game.getTraitors().contains(i)) continue;
            game.applyAction((new IncludeInTeam(i)).getAction(game.getCurrentPlayer()));
            break;
        }
        // we should now have a full team, including one traitor, and one loyalist
        assertSame(game.getPhase(), Resistance.Phase.VOTE);
        IntStream.rangeClosed(1, 5).forEach(i -> assertEquals(rl.data.get(i).size(), 2));
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

        IntStream.range(2, 7).forEach(i -> assertTrue(game.getTrajectory().get(i).getValue1().actionTaken instanceof SupportTeam));
        assertTrue(game.getTrajectory().get(7).getValue1().actionTaken instanceof VoteResult);
        VoteResult vr = (VoteResult) game.getTrajectory().get(7).getValue1().actionTaken;
        assertTrue(vr.toString().equals("VOTE_RESULT: 11111"));
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

        IntStream.range(2, 7).forEach(i -> assertTrue(game.getTrajectory().get(i).getValue1().actionTaken instanceof RejectTeam));
        assertTrue(game.getTrajectory().get(7).getValue1().actionTaken instanceof VoteResult);
        VoteResult vr = (VoteResult) game.getTrajectory().get(7).getValue1().actionTaken;
        assertTrue(vr.toString().equals("VOTE_RESULT: 00000"));
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

        assertTrue(game.getTrajectory().get(8).getValue1().actionTaken instanceof Cooperate);
        assertTrue(game.getTrajectory().get(9).getValue1().actionTaken instanceof Cooperate);
        assertTrue(game.getTrajectory().get(10).getValue1().actionTaken instanceof MissionResult);

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

        assertTrue(game.getTrajectory().get(8).getValue1().actionTaken instanceof Defect);
        assertTrue(game.getTrajectory().get(9).getValue1().actionTaken instanceof Cooperate);
        assertTrue(game.getTrajectory().get(10).getValue1().actionTaken instanceof MissionResult);
        MissionResult mr = (MissionResult) game.getTrajectory().get(10).getValue1().actionTaken;
        assertEquals(mr.getDefections(),1);
        assertEquals(mr.getTeam().size(), 2);
        assertEquals((int)mr.getTeam().get(0), traitor);
        assertEquals((int)mr.getTeam().get(1), loyalist);

        assertSame(game.getFailedVotes(), 0);
        assertSame(game.getMission(), 2);
        assertSame(game.getSuccessfulMissions(), 0);
        assertSame(game.getPhase(), Resistance.Phase.ASSEMBLE);
    }

}
