package hopshackle.simulation;

import java.util.ArrayList;

public class HardCodedDecider<A extends Agent> extends BaseDecider<A> {

	private ActionEnum<A> hardCode;

	public HardCodedDecider(ActionEnum<A> decision) {
		super(null, new ArrayList<ActionEnum<A>>());
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
	public void learnFrom(ExperienceRecord<A> exp, double maxResult) {}
	
	@Override
	public State<A> getCurrentState(A agent) {
		return new State<A>() {

			@Override
			public double[] getAsArray() {
				return new double[0];
			}

			@Override
			public String getAsString() {
				return "0.00";
			}
		};
	}
}


