package hopshackle.simulation;

import java.util.*;

public class OnInstructionTeacher<A extends Agent, S extends State<A>> extends Teacher<A, S> {
	
	protected List<List<ExperienceRecord<A, S>>> pastData = new ArrayList<List<ExperienceRecord<A, S>>>();
	protected int pastDataLimit;
	
	public OnInstructionTeacher() {
		this(0);
	}
	public OnInstructionTeacher(int pastDataToKeep) {
		pastDataLimit = pastDataToKeep;
	}
	
	@Override
	public void processEvent(AgentEvent event) {
		// do nothing
		// we wait for explicit instructions
	}

	protected void updateData() {
		List<ExperienceRecord<A, S>> newER = experienceRecordCollector.getAllExperienceRecords();
		pastData.add(newER);
		if (pastData.size() > pastDataLimit+1) {
			pastData.remove(0);
		}
		experienceRecordCollector.clearAllExperienceRecord();
	}

	public void teach() {
		updateData();
		List<ExperienceRecord<A, S>> allDataForTraining = getLastNDataSets();
		Agent a = allDataForTraining.get(0).getAgent();
		for (Decider<A, S> d : decidersToTeach) {
			d.learnFromBatch(allDataForTraining, a.getMaxScore());
		}
	}
	protected List<ExperienceRecord<A, S>> getLastNDataSets() {
		List<ExperienceRecord<A, S>> retValue = pastData.get(0);
		for (int i = 1; i <= pastDataLimit && i < pastData.size(); i++) {
			retValue.addAll(pastData.get(i));
		}
		return retValue;
	}

}
