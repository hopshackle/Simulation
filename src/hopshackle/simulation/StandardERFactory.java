package hopshackle.simulation;

import java.util.List;

public class StandardERFactory<A extends Agent> implements ExperienceRecordFactory<A> {

	@Override
	public ExperienceRecord<A> generate(A a, State<A> state, Action<A> action, List<ActionEnum<A>> possibleActions) {
		return new ExperienceRecord<A>(a, state, action, possibleActions);
	}

}
