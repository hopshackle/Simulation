package hopshackle.simulation;

import java.util.*;

public interface State {
	
	public List<? extends StateAction> getValidActions();
	
	public State applyAction(StateAction action);

}
