package hopshackle.simulation;

import java.io.Serializable;

public interface ActionEnum<A extends Agent> extends Serializable {

	public Action<A> getAction(A a);
	
	/* 
	 * The isChooseable method returns true if the pre-conditions are met
	 * for the Agent supplied to choose this action.
	 * 
	 * It should always be checked by any Decider.
	 * 
	 */
	default boolean isChooseable(A a) {return false;}
	
	default Enum<?> getEnum() {return null;}

}