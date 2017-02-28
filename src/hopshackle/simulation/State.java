package hopshackle.simulation;

public interface State<A extends Agent> {
	/*
	 * The State is responsible for keeping track of score of all the actors in a game/situation
	 * This is to cater for multi-agent environments, especially Tree-search algorithms, where the
	 * reward will be affected by the scores of other agents
	 */
	
	public double[] getAsArray();
	
	/*
	 * Returns the index of the perspective/acting agent. Used primarily to access the associated score
	 */
	public int getActorRef();
	
	/*
	 * Returns the score of all associated agents in the state (at the moment of creation)
	 */
	public double[] getScore();
	
	public String getAsString();

	public State<A> apply(ActionEnum<A> proposedAction);
	
	public State<A> clone();
}
