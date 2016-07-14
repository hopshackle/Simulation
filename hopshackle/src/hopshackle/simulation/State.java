package hopshackle.simulation;

import java.util.List;

public interface State<A extends Agent> {
	
	public double[] asArray(List<GeneticVariable<A>> variables);

}
