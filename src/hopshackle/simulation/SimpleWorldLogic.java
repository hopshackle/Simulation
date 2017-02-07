package hopshackle.simulation;

import java.util.*;

public class SimpleWorldLogic<A extends Agent> implements WorldLogic<A> {
	
	protected List<ActionEnum<A>> actionSet = new ArrayList<ActionEnum<A>>();

	public SimpleWorldLogic(List<ActionEnum<A>> actions) {
		if (actions != null) {
			for (ActionEnum<A> ae : actions)
				actionSet.add(ae);
		}
	}
	
	@Override
	public List<ActionEnum<A>> getPossibleActions(A actor) {
		List<ActionEnum<A>> chooseable = new ArrayList<ActionEnum<A>>();
		for (ActionEnum<A> option : actionSet) {
			if (option.isChooseable(actor))
				chooseable.add(option);
		}
		return chooseable;
	}

	public void setActions(List<ActionEnum<A>> actionList) {
		actionSet = new ArrayList<ActionEnum<A>>();
		for (ActionEnum<A> ae : actionList) {
			actionSet.add(ae);
		}
	}
}
