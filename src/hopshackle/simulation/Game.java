package hopshackle.simulation;

import java.util.List;

public interface Game<P extends Agent, A extends ActionEnum<P>> {
	
	public Game<P, A> clone(P perspectivePlayer);
	
	public double[] playGame();
	
	public P getCurrentPlayer();

	public int getCurrentPlayerNumber();
	
	public List<P> getAllPlayers();
	
	public int getPlayerNumber(P player);
	
	public P getPlayer(int n);

}
