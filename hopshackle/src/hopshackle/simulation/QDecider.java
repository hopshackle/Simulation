package hopshackle.simulation;

import java.util.List;

public abstract class QDecider extends BaseDecider {

	public QDecider(List<? extends ActionEnum> actions,	List<GeneticVariable> variables) {
		super(actions, variables);
	}

	public abstract double valueOption(ActionEnum option, double[] state);
	
	protected double valueOfBestAction(ExperienceRecord exp) {
		if (exp.isInFinalState()) 
			return 0.0;
		ActionEnum bestAction = getBestActionFrom(exp.getPossibleActionsFromEndState(), exp.getEndState());
		return valueOption(bestAction, exp.getEndState());
	}
		
	protected ActionEnum getBestActionFrom(List<ActionEnum> possActions, double[] state) {
		double bestValue = -Double.MAX_VALUE;
		ActionEnum bestAction = null;
		for (ActionEnum option : possActions) {
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
