package hopshackle.simulation;

public interface LookaheadFunction<A extends Agent> {

	public LookaheadState<A> apply(LookaheadState<A> currentState, ActionEnum<A> option);
	
	public LookaheadState<A> getCurrentState(A agent);
	
}
