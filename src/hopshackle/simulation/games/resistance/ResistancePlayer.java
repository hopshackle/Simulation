package hopshackle.simulation.games.resistance;

import hopshackle.simulation.*;

import java.util.*;

public class ResistancePlayer extends Agent {

    private boolean traitor = false;
    private Resistance game;
    private int number;
    private List<Integer> otherTraitors;

    public ResistancePlayer(World world, Resistance game, int number) {
        super(world);
        this.game = game;
        this.number = number;
    }

    public void setTraitor(boolean traitorStatus) {
        traitor = traitorStatus;
        otherTraitors = new ArrayList();
    }

    public void setOtherTraitor(int traitorNumber) {
        if (!otherTraitors.contains(traitorNumber))
            otherTraitors.add(traitorNumber);
    }

    public int getPlayerNumber() {
        return number;
    }

    @Override
    public Resistance getGame() {
        return game;
    }

    @Override
    public String toString() {
        return "Player " + number;
    }

    public boolean isTraitor() {
        return traitor;
    }
}
