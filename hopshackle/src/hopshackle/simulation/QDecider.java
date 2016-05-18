package hopshackle.simulation;

import java.util.List;

public abstract class QDecider<A extends Agent> extends BaseDecider<A> {

	public QDecider(List<? extends ActionEnum<A>> actions,	List<GeneticVariable> variables) {
		super(actions, variables);
	}

	public abstract double valueOption(ActionEnum<A> option, double[] state);
	
	protected double valueOfBestAction(ExperienceRecord<A> exp) {
		if (exp.isInFinalState()) 
			return 0.0;
		ActionEnum<A> bestAction = getBestActionFrom(exp.getPossibleActionsFromEndState(), exp.getEndState());
		return valueOption(bestAction, exp.getEndState());
	}
		
	protected ActionEnum<A> getBestActionFrom(List<ActionEnum<A>> possActions, double[] state) {
		double bestValue = -Double.MAX_VALUE;
		ActionEnum<A> bestAction = null;
		for (ActionEnum<A> option : possActions) {
			double value = valueOption(option, state);
			if (value > bestValue) {
				bestValue = value;
				bestAction = option;
			}
		}
		if (bestAction == null)
			throw new AssertionError("Null best action should not be possible.");
		return bestAction;
	}

}
