package hopshackle.simulation;

import hopshackle.simulation.AgentEvent.Type;

import java.util.*;

public class RealTimeTeacher<A extends Agent> implements AgentListener {

	private List<Decider<A>> decidersToTeach = new ArrayList<Decider<A>>();
	private ExperienceRecordCollector<A> experienceRecordCollector;

	public void registerToERStream(ExperienceRecordCollector<A> erc) {
		experienceRecordCollector = erc;
		experienceRecordCollector.addListener(this);
	}
	public void registerDecider(Decider<A> decider) {
		if (!decidersToTeach.contains(decider))
			decidersToTeach.add(decider);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public synchronized void processEvent(AgentEvent event) {
		// The only events that get passed through from ExperienceRecordCollector are the ones that result
		// in a completed ER. So for RealTime learning we do not need to differentiate between the event types
		A a = (A) event.getAgent();
		List<ExperienceRecord<A>> newER = experienceRecordCollector.getCompleteExperienceRecords(a);
		for (ExperienceRecord<A> er : newER) {
			for (Decider<A> d : decidersToTeach) {
				d.learnFrom(er, a.getMaxScore());
			}
			experienceRecordCollector.removeER(a, er);
		}
		if (event.getEvent() == Type.DEATH) {
			experienceRecordCollector.removeAgent(a);
		}
	} 
}

