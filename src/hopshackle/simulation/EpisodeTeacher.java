package hopshackle.simulation;

import hopshackle.simulation.AgentEvent.Type;

import java.util.List;

public class EpisodeTeacher<A extends Agent> extends Teacher<A> {

	@Override
	public void processEvent(AgentEvent event) {
		// The only events that get passed through from ExperienceRecordCollector are the ones that result
		// in a completed ER. So for RealTime learning we do not need to differentiate between the event types
		A a = (A) event.getAgent();
		if (event.getEvent() == Type.DEATH) {
			List<ExperienceRecord<A>> newER = experienceRecordCollector.getCompleteExperienceRecords(a);
			for (Decider<A> d : decidersToTeach) {
				d.learnFromBatch(newER, a.getMaxScore());
			}
			experienceRecordCollector.removeAgent(a);
		}
	}
}
