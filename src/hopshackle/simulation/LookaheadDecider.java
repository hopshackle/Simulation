package hopshackle.simulation;

import java.util.*;

public abstract class LookaheadDecider<A extends Agent> extends BaseDecider<A> {

	protected boolean useAdvantage;

	public LookaheadDecider(StateFactory<A> stateFactory) {
		super(stateFactory);
	}

	@Override
	public void injectProperties(DeciderProperties newProp) {
		super.injectProperties(newProp);
		useAdvantage = newProp.getProperty("LookaheadAdvantage", "false").equals("true");
	}

	@Override
	public List<Double> valueOptions(List<ActionEnum<A>> options, A decidingAgent) {
		List<Double> retValue = new ArrayList<Double>(options.size());
		State<A> currentState = stateFactory.getCurrentState(decidingAgent);
		for (int i = 0; i < options.size(); i++) {
			ActionEnum<A> option = options.get(i);
			double value = valueOfFutureState(currentState, option, decidingAgent);
			retValue.add(value);
		}
		return retValue;
	}

	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent) {
		// only used in Test, plus BaseDecider.valueOptions, which is overridden
		State<A> currentState = stateFactory.getCurrentState(decidingAgent);
		return valueOfFutureState(currentState, option, decidingAgent);
	}

	private double valueOfFutureState(State<A> currentState, ActionEnum<A> option, A decidingAgent) {
		State<A> futureState = currentState.apply(option);
		double retValue = value(futureState);
		if (localDebug) {
			String message = "Option " + option.toString() + " has base Value of " + retValue; //+
			//	" with state representation of: \n \t" + Arrays.toString(futureState.getAsArray());
			decidingAgent.log(message);
			log(message);
		}
		return retValue;
	}

	public abstract double value(State<A> state);
	
}
