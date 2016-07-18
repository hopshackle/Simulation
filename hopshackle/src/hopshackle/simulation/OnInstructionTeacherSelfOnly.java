package hopshackle.simulation;

import java.util.*;

/*
 * This Teacher ignores any Deciders registered to it. It pulls all experience records for 
 * each Agent, and then uses these to teach just the Decider used by that Agent 
 * (as determined by Agent.getDecider()
 * It is currently inefficient in that it separates out the full set of experience records each time
 * rather than just the most recently collected batch.
 */
public class OnInstructionTeacherSelfOnly<A extends Agent> extends OnInstructionTeacher<A> {
	
	public OnInstructionTeacherSelfOnly(int pastGamesToIncludeInTraining) {
		super(pastGamesToIncludeInTraining);
	}

	public void teach() {
		updateData();
		List<ExperienceRecord<A>> allER = getLastNDataSets();;
		Map<Decider<A>, List<ExperienceRecord<A>>> splitData = splitByDecider(allER);

		for (Decider<A> decider : splitData.keySet()) {
			List<ExperienceRecord<A>> newER = splitData.get(decider);
			decider.learnFromBatch(newER, newER.get(0).getAgent().getMaxScore());
		}
	}

	private Map<Decider<A>, List<ExperienceRecord<A>>> splitByDecider(List<ExperienceRecord<A>> allER) {
		Map<Decider<A>, List<ExperienceRecord<A>>> retValue = new HashMap<Decider<A>, List<ExperienceRecord<A>>>();
		for (ExperienceRecord<A> er : allER) {
			Decider<A> d = er.getAgent().getDecider();
			if (!retValue.containsKey(d)) {
				retValue.put(d, new ArrayList<ExperienceRecord<A>>());
			}
			retValue.get(d).add(er);
		}
		return retValue;
	}

}