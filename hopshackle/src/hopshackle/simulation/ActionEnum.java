package hopshackle.simulation;

import java.io.Serializable;

public interface ActionEnum<A extends Agent> extends Serializable {

	public String getChromosomeDesc();
	
	public Action<A> getAction(A a);
	
	/* 
	 * The isChooseable method returns true if the pre-conditions are met
	 * for the Agent supplied to choose this action.
	 * 
	 * It should always be checked by any Decider.
	 * 
	 */
	public boolean isChooseable(A a);
	
	public Enum<?> getEnum();
}