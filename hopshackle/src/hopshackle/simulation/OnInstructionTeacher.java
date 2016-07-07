package hopshackle.simulation;

import java.util.List;

public class OnInstructionTeacher<A extends Agent> extends Teacher<A> {

	@Override
	public void processEvent(AgentEvent event) {
		// do nothing
		// we wait for explicit instructions
	}

	public void teach() {
		List<ExperienceRecord<A>> newER = experienceRecordCollector.getAllExperienceRecords();
//		System.out.println("Teaching after game with " + newER.size() + " records, and " + decidersToTeach.size() + " deciders");
		Agent a = newER.get(0).getAgent();
		for (Decider<A> d : decidersToTeach) {
			d.learnFromBatch(newER, a.getMaxScore());
		}
		experienceRecordCollector.clearAllExperienceRecord();
	}

}
