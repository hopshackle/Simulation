package hopshackle.simulation;

import java.util.*;

public abstract class Teacher<A extends Agent, S extends State<A>> implements AgentListener {
	
	protected List<Decider<A, S>> decidersToTeach = new ArrayList<Decider<A, S>>();
	protected ExperienceRecordCollector<A, S> experienceRecordCollector;

	public void registerToERStream(ExperienceRecordCollector<A, S> erc) {
		experienceRecordCollector = erc;
		experienceRecordCollector.addListener(this);
	}
	public void registerDecider(Decider<A, S> decider) {
		if (!decidersToTeach.contains(decider))
			decidersToTeach.add(decider);
	}
	
	@Override
	public abstract void processEvent(AgentEvent event);

}
