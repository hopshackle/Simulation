package hopshackle.simulation.test.resistance;
import static org.junit.Assert.*;
import hopshackle.simulation.*;
import java.util.*;
import java.util.stream.*;

import hopshackle.simulation.games.resistance.VoteResult;
import org.junit.*;

public class VoteResultEquality {

    @Test
    public void voteResultEquality() {
        VoteResult vr1 = new VoteResult(new boolean[] {true, false, true});
        VoteResult vr2 = new VoteResult(new boolean[] {true, true, true});
        assertFalse("11".equals("01"));
        assertFalse(vr1.equals(vr2));
    }
}
