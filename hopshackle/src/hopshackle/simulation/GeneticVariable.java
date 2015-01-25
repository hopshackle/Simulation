package hopshackle.simulation;

public interface GeneticVariable {

	public double getValue(Object a, double var);
	// If it is just the context of this Agent, with a variable as 'x' for complex polynomials
	
	public double getValue(Object a1, Object a2);
	// If it it the context of this Agent a1 with another agent a2
		
	public String getDescriptor();
	
	/**
	 * returns true if the value of this Genetic Variable is constrained to be between -1 and +1
	 * @return
	 */
	public boolean unitaryRange();
}
