package hopshackle.simulation;

import java.awt.AWTEvent;
import java.awt.event.AWTEventListener;
import java.util.*;

public class AgentTeacher implements Teacher<Agent>, AWTEventListener {

	private HashMap<Agent, List<ExperienceRecord>> tdArrayHash = new HashMap<Agent, List<ExperienceRecord>>();
	private HashMap<Agent, Double> lastScoresOfAgents = new HashMap<Agent, Double>();

	@Override
	public synchronized boolean registerDecision(Agent a, ExperienceRecord td) {
		if (!agentAlreadySeen(a)) {
			addAgentToList(a);
			listenToAgent(a);
		}
		addDecisionToHash(a, td);
		return true;
	}

	private boolean agentAlreadySeen(Agent a) {
		if (tdArrayHash.containsKey(a))
			return true;
		return false;
	}

	private void addAgentToList(Agent a) {
		List<ExperienceRecord> tdList = new ArrayList<ExperienceRecord>();
		tdArrayHash.put(a, tdList);
		lastScoresOfAgents.put(a, a.getScore());
	}

	private void addDecisionToHash(Agent a, ExperienceRecord td) {
		List<ExperienceRecord> tdListForAgent = tdArrayHash.get(a);
		tdListForAgent.add(td);
	}

	private void listenToAgent(Agent a) {
		a.addListener(this);
	}

	@Override
	public List<ExperienceRecord> getExperienceRecords(Agent a) {
		if (agentAlreadySeen(a)) {
			List<ExperienceRecord> tdArray = tdArrayHash.get(a);
			return HopshackleUtilities.cloneList(tdArray);
		}
		return new ArrayList<ExperienceRecord>();
	}

	@Override
	public synchronized void eventDispatched(AWTEvent event) {
		if (event instanceof AgentEvent) {
			Agent a = ((AgentEvent)event).getAgent();
			switch (((AgentEvent) event).getEvent()) {
			case DEATH:
				processDecisionsForAgent(a);
				stopListeningToAgent(a);
				break;
			case DECISION_STEP_COMPLETE:
				processDecisionsForAgent(a);
				break;
			}
		}
	} 

	private void processDecisionsForAgent(Agent a) {
		List<ExperienceRecord> tdArray = tdArrayHash.get(a);
		double reward = a.getScore() - lastScoresOfAgents.get(a);
		Decider agentDecider = a.getDecider();
		if (agentDecider != null) {
			for (ExperienceRecord td : tdArray) {
				Decider d = a.getDecider();
				double[] newState = d.getCurrentState(a, a);
				List<ActionEnum> possibleActions = a.getDecider().getChooseableOptions(a, a);
				td.updateWithResults(reward, newState, possibleActions, a.isDead());
				agentDecider.learnFrom(td, a.getMaxScore());
			}
		}
		lastScoresOfAgents.put(a, a.getScore());
		tdArrayHash.put(a, new ArrayList<ExperienceRecord>());
	}

	private void stopListeningToAgent(Agent a) {
		tdArrayHash.remove(a);
		lastScoresOfAgents.remove(a);
	}
}

