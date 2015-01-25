package hopshackle.simulation;

public interface ActionEnum {

	public String getChromosomeDesc();
	
	public Action getAction(Agent a);
	
	public Action getAction(Agent a1, Agent a2);
	
	/* 
	 * The isChooseable method returns true if the pre-conditions are met
	 * for the Agent supplied to choose this action.
	 * 
	 * It should always be checked by any Decider.
	 * 
	 */
	public boolean isChooseable(Agent a);
	
	@SuppressWarnings("unchecked")
	public Enum getEnum();
}