package hopshackle.simulation.games;

/**
 * Created by james on 08/07/2017.
 */
public interface GameScoreCalculator<G extends Game> {

    public double[] finalScores(G game);
}
