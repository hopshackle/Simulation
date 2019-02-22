package hopshackle.simulation.games.resistance;

import hopshackle.simulation.ActionWithRef;
import hopshackle.simulation.games.*;

public class ResistanceAPD extends AllPlayerDeterminiser<Resistance, ResistancePlayer> {

    public ResistanceAPD(Resistance game, int perspective) {
        super(game, perspective);
    }

    @Override
    public Resistance determinise(int perspectivePlayer, Resistance game) {
        throw new AssertionError("Not yet implemented");
    }

    @Override
    public Resistance compatibilise(int perspectivePlayer, ActionWithRef<ResistancePlayer> actionWithRef, Resistance game) {
        throw new AssertionError("Not yet implemented");
    }

    @Override
    public void redeterminiseWithinIS(int playerID) {
        throw new AssertionError("Not yet implemented");

    }

    @Override
    public boolean isCompatible(ActionWithRef<ResistancePlayer> actionWithRef, Resistance masterGame, Resistance determinisation) {
        throw new AssertionError("Not yet implemented");
    }
}
