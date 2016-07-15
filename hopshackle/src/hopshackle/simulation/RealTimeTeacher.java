package hopshackle.simulation;

import hopshackle.simulation.AgentEvent.Type;

import java.util.*;

public class RealTimeTeacher<A extends Agent, S extends State<A>> extends Teacher<A, S> implements AgentListener {

	@SuppressWarnings("unchecked")
	@Override
	public synchronized void processEvent(AgentEvent event) {
		// The only events that get passed through from ExperienceRecordCollector are the ones that result
		// in a completed ER. So for RealTime learning we do not need to differentiate between the event types
		A a = (A) event.getAgent();
		List<ExperienceRecord<A, S>> newER = experienceRecordCollector.getCompleteExperienceRecords(a);
		for (ExperienceRecord<A, S> er : newER) {
			for (Decider<A, S> d : decidersToTeach) {
				d.learnFrom(er, a.getMaxScore());
			}
			experienceRecordCollector.removeER(a, er);
		}
		if (event.getEvent() == Type.DEATH) {
			experienceRecordCollector.removeAgent(a);
		}
	} 
}

