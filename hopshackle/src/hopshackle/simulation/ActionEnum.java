package hopshackle.simulation;

public interface ActionEnum<A extends Agent> {

	public String getChromosomeDesc();
	
	public Action<A> getAction(A a);
	
	public <B extends Agent> Action<A> getAction(A a1, B a2);
	
	/* 
	 * The isChooseable method returns true if the pre-conditions are met
	 * for the Agent supplied to choose this action.
	 * 
	 * It should always be checked by any Decider.
	 * 
	 */
	public boolean isChooseable(A a);
	
	public boolean isDummy();
	
	public Enum getEnum();
}