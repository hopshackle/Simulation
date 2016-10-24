package hopshackle.simulation;

import java.util.List;

public interface StateFactory<A extends Agent> {
	
	public State<A> getCurrentState(A agent);

	public <V extends GeneticVariable<A>> List<V> getVariables(); 
	
	public StateFactory<A> cloneWithNewVariables(List<GeneticVariable<A>> newVar);
	
 }
