package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.Character;

import java.awt.AWTEvent;
import java.awt.event.AWTEventListener;
import java.util.*;

public class CareerTeacher implements Teacher<Character>, AWTEventListener {

	private HashMap<Agent, ExperienceRecord> tdHash = new HashMap<Agent, ExperienceRecord>();

	@Override
	public List<ExperienceRecord> getExperienceRecords(Character decider) {
		List<ExperienceRecord> retValue = new ArrayList<ExperienceRecord>();
		retValue.add(tdHash.get(decider));
		return retValue;
	}

	@Override
	public boolean registerDecision(Character decider, ExperienceRecord decision) {
		if (!tdHash.containsKey(decider)) {
			tdHash.put(decider, decision);
			decider.addListener(this);
		}
		return true;
	}

	@Override
	public void eventDispatched(AWTEvent event) {
		if (event instanceof AgentEvent) {
			AgentEvent ae = (AgentEvent) event;
			Character a = (Character) ae.getAgent();
			switch (ae.getEvent()) {
			case DEATH:
				double xp = a.getXp();
				if (xp < 0) xp = 0.0;
				ExperienceRecord td = tdHash.get(a);
				if (td != null) {
					Decider careerDecider = a.getCareerDecider();
					td.updateWithResults(Math.sqrt(xp), careerDecider.getCurrentState(a, a), new ArrayList<ActionEnum>(), true);
					careerDecider.learnFrom(td, Math.sqrt(10000));
				}
				tdHash.remove(a);
			default:
				break;
			}
		}

	}

}
