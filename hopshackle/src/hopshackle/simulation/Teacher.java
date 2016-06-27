package hopshackle.simulation;

import java.util.*;

public abstract class Teacher<A extends Agent> implements AgentListener {
	
	protected List<Decider<A>> decidersToTeach = new ArrayList<Decider<A>>();
	protected ExperienceRecordCollector<A> experienceRecordCollector;

	public void registerToERStream(ExperienceRecordCollector<A> erc) {
		experienceRecordCollector = erc;
		experienceRecordCollector.addListener(this);
	}
	public void registerDecider(Decider<A> decider) {
		if (!decidersToTeach.contains(decider))
			decidersToTeach.add(decider);
	}
	
	@Override
	public abstract void processEvent(AgentEvent event);

}
