package hopshackle.simulation;

import java.util.*;

public class OnInstructionTeacher<A extends Agent> extends Teacher<A> {
	
	protected List<List<ExperienceRecord<A>>> pastData = new ArrayList<List<ExperienceRecord<A>>>();
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
		List<ExperienceRecord<A>> newER = experienceRecordCollector.getAllExperienceRecords();
		pastData.add(newER);
		if (pastData.size() > pastDataLimit+1) {
			pastData.remove(0);
		}
		experienceRecordCollector.clearAllExperienceRecord();
	}

	public void teach() {
		updateData();
		List<ExperienceRecord<A>> allDataForTraining = getLastNDataSets();
		Agent a = allDataForTraining.get(0).getAgent();
		for (Decider<A> d : decidersToTeach) {
	//		System.out.println(this.toString() + " teaching " + d.toString() + " with " + allDataForTraining.size() + " records.");
			d.learnFromBatch(allDataForTraining, a.getMaxScore());
		}
	}
	protected List<ExperienceRecord<A>> getLastNDataSets() {
		List<ExperienceRecord<A>> retValue = new ArrayList<ExperienceRecord<A>>();
		for (int i = 0; i <= pastDataLimit && i < pastData.size(); i++) {
			retValue.addAll(pastData.get(i));
		}
		return retValue;
	}

}
