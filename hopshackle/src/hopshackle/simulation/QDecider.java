package hopshackle.simulation;

import java.util.List;

public abstract class QDecider<A extends Agent, S extends State<A>> extends BaseDecider<A, S> {

	public QDecider(List<? extends ActionEnum<A>> actions,	List<GeneticVariable<A, S>> variables) {
		super(actions, variables);
	}

	public abstract double valueOption(ActionEnum<A> option, S state);
	
	protected double valueOfBestAction(ExperienceRecord<A, S> exp) {
		if (exp.isInFinalState()) 
			return 0.0;
		ActionEnum<A> bestAction = getBestActionFrom(exp.getPossibleActionsFromEndState(), exp.getEndState());
		return valueOption(bestAction, exp.getEndState());
	}
		
	protected ActionEnum<A> getBestActionFrom(List<ActionEnum<A>> possActions, S state) {
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
