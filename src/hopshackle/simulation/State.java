package hopshackle.simulation;


public interface State<A extends Agent> {
	
	public double[] getAsArray();
	
	public String getAsString();

}
