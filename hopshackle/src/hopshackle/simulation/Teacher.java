package hopshackle.simulation;

import java.util.List;

public interface Teacher<T extends Agent> {

	public List<ExperienceRecord<T>> getExperienceRecords(T decider);
}
