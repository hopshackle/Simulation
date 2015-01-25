package hopshackle.simulation;

public interface StateTransitionFunction {
	
	public State getSuccessorState(State startState, StateAction actionTaken);

}
