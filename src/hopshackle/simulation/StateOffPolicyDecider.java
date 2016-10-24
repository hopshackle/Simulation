package hopshackle.simulation;

import java.util.ArrayList;

public abstract class StateOffPolicyDecider<A extends Agent> extends StateDecider<A> {

	private StateGreedyDecider<A> GreedyDecider;

	public StateOffPolicyDecider(StateFactory<A> stateFactory, ArrayList<ActionEnum<A>> actions) {
		super(stateFactory, actions);
		GreedyDecider = new StateGreedyDecider<A>(stateFactory, actions);
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
	public void learnFrom(ExperienceRecord<A> exp, double maxResult) {
		// Differs from parent in NOT calling updateStateValue if we have acted off policy
		ActionEnum<A> bestAction = GreedyDecider.getBestActionFrom(exp.getPossibleActionsFromStartState(), exp.getStartState());
		if (bestAction.equals(exp.getActionTaken())) {
			super.learnFrom(exp, maxResult);
		} else {
			HopshackleState startState = getState(exp.getStartState());
			HopshackleState endState = getState(exp.getEndState());
			startState.addExperience(exp.getActionTaken().actionType, endState, exp.getReward());
			// but we omit the updateStateValue section
		}
	}
}