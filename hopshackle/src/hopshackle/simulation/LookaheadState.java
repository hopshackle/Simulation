package hopshackle.simulation;

public interface LookaheadState<A extends Agent> {
	
	public abstract void apply(ActionEnum<A> proposedAction);
	
	public abstract LookaheadState<A> clone();

}
