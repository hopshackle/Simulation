package hopshackle.simulation;

public abstract class StateOffPolicyDecider<A extends Agent> extends StateDecider<A> {

	private StateGreedyDecider<A> GreedyDecider;

	public StateOffPolicyDecider(StateFactory<A> stateFactory) {
		super(stateFactory);
		GreedyDecider = new StateGreedyDecider<A>(stateFactory);
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
		ActionEnum<A> bestAction = GreedyDecider.getBestActionFrom(exp.getPossibleActionsFromStartState(), exp.getStartState(useLookahead));
		if (bestAction.equals(exp.getActionTaken())) {
			super.learnFrom(exp, maxResult);
		} else {
			int actingAgentNumber = exp.getAgentNumber();
			double reward = exp.getReward()[actingAgentNumber-1];
			if (monteCarlo)
				reward = exp.getMonteCarloReward()[actingAgentNumber-1];
			HopshackleState startState = getState(exp.getStartState(useLookahead));
			HopshackleState endState = getState(exp.getEndState());
			startState.addExperience(exp.getActionTaken().getType(), endState, reward);
			// but we omit the updateStateValue section
		}
	}
}