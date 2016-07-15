package hopshackle.simulation;

import java.util.ArrayList;

public abstract class StateOffPolicyDecider<A extends Agent, S extends State<A>> extends StateDecider<A, S> {

	private StateGreedyDecider<A, S> GreedyDecider;

	public StateOffPolicyDecider(ArrayList<ActionEnum<A>> actions,	ArrayList<GeneticVariable<A, S>> variables) {
		super(actions, variables);
		GreedyDecider = new StateGreedyDecider<A, S>(actions, variables);
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
			HopshackleState startState = getStateAsArray(exp.getStartState());
			HopshackleState endState = getStateAsArray(exp.getEndState());
			startState.addExperience(exp.getActionTaken().actionType, endState, exp.getReward());
			// but we omit the updateStateValue section
		}
	}
}