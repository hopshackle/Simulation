package hopshackle.simulation;

import java.util.List;

public class RandomDecider<A extends Agent> extends BaseAgentDecider<A> {

	public RandomDecider(StateFactory<A> stateFactory) {
		super(stateFactory);
	}

	@Override
	public ActionEnum<A> makeDecision(A decidingAgent, List<ActionEnum<A>> options) {
		int choice = Dice.roll(1, options.size());
		return options.get(choice-1);
	}
	
	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent) {
		return 0;
	}
	@Override
	public double valueOption(ActionEnum<A> option, State<A> state) {
		return 0;
	}

	@Override
	public void learnFrom(ExperienceRecord<A> exp, double maxResult) {
	}

}
