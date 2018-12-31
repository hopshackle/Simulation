package hopshackle.simulation.games;

import hopshackle.simulation.*;
import java.util.*;

public class GameTurnCalendar implements WorldCalendar {

    private long turn = 0;

    public GameTurnCalendar(long startTurn) {
        turn = startTurn;
    }

    @Override
    public long getTime() {
        return turn;
    }

    @Override
    public void setTime(long newTime) {
        turn = newTime;
    }

    @Override
    public void setScheduledTask(TimerTask task, long delay, long period) {
        throw new AssertionError("Not implemented");
    }

    @Override
    public void setScheduledTask(TimerTask task, long delay) {
        throw new AssertionError("Not implemented");
    }

    @Override
    public void removeScheduledTask(TimerTask task) {
        throw new AssertionError("Not implemented");
    }

    @Override
    public String getDate() {
        return String.valueOf(turn);
    }

    @Override
    public String getDate(long time) {
        return String.valueOf(time);
    }
}
