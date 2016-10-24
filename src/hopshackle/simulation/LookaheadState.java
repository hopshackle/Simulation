package hopshackle.simulation;

public interface LookaheadState<A extends Agent> extends State<A> {
	
	public abstract LookaheadState<A> apply(ActionEnum<A> proposedAction);
	
	public abstract LookaheadState<A> clone();

}
