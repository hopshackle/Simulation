package hopshackle.simulation.basic;

import java.util.*;

import hopshackle.simulation.*;

public class BasicAction extends Action<BasicAgent> {

	public BasicAction(ActionEnum<BasicAgent> type, BasicAgent a, long duration, boolean recordAction) {
		this(type, a, 0l, duration, recordAction);
	}

	public BasicAction(ActionEnum<BasicAgent> type, BasicAgent a, long start, long duration, boolean recordAction) {
		this(type, HopshackleUtilities.listFromInstance(a), new ArrayList<BasicAgent>(), start, duration, recordAction);
	}

	public BasicAction(ActionEnum<BasicAgent> type, List<BasicAgent> mandatory, List<BasicAgent> optional, long startOffset, 
			long duration, boolean recordAction) {
		super(type, mandatory, includeSpouseAsAgent(optional, mandatory), startOffset, duration, recordAction);
	}
	
	private static List<BasicAgent> includeSpouseAsAgent(List<BasicAgent> startingList, List<BasicAgent> mandatoryParticipants) {
		List<BasicAgent> retValue = HopshackleUtilities.cloneList(startingList);
		List<BasicAgent> allParticipants = HopshackleUtilities.cloneList(startingList);
		allParticipants.addAll(mandatoryParticipants);
		for (BasicAgent participant : allParticipants) {
			if (participant.isMale() && participant.isMarried() && !allParticipants.contains(participant.getPartner())){
				retValue.add(participant.getPartner());
			}
		}
		return retValue;
	}
	
	@Override
	protected void doNextDecision(BasicAgent agent) {
		if (agent.isMarried() && agent.isFemale()) {
			// Do nothing
		} else {
			super.doNextDecision(actor);
		}
	}

}
