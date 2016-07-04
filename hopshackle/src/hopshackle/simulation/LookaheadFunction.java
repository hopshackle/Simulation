package hopshackle.simulation;

public interface LookaheadFunction<A extends Agent, B extends LookaheadState<A>> {

	public B apply(B currentState, ActionEnum<A> option);
	
	public B getCurrentState(A agent);
	
}
