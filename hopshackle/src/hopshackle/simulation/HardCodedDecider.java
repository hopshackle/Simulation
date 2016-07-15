package hopshackle.simulation;

import java.util.ArrayList;

public class HardCodedDecider<A extends Agent> extends SimpleStateBaseDecider<A> {

	private ActionEnum<A> hardCode;

	public HardCodedDecider(ActionEnum<A> decision) {
		super(new ArrayList<ActionEnum<A>>(), new ArrayList<GeneticVariable<A, SimpleState<A>>>());
		hardCode = decision;
	}
	
	@Override 
	public ActionEnum<A> makeDecision(A decidingAgent) {
		return hardCode;
	}

	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent) {
		return 0;
	}

	@Override
	public void learnFrom(ExperienceRecord<A, SimpleState<A>> exp, double maxResult) {
	}
}


