package hopshackle.simulation.games;

import hopshackle.simulation.*;

public interface GameListener<A extends Agent> {

    public abstract void processGameEvent(GameEvent<A> event);
}
