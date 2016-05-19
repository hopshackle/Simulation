package hopshackle.simulation;

import java.util.ArrayList;

public class StateOffPolicyDecider extends StateDecider {

	private StateGreedyDecider GreedyDecider;

	public StateOffPolicyDecider(ArrayList<ActionEnum> actions,	ArrayList<GeneticVariable> variables) {
		super(actions, variables);
		GreedyDecider = new StateGreedyDecider(actions, variables);
	}

	@Override
	public void setStateType(String stateType) {
		super.setStateType(stateType);
		if (GreedyDecider != null) {
			GreedyDecider.setStateType(stateType);
		}
	}

	@Override 
	public void setBaseValue(double baseValue){
		super.setBaseValue(baseValue);
		GreedyDecider.setBaseValue(baseValue);
	}

	@Override
	public void setPigeonHoles(int pigeonHoles) {
		super.setPigeonHoles(pigeonHoles);
		GreedyDecider.setPigeonHoles(pigeonHoles);
	}

	@Override
	public void learnFrom(ExperienceRecord exp, double maxResult) {
		// Differs from parent in NOT calling updateStateValue if we have acted off policy
		ActionEnum bestAction = GreedyDecider.getBestActionFrom(exp.getPossibleActionsFromStartState(), exp.getStartState());
		if (bestAction.equals(exp.getActionTaken())) {
			super.learnFrom(exp, maxResult);
		} else {
			HopshackleState startState = getState(exp.getStartState());
			HopshackleState endState = getState(exp.getEndState());
			startState.addExperience(exp.getActionTaken(), endState, exp.getReward());
			// but we omit the updateStateValue section
		}
	}
}