package hopshackle.simulation;

public interface GeneticVariable<A extends Agent> {

	public double getValue(A a1, Agent a2);
	// If it it the context of this Agent a1 with another agent a2
		
	public double getValue(A a1, Action<A> a2);
	// If it it the context of this Agent a1 with another agent a2
		
	public double getValue(A a1, Artefact a2);
	// If it it the context of this Agent a1 with another agent a2
		
	public double getValue(State<A> forwardState);
	
	public String getDescriptor();
	
	/**
	 * returns true if the value of this Genetic Variable is constrained to be between -1 and +1
	 * @return
	 */
	public boolean unitaryRange();
}
