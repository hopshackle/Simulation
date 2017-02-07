package hopshackle.simulation;

import java.util.List;

public abstract class QDecider<A extends Agent> extends BaseDecider<A> {
	
	private boolean monteCarlo;

	public <S extends State<A>> QDecider(StateFactory<A> stateFactory) {
		super(stateFactory);
		monteCarlo = SimProperties.getProperty("MonteCarloReward", "false").equals("true");
	}

	public abstract double valueOption(ActionEnum<A> option, State<A> state);

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
