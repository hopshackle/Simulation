package hopshackle.simulation;

import java.util.*;

public class OnInstructionTeacher<A extends Agent> extends Teacher<A> {
	
	private List<List<ExperienceRecord<A>>> pastData = new ArrayList<List<ExperienceRecord<A>>>();
	private int pastDataLimit;
	
	public OnInstructionTeacher() {
		this(0);
	}
	public OnInstructionTeacher(int pastDataToKeep) {
		pastDataLimit = pastDataToKeep;
		for (int i = 0; i <=pastDataLimit; i++) {
			pastData.add(null);
		}
	}
	
	@Override
	public void processEvent(AgentEvent event) {
		// do nothing
		// we wait for explicit instructions
	}

	public void teach() {
		List<ExperienceRecord<A>> newER = experienceRecordCollector.getAllExperienceRecords();
			pastData.add(newER);
			if (pastData.size() > pastDataLimit+1) {
				pastData.remove(0);
			}
		List<ExperienceRecord<A>> allDataForTraining = getLastNDataSets();
		Agent a = newER.get(0).getAgent();
		for (Decider<A> d : decidersToTeach) {
			d.learnFromBatch(allDataForTraining, a.getMaxScore());
		}
		experienceRecordCollector.clearAllExperienceRecord();
	}
	private List<ExperienceRecord<A>> getLastNDataSets() {
		List<ExperienceRecord<A>> retValue = pastData.get(0);
		for (int i = 1; i <= pastDataLimit; i++) {
			if (pastData.get(i) == null) break;
			retValue.addAll(pastData.get(i));
		}
		return retValue;
	}

}
