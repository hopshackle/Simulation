package hopshackle.simulation.games;

import hopshackle.simulation.*;

public abstract class GameAction<A extends Agent> extends Action<A> {

    public GameAction(GameActionEnum<A> type, A player) {
        super(type, player, true);
    }

    @Override
    public void doNextDecision() {
        // do nothing
    }

}
