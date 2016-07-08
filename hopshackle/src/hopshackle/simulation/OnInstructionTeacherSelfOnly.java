package hopshackle.simulation;

import java.util.List;

/*
 * This Teacher ignores any Deciders registered to it. It pulls all experience records for 
 * each Agent, and then uses these to teach just the Decider used by that Agent 
 * (as determined by Agent.getDecider()
 */
public class OnInstructionTeacherSelfOnly<A extends Agent> extends OnInstructionTeacher<A> {

	public void teach() {
		List<A> allAgents = experienceRecordCollector.getAllAgentsWithER();
		for (A agent : allAgents) {
			List<ExperienceRecord<A>> newER = experienceRecordCollector.getExperienceRecords(agent);
			Decider<A> d = agent.getDecider();
			d.learnFromBatch(newER, agent.getMaxScore());
		}
		experienceRecordCollector.clearAllExperienceRecord();
	}

}