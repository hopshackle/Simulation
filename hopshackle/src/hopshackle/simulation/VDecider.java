package hopshackle.simulation;

import java.util.List;

public abstract class VDecider extends BaseDecider {

	public VDecider(List<? extends ActionEnum> actions,	List<GeneticVariable> variables) {
		super(actions, variables);
	}
	
	public abstract double valueState(double[] state);

}