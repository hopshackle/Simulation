package hopshackle.simulation;

import java.util.List;

public interface Teacher<T extends Agent> {

	public boolean registerDecision(T decider, ExperienceRecord decision);
	
	public List<ExperienceRecord> getExperienceRecords(T decider);
}
