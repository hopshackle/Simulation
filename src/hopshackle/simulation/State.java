package hopshackle.simulation;

public interface State<A extends Agent> {
	
	public double[] getAsArray();
	
	public String getAsString();

	public State<A> apply(ActionEnum<A> proposedAction);
	
	public State<A> clone();
}
