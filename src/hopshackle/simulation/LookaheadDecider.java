package hopshackle.simulation;

import java.util.*;

public abstract class LookaheadDecider<A extends Agent> extends BaseDecider<A> {

	public LookaheadDecider(StateFactory<A> stateFactory) {
		super(stateFactory);
	}

	@Override
	public void injectProperties(DeciderProperties newProp) {
		super.injectProperties(newProp);
	}

	@Override
	public List<Double> valueOptions(List<ActionEnum<A>> options, A decidingAgent) {
		List<Double> retValue = new ArrayList<Double>(options.size());
		State<A> currentState = stateFactory.getCurrentState(decidingAgent);
		for (int i = 0; i < options.size(); i++) {
			ActionEnum<A> option = options.get(i);
			double value = valueOption(option, currentState);
			if (localDebug) {
				String message = "Option " + option.toString() + " has base Value of " + value; //+
				//	" with state representation of: \n \t" + Arrays.toString(futureState.getAsArray());
				decidingAgent.log(message);
				log(message);
			}
			retValue.add(value);
		}
		return retValue;
	}

	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent) {
		// only used in Test, plus BaseDecider.valueOptions, which is overridden
		State<A> currentState = stateFactory.getCurrentState(decidingAgent);
		return valueOption(option, currentState);
	}
	@Override
	public double valueOption(ActionEnum<A> option, State<A> state) {
		State<A> futureState = state.apply(option);
		return value(futureState);
	}

	public abstract double value(State<A> state);
	
}
