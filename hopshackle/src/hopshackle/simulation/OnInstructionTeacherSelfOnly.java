package hopshackle.simulation;

import java.util.*;

/*
 * This Teacher ignores any Deciders registered to it. It pulls all experience records for 
 * each Agent, and then uses these to teach just the Decider used by that Agent 
 * (as determined by Agent.getDecider()
 * It is currently inefficient in that it separates out the full set of experience records each time
 * rather than just the most recently collected batch.
 */
public class OnInstructionTeacherSelfOnly<A extends Agent, S extends State<A>> extends OnInstructionTeacher<A, S> {
	
	public OnInstructionTeacherSelfOnly(int pastGamesToIncludeInTraining) {
		super(pastGamesToIncludeInTraining);
	}

	public void teach() {
		updateData();
		List<ExperienceRecord<A, S>> allER = getLastNDataSets();;
		Map<Decider<A, S>, List<ExperienceRecord<A, S>>> splitData = splitByDecider(allER);

		for (Decider<A, S> decider : splitData.keySet()) {
			List<ExperienceRecord<A, S>> newER = splitData.get(decider);
			decider.learnFromBatch(newER, newER.get(0).getAgent().getMaxScore());
		}
	}

	private Map<Decider<A, S>, List<ExperienceRecord<A, S>>> splitByDecider(List<ExperienceRecord<A, S>> allER) {
		Map<Decider<A, S>, List<ExperienceRecord<A, S>>> retValue = new HashMap<Decider<A, S>, List<ExperienceRecord<A, S>>>();
		for (ExperienceRecord<A, S> er : allER) {
			Decider<A, S> d = er.getAgent().getDecider();
			if (!retValue.containsKey(d)) {
				retValue.put(d, new ArrayList<ExperienceRecord<A, S>>());
			}
			retValue.get(d).add(er);
		}
		return retValue;
	}

}