package hopshackle.simulation.games.resistance;

import hopshackle.simulation.games.GameScoreCalculator;

import java.util.*;

public class ResistanceScoreCalculator implements GameScoreCalculator<Resistance> {
    @Override
    public double[] finalScores(Resistance game) {
        /*
        Simply 1 for the winning team, and 0 for the losing team
         */
        if (!game.gameOver())
            throw new AssertionError("Should only be called when the game is over!");
        List<Integer> spies = game.getTraitors();
        double[] retValue = new double[game.getPlayerCount()];
        if (game.spiesHaveWon()) {
            for (int spy : spies) retValue[spy - 1] = 1.0;
        } else {
            for (int i = 0; i < retValue.length; i++) retValue[i] = 1.0;
            for (int spy : spies) retValue[spy - 1] = 0.0;
        }
        return retValue;
    }
}
