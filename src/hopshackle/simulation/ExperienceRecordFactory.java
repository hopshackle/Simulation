package hopshackle.simulation;

import java.util.List;

public interface ExperienceRecordFactory<A extends Agent>  {
	
	public ExperienceRecord<A> generate(A a, State<A> state, Action<A> action, List<ActionEnum<A>> possibleActions);
	
}

