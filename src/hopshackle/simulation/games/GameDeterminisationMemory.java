package hopshackle.simulation.games;

public abstract class GameDeterminisationMemory<G extends Game> {

    /*
    Update the provided Game to be compatible with the Determinisation Memory
    If the Memory would change something that is incompatible with the observed history
    then we ignore that part
     */
    public abstract void update(G game);
}
