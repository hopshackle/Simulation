package hopshackle.simulation;

import java.util.List;

public interface WorldLogic<A extends Agent> {

	public List<ActionEnum<A>> getPossibleActions(A actor);

}
