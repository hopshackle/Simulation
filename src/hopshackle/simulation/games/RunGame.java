package hopshackle.simulation.games;

import hopshackle.simulation.games.resistance.*;
import hopshackle.simulation.*;

public class RunGame {

    public static void main(String[] args) {
        Resistance game = new Resistance(5, 2, new World());
        for (ResistancePlayer player : game.getAllPlayers()) {
            player.setDecider(new RandomDecider<ResistancePlayer>(null));
        }
        game.playGame();
    }
}
