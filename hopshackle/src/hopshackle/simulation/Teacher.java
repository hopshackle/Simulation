package hopshackle.simulation;

import java.util.List;

public interface Teacher<T extends Agent> {

	public boolean registerDecision(T decider, ExperienceRecord<T> decision);
	
	public List<ExperienceRecord<T>> getExperienceRecords(T decider);
}
