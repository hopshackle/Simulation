package hopshackle.simulation;

import java.util.List;

public class StandardERFactory<A extends Agent> implements ExperienceRecordFactory<A> {
	
	private DeciderProperties properties;
	
	public StandardERFactory(DeciderProperties properties) {
		this.properties = properties;
	}

	@Override
	public ExperienceRecord<A> generate(A a, State<A> state, Action<A> action, List<ActionEnum<A>> possibleActions) {
		return new ExperienceRecord<A>(a, state, action, possibleActions, properties);
	}
}
