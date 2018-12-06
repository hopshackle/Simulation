package hopshackle.simulation.games;

import hopshackle.simulation.MCTS.MCTSMasterDecider;
import hopshackle.simulation.MCTS.SingletonStateFactory;
import hopshackle.simulation.games.resistance.*;
import hopshackle.simulation.*;

public class RunGame {

    public static void main(String[] args) {
        Resistance game = new Resistance(5, 2, new World());
        MCTSMasterDecider<ResistancePlayer> mctsDecider = new MCTSMasterDecider<>(new SingletonStateFactory<>(), null, null);
        mctsDecider.injectProperties(SimProperties.getDeciderProperties("GLOBAL"));
        for (ResistancePlayer player : game.getAllPlayers()) {
            player.setDecider(mctsDecider);
        }
        game.playGame();
    }
}
