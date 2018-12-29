package hopshackle.simulation.games.resistance;

import hopshackle.simulation.games.*;

public class ResistanceDeterminisationMemory extends GameDeterminisationMemory<Resistance> {

    private boolean[] traitors;

    public ResistanceDeterminisationMemory(Resistance baseGame) {
        traitors = new boolean[baseGame.getAllPlayers().size() + 1];
        for (int i = 1; i <= traitors.length; i++) {
            traitors[i] = baseGame.getPlayer(i).isTraitor();
        }
    }

    @Override
    public void update(Resistance game) {
        throw new AssertionError("Not implemented");
        // For Resistance, we implement this directly in Resistance.undeterminise()
    }

    protected boolean[] getTraitors() {
        return traitors;
    }
}
