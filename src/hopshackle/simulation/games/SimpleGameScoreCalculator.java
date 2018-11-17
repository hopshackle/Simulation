package hopshackle.simulation.games;

import hopshackle.simulation.Decider;

/**
 * Created by james on 08/07/2017.
 */
public class SimpleGameScoreCalculator implements GameScoreCalculator {

    public double[] finalScores(Game game) {
        double[] retValue = new double[game.getAllPlayers().size()];
        for (int playerNumber = 1; playerNumber <= retValue.length; playerNumber++) {
            retValue[playerNumber - 1] = game.getPlayer(playerNumber).getScore();
            // we start with their basic score - the rest considers bonuses to this based on relative position
            Decider<?> decider = game.getPlayer(playerNumber).getDecider();
            boolean onlyRewardVictory = false;
            double[] ordinalRewards = new double[retValue.length];
            if (decider != null) {
                // so each player could well have their own different utility function over ordinal position
                onlyRewardVictory = decider.getProperties().getProperty("OnlyRewardVictory", "false").equals("true");
                String rewardString = decider.getProperties().getProperty("GameOrdinalRewards", "50:0:0:0");
                String[] rewards = rewardString.split(":");
                for (int j = 0; j < Math.min(rewards.length, ordinalRewards.length); j++) {
                    // note that default is 0.0
                    ordinalRewards[j] = Double.valueOf(rewards[j]);
                }
            }
            if (onlyRewardVictory) {
                int winningPlayers = 0;
                for (int j = 0; j < retValue.length; j++)
                    if (game.getOrdinalPosition(j+1) == 1) winningPlayers++;
                if (game.getOrdinalPosition(playerNumber) == 1)
                    retValue[playerNumber-1] = 100.0 / (double) winningPlayers;
            } else {
                retValue[playerNumber-1] += ordinalRewards[game.getOrdinalPosition(playerNumber) - 1];
            }
        }

        return retValue;
    }
}
