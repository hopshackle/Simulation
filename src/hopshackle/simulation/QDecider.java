package hopshackle.simulation;

import java.util.List;

public abstract class QDecider<A extends Agent> extends BaseDecider<A> {
	
	protected boolean monteCarlo;
	protected boolean useLookahead;

	public <S extends State<A>> QDecider(StateFactory<A> stateFactory) {
		super(stateFactory);
		monteCarlo = getProperty("MonteCarloReward", "false").equals("true");
		useLookahead = getProperty("LookaheadQLearning", "false").equals("true");
	}
	
	@Override
	public void injectProperties(DeciderProperties decProp) {
		super.injectProperties(decProp);
		monteCarlo = getProperty("MonteCarloReward", "false").equals("true");
		useLookahead = getProperty("LookaheadQLearning", "false").equals("true");
	}
	
	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent) {
		State<A> state = getCurrentState(decidingAgent);
		double retValue =  valueOption(option, state);
		if (localDebug)
			decidingAgent.log("Option " + option.toString() + " has base Value of " + retValue);
		return retValue;
	}
	
	@Override
	public List<Double> valueOptions(List<ActionEnum<A>> options, A decidingAgent) {
		State<A> state = getCurrentState(decidingAgent);
		List<Double> retValue = this.valueOptions(options, state);
		return retValue;
	}
	
	public abstract double valueOption(ActionEnum<A> option, State<A> state);
	
	public abstract List<Double> valueOptions(List<ActionEnum<A>> options, State<A> state);

	protected <S extends State<A>> double valueOfBestAction(ExperienceRecord<A> exp) {
		if (exp.isInFinalState() || monteCarlo) 
			return 0.0;
		ActionEnum<A> bestAction = getBestActionFrom(exp.getPossibleActionsFromEndState(), exp.getEndState());
		return valueOption(bestAction, exp.getEndState());
	}

	protected <S extends State<A>> ActionEnum<A> getBestActionFrom(List<ActionEnum<A>> possActions, S state) {
		double bestValue = -Double.MAX_VALUE;
		ActionEnum<A> bestAction = null;
		for (ActionEnum<A> option : possActions) {
			double value = valueOption(option, state);
			if (value > bestValue) {
				bestValue = value;
				bestAction = option;
			}
		}
		return bestAction;
	}

}
