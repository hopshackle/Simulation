package hopshackle.simulation;

public interface GeneticVariable<A extends Agent> {

	public double getValue(A agent);
	
	public String getDescriptor();
	
	/**
	 * returns true if the value of this Genetic Variable is constrained to be between -1 and +1
	 * @return
	 */
	public boolean unitaryRange();
}
